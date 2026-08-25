package xyz.skifty.moonlight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import kotlinx.coroutines.delay
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.skifty.moonlight.api.ApiService
import xyz.skifty.moonlight.ext.toLocale
import xyz.skifty.moonlight.i18n.AppLanguage
import xyz.skifty.moonlight.media.SongInfo
import xyz.skifty.moonlight.preferences.AppPreferencesFactory
import xyz.skifty.moonlight.security.SecureStorageFactory
import java.util.Locale

import androidx.compose.foundation.layout.Row
import moonlight.shared.generated.resources.Res
import moonlight.shared.generated.resources.playlist_liked_songs_title
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.moonlight.media.DesktopAudioPlayer
import xyz.skifty.moonlight.media.PlaybackQueue
import xyz.skifty.moonlight.media.mpris.MprisService
import xyz.skifty.moonlight.ui.components.Sidebar
import xyz.skifty.moonlight.ui.components.nowplaying.NowPlayingBottomWidget
import xyz.skifty.moonlight.ui.screens.Screen
import xyz.skifty.moonlight.ui.screens.home.HomeScreen
import xyz.skifty.moonlight.ui.screens.login.LoginScreen
import xyz.skifty.moonlight.ui.screens.login.components.LanguageDropdown
import xyz.skifty.moonlight.ui.screens.playlist.PlaylistScreen
import xyz.skifty.moonlight.ui.theme.MoonlightTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun JvmApp() {

    val audioPlayer = remember { DesktopAudioPlayer() }
    val activeSongInfo = remember { SongInfo() }
    val playbackQueue = remember {
        PlaybackQueue(
            audioPlayer = audioPlayer,
            activeSongInfo = activeSongInfo,
        )
    }
    val apiService = remember { ApiService() }
    val secureStorage = remember { SecureStorageFactory.create() }
    val appPreferences = remember { AppPreferencesFactory.create() }
    // Only succeeds on Linux with a reachable session bus - null elsewhere, tolerated like a
    // missing OS keyring is below.
    val mprisService = remember {
        runCatching {
            MprisService(
                audioPlayer = audioPlayer,
                activeSongInfo = activeSongInfo,
                playbackQueue = playbackQueue,
            )
        }
            .getOrNull()
    }

    DisposableEffect(Unit) {
        onDispose {
            mprisService?.close()
            // Unlike the previous vlcj-backed player, this now owns a spawned mpv subprocess -
            // releasing it here (rather than never, as before) keeps a normal app quit from
            // orphaning it.
            audioPlayer.release()
        }
    }

    remember {
        SingletonImageLoader.setSafe { context ->
            ImageLoader.Builder(context)
                .components { add(KtorNetworkFetcherFactory()) }
                .build()
        }
    }

    // null = still checking for a saved session
    var screen by remember { mutableStateOf<Screen?>(null) }

    // Compose resources pick their language from java.util.Locale.getDefault() fresh on every
    // lookup, so switching languages means changing that (there's no public per-composition
    // override in this Compose Multiplatform version) and then forcing recomposition via
    // key(appLanguage) below so already-composed stringResource() calls re-run.
    val systemDefaultLocale = remember { Locale.getDefault() }
    var appLanguage by remember { mutableStateOf(AppLanguage.SYSTEM) }

    LaunchedEffect(Unit) {
        screen = try {
            val url = secureStorage.get("moonlight_api_url")
            val user = secureStorage.get("moonlight_username")
            val token = secureStorage.get("moonlight_token")
            val salt = secureStorage.get("moonlight_salt")

            if (url != null && user != null && token != null && salt != null) {
                apiService.restoreSession(url, user, token, salt)
                if (apiService.ping().isSuccess) Screen.Home else Screen.Login
            } else {
                Screen.Login
            }
        } catch (e: Exception) {
            // Secure storage being unavailable (e.g. no system keyring reachable on
            // Linux) shouldn't crash startup - just fall back to a fresh login.
            System.err.println("Could not restore saved session: ${e.message}")
            Screen.Login
        }

        if (screen == Screen.Home) {
            // Restore the last-played song as paused, not auto-played - a stale id or an
            // unreachable server here shouldn't block startup either.
            runCatching {
                appPreferences.get("moonlight_last_song_id")?.let { lastSongId ->
                    audioPlayer.prepare(apiService.getSong(lastSongId), activeSongInfo)
                }
            }
        }
    }

    LaunchedEffect(activeSongInfo.songId) {
        activeSongInfo.songId?.let { songId ->
            runCatching { appPreferences.save("moonlight_last_song_id", songId) }
        }
    }

    LaunchedEffect(Unit) {
        runCatching {
            appPreferences.get("moonlight_volume")
                ?.toIntOrNull()
                ?.let { savedVolume -> audioPlayer.setVolume(savedVolume) }
        }
    }

    LaunchedEffect(audioPlayer.volume) {
        runCatching { appPreferences.save("moonlight_volume", audioPlayer.volume.toString()) }
    }

    LaunchedEffect(
        audioPlayer.isPlaying,
        audioPlayer.volume,
        activeSongInfo.songId,
        activeSongInfo.songName,
        activeSongInfo.songArtist,
        activeSongInfo.songCoverArtUrl,
        activeSongInfo.songDurationSeconds,
        playbackQueue.currentPosition,
        playbackQueue.shuffleEnabled,
        playbackQueue.loopMode,
    ) {
        mprisService?.notifyStateChanged()
    }

    // A track finishing naturally is the queue's cue to advance (or replay, on loop-one).
    LaunchedEffect(audioPlayer.trackFinishedCount) {
        if (audioPlayer.trackFinishedCount > 0) {
            playbackQueue.onTrackFinished()
        }
    }

    // Playback (re)starting is a new "start counting from here" reference point for widgets that
    // interpolate track position locally instead of polling it - see MprisService.notifySeeked().
    // Watches playbackStartedCount rather than isPlaying: skipping straight from one playing track
    // to another never actually flips isPlaying's value, so it wouldn't retrigger this effect.
    //
    // Re-sends notifyStateChanged() here too, immediately before notifySeeked(), rather than
    // relying on the LaunchedEffect above to have already done so: activeSongInfo's fields change
    // synchronously the instant a new track is requested, well before libVLC actually confirms
    // playback started - so that effect fires early, with the *new* track's metadata but no
    // matching position yet. Sending both together, in this order, right when the position is
    // finally trustworthy gives listeners one consistent "here's the new track, here's where it
    // actually is" update instead of racing two separately-timed broadcasts.
    LaunchedEffect(audioPlayer.playbackStartedCount) {
        if (audioPlayer.playbackStartedCount > 0) {
            mprisService?.notifyStateChanged()
            mprisService?.notifySeeked(audioPlayer.lastConfirmedStartPositionMs * 1000)
        }
    }

    // Same idea for an explicit seek (in-app progress slider or an MPRIS client) - covers both
    // uniformly since audioPlayer.seek()/seekFraction() bump seekCount regardless of who called them.
    LaunchedEffect(audioPlayer.seekCount) {
        if (audioPlayer.seekCount > 0) {
            mprisService?.notifySeeked()
        }
    }

    // Belt-and-braces re-anchor while playing: some MPRIS clients interpolate position locally
    // using their own clock, which can drift from the real audio clock over time between anchors -
    // periodically re-sending the live position bounds how far that can drift, and self-heals if a
    // client ever misses one of the event-driven notifySeeked() calls above.
    LaunchedEffect(audioPlayer.isPlaying) {
        while (audioPlayer.isPlaying) {
            mprisService?.notifySeeked()
            delay(3000)
        }
    }

    key(appLanguage) {
        MoonlightTheme {
            Column(
                modifier = Modifier
                    .safeContentPadding()
                    .fillMaxSize(),
            ) {
                // Only shown on the login screen - the chosen language still applies everywhere
                // else too (it's a Column-scoped state above, not tied to this row's visibility).
                if (screen == Screen.Login) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        LanguageDropdown(
                            selected = appLanguage,
                            onSelect = { language ->
                                Locale.setDefault(language.toLocale(systemDefaultLocale))
                                appLanguage = language
                            },
                        )
                    }
                }

                // Main content takes all available space
                Row(
                    modifier = Modifier.weight(1f)
                        .fillMaxWidth(),
                ) {
                    if (screen == Screen.Home || screen == Screen.LikedSongs || screen is Screen.Playlist) {
                        Sidebar(
                            apiService = apiService,
                            onHomeClick = { screen = Screen.Home },
                            onLikedSongsClick = { screen = Screen.LikedSongs },
                            onPlaylistClick = { playlist ->
                                screen = Screen.Playlist(playlist.id, playlist.name)
                            },
                        )
                    }

                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier.weight(1f)
                            .verticalScroll(scrollState)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        when (val currentScreen = screen) {
                            null -> Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }

                            Screen.Home -> HomeScreen()
                            Screen.Login -> LoginScreen(
                                apiService,
                                onLoginSuccess = { screen = Screen.Home },
                            )

                            Screen.LikedSongs -> PlaylistScreen(
                                apiService = apiService,
                                audioPlayer = audioPlayer,
                                activeSongInfo = activeSongInfo,
                                playbackQueue = playbackQueue,
                                playlistId = null,
                                playlistName = stringResource(Res.string.playlist_liked_songs_title),
                            )

                            is Screen.Playlist -> PlaylistScreen(
                                apiService = apiService,
                                audioPlayer = audioPlayer,
                                activeSongInfo = activeSongInfo,
                                playbackQueue = playbackQueue,
                                playlistId = currentScreen.playlistId,
                                playlistName = currentScreen.playlistName,
                            )
                        }
                    }
                }

                if (activeSongInfo.songId != null) {
                    NowPlayingBottomWidget(
                        audioPlayer = audioPlayer,
                        activeSongInfo = activeSongInfo,
                        playbackQueue = playbackQueue,
                    )
                }
            }
        }
    }
}
