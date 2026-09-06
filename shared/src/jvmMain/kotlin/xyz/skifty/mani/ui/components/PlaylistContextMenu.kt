package xyz.skifty.mani.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.context_menu_play
import mani.shared.generated.resources.playlist_context_menu_delete
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.ext.popupContainer
import xyz.skifty.mani.media.PlaylistInfo
import xyz.skifty.mani.ui.components.util.FixedPositionProvider

/** Right-click menu for a playlist icon in the sidebar - Play (replace the active queue with the
 *  whole playlist) and Delete (opens a confirmation dialog - see [DeletePlaylistDialog]). Same
 *  raw-[Popup]-via-[FixedPositionProvider] approach as [SongContextMenu], for the same reason. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PlaylistContextMenu(
    positionInWindow: Offset,
    onDismissRequest: () -> Unit,
    playlist: PlaylistInfo,
    onPlay: () -> Unit,
    onDeleteRequest: () -> Unit,
) {
    Popup(
        popupPositionProvider = remember(positionInWindow) {
            FixedPositionProvider(IntOffset(positionInWindow.x.toInt(), positionInWindow.y.toInt()))
        },
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        Surface(
            modifier = Modifier.width(IntrinsicSize.Max),
            shape = MaterialTheme.shapes.extraSmall,
            color = MaterialTheme.colorScheme.popupContainer,
            tonalElevation = 3.dp,
            shadowElevation = 3.dp,
        ) {
            Column {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.context_menu_play)) },
                    onClick = {
                        onPlay()
                        onDismissRequest()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.playlist_context_menu_delete)) },
                    onClick = {
                        onDeleteRequest()
                        onDismissRequest()
                    },
                )
            }
        }
    }
}
