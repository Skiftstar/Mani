package xyz.skifty.mani.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import xyz.skifty.mani.media.SongInfo

private const val TILES_PER_ROW = 3
private const val TILES_PER_PAGE = TILES_PER_ROW * TILES_PER_ROW
private val GRID_SPACING = 8.dp
private val GRID_HORIZONTAL_PADDING = 16.dp
private val PAGE_DOT_SIZE = 6.dp
private val PAGE_DOT_SPACING = 6.dp

/** Android's paginated-3x3-grid style for the Favorites shelf, in place of [HomeShelf]'s
 *  horizontal-scrolling one - swiped 9 songs at a time via [HorizontalPager], each tile showing
 *  its title overlaid on the cover art (see [FavoriteGridTile]) rather than below it. */
@Composable
actual fun FavoritesShelf(
    title: String,
    songs: List<SongInfo>,
    onSongClick: (index: Int) -> Unit,
) {
    if (songs.isEmpty()) {
        return
    }

    val pages = songs.chunked(TILES_PER_PAGE)
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column {
        Text(
            text = title,
            style = homeShelfTitleStyle(),
            modifier = Modifier.padding(horizontal = GRID_HORIZONTAL_PADDING),
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        ) {
            val tileSize = (maxWidth - GRID_HORIZONTAL_PADDING * 2 - GRID_SPACING * (TILES_PER_ROW - 1)) / TILES_PER_ROW
            val pagerHeight = tileSize * TILES_PER_ROW + GRID_SPACING * (TILES_PER_ROW - 1)

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(pagerHeight),
            ) { page ->
                val pageSongs = pages[page]
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = GRID_HORIZONTAL_PADDING),
                    verticalArrangement = Arrangement.spacedBy(GRID_SPACING),
                ) {
                    for (rowStart in pageSongs.indices step TILES_PER_ROW) {
                        val row = pageSongs.subList(rowStart, minOf(rowStart + TILES_PER_ROW, pageSongs.size))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(GRID_SPACING),
                        ) {
                            row.forEachIndexed { indexInRow, songInfo ->
                                val indexInPage = rowStart + indexInRow
                                FavoriteGridTile(
                                    songInfo = songInfo,
                                    size = tileSize,
                                    onClick = { onSongClick(page * TILES_PER_PAGE + indexInPage) },
                                )
                            }
                            // Missing cells on a partial final row - an invisible same-size spacer
                            // instead of letting the real tiles stretch to fill the gap.
                            repeat(TILES_PER_ROW - row.size) {
                                Spacer(modifier = Modifier.size(tileSize))
                            }
                        }
                    }
                }
            }
        }

        if (pages.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(PAGE_DOT_SPACING, Alignment.CenterHorizontally),
            ) {
                repeat(pages.size) { page ->
                    val isCurrent = page == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .size(PAGE_DOT_SIZE)
                            .clip(CircleShape)
                            .background(
                                if (isCurrent) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            ),
                    )
                }
            }
        }
    }
}
