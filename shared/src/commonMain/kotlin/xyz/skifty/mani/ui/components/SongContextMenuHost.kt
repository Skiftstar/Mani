package xyz.skifty.mani.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import xyz.skifty.mani.api.ApiService
import xyz.skifty.mani.media.PlaylistInfo
import xyz.skifty.mani.media.PlaylistLibrary
import xyz.skifty.mani.media.SongInfo

/** Renders a right-click context menu for a song at [positionInWindow] once one's been requested
 *  (null otherwise) - a no-op on platforms with no right-click concept at all. See jvmMain's
 *  actual, which renders the real desktop-only `SongContextMenu`. */
@Composable
expect fun SongContextMenuHost(
    positionInWindow: Offset?,
    onDismissRequest: () -> Unit,
    songInfo: SongInfo,
    apiService: ApiService,
    playlistLibrary: PlaylistLibrary,
    onPlay: () -> Unit,
    onAddToQueue: () -> Unit,
    onToggleStar: () -> Unit,
    onAddToPlaylist: (PlaylistInfo) -> Unit,
    onRemoveFromPlaylist: (() -> Unit)?,
)
