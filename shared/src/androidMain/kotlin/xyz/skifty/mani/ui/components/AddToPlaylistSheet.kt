package xyz.skifty.mani.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.context_menu_no_playlists
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.api.ApiService
import xyz.skifty.mani.media.PlaylistInfo
import xyz.skifty.mani.media.PlaylistLibrary

/** Mobile counterpart to desktop's `AddToPlaylistSubmenu` - a second bottom sheet listing every
 *  playlist to add a song to, shown after tapping "Add to Playlist" in [SongContextMenuSheet].
 *  Triggers [PlaylistLibrary.ensureLoaded] the first time it's shown, same as desktop, so the
 *  underlying fetch only ever happens once per session. No membership checkmarks, matching
 *  desktop - selecting a playlist the song is already in is a silent no-op (see
 *  `PlaylistSongTable.addToPlaylist`). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
    apiService: ApiService,
    playlistLibrary: PlaylistLibrary,
    onDismissRequest: () -> Unit,
    onSelectPlaylist: (PlaylistInfo) -> Unit,
) {
    LaunchedEffect(Unit) {
        playlistLibrary.ensureLoaded(apiService)
    }

    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        val playlists = playlistLibrary.playlists
        when {
            playlists == null -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            playlists.isEmpty() -> ListItem(
                headlineContent = { Text(stringResource(Res.string.context_menu_no_playlists)) },
            )

            else -> LazyColumn {
                items(playlists) { playlist ->
                    ListItem(
                        headlineContent = { Text(playlist.name) },
                        modifier = Modifier.clickable { onSelectPlaylist(playlist) },
                    )
                }
            }
        }
    }
}
