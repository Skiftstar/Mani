package xyz.skifty.moonlight.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import moonlight.shared.generated.resources.Res
import moonlight.shared.generated.resources.cd_albums
import moonlight.shared.generated.resources.cd_history
import moonlight.shared.generated.resources.cd_home
import moonlight.shared.generated.resources.cd_liked_songs
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.moonlight.api.ApiService
import xyz.skifty.moonlight.media.PlaylistInfo
import xyz.skifty.moonlight.media.PlaylistLibrary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Sidebar(
    apiService: ApiService,
    playlistLibrary: PlaylistLibrary,
    onHomeClick: () -> Unit,
    onLikedSongsClick: () -> Unit,
    onPlaylistClick: (PlaylistInfo) -> Unit,
) {

    // Reads playlistLibrary directly rather than keeping its own separate fetch/state - anything
    // that refreshes playlistLibrary (e.g. adding a song to a playlist, which can change a
    // playlist's auto-derived cover art) is picked up here automatically too, since it's the same
    // Compose state either way.
    val playlists = playlistLibrary.playlists ?: emptyList()

    LaunchedEffect(Unit) {
        playlistLibrary.ensureLoaded(apiService)
    }

    Row(modifier = Modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .width(64.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(onClick = onHomeClick) {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = stringResource(Res.string.cd_home),
                )
            }
            IconButton(onClick = onLikedSongsClick) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = stringResource(Res.string.cd_liked_songs),
                )
            }
            IconButton(onClick = { /* TODO: history */ }) {
                Icon(
                    imageVector = Icons.Filled.History,
                    contentDescription = stringResource(Res.string.cd_history),
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items(playlists) { playlist ->
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                        tooltip = {
                            PlainTooltip {
                                Text(playlist.name)
                            }
                        },
                        state = rememberTooltipState(),
                    ) {
                        AsyncImage(
                            model = playlist.coverArtUrl,
                            contentDescription = playlist.name,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    onPlaylistClick(playlist)
                                },
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            }
        }
        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
