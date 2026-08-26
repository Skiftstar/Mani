package xyz.skifty.mani.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import xyz.skifty.mani.api.ApiService
import xyz.skifty.mani.media.PlaylistInfo
import xyz.skifty.mani.media.PlaylistLibrary
import xyz.skifty.mani.media.SongInfo

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
    positionInWindow?.let { position ->
        SongContextMenu(
            positionInWindow = position,
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
