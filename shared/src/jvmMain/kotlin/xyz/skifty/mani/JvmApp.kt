package xyz.skifty.mani

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import kotlinx.coroutines.delay
import androidx.compose.ui.tooling.preview.Preview
import xyz.skifty.mani.ext.toLocale
import xyz.skifty.mani.i18n.AppLanguage
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
import xyz.skifty.mani.ui.components.util.LocalTextFieldFocusTracker
import xyz.skifty.mani.ui.components.util.TextFieldFocusTracker
import xyz.skifty.mani.ui.screens.Screen
import xyz.skifty.mani.ui.screens.home.HomeScreen
import xyz.skifty.mani.ui.screens.login.LoginScreen
import xyz.skifty.mani.ui.screens.login.components.LanguageDropdown
import xyz.skifty.mani.ui.screens.playlist.PlaylistScreen
import xyz.skifty.mani.ui.screens.search.SearchScreen
import xyz.skifty.mani.ui.theme.ManiTheme

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

    // Search bar text, hoisted here (not modeled as data on Screen.Search) since it's transient
    // input, not navigation state - see SearchBar/SearchScreen.
    var searchQuery by remember { mutableStateOf("") }

    // The screen that was active right before searchQuery went from empty to non-empty - restored
    // when searchQuery goes back to empty. null only before any search has started.
    var screenBeforeSearch by remember { mutableStateOf<Screen?>(null) }

    // All non-search navigation (sidebar clicks, login success) goes through this rather than
    // assigning appShellState.screen directly, so it can also drop any in-progress search -
    // without this, a sidebar click while mid-search would leave the screen and searchQuery out of
    // sync (content switches away from Search, but the search bar still shows the old query and
    // would restore the *pre-search* screen, not the one just navigated to, the next time it's
    // cleared).
    fun navigate(target: Screen) {
        appShellState.navigate(target)
        searchQuery = ""
        screenBeforeSearch = null
    }

    // Wired to SearchBar's onQueryChange - synchronous (not a LaunchedEffect) so the empty <->
    // non-empty transition and the screen it triggers always happen together, with no
    // recomposition-timing gap between them. Deleting the last character by hand and tapping the
    // trailing clear button both simply call this with "", so both behave identically.
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

    // Space toggles play/pause from anywhere in the window, except while a text field has focus
    // (search box, login fields, and any added later - see LocalTextFieldFocusTracker).
    val textFieldFocusTracker = remember { TextFieldFocusTracker() }
    var isSpaceKeyDown by remember { mutableStateOf(false) }
    val rootFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        // So the root already sits somewhere in the focus chain even before the user clicks
        // anything - onPreviewKeyEvent below only sees key events when focus is somewhere within
        // its own subtree.
        rootFocusRequester.requestFocus()
    }

    key(appLanguage) {
        ManiTheme {
            CompositionLocalProvider(LocalTextFieldFocusTracker provides textFieldFocusTracker) {
                Column(
                    modifier = Modifier
                        .safeContentPadding()
                        .fillMaxSize()
                        // Clicking empty space elsewhere in the app (not a text field, not some
                        // other focusable element - those consume the click themselves before it
                        // reaches here) unfocuses whatever text field currently has focus, rather
                        // than leaving it focused until something else is explicitly clicked.
                        // Moves focus back to the root itself (not focusManager.clearFocus(),
                        // which clears it entirely) - onPreviewKeyEvent below only sees key events
                        // when focus is somewhere within this Column's own subtree, so Space would
                        // stop working the moment nothing at all was focused.
                        .pointerInput(Unit) {
                            detectTapGestures { rootFocusRequester.requestFocus() }
                        }
                        .focusRequester(rootFocusRequester)
                        .focusTarget()
                        // onPreviewKeyEvent (top-down) rather than onKeyEvent (bottom-up): placed
                        // on the app's own root, above every screen in the focus hierarchy, so it
                        // sees Space regardless of what currently holds focus deeper in the tree -
                        // and consuming it here first (returning true) stops it from also reaching
                        // a focused button and triggering *that* button's own native
                        // Space-activates-click behavior, matching how Space behaves in essentially
                        // every media player: unconditional play/pause unless you're actively typing.
                        .onPreviewKeyEvent { event ->
                            if (event.key != Key.Spacebar || textFieldFocusTracker.isAnyFieldFocused) {
                                return@onPreviewKeyEvent false
                            }
                            when (event.type) {
                                // Holding Space down fires repeated KeyDown events at the OS's
                                // key-repeat rate - the isSpaceKeyDown latch means only the first
                                // one (until the next KeyUp) actually toggles playback.
                                KeyEventType.KeyDown -> {
                                    if (!isSpaceKeyDown) {
                                        isSpaceKeyDown = true
                                        audioPlayer.togglePlayPause()
                                    }
                                    true
                                }

                                KeyEventType.KeyUp -> {
                                    isSpaceKeyDown = false
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

                    // Main content takes all available space
                    Row(
                        modifier = Modifier.weight(1f)
                            .fillMaxWidth(),
                    ) {
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
                            // below, so it stays visible at the top of the content area - next to
                            // the sidebar, not above it - regardless of how far the screen content
                            // itself is scrolled.
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

                                        Screen.Home -> HomeScreen()
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
                                        Screen.Library, Screen.Profile, Screen.NowPlaying -> Unit
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
}
