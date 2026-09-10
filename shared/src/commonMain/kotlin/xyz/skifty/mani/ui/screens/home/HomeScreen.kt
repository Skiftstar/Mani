package xyz.skifty.mani.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.home_favorites_title
import mani.shared.generated.resources.home_liked_random_title
import mani.shared.generated.resources.home_random_songs_title
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.api.ApiService
import xyz.skifty.mani.media.PlaybackQueue
import xyz.skifty.mani.media.SongInfo
import xyz.skifty.mani.ui.screens.home.components.HomeShelf

// How many songs each shelf loads/shows - matches getRandomSongs' own size param, and doubles as
// the sample size for the client-side shuffle of the (usually much larger) Liked Songs shelf.
private const val HOME_SHELF_SONG_COUNT = 30

// Separate cap for the Favorites shelf - the user asked for up to the top 50 from the last 30
// days, not the same count as the other two shelves.
private const val HOME_FAVORITES_SONG_COUNT = 50

@Composable
fun HomeScreen(apiService: ApiService, playbackQueue: PlaybackQueue) {
    var favoriteSongs by remember { mutableStateOf<List<SongInfo>>(emptyList()) }
    var randomSongs by remember { mutableStateOf<List<SongInfo>>(emptyList()) }
    var likedRandomSongs by remember { mutableStateOf<List<SongInfo>>(emptyList()) }

    LaunchedEffect(Unit) {
        randomSongs = apiService.getRandomSongs(HOME_SHELF_SONG_COUNT)
        likedRandomSongs = apiService.getStarredSongs()
            .shuffled()
            .take(HOME_SHELF_SONG_COUNT)
        // Fetched last, not first, despite being shown first below - a custom, possibly slow or
        // unreachable endpoint (see getRecapTopSongs' own doc comment) shouldn't delay the two
        // shelves every server actually supports; visual order and fetch order don't need to
        // match since each shelf's state updates (and recomposes) independently.
        favoriteSongs = apiService.getRecapTopSongs(HOME_FAVORITES_SONG_COUNT)
    }

    Column(
        modifier = Modifier.padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // Hides itself entirely (see HomeShelf's own empty-list handling) on a server without
        // the custom getRecap endpoint - getRecapTopSongs() degrades to emptyList() rather than
        // throwing, see its own doc comment.
        HomeShelf(
            title = stringResource(Res.string.home_favorites_title),
            songs = favoriteSongs,
            onSongClick = { index ->
                playbackQueue.start(favoriteSongs, index, sourceId = null)
            },
        )
        HomeShelf(
            title = stringResource(Res.string.home_random_songs_title),
            songs = randomSongs,
            onSongClick = { index ->
                playbackQueue.start(randomSongs, index, sourceId = null)
            },
        )
        HomeShelf(
            title = stringResource(Res.string.home_liked_random_title),
            songs = likedRandomSongs,
            onSongClick = { index ->
                playbackQueue.start(likedRandomSongs, index, sourceId = null)
            },
        )
    }
}
