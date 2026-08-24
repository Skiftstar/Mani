package xyz.skifty.moonlight.media

import androidx.compose.runtime.mutableStateOf
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class DesktopAudioPlayer {

    private val factory = MediaPlayerFactory()
    private val player: MediaPlayer = factory.mediaPlayers().newMediaPlayer()
    var isPlaying: Boolean by mutableStateOf(false)
        private set

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

    fun length(): Long {
        if (player.media() == null || player.media().info() == null) {
            return 0
        }
        return player.media().info().duration()
    }

    fun play(songInfo: SongInfo, activeSongInfo: SongInfo) {
        player.media().play(songInfo.songPlaybackUrl?:"")
        activeSongInfo.setSong(songInfo)
        isPlaying = true
    }

    fun stop(activeSongInfo: SongInfo) {
        player.controls().stop()
        activeSongInfo.clear()
        isPlaying = false
    }

    fun pause() {
        player.controls().pause()
        isPlaying = !player.status().isPlaying
    }

    fun seek(ms: Long) {
        player.controls().setTime(ms)
    }

    fun seekFraction(fraction: Float) {
        player.controls().setPosition(fraction.coerceIn(0f, 1f))
    }

    /** Get current playback time in milliseconds */
    fun currentPosition(): Long {
        return player.status().time()
    }

    fun setVolume(volume: Int) {
        player.audio().setVolume(volume.coerceIn(0, 100))
    }

    fun release() {
        player.release()
        factory.release()
    }
}
