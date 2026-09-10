package xyz.skifty.mani.ui.screens.home.components

import androidx.compose.runtime.Composable
import xyz.skifty.mani.media.SongInfo

/** Desktop keeps the plain horizontal-scrolling shelf style - unlike Android, there's no request
 *  to change this one shelf's look here, so this just delegates straight through. */
@Composable
actual fun FavoritesShelf(
    title: String,
    songs: List<SongInfo>,
    onSongClick: (index: Int) -> Unit,
) {
    HomeShelf(
        title = title,
        songs = songs,
        onSongClick = onSongClick,
    )
}
