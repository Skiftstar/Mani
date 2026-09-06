package xyz.skifty.mani.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import xyz.skifty.mani.media.PlaylistInfo

@Composable
actual fun PlaylistContextMenuHost(
    positionInWindow: Offset?,
    onDismissRequest: () -> Unit,
    playlist: PlaylistInfo,
    onPlay: () -> Unit,
    onDeleteRequest: () -> Unit,
) {
    positionInWindow?.let { position ->
        PlaylistContextMenu(
            positionInWindow = position,
            onDismissRequest = onDismissRequest,
            playlist = playlist,
            onPlay = onPlay,
            onDeleteRequest = onDeleteRequest,
        )
    }
}
