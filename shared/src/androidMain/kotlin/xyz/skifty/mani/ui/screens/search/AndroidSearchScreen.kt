package xyz.skifty.mani.ui.screens.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.cd_clear_search
import mani.shared.generated.resources.cd_search
import mani.shared.generated.resources.search_placeholder
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.api.ApiService
import xyz.skifty.mani.media.AudioPlayer
import xyz.skifty.mani.media.PlaybackQueue
import xyz.skifty.mani.media.PlaylistLibrary
import xyz.skifty.mani.media.SongInfo

/** Android's "Search" tab - owns its own query field (desktop's equivalent, [SearchBar], lives in
 *  the app-wide top bar instead, since search is reached differently there) and scroll container
 *  (the shared [SearchScreen] doesn't own one itself, expecting to share whatever scrollable
 *  content area it's placed in - desktop's is the whole app's content column). */
@Composable
fun AndroidSearchScreen(
    apiService: ApiService,
    audioPlayer: AudioPlayer,
    activeSongInfo: SongInfo,
    playbackQueue: PlaybackQueue,
    playlistLibrary: PlaylistLibrary,
) {
    var query by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Fully rounded (pill-shaped) rather than Material3's default lightly-rounded outline -
        // the mobile search-bar convention (matching the reference design) rather than a desktop-
        // style form field. A filled TextField with its indicator line suppressed, not
        // OutlinedTextField, since the reference has no visible border either.
        TextField(
            value = query,
            onValueChange = { newQuery -> query = newQuery },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text(stringResource(Res.string.search_placeholder)) },
            leadingIcon = {
                Icon(imageVector = Icons.Filled.Search, contentDescription = stringResource(Res.string.cd_search))
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = stringResource(Res.string.cd_clear_search),
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(percent = 50),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
        ) {
            SearchScreen(
                apiService = apiService,
                audioPlayer = audioPlayer,
                activeSongInfo = activeSongInfo,
                playbackQueue = playbackQueue,
                playlistLibrary = playlistLibrary,
                query = query,
                scrollState = scrollState,
            )
        }
    }
}
