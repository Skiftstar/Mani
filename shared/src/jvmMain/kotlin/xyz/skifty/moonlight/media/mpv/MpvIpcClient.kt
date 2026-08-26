package xyz.skifty.moonlight.media.mpv

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.ByteChannel
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

private const val COMMAND_TIMEOUT_MS = 3_000L

// mpv's JSON IPC messages carry fields MpvIpcMessage doesn't model (e.g. end-file's
// playlist_entry_id) - without this, decoding any such message throws and the whole message
// (including fields callers do care about, like end-file's `reason`) is dropped.
private val json = Json {
    ignoreUnknownKeys = true
}

/**
 * Spawns mpv as a subprocess and drives it over its JSON IPC socket
 * (https://mpv.io/manual/stable/#json-ipc) - one line of JSON per command/reply/event.
 *
 * [sendCommand] blocks until mpv's matching reply arrives (or times out), and property/event
 * callbacks registered via [observeProperty]/[onEvent] run synchronously on the IPC reader thread
 * rather than being hopped elsewhere - both by design. Callers in this codebase (MprisService's
 * D-Bus handlers in particular) rely on state mutations completing before the calling method
 * returns, exactly like DesktopAudioPlayer's previous vlcj-backed implementation did (see
 * MprisService's threading comment) - dbus-java withholds its reply until the handler returns,
 * and clients commonly re-query state immediately after receiving it.
 *
 * Construction throws if mpv can't be spawned or its IPC socket can't be reached, which callers
 * are expected to surface as a startup error - mpv is a hard requirement for playback, unlike the
 * optional MPRIS/secure-storage integrations elsewhere in this codebase that tolerate being
 * unavailable.
 */
class MpvIpcClient(
    private val transport: MpvIpcTransport = MpvIpcTransportFactory.create(),
) {

    private val process: Process
    private val channel: ByteChannel
    private val writeLock = Any()

    private val nextRequestId = AtomicInteger(1)
    private val nextObserverId = AtomicInteger(1)
    private val pendingReplies = ConcurrentHashMap<Int, CompletableFuture<MpvIpcMessage>>()
    private val propertyObservers = ConcurrentHashMap<Int, (JsonElement) -> Unit>()
    private val eventListeners = ConcurrentHashMap<String, MutableList<(MpvIpcMessage) -> Unit>>()

    @Volatile
    private var closed = false

    private val readerThread: Thread

    init {
        val socketPath = transport.createEndpointPath()

        process = try {
            ProcessBuilder(
                resolveMpvExecutable(), "--idle=yes", "--no-video", "--input-ipc-server=$socketPath",
            )
                // mpv logs to stdout/stderr regardless of --no-terminal - discard rather than pipe,
                // so it can't ever block on a full pipe buffer we're not draining, and doesn't spam
                // Moonlight's own console the way libVLC's did.
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
        } catch (e: IOException) {
            throw IllegalStateException(
                "Moonlight needs the 'mpv' command for audio playback, but it wasn't found on " +
                    "PATH. Install it via your package manager, e.g.:\n" +
                    mpvInstallInstructions() +
                    "\nthen restart Moonlight.",
                e,
            )
        }

        channel = transport.connect(socketPath)

        readerThread = Thread(::readLoop, "mpv-ipc-reader")
            .apply {
                isDaemon = true
                start()
            }

        Runtime.getRuntime().addShutdownHook(
            Thread {
                // Belt-and-braces: an abrupt JVM exit (killed rather than closed through the app
                // window) skips Compose's DisposableEffect lifecycle, which is where close() is
                // normally called from - without this, that path would orphan a running mpv
                // process indefinitely.
                runCatching { close() }
            },
        )
    }

    /** Sends an mpv IPC command (e.g. `sendCommand("loadfile", url, "replace")`) and blocks until
     *  mpv's reply arrives, returning its `data` field (often null for commands with no result). */
    fun sendCommand(vararg args: Any): JsonElement? {
        val requestId = nextRequestId.getAndIncrement()
        val future = CompletableFuture<MpvIpcMessage>()
        pendingReplies[requestId] = future

        val request = MpvCommandRequest(
            command = args.map { arg -> arg.toJsonElement() },
            requestId = requestId,
        )
        writeLine(json.encodeToString(MpvCommandRequest.serializer(), request))

        val reply = try {
            future.get(COMMAND_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            pendingReplies.remove(requestId)
            throw IllegalStateException("mpv command ${args.toList()} timed out or failed", e)
        }

        if (reply.error != null && reply.error != "success") {
            throw IllegalStateException("mpv command ${args.toList()} failed: ${reply.error}")
        }
        return reply.data
    }

    /** Registers [onChange] to be called (on the IPC reader thread - see the class doc) whenever
     *  mpv's [name] property changes. */
    fun observeProperty(
        name: String,
        onChange: (JsonElement) -> Unit,
    ) {
        val observerId = nextObserverId.getAndIncrement()
        propertyObservers[observerId] = onChange
        sendCommand("observe_property", observerId, name)
    }

    /** Registers [callback] to be called (on the IPC reader thread) whenever mpv emits an event
     *  named [name] that isn't a property change (e.g. "file-loaded", "end-file"). */
    fun onEvent(
        name: String,
        callback: (MpvIpcMessage) -> Unit,
    ) {
        eventListeners.getOrPut(name) { mutableListOf() }
            .add(callback)
    }

    fun close() {
        if (closed) {
            return
        }
        closed = true
        runCatching { sendCommand("quit") }
        runCatching { channel.close() }
        runCatching {
            if (!process.waitFor(1, TimeUnit.SECONDS)) {
                process.destroyForcibly()
            }
        }
        readerThread.interrupt()
    }

    // Writes and reads talk to the channel directly via ByteBuffer, rather than through
    // Channels.newInputStream()/newOutputStream(): those convenience wrappers, for a
    // SelectableChannel like SocketChannel, serialize through one shared blocking-mode
    // coordination lock covering *both* directions - so a blocking read (which sits parked until
    // mpv sends something) holds that lock for its entire duration and starves any concurrent
    // write, deadlocking a request/reply protocol like this one against its own read loop.
    // SocketChannel's own read(ByteBuffer)/write(ByteBuffer) have independent locks and are
    // exactly what NIO channels are meant to support genuinely concurrent duplex I/O with.
    private fun writeLine(json: String) {
        val buffer = ByteBuffer.wrap((json + "\n").toByteArray(StandardCharsets.UTF_8))
        synchronized(writeLock) {
            while (buffer.hasRemaining()) {
                channel.write(buffer)
            }
        }
    }

    private fun readLoop() {
        val readBuffer = ByteBuffer.allocate(8192)
        val lineBuffer = ByteArrayOutputStream()
        try {
            while (!closed) {
                readBuffer.clear()
                val bytesRead = channel.read(readBuffer)
                if (bytesRead == -1) {
                    break
                }
                if (bytesRead == 0) {
                    continue
                }
                readBuffer.flip()
                while (readBuffer.hasRemaining()) {
                    val byte = readBuffer.get()
                    if (byte == '\n'.code.toByte()) {
                        val line = lineBuffer.toString(StandardCharsets.UTF_8)
                        lineBuffer.reset()
                        if (line.isNotBlank()) {
                            handleLine(line)
                        }
                    } else {
                        lineBuffer.write(byte.toInt())
                    }
                }
            }
        } catch (e: IOException) {
            if (!closed) {
                System.err.println("mpv IPC connection lost: ${e.message}")
            }
        }
    }

    private fun handleLine(line: String) {
        val message = try {
            json.decodeFromString(MpvIpcMessage.serializer(), line)
        } catch (e: Exception) {
            System.err.println("Could not parse mpv IPC message, ignoring: $line (${e.message})")
            return
        }

        val requestId = message.requestId
        if (requestId != null) {
            pendingReplies.remove(requestId)
                ?.complete(message)
            return
        }

        val eventName = message.event
            ?: return
        if (eventName == "property-change") {
            val observerId = message.id
                ?: return
            val data = message.data
                ?: return
            propertyObservers[observerId]?.invoke(data)
            return
        }

        eventListeners[eventName]?.forEach { listener -> listener(message) }
    }

}

/** The `mpv` executable to spawn - a bundled copy under this packaged app's own resources
 *  directory if one's there (Windows only for now - see `desktopApp/build.gradle.kts`'s
 *  `appResourcesRootDir`, since Windows has no system package manager to depend on mpv through
 *  the way the Linux `.deb` does), otherwise a bare PATH lookup. `compose.application.resources.dir`
 *  is only set at all inside a packaged app - never during a plain `./gradlew :desktopApp:run` -
 *  so local dev runs always fall through to PATH, same as before this existed. */
private fun resolveMpvExecutable(): String {
    val exeName = if (System.getProperty("os.name").lowercase().contains("win")) "mpv.exe" else "mpv"
    val resourcesDir = System.getProperty("compose.application.resources.dir")
        ?: return exeName
    val bundled = File(resourcesDir, exeName)
    return if (bundled.exists()) bundled.absolutePath else exeName
}

private fun mpvInstallInstructions(): String {
    val os = System.getProperty("os.name")
        .lowercase()
    return when {
        os.contains("win") ->
            "  Scoop:  scoop bucket add extras; scoop install mpv\n" +
                "  winget: winget install --id shinchiro.mpv\n" +
                "  or download a build from https://mpv.io/installation/ and add it to PATH"

        os.contains("nux") || os.contains("nix") ->
            "  Debian/Ubuntu: sudo apt install mpv\n" +
                "  Fedora:        sudo dnf install mpv\n" +
                "  Arch:          sudo pacman -S mpv"

        else ->
            "  see https://mpv.io/installation/ for install instructions"
    }
}

private fun Any.toJsonElement(): JsonElement = when (this) {
    is String -> JsonPrimitive(this)
    is Int -> JsonPrimitive(this)
    is Long -> JsonPrimitive(this)
    is Double -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    else -> JsonPrimitive(this.toString())
}
