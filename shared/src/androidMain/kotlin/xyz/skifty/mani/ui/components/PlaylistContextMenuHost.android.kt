package xyz.skifty.mani.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import xyz.skifty.mani.media.PlaylistInfo

// Same reasoning as SongContextMenuHost.android.kt - positionInWindow is set by a long-press
// (see PlaylistLibraryRow's combinedClickable), and only its nullness matters here.
@Composable
actual fun PlaylistContextMenuHost(
    positionInWindow: Offset?,
    onDismissRequest: () -> Unit,
    playlist: PlaylistInfo,
    onPlay: () -> Unit,
    onDeleteRequest: () -> Unit,
) {
    if (positionInWindow != null) {
        PlaylistContextMenuSheet(
            onDismissRequest = onDismissRequest,
            playlist = playlist,
            onPlay = onPlay,
            onDeleteRequest = onDeleteRequest,
        )
    }
}
