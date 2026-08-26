package xyz.skifty.mani.ui.screens.search

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.search_no_results
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.api.ApiService
import xyz.skifty.mani.media.DesktopAudioPlayer
import xyz.skifty.mani.media.PlaybackQueue
import xyz.skifty.mani.media.PlaylistLibrary
import xyz.skifty.mani.media.SongInfo
import xyz.skifty.mani.ui.components.PlaylistSongTable

// How long to wait after the user stops typing before actually calling search3() - short enough
// to feel responsive, long enough to avoid firing a request per keystroke.
private const val SEARCH_DEBOUNCE_MS = 300L

// Songs fetched per page, both for the initial load and each subsequent infinite-scroll page.
private const val SEARCH_PAGE_SIZE = 25

/** Shown whenever the top search bar's query is non-empty (see JvmApp) - debounces [query] and
 *  renders matching songs via search3 (songs only; no album/artist results are fetched), loading
 *  further pages as [scrollState] (shared with the rest of the app's content area, since this
 *  screen doesn't own its own scrollable container) nears its bottom.
 *  Clicking a result clears the queue and plays only that song, rather than queuing the rest of
 *  the result set. */
@Composable
fun SearchScreen(
    apiService: ApiService,
    audioPlayer: DesktopAudioPlayer,
    activeSongInfo: SongInfo,
    playbackQueue: PlaybackQueue,
    playlistLibrary: PlaylistLibrary,
    query: String,
    scrollState: ScrollState,
) {

    // null = the initial page for the current query is in flight (including the debounce wait);
    // non-null = songs loaded so far, possibly empty - mirrors PlaylistScreen's
    // `details: PlaylistDetails?`. Further pages are appended in place once fetched.
    var results by remember { mutableStateOf<List<SongInfo>?>(null) }

    // True while a *subsequent* page is being fetched - kept separate from `results == null`
    // (which only means "no page has loaded yet at all") so the infinite-scroll trigger below
    // doesn't fire a second overlapping request for the same page.
    var isLoadingMore by remember { mutableStateOf(false) }

    // False once a fetched page comes back with fewer than SEARCH_PAGE_SIZE songs - the standard
    // "short page means we've reached the end" signal, avoiding pointless further requests.
    var hasMore by remember { mutableStateOf(true) }

    LaunchedEffect(query) {
        hasMore = true
        isLoadingMore = false
        if (query.isBlank()) {
            results = emptyList()
            return@LaunchedEffect
        }
        results = null
        delay(SEARCH_DEBOUNCE_MS)
        val firstPage = apiService.search3(query, songCount = SEARCH_PAGE_SIZE)
        results = firstPage
        hasMore = firstPage.size == SEARCH_PAGE_SIZE
    }

    // Fires whenever the shared scroll position changes, or whenever more results just landed
    // (a page that still doesn't fill the viewport needs to immediately trigger the next one,
    // since the user has no more room left to scroll and re-trigger this themselves).
    LaunchedEffect(scrollState.value, results) {
        val currentResults = results
            ?: return@LaunchedEffect
        if (!hasMore || isLoadingMore) {
            return@LaunchedEffect
        }
        if (scrollState.value < scrollState.maxValue) {
            return@LaunchedEffect
        }
        isLoadingMore = true
        val nextPage = apiService.search3(query, songCount = SEARCH_PAGE_SIZE, songOffset = currentResults.size)
        results = currentResults + nextPage
        hasMore = nextPage.size == SEARCH_PAGE_SIZE
        isLoadingMore = false
    }

    val currentResults = results
    when {
        currentResults == null ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

        currentResults.isEmpty() ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.search_no_results),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

        else ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                PlaylistSongTable(
                    songs = currentResults,
                    audioPlayer = audioPlayer,
                    activeSongInfo = activeSongInfo,
                    apiService = apiService,
                    playbackQueue = playbackQueue,
                    playlistLibrary = playlistLibrary,
                    onSongClick = { index ->
                        // Clears the queue and plays only the clicked song - a singleton list
                        // makes hasNext/hasPrevious correctly report false.
                        playbackQueue.start(
                            newSongs = listOf(currentResults[index]),
                            startIndex = 0,
                            sourceId = null,
                        )
                    },
                )

                if (isLoadingMore) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }
    }
}
