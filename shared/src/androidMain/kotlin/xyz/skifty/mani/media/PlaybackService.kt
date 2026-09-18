package xyz.skifty.mani.media

import android.app.PendingIntent
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import androidx.compose.runtime.snapshotFlow
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.cd_loop
import mani.shared.generated.resources.cd_star
import mani.shared.generated.resources.cd_unstar
import org.jetbrains.compose.resources.getString
import org.koin.android.ext.android.inject
import xyz.skifty.mani.api.ApiService
import xyz.skifty.mani.ext.toggleStar
import xyz.skifty.mani.shared.R

private const val TAG = "PlaybackService"

private const val ACTION_TOGGLE_STAR = "xyz.skifty.mani.TOGGLE_STAR"
private const val ACTION_CYCLE_LOOP = "xyz.skifty.mani.CYCLE_LOOP"

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
    private val visualizerState: VisualizerState by inject()
    private val apiService: ApiService by inject()
    private val playlistLibrary: PlaylistLibrary by inject()
    private val activeSongInfo: SongInfo by inject()

    private lateinit var mediaSession: MediaSession
    private lateinit var player: ExoPlayer
    private lateinit var audioSessionVisualizer: AudioSessionVisualizer
    private lateinit var sessionCallback: PlaybackSessionCallback

    // Backs the notification's star/repeat custom actions below (both the state-change observer
    // and each tap's own handling in PlaybackSessionCallback.onCustomCommand) - cancelled in
    // onDestroy() along with everything else this service owns.
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setHandleAudioBecomingNoisy(true)
            .build()

        audioSessionVisualizer = AudioSessionVisualizer(
            context = this,
            visualizerState = visualizerState,
        )

        // Generated and assigned ourselves, rather than waiting on Player.Listener's
        // onAudioSessionIdChanged - ExoPlayer typically auto-assigns its own session id during
        // construction itself (i.e. inside the ExoPlayer.Builder(...).build() call above), which
        // would fire that one-shot callback before addListener() below could ever be reached,
        // silently missing it forever. Assigning our own id upfront sidesteps that race entirely -
        // we already know the id, no callback needed to learn it.
        val audioSessionId = getSystemService(AudioManager::class.java)
            ?.generateAudioSessionId()
            ?: C.AUDIO_SESSION_ID_UNSET
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) {
            Log.w(TAG, "Could not generate an audio session id - visualizer will stay idle.")
        } else {
            player.setAudioSessionId(audioSessionId)
            audioSessionVisualizer.attach(audioSessionId)
        }

        player.addListener(VisualizerCaptureListener(audioSessionVisualizer))

        sessionCallback = PlaybackSessionCallback(
            playbackQueue = playbackQueue,
            apiService = apiService,
            playlistLibrary = playlistLibrary,
            activeSongInfo = activeSongInfo,
            scope = serviceScope,
        )

        // MediaSession.Callback.onConnect()'s setAvailablePlayerCommands() only grants a *controller*
        // permission to ask for a command - it can't make the command itself actually available if
        // the real player disagrees. Media3's built-in notification (and any other command-
        // availability-driven UI) reads button enablement straight off the real player's own
        // Player.availableCommands, not off any per-controller override, so a plain
        // MediaSession.Builder(this, player) here left "skip forward" looking unavailable no matter
        // what onConnect() granted - the raw ExoPlayer never has a next item (single-item player, see
        // this class's own doc comment) and so never reports COMMAND_SEEK_TO_NEXT itself. Wrapping the
        // player before handing it to the session, so the *real* reported commands include next/
        // previous, is the actual fix - see NextPreviousAlwaysAvailablePlayer below.
        mediaSession = MediaSession.Builder(this, NextPreviousAlwaysAvailablePlayer(player))
            .setCallback(sessionCallback)
            .apply {
                // Tapping the notification/lock-screen artwork itself (not one of its action
                // buttons) should reopen the app, same as tapping its home-screen icon. Built off
                // packageManager's own launch intent rather than a direct MainActivity reference -
                // this class lives in :shared, which androidApp depends on, not the other way
                // around, so it can't name androidApp's MainActivity at compile time.
                packageManager.getLaunchIntentForPackage(packageName)?.let { launchIntent ->
                    setSessionActivity(
                        PendingIntent.getActivity(
                            this@PlaybackService,
                            0,
                            launchIntent,
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                        ),
                    )
                }
            }
            .build()

        observeCustomLayoutState()
    }

    // Keeps the notification's star/repeat action icons in sync with in-app state - activeSongInfo
    // .starred and playbackQueue.loopMode are the exact same Compose mutableStateOf properties
    // NowPlayingTitleRow/NowPlayingControls already read directly, just observed here outside a
    // composable via snapshotFlow instead.
    private fun observeCustomLayoutState() {
        serviceScope.launch {
            snapshotFlow { activeSongInfo.starred to playbackQueue.loopMode }
                .distinctUntilChanged()
                .collect {
                    mediaSession.setCustomLayout(buildCustomLayout())
                }
        }
    }

    private suspend fun buildCustomLayout(): List<CommandButton> = listOf(
        CommandButton.Builder(CommandButton.ICON_UNDEFINED)
            .setDisplayName(getString(if (activeSongInfo.starred) Res.string.cd_unstar else Res.string.cd_star))
            .setIconResId(if (activeSongInfo.starred) R.drawable.ic_star_filled else R.drawable.ic_star_outline)
            .setSessionCommand(SessionCommand(ACTION_TOGGLE_STAR, Bundle.EMPTY))
            .setEnabled(true)
            .build(),
        CommandButton.Builder(CommandButton.ICON_UNDEFINED)
            .setDisplayName(getString(Res.string.cd_loop))
            .setIconResId(
                // The in-app controls tell OFF/ALL apart by tint alone (see NowPlayingControls/
                // PlaybackButtons) - the system notification's action icons are rendered monochrome
                // by the platform, so that trick doesn't carry over here. ic_repeat_on's dot is the
                // notification-safe equivalent of that active-state tint.
                when (playbackQueue.loopMode) {
                    LoopMode.OFF -> R.drawable.ic_repeat
                    LoopMode.ALL -> R.drawable.ic_repeat_on
                    LoopMode.ONE -> R.drawable.ic_repeat_one
                },
            )
            .setSessionCommand(SessionCommand(ACTION_CYCLE_LOOP, Bundle.EMPTY))
            .setEnabled(true)
            .build(),
    )

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = mediaSession

    // MediaSessionService's own default onTaskRemoved() deliberately keeps a still-playing
    // session alive as a background service. This app wants the opposite: swiping Mani away from
    // recents should stop playback entirely, same as force-stopping it, rather than leaving an
    // orphaned service running. stopSelf() tears the service down, which triggers onDestroy()
    // below to release the player/session/visualizer the normal way.
    override fun onTaskRemoved(rootIntent: Intent?) {
        player.stop()
        stopSelf()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        audioSessionVisualizer.release()
        mediaSession.player.release()
        mediaSession.release()
        super.onDestroy()
    }

}

/** Ties [AudioSessionVisualizer]'s data capture to actual playback state - a plain Player.Listener,
 *  attached directly to the player (not routed through AndroidAudioPlayer's MediaController, which
 *  never exposes audioSessionId), since this is one-way telemetry off the real player, not
 *  playback control. */
private class VisualizerCaptureListener(
    private val audioSessionVisualizer: AudioSessionVisualizer,
) : Player.Listener {

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        audioSessionVisualizer.setCaptureEnabled(isPlaying)
    }

}

/** Makes [COMMAND_SEEK_TO_NEXT] (and, redundantly but harmlessly, [COMMAND_SEEK_TO_PREVIOUS] -
 *  Media3 already always reports that one available, even with nothing before the current item; it
 *  just restarts it) look available on the wrapped single-item player, which otherwise never has a
 *  next item to report (see [PlaybackService]'s own doc comment for why it's single-item in the
 *  first place). This is *reported* availability only, purely for any command-availability-driven
 *  UI (the system media notification, lock screen, headset buttons) to enable its "skip forward"
 *  button in the first place - actually calling it is separately intercepted, before it would ever
 *  reach this player, by [PlaybackSessionCallback.onPlayerCommandRequest] below, and rerouted to
 *  [PlaybackQueue] instead. */
private class NextPreviousAlwaysAvailablePlayer(
    player: Player,
) : ForwardingPlayer(player) {

    override fun getAvailableCommands(): Player.Commands =
        super.getAvailableCommands()
            .buildUpon()
            .add(Player.COMMAND_SEEK_TO_NEXT)
            .add(Player.COMMAND_SEEK_TO_PREVIOUS)
            .build()

    override fun isCommandAvailable(command: Int): Boolean =
        command == Player.COMMAND_SEEK_TO_NEXT ||
            command == Player.COMMAND_SEEK_TO_PREVIOUS ||
            super.isCommandAvailable(command)

    override fun hasNextMediaItem(): Boolean = true

}

/** Grants every connecting controller (the system media notification/lock screen included)
 *  next/previous commands, then intercepts them before the player ever sees them, routing them to
 *  [playbackQueue] instead. Also handles the notification's star/repeat custom actions - see
 *  [ACTION_TOGGLE_STAR]/[ACTION_CYCLE_LOOP] and [onCustomCommand] below. */
private class PlaybackSessionCallback(
    private val playbackQueue: PlaybackQueue,
    private val apiService: ApiService,
    private val playlistLibrary: PlaylistLibrary,
    private val activeSongInfo: SongInfo,
    private val scope: CoroutineScope,
) : MediaSession.Callback {

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ): MediaSession.ConnectionResult {
        val availableSessionCommands = SessionCommands.Builder()
            .add(SessionCommand(ACTION_TOGGLE_STAR, Bundle.EMPTY))
            .add(SessionCommand(ACTION_CYCLE_LOOP, Bundle.EMPTY))
            .build()
        val availablePlayerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS
            .buildUpon()
            .add(Player.COMMAND_SEEK_TO_NEXT)
            .add(Player.COMMAND_SEEK_TO_PREVIOUS)
            .build()
        return MediaSession.ConnectionResult.AcceptedResultBuilder(session, controller)
            .setAvailableSessionCommands(availableSessionCommands)
            .setAvailablePlayerCommands(availablePlayerCommands)
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

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle,
    ): ListenableFuture<SessionResult> {
        when (customCommand.customAction) {
            ACTION_TOGGLE_STAR -> {
                scope.launch {
                    apiService.toggleStar(activeSongInfo, playlistLibrary, playbackQueue)
                }
            }

            ACTION_CYCLE_LOOP -> {
                playbackQueue.cycleLoopMode()
            }
        }
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
    }

}
