package xyz.skifty.mani.ui.screens.home.components

import androidx.compose.runtime.Composable
import xyz.skifty.mani.media.SongInfo

/** The Home screen's "Your Favorites" shelf - same shape as [HomeShelf], but Android renders it
 *  as a paginated 3x3 grid instead of [HomeShelf]'s horizontal-scrolling style (see the actuals'
 *  own doc comments). Desktop's actual just delegates straight to [HomeShelf], unchanged. */
@Composable
expect fun FavoritesShelf(
    title: String,
    songs: List<SongInfo>,
    onSongClick: (index: Int) -> Unit,
)
