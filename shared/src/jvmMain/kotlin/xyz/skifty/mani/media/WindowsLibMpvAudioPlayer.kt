package xyz.skifty.mani.media

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sun.jna.StringArray
import xyz.skifty.mani.media.mpv.LibMpv
import xyz.skifty.mani.media.mpv.MPV_END_FILE_REASON_EOF
import xyz.skifty.mani.media.mpv.MPV_EVENT_END_FILE
import xyz.skifty.mani.media.mpv.MPV_EVENT_FILE_LOADED
import xyz.skifty.mani.media.mpv.MPV_EVENT_PROPERTY_CHANGE
import xyz.skifty.mani.media.mpv.MPV_EVENT_SHUTDOWN
import xyz.skifty.mani.media.mpv.MPV_FORMAT_DOUBLE
import xyz.skifty.mani.media.mpv.MPV_FORMAT_FLAG
import xyz.skifty.mani.media.mpv.MpvEventEndFile
import xyz.skifty.mani.media.mpv.MpvEventProperty

// mpv_wait_event's timeout - bounded rather than infinite purely so the poll loop below notices
// `closed` and exits promptly on release(), the same reason MpvIpcClient's own readLoop() checks
// `!closed` every iteration instead of blocking forever on a single read.
private const val EVENT_POLL_TIMEOUT_SECONDS = 1.0

/** Windows-only [AudioPlayer], backed by libmpv (mpv's own C embedding API,
 * https://github.com/mpv-player/mpv/blob/master/DOCS/man/libmpv.rst) via JNA rather than
 * [DesktopAudioPlayer]'s subprocess-plus-JSON-IPC approach - see DesktopModule.kt's `os.name`
 * dispatch. Exists because mpv's old hand-rolled Win32 named-pipe IPC transport (since deleted -
 * `WindowsMpvIpcTransport`, formerly this project's Windows counterpart to
 * `LinuxMpvIpcTransport`) was a disproportionate source of Windows-only playback errors compared
 * to Linux's plain Unix-domain-socket transport - unlike the two things tried before this
 * (`javafx.media`, `goxr3plus/java-stream-player`), this keeps mpv's own proven decode/seek engine
 * entirely intact (the same one Linux already uses reliably every day) and only replaces the
 * fragile *transport*: direct in-process function calls into `libmpv-2.dll` instead of spawning
 * `mpv.exe` and talking JSON over a named pipe. Linux keeps using [DesktopAudioPlayer]/mpv's JSON
 * IPC entirely unchanged.
 *
 * Structurally mirrors [DesktopAudioPlayer] as closely as libmpv's API shape allows - same command
 * names (`loadfile`, `seek ... absolute`, `stop`), same observed properties (`pause`, `time-pos`,
 * `duration`), same `pendingStartPositionMs`/`pendingIsPlaying` mailbox pattern for the same
 * `file-loaded`-races-the-caller reason documented there. The mechanics of *sending* those
 * commands and *receiving* those events differ (direct JNA calls plus a dedicated
 * `mpv_wait_event()` poll thread here, instead of writing/reading JSON lines over a channel), but
 * the mpv-specific knowledge encoded in each command/workaround is the same underlying engine
 * either way. */
class WindowsLibMpvAudioPlayer : AudioPlayer {

    private val handle = LibMpv.INSTANCE.mpv_create()
        ?: throw IllegalStateException("libmpv: mpv_create() returned null")

    @Volatile
    private var closed = false

    private val eventThread: Thread

    init {
        // Audio-only, no window ever created - mirrors MpvIpcClient's own subprocess flags
        // (`--idle=yes --no-video`) rather than the CLI's shorthand, since these are libmpv options,
        // not command-line arguments. Set before mpv_initialize(), as libmpv's docs require for
        // startup options.
        LibMpv.INSTANCE.mpv_set_option_string(handle, "idle", "yes")
        LibMpv.INSTANCE.mpv_set_option_string(handle, "vid", "no")

        val initResult = LibMpv.INSTANCE.mpv_initialize(handle)
        if (initResult < 0) {
            throw IllegalStateException(
                "libmpv: mpv_initialize() failed: ${LibMpv.INSTANCE.mpv_error_string(initResult)}",
            )
        }

        LibMpv.INSTANCE.mpv_observe_property(handle, 1, "pause", MPV_FORMAT_FLAG)
        LibMpv.INSTANCE.mpv_observe_property(handle, 2, "time-pos", MPV_FORMAT_DOUBLE)
        LibMpv.INSTANCE.mpv_observe_property(handle, 3, "duration", MPV_FORMAT_DOUBLE)

        eventThread = Thread(::eventLoop, "libmpv-event-loop")
            .apply {
                isDaemon = true
                start()
            }

        Runtime.getRuntime().addShutdownHook(
            Thread {
                // Belt-and-braces, same reasoning as MpvIpcClient's own shutdown hook: an abrupt
                // JVM exit skips Compose's DisposableEffect lifecycle (where release() is normally
                // called from), which would otherwise leave mpv's internal state dangling.
                runCatching { release() }
            },
        )
    }

    override var isPlaying: Boolean by mutableStateOf(false)
        private set

    // Named differently from `volume` below - see DesktopAudioPlayer's identical volumeState for
    // why: a Kotlin property named `volume` would generate a synthetic setVolume(Int) JVM setter
    // that clashes with the interface's own setVolume(Int) function (same JVM signature).
    private var volumeState: Int by mutableStateOf(100)
    override val volume: Int get() = volumeState

    override var seekCount: Int by mutableStateOf(0)
        private set

    override var trackFinishedCount: Int by mutableStateOf(0)
        private set

    override var hasReachedEnd: Boolean by mutableStateOf(false)
        private set

    override var playbackStartedCount: Int by mutableStateOf(0)
        private set

    override var lastConfirmedStartPositionMs: Long by mutableStateOf(0L)
        private set

    override var lastTrackLeft: TrackLeftEvent? by mutableStateOf(null)
        private set

    override var trackLeftCount: Int by mutableStateOf(0)
        private set

    // Cached from mpv's "time-pos"/"duration" property-change events, pushed asynchronously on the
    // event-poll thread, rather than queried live on every call - currentPosition() is polled 5x/sec
    // by NowPlayingBottomWidget, and a cached read is far cheaper than a fresh native call per poll.
    @Volatile
    private var cachedPositionMs: Long = 0L

    @Volatile
    private var cachedDurationMs: Long = 0L

    // Same mailbox pattern as DesktopAudioPlayer, for the same reason - see that class's own
    // pendingStartPositionMs/pendingIsPlaying doc comments for the full explanation (a fresh
    // loadfile's "file-loaded" confirmation is asynchronous, and can race a caller reading
    // cachedPositionMs/isPlaying in the gap before it arrives).
    private var pendingStartPositionMs: Long? = null
    private var pendingIsPlaying: Boolean? = null

    // Real elapsed wall-clock time spent actually playing the current track - see
    // DesktopAudioPlayer's identical fields/listenedMs() doc comment.
    private var listenStartedAtMs: Long? = null
    private var accumulatedListenMs: Long = 0L

    private fun command(vararg args: String) {
        LibMpv.INSTANCE.mpv_command(handle, StringArray(args))
    }

    private fun eventLoop() {
        while (!closed) {
            val event = LibMpv.INSTANCE.mpv_wait_event(handle, EVENT_POLL_TIMEOUT_SECONDS)
            when (event.eventId) {
                MPV_EVENT_SHUTDOWN -> return

                MPV_EVENT_PROPERTY_CHANGE -> {
                    val pointer = event.data
                        ?: continue
                    handlePropertyChange(MpvEventProperty(pointer))
                }

                MPV_EVENT_FILE_LOADED -> {
                    lastConfirmedStartPositionMs = pendingStartPositionMs ?: cachedPositionMs
                    pendingStartPositionMs = null
                    pendingIsPlaying?.let { playing -> setIsPlaying(playing) }
                    pendingIsPlaying = null
                    playbackStartedCount++
                }

                MPV_EVENT_END_FILE -> {
                    setIsPlaying(false)
                    val pointer = event.data
                    val reason = pointer?.let { MpvEventEndFile(it).reason }
                    if (reason == MPV_END_FILE_REASON_EOF) {
                        trackFinishedCount++
                        hasReachedEnd = true
                    }
                }
            }
        }
    }

    private fun handlePropertyChange(property: MpvEventProperty) {
        val dataPointer = property.data
            ?: return
        when (property.name) {
            "pause" -> setIsPlaying(dataPointer.getInt(0) == 0)
            "time-pos" -> cachedPositionMs = (dataPointer.getDouble(0) * 1000).toLong()
            "duration" -> cachedDurationMs = (dataPointer.getDouble(0) * 1000).toLong()
        }
    }

    /** Routes every isPlaying transition through here rather than writing the backing field
     *  directly, so [accumulatedListenMs] - real elapsed time spent actually playing, not just
     *  time since playback started - stays accurate across pauses. Identical to
     *  DesktopAudioPlayer's own. */
    private fun setIsPlaying(playing: Boolean) {
        if (playing == isPlaying) {
            return
        }
        val now = System.currentTimeMillis()
        if (playing) {
            listenStartedAtMs = now
        } else {
            listenStartedAtMs?.let { startedAt -> accumulatedListenMs += now - startedAt }
            listenStartedAtMs = null
        }
        isPlaying = playing
    }

    private fun currentAccumulatedListenMs(): Long {
        val inProgress = listenStartedAtMs?.let { startedAt -> System.currentTimeMillis() - startedAt } ?: 0L
        return accumulatedListenMs + inProgress
    }

    private fun resetListenTracking() {
        accumulatedListenMs = 0L
        listenStartedAtMs = if (isPlaying) System.currentTimeMillis() else null
    }

    override fun listenedMs(): Long = currentAccumulatedListenMs()

    private fun captureTrackLeft(activeSongInfo: SongInfo) {
        val songId = activeSongInfo.songId ?: return
        lastTrackLeft = TrackLeftEvent(
            songId = songId,
            listenedMs = currentAccumulatedListenMs(),
            durationMs = cachedDurationMs,
            playThroughToken = playbackStartedCount,
        )
        trackLeftCount++
    }

    override fun length(): Long = cachedDurationMs

    override fun play(songInfo: SongInfo, activeSongInfo: SongInfo) {
        captureTrackLeft(activeSongInfo)
        resetListenTracking()
        hasReachedEnd = false
        pendingStartPositionMs = 0L
        pendingIsPlaying = true
        cachedPositionMs = 0L
        cachedDurationMs = 0L
        command("loadfile", songInfo.songPlaybackUrl ?: "", "replace")
        // Same reasoning as DesktopAudioPlayer.play()'s identical trailing set_property (not a
        // loadfile load-time option) - see that function's own comment for the LoopMode.ONE race
        // this avoids.
        LibMpv.INSTANCE.mpv_set_property_string(handle, "pause", "no")
        activeSongInfo.setSong(songInfo)
        setIsPlaying(true)
    }

    override fun prepare(songInfo: SongInfo, activeSongInfo: SongInfo) {
        captureTrackLeft(activeSongInfo)
        resetListenTracking()
        hasReachedEnd = false
        pendingStartPositionMs = 0L
        pendingIsPlaying = false
        cachedPositionMs = 0L
        cachedDurationMs = 0L
        command("loadfile", songInfo.songPlaybackUrl ?: "", "replace")
        LibMpv.INSTANCE.mpv_set_property_string(handle, "pause", "yes")
        activeSongInfo.setSong(songInfo)
        setIsPlaying(false)
    }

    override fun stop(activeSongInfo: SongInfo) {
        captureTrackLeft(activeSongInfo)
        command("stop")
        activeSongInfo.clear()
        setIsPlaying(false)
    }

    override fun resume() {
        if (!isPlaying) {
            LibMpv.INSTANCE.mpv_set_property_string(handle, "pause", "no")
            pendingIsPlaying = true
            setIsPlaying(true)
        }
    }

    override fun pauseOnly() {
        if (isPlaying) {
            LibMpv.INSTANCE.mpv_set_property_string(handle, "pause", "yes")
            pendingIsPlaying = false
            setIsPlaying(false)
        }
    }

    override fun togglePlayPause() {
        if (isPlaying) {
            pauseOnly()
        } else {
            resume()
        }
    }

    override fun seek(ms: Long) {
        command("seek", (ms / 1000.0).toString(), "absolute")
        seekCount++
    }

    override fun seekFraction(fraction: Float) {
        val targetMs = (fraction.coerceIn(0f, 1f) * cachedDurationMs).toLong()
        command("seek", (targetMs / 1000.0).toString(), "absolute")
        seekCount++
    }

    override fun currentPosition(): Long = cachedPositionMs

    override fun setVolume(volume: Int) {
        val clamped = volume.coerceIn(0, 100)
        LibMpv.INSTANCE.mpv_set_property_string(handle, "volume", clamped.toString())
        volumeState = clamped
    }

    override fun release() {
        if (closed) {
            return
        }
        closed = true
        // Waits for eventLoop() to actually return (it re-checks `closed` at most
        // EVENT_POLL_TIMEOUT_SECONDS after this is set) before destroying the handle, rather than
        // risking a concurrent mpv_wait_event() call on another thread racing this thread's
        // mpv_terminate_destroy() - bounded, same shape as MpvIpcClient.close()'s own
        // process.waitFor(1, TimeUnit.SECONDS).
        eventThread.join((EVENT_POLL_TIMEOUT_SECONDS * 1000).toLong() + 500)
        LibMpv.INSTANCE.mpv_terminate_destroy(handle)
    }

}
