package xyz.skifty.mani.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import xyz.skifty.mani.media.PlaylistInfo

/** Renders a right-click (desktop) or long-press (Android) context menu for [playlist] at
 *  [positionInWindow] once one's been requested (null otherwise) - see [SongContextMenuHost] for
 *  the identical pattern this mirrors. [onDeleteRequest] only opens the confirmation dialog; it
 *  doesn't delete anything itself - see [DeletePlaylistDialog]. */
@Composable
expect fun PlaylistContextMenuHost(
    positionInWindow: Offset?,
    onDismissRequest: () -> Unit,
    playlist: PlaylistInfo,
    onPlay: () -> Unit,
    onDeleteRequest: () -> Unit,
)
