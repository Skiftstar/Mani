package xyz.skifty.mani.media

/** Cross-platform playback contract - `DesktopAudioPlayer` wraps mpv over IPC on desktop,
 *  `AndroidAudioPlayer` fronts a Media3 `MediaSession`/`ExoPlayer` on Android. A plain interface
 *  rather than an `expect`/`actual` class: the Android implementation needs a `Context` and an
 *  async `MediaController` connection that an `expect class`'s shared constructor shape can't
 *  accommodate, while a Koin-provided interface handles arbitrary per-platform construction
 *  naturally. `PlaybackQueue` is the only thing that drives this - it stays unaware queues exist. */
interface AudioPlayer {

    val isPlaying: Boolean
    val volume: Int
    val seekCount: Int
    val trackFinishedCount: Int
    val playbackStartedCount: Int
    val lastConfirmedStartPositionMs: Long
    val lastTrackLeft: TrackLeftEvent?
    val trackLeftCount: Int

    fun play(songInfo: SongInfo, activeSongInfo: SongInfo)
    fun prepare(songInfo: SongInfo, activeSongInfo: SongInfo)
    fun stop(activeSongInfo: SongInfo)
    fun resume()
    fun pauseOnly()
    fun togglePlayPause()
    fun seek(ms: Long)
    fun seekFraction(fraction: Float)
    fun currentPosition(): Long
    fun length(): Long
    fun listenedMs(): Long
    fun setVolume(volume: Int)
    fun release()

}
