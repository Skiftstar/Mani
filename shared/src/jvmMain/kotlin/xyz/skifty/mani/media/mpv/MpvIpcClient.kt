package xyz.skifty.mani.media.mpv

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
    // The only remaining MpvIpcTransport implementation - Windows used to pick between this and
    // WindowsMpvIpcTransport here via a small factory, but that transport (and the factory
    // choosing between them) was deleted once WindowsLibMpvAudioPlayer replaced it: Windows now
    // talks to mpv through a direct libmpv binding instead of this class's subprocess+JSON-IPC
    // approach at all, so DesktopAudioPlayer (this class's only caller) is Linux-only in practice.
    private val transport: MpvIpcTransport = LinuxMpvIpcTransport(),
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
                resolveMpvExecutable(),
                // Unlike libmpv (WindowsLibMpvAudioPlayer's backend, which defaults to ignoring
                // user config), this spawns the real mpv CLI binary, which loads the user's own
                // ~/.config/mpv/mpv.conf by default - e.g. a common save-position-on-quit=yes
                // there makes mpv silently write a watch-later position for every track this class
                // ever replaces, and resume from it the next time that same URL is loaded, which
                // looks exactly like a track "remembering" where a previous skip left off. Mani
                // drives mpv purely as a headless playback engine and needs deterministic behavior
                // regardless of whatever the user has configured for their own interactive/video
                // mpv usage, so this ignores all such files entirely.
                "--no-config",
                "--idle=yes", "--no-video", "--input-ipc-server=$socketPath",
            )
                // mpv logs to stdout/stderr regardless of --no-terminal - discard rather than pipe,
                // so it can't ever block on a full pipe buffer we're not draining, and doesn't spam
                // Mani's own console the way libVLC's did.
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
        } catch (e: IOException) {
            throw IllegalStateException(
                "Mani needs the 'mpv' command for audio playback, but it wasn't found on " +
                    "PATH. Install it via your package manager, e.g.:\n" +
                    mpvInstallInstructions() +
                    "\nthen restart Mani.",
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

/** The `mpv` executable to spawn - a bare PATH lookup for the system `mpv` package the `.deb`
 *  already depends on (see `addMpvDependencyToDeb` in `desktopApp/build.gradle.kts`). This class
 *  ([MpvIpcClient], owned by [DesktopAudioPlayer]) is only ever constructed on Linux now -
 *  DesktopModule.kt dispatches Windows to `WindowsLibMpvAudioPlayer`'s direct libmpv binding
 *  instead, which talks to mpv in-process and never spawns this executable at all. */
private fun resolveMpvExecutable(): String {
    val resourcesDir = System.getProperty("compose.application.resources.dir")
        ?: return "mpv"
    val bundled = File(resourcesDir, "mpv")
    return if (bundled.exists()) bundled.absolutePath else "mpv"
}

private fun mpvInstallInstructions(): String =
    "  Debian/Ubuntu: sudo apt install mpv\n" +
        "  Fedora:        sudo dnf install mpv\n" +
        "  Arch:          sudo pacman -S mpv"

private fun Any.toJsonElement(): JsonElement = when (this) {
    is String -> JsonPrimitive(this)
    is Int -> JsonPrimitive(this)
    is Long -> JsonPrimitive(this)
    is Double -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    else -> JsonPrimitive(this.toString())
}
