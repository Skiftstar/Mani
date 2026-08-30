package xyz.skifty.mani.ui.screens.queue

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.queue_empty_state
import mani.shared.generated.resources.queue_title
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.media.PlaybackQueue
import xyz.skifty.mani.ui.components.QueueSongRow

// How far down (as a fraction of the screen's own height) a pull-past-the-top-of-the-list needs
// to go before it counts as a deliberate swipe-to-dismiss rather than a small overscroll that
// should snap back - mirrors NowPlayingScreen's own SWIPE_UP_THRESHOLD_FRACTION.
private const val SWIPE_DOWN_THRESHOLD_FRACTION = 0.12f

private val QUEUE_HORIZONTAL_MARGIN = 24.dp

/** The queue's upcoming songs - reached by swiping up from Now Playing when something's actually
 *  queued next (see AndroidApp's navigateToQueueSource()). Each row is clickable to skip straight
 *  to it ([PlaybackQueue.skipTo]) with its own remove button ([PlaybackQueue.removeAt]).
 *
 *  Swiping down dismisses back to Now Playing ([onSwipeDown]) - but only once the list is already
 *  scrolled to its top; a [NestedScrollConnection] (rather than NowPlayingScreen's plain
 *  drag-gesture approach, which has no scrollable content to conflict with) is what lets this
 *  gesture coexist with the list's own scrolling instead of hijacking it. */
@Composable
fun QueueScreen(
    playbackQueue: PlaybackQueue,
    onSwipeDown: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var containerHeightPx by remember { mutableIntStateOf(0) }
    val offsetY = remember { Animatable(0f) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val alreadyPulling = offsetY.value > 0f
                val isAtTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                val isDraggingDown = available.y > 0f
                if (!alreadyPulling && !(isAtTop && isDraggingDown)) {
                    return Offset.Zero
                }
                val newOffset = (offsetY.value + available.y).coerceAtLeast(0f)
                val consumed = newOffset - offsetY.value
                scope.launch { offsetY.snapTo(newOffset) }
                return Offset(0f, consumed)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (offsetY.value <= 0f) {
                    return Velocity.Zero
                }
                val threshold = containerHeightPx * SWIPE_DOWN_THRESHOLD_FRACTION
                if (offsetY.value > threshold) {
                    offsetY.animateTo(
                        targetValue = containerHeightPx.toFloat(),
                        animationSpec = tween(durationMillis = 220),
                    )
                    onSwipeDown()
                } else {
                    offsetY.animateTo(0f, animationSpec = tween(durationMillis = 200))
                }
                return Velocity.Zero
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size -> containerHeightPx = size.height }
            .offset { IntOffset(0, offsetY.value.roundToInt()) }
            .nestedScroll(nestedScrollConnection),
    ) {
        Text(
            text = stringResource(Res.string.queue_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = QUEUE_HORIZONTAL_MARGIN, vertical = 16.dp),
        )

        val upcoming = playbackQueue.upcoming
        if (upcoming.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.queue_empty_state),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
            ) {
                items(upcoming, key = { entry -> entry.position }) { entry ->
                    QueueSongRow(
                        songInfo = entry.song,
                        onClick = {
                            playbackQueue.skipTo(entry.position)
                        },
                        onRemove = {
                            playbackQueue.removeAt(entry.position)
                        },
                        modifier = Modifier.padding(horizontal = QUEUE_HORIZONTAL_MARGIN, vertical = 8.dp),
                    )
                }
            }
        }
    }
}
