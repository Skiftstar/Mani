package xyz.skifty.mani

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import org.koin.compose.koinInject
import xyz.skifty.mani.api.ApiService
import xyz.skifty.mani.media.AudioPlayer
import xyz.skifty.mani.media.PlaybackQueue
import xyz.skifty.mani.media.PlaylistLibrary
import xyz.skifty.mani.media.SongInfo
import xyz.skifty.mani.preferences.AppPreferences
import xyz.skifty.mani.security.SecureStorage
import xyz.skifty.mani.ui.screens.Screen

// A song counts as "listened to" for scrobbling once at least half of it, or this many ms of it
// (whichever is reached first), has actually been heard - see scrobbleIfNeeded below. Matches the
// same rule most scrobblers (e.g. last.fm) use.
private const val SCROBBLE_MIN_LISTEN_MS = 4 * 60 * 1000L

/** The platform-agnostic slice of app-shell state - which screen is active and the app-wide
 *  singletons every screen needs - built and driven by [rememberAppShellState]. Chrome (sidebar
 *  vs. bottom nav, desktop-only MPRIS effects, the Space-to-pause keybind, search-bar state) stays
 *  local to each platform's own composable (JvmApp/AndroidApp), not here. */
class AppShellState(
    val audioPlayer: AudioPlayer,
    val activeSongInfo: SongInfo,
    val playbackQueue: PlaybackQueue,
    val playlistLibrary: PlaylistLibrary,
    val apiService: ApiService,
    val secureStorage: SecureStorage,
    val appPreferences: AppPreferences,
) {

    // null = still checking for a saved session.
    var screen by mutableStateOf<Screen?>(null)
        internal set

    fun navigate(target: Screen) {
        screen = target
    }

    /** Clears the current session, both in-memory ([ApiService]) and persisted ([SecureStorage]),
     *  and returns to the login screen - the inverse of [rememberAppShellState]'s startup session
     *  restore. Storage deletion failures (e.g. no system keyring reachable) shouldn't block
     *  logging out - the in-memory session is cleared and the user is sent to Login regardless.
     *  Also stops playback and clears [activeSongInfo] - otherwise the mini-player/now-playing
     *  chrome (gated on activeSongInfo.songId != null) would keep showing over the login screen,
     *  still playing audio for whoever's about to log in next. */
    fun logout() {
        audioPlayer.stop(activeSongInfo)
        apiService.clearSession()
        runCatching {
            secureStorage.delete("mani_api_url")
            secureStorage.delete("mani_username")
            secureStorage.delete("mani_token")
            secureStorage.delete("mani_salt")
        }
        navigate(Screen.Login)
    }

}

/** Builds an [AppShellState] and runs its side effects - session restore, last-song/volume
 *  persistence, scrobbling, and the shared Coil `ImageLoader` setup (cover art loads over the same
 *  Ktor engine as the rest of the app) - once per platform's app-shell composable. This is logic
 *  that used to live only inside JvmApp.kt despite being entirely platform-agnostic; moved here so
 *  Android doesn't need (and can't accidentally drift from) a second copy of it. */
@Composable
fun rememberAppShellState(): AppShellState {
    val audioPlayer = koinInject<AudioPlayer>()
    val activeSongInfo = koinInject<SongInfo>()
    val playbackQueue = koinInject<PlaybackQueue>()
    val playlistLibrary = koinInject<PlaylistLibrary>()
    val apiService = koinInject<ApiService>()
    val secureStorage = koinInject<SecureStorage>()
    val appPreferences = koinInject<AppPreferences>()

    val state = remember {
        AppShellState(
            audioPlayer = audioPlayer,
            activeSongInfo = activeSongInfo,
            playbackQueue = playbackQueue,
            playlistLibrary = playlistLibrary,
            apiService = apiService,
            secureStorage = secureStorage,
            appPreferences = appPreferences,
        )
    }

    remember {
        SingletonImageLoader.setSafe { context ->
            ImageLoader.Builder(context)
                .components { add(KtorNetworkFetcherFactory()) }
                .build()
        }
    }

    LaunchedEffect(Unit) {
        state.screen = try {
            val url = secureStorage.get("mani_api_url")
            val user = secureStorage.get("mani_username")
            val token = secureStorage.get("mani_token")
            val salt = secureStorage.get("mani_salt")

            if (url != null && user != null && token != null && salt != null) {
                apiService.restoreSession(url, user, token, salt)
                if (apiService.ping().isSuccess) Screen.Home else Screen.Login
            } else {
                Screen.Login
            }
        } catch (e: Exception) {
            // Secure storage being unavailable (e.g. no system keyring reachable on
            // Linux) shouldn't crash startup - just fall back to a fresh login.
            println("Could not restore saved session: ${e.message}")
            Screen.Login
        }

        if (state.screen == Screen.Home) {
            // Restore the last-played song as paused, not auto-played - a stale id or an
            // unreachable server here shouldn't block startup either.
            runCatching {
                appPreferences.get("mani_last_song_id")?.let { lastSongId ->
                    audioPlayer.prepare(apiService.getSong(lastSongId), activeSongInfo)
                }
            }
        }
    }

    LaunchedEffect(activeSongInfo.songId) {
        activeSongInfo.songId?.let { songId ->
            runCatching { appPreferences.save("mani_last_song_id", songId) }
        }
    }

    LaunchedEffect(Unit) {
        runCatching {
            appPreferences.get("mani_volume")
                ?.toIntOrNull()
                ?.let { savedVolume -> audioPlayer.setVolume(savedVolume) }
        }
    }

    LaunchedEffect(audioPlayer.volume) {
        runCatching { appPreferences.save("mani_volume", audioPlayer.volume.toString()) }
    }

    // Guards against scrobbling the same song twice: a natural finish (trackFinishedCount, below)
    // and the queue's subsequent auto-advance (which also raises trackLeftCount, since it goes
    // through audioPlayer.play() like any other track change) would otherwise both try to
    // scrobble the same just-completed song.
    var lastScrobbledSongId by remember { mutableStateOf<String?>(null) }

    // Subsonic doesn't enforce a minimum-listen rule itself - scrobbling is meant to represent an
    // actual listen, so only report one once [listenedMs] (real elapsed playing time, not
    // position - see AudioPlayer.listenedMs()) crosses SCROBBLE_MIN_LISTEN_MS.
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
    // playlist's queue, or an explicit stop - see AudioPlayer's captureTrackLeft implementations.
    LaunchedEffect(audioPlayer.trackLeftCount) {
        audioPlayer.lastTrackLeft?.let { left -> scrobbleIfNeeded(left.songId, left.listenedMs, left.durationMs) }
    }

    return state
}
