package xyz.skifty.mani.media

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionResult
import org.koin.android.ext.android.inject

/** Owns the single ExoPlayer instance actually doing playback, as a foreground service so it
 *  keeps running whether or not any UI/MediaController is currently attached - Android's answer
 *  to desktop's own mpv subprocess outliving JvmApp's composition. [AndroidAudioPlayer] never
 *  touches this player directly; it only ever talks to it through a MediaController, the same way
 *  a remote MPRIS client talks to desktop's [DesktopAudioPlayer] through D-Bus rather than in
 *  process.
 *
 *  Deliberately a single-item player, not Media3's own multi-item playlist - [PlaybackQueue]
 *  already owns queue/shuffle/loop order on both platforms (mirroring how MprisService.Next()/
 *  Previous() already just delegate to it today), and letting ExoPlayer's own playlist run
 *  alongside it would mean two queue models that can drift out of sync. Next/previous requests
 *  (lock-screen, notification, headset buttons) are intercepted in [PlaybackSessionCallback] below
 *  and routed to that same [playbackQueue] instead of the player's own (empty) playlist. */
class PlaybackService : MediaSessionService() {

    private val playbackQueue: PlaybackQueue by inject()

    private lateinit var mediaSession: MediaSession

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(PlaybackSessionCallback(playbackQueue))
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = mediaSession

    override fun onDestroy() {
        mediaSession.player.release()
        mediaSession.release()
        super.onDestroy()
    }

}

/** Grants every connecting controller (the system media notification/lock screen included)
 *  next/previous commands regardless of the underlying single-item player's own timeline state
 *  (which would otherwise report neither as available - there's never a "next" item on the
 *  player's own empty playlist), then intercepts them before the player ever sees them, routing
 *  them to [playbackQueue] instead. */
private class PlaybackSessionCallback(
    private val playbackQueue: PlaybackQueue,
) : MediaSession.Callback {

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ): MediaSession.ConnectionResult {
        val availableCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS
            .buildUpon()
            .add(Player.COMMAND_SEEK_TO_NEXT)
            .add(Player.COMMAND_SEEK_TO_PREVIOUS)
            .build()
        return MediaSession.ConnectionResult.AcceptedResultBuilder(session, controller)
            .setAvailablePlayerCommands(availableCommands)
            .build()
    }

    // Deprecated in media3-session 1.11.0 with no direct replacement found (checked the library's
    // release notes and MediaSession.Callback's full method list by hand) - every other Callback
    // method either fires after the fact (onPlayerInteractionFinished) or serves an unrelated
    // purpose (onConnect, onCustomCommand, onSetMediaItems, ...); this remains the only hook that
    // can both intercept a command before the player acts on it *and* suppress that default
    // handling, which next/previous routing to playbackQueue needs.
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onPlayerCommandRequest(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        playerCommand: Int,
    ): Int {
        return when (playerCommand) {
            Player.COMMAND_SEEK_TO_NEXT -> {
                playbackQueue.next()
                SessionResult.RESULT_ERROR_NOT_SUPPORTED
            }

            Player.COMMAND_SEEK_TO_PREVIOUS -> {
                playbackQueue.previous()
                SessionResult.RESULT_ERROR_NOT_SUPPORTED
            }

            else -> SessionResult.RESULT_SUCCESS
        }
    }

}
