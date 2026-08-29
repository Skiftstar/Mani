package xyz.skifty.mani.media

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Media3-backed [AudioPlayer] - a thin MediaController client, not an ExoPlayer owner itself: the
 *  actual ExoPlayer instance lives in [PlaybackService], a foreground MediaSessionService, so
 *  playback survives this class (and the whole UI) being gone. Requests made before the
 *  controller finishes connecting are dropped rather than queued - in practice the only caller
 *  this could affect is a startup "restore last song as paused" [prepare] call landing in a very
 *  small window, which is a lower-stakes miss than the added complexity of queuing would be worth. */
class AndroidAudioPlayer(private val context: Context) : AudioPlayer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var controller: MediaController? = null

    override var isPlaying: Boolean by mutableStateOf(false)
        private set

    // Backing state for `volume` below - named differently so its Kotlin-generated setter
    // doesn't clash with the setVolume(Int) function's identical JVM signature.
    private var volumeState: Int by mutableStateOf(100)
    override val volume: Int get() = volumeState

    override var seekCount: Int by mutableStateOf(0)
        private set

    override var trackFinishedCount: Int by mutableStateOf(0)
        private set

    // See AudioPlayer.hasReachedEnd - set on the same STATE_ENDED transition as trackFinishedCount
    // above, cleared the moment a fresh play()/prepare() sets a new media item.
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

    // Same wall-clock accumulation approach as DesktopAudioPlayer - see its own comments on the
    // equivalent fields for the full reasoning (real elapsed listening time, paused time
    // excluded, unaffected by seeking).
    private var listenStartedAtMs: Long? = null
    private var accumulatedListenMs: Long = 0L

    init {
        scope.launch {
            val connected = connectController()
            connected.addListener(PlayerListener())
            controller = connected
        }
    }

    private suspend fun connectController(): MediaController = suspendCancellableCoroutine { continuation ->
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        future.addListener(
            { continuation.resume(future.get()) },
            MoreExecutors.directExecutor(),
        )
    }

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
        activeSongInfo.songId?.let { songId ->
            lastTrackLeft = TrackLeftEvent(songId, currentAccumulatedListenMs(), length())
            trackLeftCount++
        }
    }

    override fun length(): Long = controller?.duration?.coerceAtLeast(0) ?: 0L

    private fun mediaItemFor(songInfo: SongInfo): MediaItem =
        MediaItem.Builder()
            .setMediaId(songInfo.songId ?: "")
            .setUri(songInfo.songPlaybackUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(songInfo.songName)
                    .setArtist(songInfo.songArtist)
                    .setArtworkUri(songInfo.songCoverArtUrl?.let { url -> Uri.parse(url) })
                    .build(),
            )
            .build()

    override fun play(songInfo: SongInfo, activeSongInfo: SongInfo) {
        captureTrackLeft(activeSongInfo)
        resetListenTracking()
        hasReachedEnd = false
        activeSongInfo.setSong(songInfo)
        controller?.apply {
            setMediaItem(mediaItemFor(songInfo))
            prepare()
            play()
        }
        setIsPlaying(true)
    }

    /** Loads [songInfo] without starting playback - used to restore the last-played song as
     *  paused on app startup, without auto-playing it. */
    override fun prepare(songInfo: SongInfo, activeSongInfo: SongInfo) {
        captureTrackLeft(activeSongInfo)
        resetListenTracking()
        hasReachedEnd = false
        activeSongInfo.setSong(songInfo)
        controller?.apply {
            setMediaItem(mediaItemFor(songInfo))
            prepare()
        }
        setIsPlaying(false)
    }

    override fun stop(activeSongInfo: SongInfo) {
        captureTrackLeft(activeSongInfo)
        controller?.stop()
        activeSongInfo.clear()
        setIsPlaying(false)
    }

    /** Resumes/starts playback - a no-op if already playing. */
    override fun resume() {
        if (!isPlaying) {
            controller?.play()
        }
    }

    /** Pauses playback - a no-op if already paused. */
    override fun pauseOnly() {
        if (isPlaying) {
            controller?.pause()
        }
    }

    override fun togglePlayPause() {
        if (isPlaying) pauseOnly() else resume()
    }

    override fun seek(ms: Long) {
        controller?.seekTo(ms)
        seekCount++
    }

    override fun seekFraction(fraction: Float) {
        val targetMs = (fraction.coerceIn(0f, 1f) * length()).toLong()
        controller?.seekTo(targetMs)
        seekCount++
    }

    override fun currentPosition(): Long = controller?.currentPosition?.coerceAtLeast(0) ?: 0L

    override fun setVolume(volume: Int) {
        val clamped = volume.coerceIn(0, 100)
        controller?.volume = clamped / 100f
        volumeState = clamped
    }

    override fun release() {
        controller?.release()
        controller = null
    }

    private inner class PlayerListener : Player.Listener {

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            setIsPlaying(isPlaying)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            lastConfirmedStartPositionMs = controller?.currentPosition ?: 0L
            playbackStartedCount++
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                trackFinishedCount++
                hasReachedEnd = true
            }
        }

    }

}
