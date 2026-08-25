package xyz.skifty.moonlight.media

import androidx.compose.runtime.mutableStateOf
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class DesktopAudioPlayer {

    private val factory = MediaPlayerFactory()
    private val player: MediaPlayer = factory.mediaPlayers()
        .newMediaPlayer()
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

    // Bumped whenever libVLC *confirms* playback has genuinely (re)started - a fresh play, a
    // resume, or a skip straight from one playing track to another. Deliberately not the same
    // thing as isPlaying: going from "playing track A" to "playing track B" never actually flips
    // isPlaying's value (true -> true), so it wouldn't retrigger a LaunchedEffect keyed on it -
    // this counter always ticks, and only once the position it'd report is actually trustworthy
    // (unlike the optimistic isPlaying writes below, which fire before libVLC has loaded anything).
    var playbackStartedCount: Int by mutableStateOf(0)
        private set

    // The position to report as of the most recent playbackStartedCount bump (ms) - see
    // lastConfirmedStartPositionMs and pendingStartPositionMs below.
    var lastConfirmedStartPositionMs: Long by mutableStateOf(0L)
        private set

    // Set right before play() issues a fresh play(url) - which always starts a brand new track at
    // position 0 - so the playing() callback below can report a definitely-correct position
    // immediately, instead of querying player.status().time(): during a media switch, that can
    // still briefly reflect the *previous* track's time right as playing() fires, which is exactly
    // the kind of stale read that made MPRIS position widgets look like they'd skipped into the
    // new track already part-way through. Left null for resume()/togglePlayPause(), where the
    // media doesn't change and a live query is safe.
    private var pendingStartPositionMs: Long? = null

    init {
        player.events()
            .addMediaPlayerEventListener(
                object : MediaPlayerEventAdapter() {
                    // vlcj's controls (play/pause/stop) are asynchronous - the calling code setting
                    // isPlaying right after issuing one is only a best-effort guess, and can end up
                    // stuck wrong (e.g. two toggles racing each other). These callbacks fire once
                    // libVLC actually confirms the transition, so they're the source of truth that
                    // self-corrects isPlaying whenever it drifts from what's really playing.
                    override fun playing(mp: MediaPlayer) {
                        isPlaying = true
                        lastConfirmedStartPositionMs = pendingStartPositionMs
                            ?: currentPosition()
                        pendingStartPositionMs = null
                        playbackStartedCount++
                    }

                    override fun paused(mp: MediaPlayer) {
                        isPlaying = false
                    }

                    override fun stopped(mp: MediaPlayer) {
                        isPlaying = false
                    }

                    override fun finished(mp: MediaPlayer) {
                        isPlaying = false
                        trackFinishedCount++
                    }

                    override fun error(mp: MediaPlayer) {
                        isPlaying = false
                    }
                },
            )
    }

    fun length(): Long {
        if (player.media() == null || player.media()
                .info() == null
        ) {
            return 0
        }
        return player.media()
            .info()
            .duration()
    }

    fun play(songInfo: SongInfo, activeSongInfo: SongInfo) {
        pendingStartPositionMs = 0L
        player.media()
            .play(songInfo.songPlaybackUrl ?: "")
        activeSongInfo.setSong(songInfo)
        isPlaying = true
    }

    /** Loads [songInfo] without starting playback - used to restore the last-played song as
     *  paused on app startup, without auto-playing it. */
    fun prepare(songInfo: SongInfo, activeSongInfo: SongInfo) {
        player.media()
            .prepare(songInfo.songPlaybackUrl ?: "")
        activeSongInfo.setSong(songInfo)
        isPlaying = false
    }

    fun stop(activeSongInfo: SongInfo) {
        player.controls()
            .stop()
        activeSongInfo.clear()
        isPlaying = false
    }

    /** Resumes/starts playback - a no-op if already playing. Unlike [togglePlayPause], this
     *  won't flip a playing track to paused, matching MPRIS's idempotent `Play()`. */
    fun resume() {
        if (!isPlaying) {
            player.controls()
                .play()
            isPlaying = player.status().isPlaying
        }
    }

    /** Pauses playback - a no-op if already paused. Matches MPRIS's idempotent `Pause()`. */
    fun pauseOnly() {
        if (isPlaying) {
            player.controls()
                .pause()
            isPlaying = player.status().isPlaying
        }
    }

    fun togglePlayPause() {
        // controls().pause() only toggles an already-playing media - it won't start one that
        // was merely prepare()d, so the first resume after prepare() must go through play().
        if (isPlaying) pauseOnly() else resume()
    }

    fun seek(ms: Long) {
        player.controls()
            .setTime(ms)
        seekCount++
    }

    fun seekFraction(fraction: Float) {
        player.controls()
            .setPosition(fraction.coerceIn(0f, 1f))
        seekCount++
    }

    /** Get current playback time in milliseconds - libVLC reports -1 when it's not yet known
     *  (e.g. right as a new track is still opening/buffering), which is never a meaningful
     *  position for any caller, in-app or MPRIS. */
    fun currentPosition(): Long {
        return player.status()
            .time()
            .coerceAtLeast(0)
    }

    fun setVolume(volume: Int) {
        val clamped = volume.coerceIn(0, 100)
        player.audio()
            .setVolume(clamped)
        volumeState = clamped
    }

    fun release() {
        player.release()
        factory.release()
    }
}
