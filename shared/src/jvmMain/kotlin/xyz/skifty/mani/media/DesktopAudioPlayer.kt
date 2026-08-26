package xyz.skifty.mani.media

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import xyz.skifty.mani.media.mpv.MpvIpcClient

class DesktopAudioPlayer {

    private val mpv = MpvIpcClient()

    var isPlaying: Boolean by mutableStateOf(false)
        private set

    // Backing state for `volume` below - named differently so its Kotlin-generated setter
    // doesn't clash with the setVolume(Int) function's identical JVM signature.
    private var volumeState: Int by mutableStateOf(100)
    val volume: Int get() = volumeState

    // Bumped on every explicit seek, regardless of who asked for it (the in-app progress slider
    // or an MPRIS client) - a single reactive hook JvmApp can watch to tell MPRIS listeners the
    // position just jumped, distinct from ordinary forward playback (see MprisService.notifySeeked()).
    var seekCount: Int by mutableStateOf(0)
        private set

    // Bumped whenever a track finishes playing naturally - JvmApp watches this to advance the
    // playback queue, same "Compose-observable counter as an event hook" shape as seekCount.
    var trackFinishedCount: Int by mutableStateOf(0)
        private set

    // Bumped whenever mpv *confirms* playback has genuinely (re)started - a fresh play, a resume,
    // or a skip straight from one playing track to another. Deliberately not the same thing as
    // isPlaying: going from "playing track A" to "playing track B" never actually flips isPlaying's
    // value (true -> true), so it wouldn't retrigger a LaunchedEffect keyed on it - this counter
    // always ticks, and only once the position it'd report is actually trustworthy (unlike the
    // optimistic isPlaying write in play(), which fires before mpv has confirmed anything).
    var playbackStartedCount: Int by mutableStateOf(0)
        private set

    // The position to report as of the most recent playbackStartedCount bump (ms) - see
    // lastConfirmedStartPositionMs and pendingStartPositionMs below.
    var lastConfirmedStartPositionMs: Long by mutableStateOf(0L)
        private set

    // Cached from mpv's "time-pos"/"duration" property-change events, pushed asynchronously on
    // the IPC reader thread, rather than queried live on every call - currentPosition() is polled
    // 5x/sec by NowPlayingBottomWidget, and a cached read is far cheaper than an IPC round-trip
    // per poll.
    private var cachedPositionMs: Long by mutableStateOf(0L)
    private var cachedDurationMs: Long by mutableStateOf(0L)

    // Set right before play() issues a fresh loadfile - which always starts a brand new track at
    // position 0 - so the "file-loaded" handler below can report a definitely-correct position
    // immediately, instead of trusting cachedPositionMs: during a track switch, that can still
    // briefly reflect the *previous* track's time right as "file-loaded" fires, which is exactly
    // the kind of stale read that made MPRIS position widgets look like they'd skipped into the
    // new track already part-way through. Left null for resume()/togglePlayPause(), where the
    // media doesn't change and a live read is safe.
    private var pendingStartPositionMs: Long? = null

    // Set right before play()/prepare() issues a fresh loadfile, to whichever isPlaying value that
    // load is meant to end up at - consumed by the "file-loaded" handler below to reassert it once
    // the new track is confirmed loaded. Needed because switching tracks makes mpv end the
    // *outgoing* file first (loadfile's "replace" doesn't wait for a clean stop), which fires
    // end-file for it same as a genuine stop/EOF would - end-file's own isPlaying = false would
    // otherwise stick, since mpv's "pause" property change notification (the other thing that
    // writes isPlaying) only fires on an actual value *change*, and it never truly changes here if
    // the player was already playing before the switch and still is straight after it.
    private var pendingIsPlaying: Boolean? = null

    // Real elapsed wall-clock time spent actually playing the current track - paused time
    // excluded, unaffected by seeking - the input to JvmApp's scrobble-threshold decision (see
    // listenedMs() below), as opposed to position, which seeking straight to a threshold and
    // immediately skipping away would let you fake.
    private var listenStartedAtMs: Long? = null
    private var accumulatedListenMs: Long = 0L

    // Describes whatever song play()/prepare()/stop() just replaced or cleared, captured right
    // before that happens - see TrackLeftEvent and JvmApp's scrobbleIfNeeded.
    var lastTrackLeft: TrackLeftEvent? by mutableStateOf(null)
        private set

    var trackLeftCount: Int by mutableStateOf(0)
        private set

    init {
        // mpv pushes property changes asynchronously on its own IPC reader thread rather than us
        // polling for them - writing straight to Compose state from that thread is safe, exactly
        // like this same class's vlcj event listener used to do (see MprisService's threading
        // comment for why that matters).
        mpv.observeProperty("pause") { data ->
            data.jsonPrimitive.booleanOrNull?.let { paused -> setIsPlaying(!paused) }
        }
        mpv.observeProperty("time-pos") { data ->
            data.jsonPrimitive.doubleOrNull?.let { seconds -> cachedPositionMs = (seconds * 1000).toLong() }
        }
        mpv.observeProperty("duration") { data ->
            data.jsonPrimitive.doubleOrNull?.let { seconds -> cachedDurationMs = (seconds * 1000).toLong() }
        }

        mpv.onEvent("file-loaded") {
            lastConfirmedStartPositionMs = pendingStartPositionMs ?: cachedPositionMs
            pendingStartPositionMs = null
            pendingIsPlaying?.let { playing -> setIsPlaying(playing) }
            pendingIsPlaying = null
            playbackStartedCount++
        }

        mpv.onEvent("end-file") { message ->
            setIsPlaying(false)
            if (message.reason == "eof") {
                trackFinishedCount++
            }
        }
    }

    /** Routes every isPlaying transition through here rather than writing the backing field
     *  directly, so [accumulatedListenMs] - real elapsed time spent actually playing, not just
     *  time since playback started - stays accurate across pauses. */
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
        // If already playing right as a new track loads (e.g. skipping mid-playback), restart the
        // clock immediately rather than waiting for setIsPlaying() to notice a change - it won't,
        // since isPlaying's value (true) isn't actually changing.
        listenStartedAtMs = if (isPlaying) System.currentTimeMillis() else null
    }

    /** How much of the currently-loaded track has actually been listened to in real time - paused
     *  time excluded, and unaffected by seeking (unlike [currentPosition], which reflects where
     *  playback currently sits and *does* jump on a seek). This, not position, is the input to
     *  JvmApp's scrobble-threshold decision - seeking straight to the threshold and immediately
     *  skipping away shouldn't count as a real listen. */
    fun listenedMs(): Long = currentAccumulatedListenMs()

    private fun captureTrackLeft(activeSongInfo: SongInfo) {
        activeSongInfo.songId?.let { songId ->
            lastTrackLeft = TrackLeftEvent(songId, currentAccumulatedListenMs(), cachedDurationMs)
            trackLeftCount++
        }
    }

    fun length(): Long = cachedDurationMs

    fun play(songInfo: SongInfo, activeSongInfo: SongInfo) {
        captureTrackLeft(activeSongInfo)
        resetListenTracking()
        pendingStartPositionMs = 0L
        pendingIsPlaying = true
        // A fresh loadfile always starts at 0 - clear the cache immediately rather than waiting
        // for mpv's own "time-pos" event for the new track to arrive, so a poll landing in that
        // gap (see NowPlayingBottomWidget) can't briefly read the *previous* track's position.
        cachedPositionMs = 0L
        // Same reasoning for duration: left uncleared, it keeps reporting the *previous* track's
        // length until mpv's own "duration" event for the new one arrives - and seekFraction()
        // scales a seek against whatever length() currently returns, so a seek issued in that gap
        // would land at completely the wrong position (wrong track's length, right fraction).
        cachedDurationMs = 0L
        mpv.sendCommand("loadfile", songInfo.songPlaybackUrl ?: "", "replace")
        activeSongInfo.setSong(songInfo)
        setIsPlaying(true)
    }

    /** Loads [songInfo] without starting playback - used to restore the last-played song as
     *  paused on app startup, without auto-playing it. */
    fun prepare(songInfo: SongInfo, activeSongInfo: SongInfo) {
        captureTrackLeft(activeSongInfo)
        resetListenTracking()
        pendingStartPositionMs = 0L
        pendingIsPlaying = false
        cachedPositionMs = 0L
        cachedDurationMs = 0L
        // The third positional argument (playlist insertion index) must be explicitly -1 for the
        // fourth (per-file options) to be honored, since mpv 0.38.0 - see the loadfile docs.
        // Setting pause=yes here as a load-time option, rather than a follow-up set_property
        // command, avoids a race where playback could briefly start before a separate pause call
        // lands.
        mpv.sendCommand("loadfile", songInfo.songPlaybackUrl ?: "", "replace", -1, "pause=yes")
        activeSongInfo.setSong(songInfo)
        setIsPlaying(false)
    }

    fun stop(activeSongInfo: SongInfo) {
        captureTrackLeft(activeSongInfo)
        mpv.sendCommand("stop")
        activeSongInfo.clear()
        setIsPlaying(false)
    }

    /** Resumes/starts playback - a no-op if already playing. Unlike [togglePlayPause], this
     *  won't flip a playing track to paused, matching MPRIS's idempotent `Play()`. */
    fun resume() {
        if (!isPlaying) {
            mpv.sendCommand("set_property", "pause", false)
            setIsPlaying(true)
        }
    }

    /** Pauses playback - a no-op if already paused. Matches MPRIS's idempotent `Pause()`. */
    fun pauseOnly() {
        if (isPlaying) {
            mpv.sendCommand("set_property", "pause", true)
            setIsPlaying(false)
        }
    }

    fun togglePlayPause() {
        if (isPlaying) pauseOnly() else resume()
    }

    fun seek(ms: Long) {
        mpv.sendCommand("seek", ms / 1000.0, "absolute")
        seekCount++
    }

    fun seekFraction(fraction: Float) {
        val targetMs = (fraction.coerceIn(0f, 1f) * cachedDurationMs).toLong()
        mpv.sendCommand("seek", targetMs / 1000.0, "absolute")
        seekCount++
    }

    /** Get current playback time in milliseconds, from the cached value kept up to date by mpv's
     *  "time-pos" property-change events. */
    fun currentPosition(): Long = cachedPositionMs

    fun setVolume(volume: Int) {
        val clamped = volume.coerceIn(0, 100)
        mpv.sendCommand("set_property", "volume", clamped)
        volumeState = clamped
    }

    fun release() {
        mpv.close()
    }
}
