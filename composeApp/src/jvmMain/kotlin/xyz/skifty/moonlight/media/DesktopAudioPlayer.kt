package xyz.skifty.moonlight.media

import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter

class DesktopAudioPlayer {

    private val factory = MediaPlayerFactory()
    private val player: MediaPlayer = factory.mediaPlayers().newMediaPlayer()

    init {
        player.events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
            override fun finished(mp: MediaPlayer) {
                println("Playback finished")
            }

            override fun error(mp: MediaPlayer) {
                println("Playback error")
            }
        })
    }

    /** Play a URL or local file */
    fun play(url: String) {
        player.media().play(url)
    }

    /** Stop playback */
    fun stop() {
        player.controls().stop()
    }

    /** Pause playback */
    fun pause() {
        player.controls().pause()
    }

    /** Seek to position in milliseconds */
    fun seek(ms: Long) {
        player.controls().setTime(ms)
    }

    /** Get current playback time in milliseconds */
    fun currentPosition(): Long {
        return player.status().time()
    }

    /** Set volume 0–100 */
    fun setVolume(volume: Int) {
        player.audio().setVolume(volume.coerceIn(0, 100))
    }

    /** Release resources */
    fun release() {
        player.release()
        factory.release()
    }
}
