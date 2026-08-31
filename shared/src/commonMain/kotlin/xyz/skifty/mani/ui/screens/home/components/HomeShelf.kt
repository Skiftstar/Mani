package xyz.skifty.mani.ui.screens.home.components

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.cd_shelf_scroll_left
import mani.shared.generated.resources.cd_shelf_scroll_right
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.media.SongInfo

private val SHELF_ROW_HEIGHT = 190.dp

@Composable
fun HomeShelf(title: String, songs: List<SongInfo>, onSongClick: (index: Int) -> Unit) {
    if (songs.isEmpty()) {
        return
    }

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    fun scrollByPage(forward: Boolean) {
        val viewportWidth = lazyListState.layoutInfo.viewportSize.width.toFloat()
        val distance = if (forward) viewportWidth else -viewportWidth
        coroutineScope.launch {
            lazyListState.animateScrollBy(distance)
        }
    }

    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 12.dp),
        ) {
            if (showHomeShelfPageArrows) {
                IconButton(
                    onClick = { scrollByPage(forward = false) },
                    enabled = lazyListState.canScrollBackward,
                ) {
                    Icon(
                        imageVector = Icons.Filled.ChevronLeft,
                        contentDescription = stringResource(Res.string.cd_shelf_scroll_left),
                    )
                }
            }
            LazyRow(
                state = lazyListState,
                contentPadding = PaddingValues(horizontal = if (showHomeShelfPageArrows) 4.dp else 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(SHELF_ROW_HEIGHT),
            ) {
                itemsIndexed(songs) { index, songInfo ->
                    HomeSongTile(
                        songInfo = songInfo,
                        onClick = {
                            onSongClick(index)
                        },
                    )
                }
            }
            if (showHomeShelfPageArrows) {
                IconButton(
                    onClick = { scrollByPage(forward = true) },
                    enabled = lazyListState.canScrollForward,
                ) {
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = stringResource(Res.string.cd_shelf_scroll_right),
                    )
                }
            }
        }
    }
}
