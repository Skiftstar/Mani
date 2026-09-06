package xyz.skifty.mani.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import xyz.skifty.mani.api.ApiService
import xyz.skifty.mani.media.PlaylistInfo
import xyz.skifty.mani.media.PlaylistLibrary
import xyz.skifty.mani.media.SongInfo

// No right-click concept on touch - positionInWindow is set here by a long-press instead (see
// PlaylistSongRow's combinedClickable), and only its nullness matters: the actual coordinate is
// meaningless to a bottom sheet, which always anchors to the bottom of the screen regardless of
// where the press happened.
@Composable
actual fun SongContextMenuHost(
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
) {
    if (positionInWindow != null) {
        SongContextMenuSheet(
            onDismissRequest = onDismissRequest,
            songInfo = songInfo,
            apiService = apiService,
            playlistLibrary = playlistLibrary,
            onPlay = onPlay,
            onAddToQueue = onAddToQueue,
            onToggleStar = onToggleStar,
            onAddToPlaylist = onAddToPlaylist,
            onRemoveFromPlaylist = onRemoveFromPlaylist,
        )
    }
}
