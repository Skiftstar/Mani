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
import xyz.skifty.mani.media.AudioPlayer
import xyz.skifty.mani.media.PlaybackQueue
import xyz.skifty.mani.media.PlaylistLibrary
import xyz.skifty.mani.media.SongInfo
import xyz.skifty.mani.ui.components.PlaylistSongTable
import xyz.skifty.mani.ui.components.playlistTableHorizontalPadding

// How long to wait after the user stops typing before actually calling search3()
private const val SEARCH_DEBOUNCE_MS = 300L

// Songs fetched per "page"
private const val SEARCH_PAGE_SIZE = 25

@Composable
fun SearchScreen(
    apiService: ApiService,
    audioPlayer: AudioPlayer,
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

    // true while loading another page of results
    var isLoadingMore by remember { mutableStateOf(false) }

    // additional fetching possible (more results than 1 page size)
    var hasMore by remember { mutableStateOf(true) }

    LaunchedEffect(query) {
        hasMore = true
        isLoadingMore = false
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
                    .padding(horizontal = playlistTableHorizontalPadding, vertical = 24.dp),
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
                        // Clears the queue and plays only the clicked song
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
