package xyz.skifty.mani

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.playlist_liked_songs_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import xyz.skifty.mani.media.VisualizerState
import xyz.skifty.mani.ui.components.BottomNavBar
import xyz.skifty.mani.ui.components.nowplaying.MiniPlayerBar
import xyz.skifty.mani.ui.screens.Screen
import xyz.skifty.mani.ui.screens.home.HomeScreen
import xyz.skifty.mani.ui.screens.library.PlaylistLibraryListScreen
import xyz.skifty.mani.ui.screens.login.LoginScreen
import xyz.skifty.mani.ui.screens.nowplaying.NowPlayingScreen
import xyz.skifty.mani.ui.screens.playlist.PlaylistScreen
import xyz.skifty.mani.ui.screens.profile.ProfileScreen
import xyz.skifty.mani.ui.screens.queue.QueueScreen
import xyz.skifty.mani.ui.screens.search.AndroidSearchScreen
import xyz.skifty.mani.ui.theme.ManiTheme

// Shared between MiniPlayerBar (in the Scaffold's bottomBar) and NowPlayingScreen's own player
// card (layered over the main content) - matching Modifier.sharedBounds() keys are how Compose's
// shared-element transition pairs the two across otherwise-unrelated places in the tree.
private const val NOW_PLAYING_SHARED_KEY = "now_playing_card"

/** The Android analog of desktop's JvmApp() - bottom nav + mini-player chrome instead of a
 *  sidebar, everything else (session restore, persistence, scrobbling) shared via
 *  [rememberAppShellState]. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AndroidApp() {
    val appShellState = rememberAppShellState()
    val screen = appShellState.screen
    val audioPlayer = appShellState.audioPlayer
    val activeSongInfo = appShellState.activeSongInfo
    val playbackQueue = appShellState.playbackQueue
    val playlistLibrary = appShellState.playlistLibrary
    val apiService = appShellState.apiService
    // Android-only readout of the actual audio session's FFT data - not part of AppShellState
    // itself since it's never bound on desktop (see VisualizerState/AudioSessionVisualizer's own
    // doc comments).
    val visualizerState = koinInject<VisualizerState>()

    // Which screen to return to on collapse - the one that was active right before Now Playing
    // was opened, same "remember what came before, restore it" shape as JvmApp's own
    // screenBeforeSearch.
    var screenBeforeNowPlaying by remember { mutableStateOf<Screen>(Screen.Home) }

    fun openNowPlaying() {
        screen?.let { current ->
            if (current != Screen.NowPlaying) {
                screenBeforeNowPlaying = current
            }
        }
        appShellState.navigate(Screen.NowPlaying)
    }

    fun collapseNowPlaying() {
        appShellState.navigate(screenBeforeNowPlaying)
    }

    // Where a swipe-up on the Now Playing screen lands. If there's actually something queued up
    // next, that's the Queue screen itself; otherwise, the same fallback as before this existed -
    // the playlist the current queue came from, the Liked Songs pseudo-playlist if it came from
    // there instead (currentSourceId == null but something *is* queued - see PlaybackQueue's own
    // doc comment), or Home if nothing's queued at all.
    fun navigateToQueueSource() {
        if (playbackQueue.hasNext) {
            appShellState.navigate(Screen.Queue)
            return
        }
        val sourceId = playbackQueue.currentSourceId
        val target = when {
            sourceId != null -> Screen.Playlist(
                playlistId = sourceId,
                playlistName = playlistLibrary.playlists?.firstOrNull { playlist -> playlist.id == sourceId }?.name ?: "",
            )

            playbackQueue.songs.isNotEmpty() -> Screen.LikedSongs
            else -> Screen.Home
        }
        appShellState.navigate(target)
    }

    ManiTheme {
        SharedTransitionLayout {
            Scaffold(
                bottomBar = {
                    Column {
                        AnimatedVisibility(visible = activeSongInfo.songId != null && screen != Screen.NowPlaying) {
                            MiniPlayerBar(
                                audioPlayer = audioPlayer,
                                activeSongInfo = activeSongInfo,
                                onExpand = ::openNowPlaying,
                                modifier = Modifier.sharedBounds(
                                    rememberSharedContentState(key = NOW_PLAYING_SHARED_KEY),
                                    animatedVisibilityScope = this@AnimatedVisibility,
                                ),
                            )
                        }
                        AnimatedVisibility(
                            visible = screen != null && screen != Screen.Login && screen != Screen.NowPlaying,
                        ) {
                            BottomNavBar(
                                selected = screen ?: Screen.Home,
                                onSelect = { target -> appShellState.navigate(target) },
                            )
                        }
                    }
                },
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize(),
                ) {
                    when (val currentScreen = screen) {
                        null -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }

                        Screen.Login -> LoginScreen(
                            apiService,
                            onLoginSuccess = { appShellState.navigate(Screen.Home) },
                        )

                        // Wrapped in its own scrolling Column, same as the Playlist/LikedSongs
                        // branches below - HomeScreen itself doesn't self-scroll (it's shared
                        // with JvmApp's Screen.Home branch, which already provides one).
                        Screen.Home -> Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                        ) {
                            HomeScreen(
                                apiService = apiService,
                                playbackQueue = playbackQueue,
                            )
                        }

                        Screen.Library -> PlaylistLibraryListScreen(
                            apiService = apiService,
                            playlistLibrary = playlistLibrary,
                            onPlaylistClick = { playlist ->
                                appShellState.navigate(Screen.Playlist(playlist.id, playlist.name))
                            },
                        )

                        Screen.Profile -> ProfileScreen(
                            showVisualizer = appShellState.showVisualizer,
                            onShowVisualizerChange = appShellState::setShowVisualizer,
                            onLogout = { appShellState.logout() },
                        )

                        is Screen.Playlist -> Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                        ) {
                            PlaylistScreen(
                                apiService = apiService,
                                audioPlayer = audioPlayer,
                                activeSongInfo = activeSongInfo,
                                playbackQueue = playbackQueue,
                                playlistLibrary = playlistLibrary,
                                playlistId = currentScreen.playlistId,
                                playlistName = currentScreen.playlistName,
                            )
                        }

                        Screen.LikedSongs -> Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                        ) {
                            PlaylistScreen(
                                apiService = apiService,
                                audioPlayer = audioPlayer,
                                activeSongInfo = activeSongInfo,
                                playbackQueue = playbackQueue,
                                playlistLibrary = playlistLibrary,
                                playlistId = null,
                                playlistName = stringResource(Res.string.playlist_liked_songs_title),
                            )
                        }

                        Screen.Search -> AndroidSearchScreen(
                            apiService = apiService,
                            audioPlayer = audioPlayer,
                            activeSongInfo = activeSongInfo,
                            playbackQueue = playbackQueue,
                            playlistLibrary = playlistLibrary,
                        )

                        // Reached only via navigateToQueueSource() below (swipe-up from Now
                        // Playing) - openNowPlaying() as the swipe-down-back callback (rather than
                        // navigating straight to Screen.NowPlaying) also records
                        // screenBeforeNowPlaying = Screen.Queue, so a later back-press from Now
                        // Playing correctly returns here too.
                        Screen.Queue -> QueueScreen(
                            playbackQueue = playbackQueue,
                            onSwipeDown = ::openNowPlaying,
                        )

                        // NowPlaying is handled below, layered over this Box instead of being one
                        // of these branches, so it can animate in/out via its own AnimatedVisibility
                        // rather than being structurally removed/re-added.
                        Screen.NowPlaying -> Unit
                    }

                    AnimatedVisibility(visible = screen == Screen.NowPlaying) {
                        NowPlayingScreen(
                            apiService = apiService,
                            audioPlayer = audioPlayer,
                            activeSongInfo = activeSongInfo,
                            playbackQueue = playbackQueue,
                            playlistLibrary = playlistLibrary,
                            showVisualizer = appShellState.showVisualizer,
                            visualizerState = visualizerState,
                            onCollapse = ::collapseNowPlaying,
                            onSwipeUp = ::navigateToQueueSource,
                            cardModifier = Modifier.sharedBounds(
                                rememberSharedContentState(key = NOW_PLAYING_SHARED_KEY),
                                animatedVisibilityScope = this@AnimatedVisibility,
                            ),
                        )
                    }
                }
            }
        }
    }
}
