package xyz.skifty.moonlight

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
import xyz.skifty.moonlight.media.PlaylistLibrary
import xyz.skifty.moonlight.media.mpris.MprisService
import xyz.skifty.moonlight.ui.components.AutoHidingScrollbar
import xyz.skifty.moonlight.ui.components.SearchBar
import xyz.skifty.moonlight.ui.components.Sidebar
import xyz.skifty.moonlight.ui.components.nowplaying.NowPlayingBottomWidget
import xyz.skifty.moonlight.ui.components.util.LocalTextFieldFocusTracker
import xyz.skifty.moonlight.ui.components.util.TextFieldFocusTracker
import xyz.skifty.moonlight.ui.screens.Screen
import xyz.skifty.moonlight.ui.screens.home.HomeScreen
import xyz.skifty.moonlight.ui.screens.login.LoginScreen
import xyz.skifty.moonlight.ui.screens.login.components.LanguageDropdown
import xyz.skifty.moonlight.ui.screens.playlist.PlaylistScreen
import xyz.skifty.moonlight.ui.screens.search.SearchScreen
import xyz.skifty.moonlight.ui.theme.MoonlightTheme

// A song counts as "listened to" for scrobbling once at least half of it, or this many ms of it
// (whichever is reached first), has actually been heard - see scrobbleIfNeeded below. Matches
// the same rule most scrobblers (e.g. last.fm) use.
private const val SCROBBLE_MIN_LISTEN_MS = 4 * 60 * 1000L

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
    val playlistLibrary = remember { PlaylistLibrary() }
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
            .onFailure { e ->
                // Previously swallowed with zero diagnostic output - impossible to tell "no
                // session bus reachable" (expected/tolerated) apart from a real bug without this.
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

    remember {
        SingletonImageLoader.setSafe { context ->
            ImageLoader.Builder(context)
                .components { add(KtorNetworkFetcherFactory()) }
                .build()
        }
    }

    // null = still checking for a saved session
    var screen by remember { mutableStateOf<Screen?>(null) }

    // Search bar text, hoisted here (not modeled as data on Screen.Search) since it's transient
    // input, not navigation state - see SearchBar/SearchScreen.
    var searchQuery by remember { mutableStateOf("") }

    // The screen that was active right before searchQuery went from empty to non-empty - restored
    // when searchQuery goes back to empty. null only before any search has started.
    var screenBeforeSearch by remember { mutableStateOf<Screen?>(null) }

    // All non-search navigation (sidebar clicks, login success) goes through this rather than
    // assigning `screen` directly, so it can also drop any in-progress search - without this, a
    // sidebar click while mid-search would leave `screen` and `searchQuery` out of sync (content
    // switches away from Search, but the search bar still shows the old query and would restore
    // the *pre-search* screen, not the one just navigated to, the next time it's cleared).
    fun navigate(target: Screen) {
        screen = target
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
            if (screen != Screen.Search) {
                screenBeforeSearch = screen
            }
            screen = Screen.Search
        } else if (!wasEmpty && isEmpty) {
            screen = screenBeforeSearch ?: Screen.Home
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

    // Guards against scrobbling the same song twice: a natural finish (trackFinishedCount, below)
    // and the queue's subsequent auto-advance (which also raises trackLeftCount, since it goes
    // through audioPlayer.play() like any other track change) would otherwise both try to
    // scrobble the same just-completed song.
    var lastScrobbledSongId by remember { mutableStateOf<String?>(null) }

    // Subsonic doesn't enforce a minimum-listen rule itself - scrobbling is meant to represent an
    // actual listen, so only report one once [listenedMs] (real elapsed playing time, not
    // position - see DesktopAudioPlayer.listenedMs()) crosses SCROBBLE_MIN_LISTEN_MS.
    suspend fun scrobbleIfNeeded(songId: String?, listenedMs: Long, durationMs: Long) {
        if (songId == null || songId == lastScrobbledSongId || durationMs <= 0) {
            return
        }
        val thresholdMs = minOf(durationMs / 2, SCROBBLE_MIN_LISTEN_MS)
        if (listenedMs >= thresholdMs) {
            lastScrobbledSongId = songId
            apiService.scrobble(songId)
        }
    }

    // A track finishing naturally is the queue's cue to advance (or replay, on loop-one).
    LaunchedEffect(audioPlayer.trackFinishedCount) {
        if (audioPlayer.trackFinishedCount > 0) {
            scrobbleIfNeeded(activeSongInfo.songId, audioPlayer.listenedMs(), audioPlayer.length())
            playbackQueue.onTrackFinished()
        }
    }

    // Scrobbles whatever song was just left behind - a skip, a previous, starting a different
    // playlist's queue, or an explicit stop - see DesktopAudioPlayer.captureTrackLeft().
    LaunchedEffect(audioPlayer.trackLeftCount) {
        audioPlayer.lastTrackLeft?.let { left -> scrobbleIfNeeded(left.songId, left.listenedMs, left.durationMs) }
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
        MoonlightTheme {
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
                        if (screen == Screen.Home || screen == Screen.LikedSongs || screen == Screen.Search || screen is Screen.Playlist) {
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
                            if (screen != null && screen != Screen.Login) {
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
