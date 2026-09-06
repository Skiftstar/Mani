package xyz.skifty.mani.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistRemove
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.context_menu_add_to_playlist
import mani.shared.generated.resources.context_menu_add_to_queue
import mani.shared.generated.resources.context_menu_like
import mani.shared.generated.resources.context_menu_play
import mani.shared.generated.resources.context_menu_remove_from_playlist
import mani.shared.generated.resources.context_menu_unlike
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.api.ApiService
import xyz.skifty.mani.media.PlaylistInfo
import xyz.skifty.mani.media.PlaylistLibrary
import xyz.skifty.mani.media.SongInfo

/** Mobile counterpart to desktop's `SongContextMenu` - a [ModalBottomSheet] with the same item
 *  set/order as desktop (Play, Add to Queue, Like/Unlike, Add to Playlist, and - only when
 *  [onRemoveFromPlaylist] is non-null - Remove from Playlist), shown on a long-press instead of a
 *  right-click. Every item dismisses the sheet after its action, except Add to Playlist, which
 *  swaps to [AddToPlaylistSheet] instead. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongContextMenuSheet(
    onDismissRequest: () -> Unit,
    songInfo: SongInfo,
    apiService: ApiService,
    playlistLibrary: PlaylistLibrary,
    onPlay: () -> Unit,
    onAddToQueue: () -> Unit,
    onToggleStar: () -> Unit,
    onAddToPlaylist: (PlaylistInfo) -> Unit,
    onRemoveFromPlaylist: (() -> Unit)?,
) {
    var isChoosingPlaylist by remember { mutableStateOf(false) }

    if (isChoosingPlaylist) {
        AddToPlaylistSheet(
            apiService = apiService,
            playlistLibrary = playlistLibrary,
            onDismissRequest = onDismissRequest,
            onSelectPlaylist = { playlist ->
                onAddToPlaylist(playlist)
                onDismissRequest()
            },
        )
    } else {
        ModalBottomSheet(onDismissRequest = onDismissRequest) {
            ListItem(
                headlineContent = { Text(stringResource(Res.string.context_menu_play)) },
                leadingContent = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                modifier = Modifier.clickable {
                    onPlay()
                    onDismissRequest()
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(Res.string.context_menu_add_to_queue)) },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null) },
                modifier = Modifier.clickable {
                    onAddToQueue()
                    onDismissRequest()
                },
            )
            ListItem(
                headlineContent = {
                    Text(
                        stringResource(
                            if (songInfo.starred) Res.string.context_menu_unlike else Res.string.context_menu_like,
                        ),
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = if (songInfo.starred) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                    )
                },
                modifier = Modifier.clickable {
                    onToggleStar()
                    onDismissRequest()
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(Res.string.context_menu_add_to_playlist)) },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
                modifier = Modifier.clickable { isChoosingPlaylist = true },
            )
            onRemoveFromPlaylist?.let { remove ->
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.context_menu_remove_from_playlist)) },
                    leadingContent = { Icon(Icons.Filled.PlaylistRemove, contentDescription = null) },
                    modifier = Modifier.clickable {
                        remove()
                        onDismissRequest()
                    },
                )
            }
        }
    }
}
