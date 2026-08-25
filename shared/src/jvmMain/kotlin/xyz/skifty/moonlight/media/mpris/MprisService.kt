package xyz.skifty.moonlight.media.mpris

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import org.freedesktop.dbus.DBusPath
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.interfaces.Properties
import org.freedesktop.dbus.types.Variant
import xyz.skifty.moonlight.media.DesktopAudioPlayer
import xyz.skifty.moonlight.media.SongInfo
import kotlin.math.roundToInt

private const val SERVICE_NAME = "org.mpris.MediaPlayer2.moonlight"
private const val OBJECT_PATH = "/org/mpris/MediaPlayer2"
private const val PLAYER_INTERFACE_NAME = "org.mpris.MediaPlayer2.Player"
private const val NO_TRACK_PATH = "/org/mpris/MediaPlayer2/TrackList/NoTrack"

/**
 * Registers Moonlight as an MPRIS player on the session bus, backed by [audioPlayer]/[activeSongInfo]'s
 * real state, so shell media widgets (Noctalia, waybar, etc.) recognize it and can send it commands.
 *
 * Connecting requires a reachable D-Bus session bus (Linux only) - construction throws if none is
 * available, which callers are expected to swallow (e.g. via `runCatching`) the same way this
 * codebase already tolerates a missing OS keyring.
 */
class MprisService(
    private val audioPlayer: DesktopAudioPlayer,
    private val activeSongInfo: SongInfo,
) : MprisRoot, MprisPlayerInterface {

    // Incoming MPRIS calls arrive on dbus-java's own connection thread - hop onto the Compose/Swing
    // UI thread before touching audioPlayer/activeSongInfo, so state mutation always happens from a
    // consistent thread.
    private val scope = CoroutineScope(Dispatchers.Swing + SupervisorJob())

    private val connection = DBusConnectionBuilder.forSessionBus()
        .build()

    init {
        connection.requestBusName(SERVICE_NAME)
        connection.exportObject(OBJECT_PATH, this)
    }

    override fun getObjectPath(): String = OBJECT_PATH

    // --- org.mpris.MediaPlayer2 (root) ---

    override fun Raise() {
        // Not implemented - no window-focus hook is threaded down to here yet.
    }

    override fun Quit() {
        // Not implemented - see Raise().
    }

    override fun isCanQuit(): Boolean = false
    override fun isCanRaise(): Boolean = false
    override fun isHasTrackList(): Boolean = false
    override fun getIdentity(): String = "Moonlight"
    override fun getSupportedUriSchemes(): List<String> = emptyList()
    override fun getSupportedMimeTypes(): List<String> = emptyList()

    // --- org.mpris.MediaPlayer2.Player ---

    override fun Next() {
        // No queue behind this yet - see isCanGoNext().
    }

    override fun Previous() {
        // No queue behind this yet - see isCanGoPrevious().
    }

    override fun Pause() {
        scope.launch { audioPlayer.pauseOnly() }
    }

    override fun PlayPause() {
        scope.launch { audioPlayer.togglePlayPause() }
    }

    override fun Stop() {
        scope.launch { audioPlayer.stop(activeSongInfo) }
    }

    override fun Play() {
        scope.launch { audioPlayer.resume() }
    }

    override fun Seek(offsetMicroseconds: Long) {
        // audioPlayer.seek() bumps seekCount itself - JvmApp's watcher on that covers notifying
        // MPRIS listeners here too, regardless of who asked for the seek.
        scope.launch {
            val newPositionMs = (audioPlayer.currentPosition() + offsetMicroseconds / 1000)
                .coerceAtLeast(0)
            audioPlayer.seek(newPositionMs)
        }
    }

    override fun SetPosition(trackId: DBusPath, positionMicroseconds: Long) {
        val currentSongId = activeSongInfo.songId ?: return
        if (trackId.path != trackObjectPath(currentSongId)) {
            return
        }
        scope.launch { audioPlayer.seek(positionMicroseconds / 1000) }
    }

    override fun OpenUri(uri: String) {
        // Not supported - Moonlight only plays tracks from its own configured server.
    }

    override fun getPlaybackStatus(): String = when {
        activeSongInfo.songId == null -> "Stopped"
        audioPlayer.isPlaying -> "Playing"
        else -> "Paused"
    }

    override fun getLoopStatus(): String = "None"
    override fun getRate(): Double = 1.0
    override fun isShuffle(): Boolean = false

    override fun getMetadata(): Map<String, Variant<*>> {
        val songId = activeSongInfo.songId
            ?: return mapOf("mpris:trackid" to Variant(DBusPath(NO_TRACK_PATH)))

        val metadata = mutableMapOf<String, Variant<*>>(
            "mpris:trackid" to Variant(DBusPath(trackObjectPath(songId))),
        )
        activeSongInfo.songName?.let { metadata["xesam:title"] = Variant(it) }
        activeSongInfo.songArtist?.let {
            // Single-element lists erase to java.util.Collections$SingletonList at runtime, which
            // the reflection-based single-arg Variant constructor can't derive a D-Bus signature
            // for - "as" (array of strings) must be given explicitly.
            metadata["xesam:artist"] = Variant(
                /* value = */ listOf(it),
                /* signature = */ "as",
            )
        }
        activeSongInfo.songCoverArtUrl?.let { metadata["mpris:artUrl"] = Variant(it) }
        activeSongInfo.songDurationSeconds?.let {
            metadata["mpris:length"] = Variant(it.toLong() * 1_000_000L)
        }
        return metadata
    }

    override fun getVolume(): Double = audioPlayer.volume / 100.0

    override fun setVolume(volume: Double) {
        scope.launch {
            audioPlayer.setVolume((volume * 100).roundToInt())
        }
    }

    override fun getPosition(): Long = audioPlayer.currentPosition() * 1000

    override fun getMinimumRate(): Double = 1.0
    override fun getMaximumRate(): Double = 1.0

    override fun isCanGoNext(): Boolean = false
    override fun isCanGoPrevious(): Boolean = false
    override fun isCanPlay(): Boolean = true
    override fun isCanPause(): Boolean = true
    override fun isCanSeek(): Boolean = true
    override fun isCanControl(): Boolean = true

    /** Call whenever playback state/track/volume changes, so listeners update immediately instead
     *  of waiting on their own polling. */
    fun notifyStateChanged() {
        try {
            val changedProperties = mapOf(
                "PlaybackStatus" to Variant(getPlaybackStatus()),
                "Metadata" to Variant(
                    /* value = */ getMetadata(),
                    /* signature = */ "a{sv}",
                ),
                "Volume" to Variant(getVolume()),
            )
            connection.sendMessage(
                Properties.PropertiesChanged(
                    /* path = */ OBJECT_PATH,
                    /* interfaceName = */ PLAYER_INTERFACE_NAME,
                    /* propertiesChanged = */ changedProperties,
                    /* propertiesRemoved = */ emptyList(),
                ),
            )
        } catch (e: Exception) {
            System.err.println("Failed to notify MPRIS listeners of a state change: ${e.message}")
        }
    }

    /** Call whenever the position changes in a way a listener can't derive by just letting time
     *  pass (an explicit seek, or playback starting/resuming - see [MprisPlayerInterface.Seeked]).
     *  Widgets that interpolate position locally (as the spec expects) use this as their cue to
     *  (re-)anchor that interpolation, rather than polling `Position` continuously. */
    fun notifySeeked() {
        try {
            connection.sendMessage(
                MprisPlayerInterface.Seeked(
                    /* path = */ OBJECT_PATH,
                    /* positionMicroseconds = */ getPosition(),
                ),
            )
        } catch (e: Exception) {
            System.err.println("Failed to notify MPRIS listeners of a seek: ${e.message}")
        }
    }

    fun close() {
        try {
            connection.unExportObject(OBJECT_PATH)
            connection.releaseBusName(SERVICE_NAME)
            connection.disconnect()
        } catch (e: Exception) {
            System.err.println("Failed to cleanly disconnect the MPRIS D-Bus service: ${e.message}")
        }
    }

    private fun trackObjectPath(songId: String): String {
        val sanitized = songId.map { char -> if (char.isLetterOrDigit() || char == '_') char else '_' }
            .joinToString("")
        return "/xyz/skifty/moonlight/track/$sanitized"
    }

}
