package xyz.skifty.mani.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import xyz.skifty.mani.api.ApiService
import xyz.skifty.mani.media.PlaylistInfo
import xyz.skifty.mani.media.PlaylistLibrary
import xyz.skifty.mani.media.SongInfo

// No right-click concept on touch, and positionInWindow is never set on Android in the first
// place (Modifier.detectSecondaryClick's Android actual is a no-op) - see PlaylistSongRow's own
// doc comment.
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
    // Intentionally empty.
}
