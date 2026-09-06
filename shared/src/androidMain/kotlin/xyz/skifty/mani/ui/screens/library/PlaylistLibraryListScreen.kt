package xyz.skifty.mani.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.cd_create_playlist
import mani.shared.generated.resources.cd_playlist_cover
import mani.shared.generated.resources.nav_playlists
import mani.shared.generated.resources.playlist_song_count
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.api.ApiService
import xyz.skifty.mani.media.PlaylistInfo
import xyz.skifty.mani.media.PlaylistLibrary
import xyz.skifty.mani.ui.components.CreatePlaylistDialog

/** Android's "Library" tab - the full-width equivalent of desktop's narrow icon-only Sidebar
 *  playlist rail, reading from the same [playlistLibrary] cache. */
@Composable
fun PlaylistLibraryListScreen(
    apiService: ApiService,
    playlistLibrary: PlaylistLibrary,
    onPlaylistClick: (PlaylistInfo) -> Unit,
) {
    val playlists = playlistLibrary.playlists ?: emptyList()
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        playlistLibrary.ensureLoaded(apiService)
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(Res.string.nav_playlists),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                )
                IconButton(onClick = { showCreatePlaylistDialog = true }) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(Res.string.cd_create_playlist),
                    )
                }
            }
        }
        items(playlists) { playlist ->
            PlaylistLibraryRow(
                playlist = playlist,
                onClick = { onPlaylistClick(playlist) },
            )
        }
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            apiService = apiService,
            playlistLibrary = playlistLibrary,
            onDismissRequest = { showCreatePlaylistDialog = false },
        )
    }
}

@Composable
private fun PlaylistLibraryRow(playlist: PlaylistInfo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AsyncImage(
            model = playlist.coverArtUrl,
            contentDescription = stringResource(Res.string.cd_playlist_cover),
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop,
        )
        Column {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = pluralStringResource(Res.plurals.playlist_song_count, playlist.songCount, playlist.songCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
