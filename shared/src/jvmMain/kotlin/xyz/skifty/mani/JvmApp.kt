package xyz.skifty.mani

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import xyz.skifty.mani.ext.toLocale
import xyz.skifty.mani.i18n.AppLanguage
import xyz.skifty.mani.media.DesktopAudioPlayer
import java.util.Locale

import androidx.compose.foundation.layout.Row
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.playlist_liked_songs_title
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.media.mpris.MprisService
import xyz.skifty.mani.ui.components.AutoHidingScrollbar
import xyz.skifty.mani.ui.components.SearchBar
import xyz.skifty.mani.ui.components.Sidebar
import xyz.skifty.mani.ui.components.nowplaying.NowPlayingBottomWidget
import xyz.skifty.mani.ui.components.nowplayingpanel.NowPlayingPanel
import xyz.skifty.mani.ui.components.nowplayingpanel.QueuePanel
import xyz.skifty.mani.ui.components.util.LocalTextFieldFocusTracker
import xyz.skifty.mani.ui.components.util.TextFieldFocusTracker
import xyz.skifty.mani.ui.screens.Screen
import xyz.skifty.mani.ui.screens.home.HomeScreen
import xyz.skifty.mani.ui.screens.login.LoginScreen
import xyz.skifty.mani.ui.screens.login.components.LanguageDropdown
import xyz.skifty.mani.ui.screens.playlist.PlaylistScreen
import xyz.skifty.mani.ui.screens.search.SearchScreen
import xyz.skifty.mani.ui.theme.ManiTheme

// Below this window width, showing NowPlayingPanel alongside the sidebar would leave too little
// room for the main content to stay usable, so it's hidden entirely instead of shrinking further.
private val MIN_WINDOW_WIDTH_FOR_NOW_PLAYING_PANEL = 900.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun JvmApp() {

    // The platform-agnostic parts (session restore, last-song/volume persistence, scrobbling) live
    // in AppShellState now, shared with AndroidApp() - see its own doc comment.
    val appShellState = rememberAppShellState()
    val audioPlayer = appShellState.audioPlayer
    val activeSongInfo = appShellState.activeSongInfo
    val playbackQueue = appShellState.playbackQueue
    val playlistLibrary = appShellState.playlistLibrary
    val apiService = appShellState.apiService

    // Not Koin-resolved, unlike the above - Koin's single {} throws if its factory lambda returns
    // null (confirmed by hand: IllegalStateException "Single instance created couldn't return
    // value" at composition time), which constructing this needs to be able to do: only succeeds
    // on Linux with a reachable session bus, null elsewhere, tolerated like a missing OS keyring.
    val mprisService = remember {
        runCatching {
            MprisService(
                audioPlayer = audioPlayer,
                activeSongInfo = activeSongInfo,
                playbackQueue = playbackQueue,
            )
        }
            .onFailure { e ->
                System.err.println("MPRIS unavailable, continuing without it: $e")
                e.printStackTrace()
            }
            .getOrNull()
            // Notified directly from the exact call sites that confirm each event, rather than via
            // a LaunchedEffect watching seekCount/playbackStartedCount - see those properties' own
            // doc comments on DesktopAudioPlayer.
            ?.also { service ->
                (audioPlayer as? DesktopAudioPlayer)?.let { desktopAudioPlayer ->
                    desktopAudioPlayer.onSeeked = { service.notifySeeked() }
                    desktopAudioPlayer.onPlaybackStarted = { positionMs ->
                        service.notifyStateChanged()
                        service.notifySeeked(positionMs * 1000)
                    }
                }
            }
    }

    DisposableEffect(Unit) {
        onDispose {
            mprisService?.close()
            audioPlayer.release()
        }
    }


    var searchQuery by remember { mutableStateOf("") }
    // keep track of screen before searching so we can go back to it
    // when query is cleared
    var screenBeforeSearch by remember { mutableStateOf<Screen?>(null) }

    // flag to toggle between now playing and queue in side panel
    var isQueueViewActive by remember { mutableStateOf(false) }

    // custom navigate function since we need to reset search param
    // otherwise we'd be stuck on search screen
    fun navigate(target: Screen) {
        appShellState.navigate(target)
        searchQuery = ""
        screenBeforeSearch = null
    }

    // Wired to SearchBar's onQueryChange - synchronous (not a LaunchedEffect) so the empty <->
    // non-empty transition and the screen it triggers always happen together, with no
    // recomposition-timing gap between them.
    fun onSearchQueryChange(newQuery: String) {
        val wasEmpty = searchQuery.isEmpty()
        val isEmpty = newQuery.isEmpty()
        if (wasEmpty && !isEmpty) {
            if (appShellState.screen != Screen.Search) {
                screenBeforeSearch = appShellState.screen
            }
            appShellState.screen = Screen.Search
        } else if (!wasEmpty && isEmpty) {
            appShellState.screen = screenBeforeSearch ?: Screen.Home
            screenBeforeSearch = null
        }
        searchQuery = newQuery
    }

    // Compose resources pick their language from java.util.Locale.getDefault() fresh on every
    // lookup, so switching languages means changing that (there's no public per-composition
    // override in this Compose Multiplatform version) and then forcing recomposition via
    // key(appLanguage) below so already-composed stringResource() calls re-run.
    val systemDefaultLocale = remember { Locale.getDefault() }
    var appLanguage by remember { mutableStateOf(AppLanguage.SYSTEM) }

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

    // Space toggles play/pause from anywhere in the window, except while a text field has focus
    // (search box, login fields, and any added later - see LocalTextFieldFocusTracker).
    val textFieldFocusTracker = remember { TextFieldFocusTracker() }
    val rootFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        // Focus root on launch, so local keybind Space works
        rootFocusRequester.requestFocus()
    }

    key(appLanguage) {
        ManiTheme {
            CompositionLocalProvider(LocalTextFieldFocusTracker provides textFieldFocusTracker) {
                Column(
                    modifier = Modifier
                        .safeContentPadding()
                        .fillMaxSize()
                        // focus root instead of clearing focus so local keybind still works
                        // handle click on empty space to clear textfield focus
                        .pointerInput(Unit) {
                            detectTapGestures { rootFocusRequester.requestFocus() }
                        }
                        .focusRequester(rootFocusRequester)
                        .focusTarget()
                        // onPreviewKeyEvent consumes events first at root level, instead
                        // of onKeyEvent which consumes on component event first, so we use PreviewKeyEvent
                        // that way we dont accidentally hit a button or smth
                        .onPreviewKeyEvent { event ->
                            if (event.key != Key.Spacebar || textFieldFocusTracker.isAnyFieldFocused) {
                                return@onPreviewKeyEvent false
                            }
                            when (event.type) {
                                KeyEventType.KeyDown -> {
                                    playbackQueue.togglePlayPause()
                                    true
                                }

                                else -> false
                            }
                        },
                ) {
                    // Only shown on the login screen - the chosen language still applies
                    // everywhere else too (it's a Column-scoped state above, not tied to this
                    // row's visibility).
                    if (appShellState.screen == Screen.Login) {
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

                    // Main content takes all available space. Wrapped in BoxWithConstraints
                    // (rather than the plain Row this used to be) so NowPlayingPanel's visibility
                    // can depend on how much width is actually available - see
                    // MIN_WINDOW_WIDTH_FOR_NOW_PLAYING_PANEL.
                    BoxWithConstraints(
                        modifier = Modifier.weight(1f)
                            .fillMaxWidth(),
                    ) {
                        // isQueueViewActive forces the panel open regardless of window width -
                        // opening the queue view is a deliberate user action (the bottom widget's
                        // queue button), so it shouldn't be silently unreachable on a narrow window.
                        val showNowPlayingPanel = activeSongInfo.songId != null &&
                            (isQueueViewActive || maxWidth >= MIN_WINDOW_WIDTH_FOR_NOW_PLAYING_PANEL)

                        Row(modifier = Modifier.fillMaxSize()) {
                            if (appShellState.screen == Screen.Home || appShellState.screen == Screen.LikedSongs || appShellState.screen == Screen.Search || appShellState.screen is Screen.Playlist) {
                                Sidebar(
                                    apiService = apiService,
                                    playlistLibrary = playlistLibrary,
                                    onHomeClick = { navigate(Screen.Home) },
                                    onLikedSongsClick = { navigate(Screen.LikedSongs) },
                                    onPlaylistClick = { playlist ->
                                        navigate(Screen.Playlist(playlist.id, playlist.name))
                                    },
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f)
                                    .fillMaxHeight(),
                            ) {
                                // Hidden while still restoring a saved session (screen == null, no
                                // ApiService session yet) and on the login screen itself (searching
                                // would fail either way). Fixed here, above the scrollable Column
                                // below, so it stays visible at the top of the content area - next
                                // to the sidebar, not above it - regardless of how far the screen
                                // content itself is scrolled.
                                if (appShellState.screen != null && appShellState.screen != Screen.Login) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                    ) {
                                        SearchBar(
                                            query = searchQuery,
                                            onQueryChange = ::onSearchQueryChange,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                }

                                val scrollState = rememberScrollState()
                                Box(
                                    modifier = Modifier.weight(1f)
                                        .fillMaxWidth(),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(scrollState),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        when (val currentScreen = appShellState.screen) {
                                            null -> Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                CircularProgressIndicator()
                                            }

                                            Screen.Home -> HomeScreen(
                                                apiService = apiService,
                                                playbackQueue = playbackQueue,
                                            )
                                            Screen.Login -> LoginScreen(
                                                apiService,
                                                onLoginSuccess = { navigate(Screen.Home) },
                                            )

                                            Screen.LikedSongs -> PlaylistScreen(
                                                apiService = apiService,
                                                audioPlayer = audioPlayer,
                                                activeSongInfo = activeSongInfo,
                                                playbackQueue = playbackQueue,
                                                playlistLibrary = playlistLibrary,
                                                playlistId = null,
                                                playlistName = stringResource(Res.string.playlist_liked_songs_title),
                                            )

                                            is Screen.Playlist -> PlaylistScreen(
                                                apiService = apiService,
                                                audioPlayer = audioPlayer,
                                                activeSongInfo = activeSongInfo,
                                                playbackQueue = playbackQueue,
                                                playlistLibrary = playlistLibrary,
                                                playlistId = currentScreen.playlistId,
                                                playlistName = currentScreen.playlistName,
                                            )

                                            Screen.Search -> SearchScreen(
                                                apiService = apiService,
                                                audioPlayer = audioPlayer,
                                                activeSongInfo = activeSongInfo,
                                                playbackQueue = playbackQueue,
                                                playlistLibrary = playlistLibrary,
                                                query = searchQuery,
                                                scrollState = scrollState,
                                            )

                                            // Android-only destinations - desktop's Sidebar never
                                            // navigates to any of these, see Screen.kt.
                                            Screen.Library, Screen.Profile, Screen.NowPlaying, Screen.Queue -> Unit
                                        }
                                    }

                                    AutoHidingScrollbar(
                                        scrollState = scrollState,
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .fillMaxHeight(),
                                    )
                                }
                            }

                            if (showNowPlayingPanel) {
                                if (isQueueViewActive) {
                                    QueuePanel(
                                        playbackQueue = playbackQueue,
                                        onClose = { isQueueViewActive = false },
                                    )
                                } else {
                                    NowPlayingPanel(
                                        apiService = apiService,
                                        playlistLibrary = playlistLibrary,
                                        activeSongInfo = activeSongInfo,
                                        playbackQueue = playbackQueue,
                                    )
                                }
                            }
                        }
                    }

                    if (activeSongInfo.songId != null) {
                        NowPlayingBottomWidget(
                            audioPlayer = audioPlayer,
                            activeSongInfo = activeSongInfo,
                            playbackQueue = playbackQueue,
                            isQueueViewActive = isQueueViewActive,
                            onToggleQueueView = { isQueueViewActive = !isQueueViewActive },
                        )
                    }
                }
            }
        }
    }
}
