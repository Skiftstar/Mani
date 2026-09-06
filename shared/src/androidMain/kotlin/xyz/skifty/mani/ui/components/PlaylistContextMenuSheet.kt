package xyz.skifty.mani.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.context_menu_play
import mani.shared.generated.resources.playlist_context_menu_delete
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.media.PlaylistInfo

/** Mobile counterpart to desktop's `PlaylistContextMenu` - a [ModalBottomSheet] with the same two
 *  entries (Play, Delete), shown on a long-press instead of a right-click. Delete is tinted
 *  `colorScheme.error`, the standard destructive-action treatment, and only opens the
 *  confirmation dialog (see `DeletePlaylistDialog`) rather than deleting immediately. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistContextMenuSheet(
    onDismissRequest: () -> Unit,
    playlist: PlaylistInfo,
    onPlay: () -> Unit,
    onDeleteRequest: () -> Unit,
) {
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
            headlineContent = { Text(stringResource(Res.string.playlist_context_menu_delete)) },
            leadingContent = {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            colors = ListItemDefaults.colors(headlineColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.clickable {
                onDeleteRequest()
                onDismissRequest()
            },
        )
    }
}
