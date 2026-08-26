package xyz.skifty.mani.ui.components.nowplaying.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import xyz.skifty.mani.media.AudioPlayer

/**
 * A self-drawn seek bar, same approach as [MiniVolumeSlider] and for the same reason: Material3's
 * [androidx.compose.material3.Slider] doesn't let a consumer reliably control where its custom
 * thumb/track slots sit within its own bounds, which matters here since the track needs to sit
 * flush at the very top of the now-playing widget with no gap above it. The track sits at the top
 * of this composable's own bounds; the hover thumb is centered on the track's vertical midpoint,
 * which means it overflows slightly above this composable's own top edge (harmless - Compose
 * doesn't clip a Box's children to its own bounds by default).
 */
@Composable
fun ProgressSlider(
    audioPlayer: AudioPlayer,
    positionMs: Long,
    maxLengthMs: Long,
    isDragging: Boolean,
    setProgress: (Long) -> Unit,
    setDragging: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    trackHeight: Dp = 4.dp,
    thumbSize: Dp = 12.dp,
) {

    val fractionFromPlayer =
        if (maxLengthMs > 0) positionMs.toFloat() / maxLengthMs else 0f

    var sliderFraction by remember { mutableFloatStateOf(fractionFromPlayer) }

    LaunchedEffect(fractionFromPlayer) {
        if (!isDragging) {
            sliderFraction = fractionFromPlayer
        }
    }

    val hoverInteractionSource = remember { MutableInteractionSource() }
    val isHovered by hoverInteractionSource.collectIsHoveredAsState()

    // isDragging (not just isHovered) also reveals the thumb - hover never fires for a plain touch
    // drag (no touch equivalent of mouse hover), so without this the thumb would never appear at
    // all on a touch-only platform, even mid-drag.
    val thumbAlpha by animateFloatAsState(
        targetValue = if (isHovered || isDragging) 1f else 0f,
        label = "progressThumbAlpha",
    )

    var widthPx by remember { mutableFloatStateOf(0f) }
    // pointerInput(Unit) below sets up its gesture-handling coroutine once and never restarts it
    // (its key never changes) - reading the plain maxLengthMs parameter directly from inside would
    // permanently freeze commitSeek() to whatever value was current the very first time this
    // composable was placed (in practice, the very first track's length), silently wrong for every
    // track after that. rememberUpdatedState keeps a reference commitSeek() can read fresh through
    // even from that long-lived coroutine.
    val currentMaxLengthMs by rememberUpdatedState(maxLengthMs)
    val density = LocalDensity.current
    val thumbSizePx = with(density) { thumbSize.toPx() }
    // Centers the thumb on the track's vertical midpoint - negative since the thumb is taller
    // than the track, so it overflows a little above this composable's own top edge (Compose
    // doesn't clip a Box's children to its bounds by default, so that's fine).
    val thumbOffsetYPx = with(density) { ((trackHeight - thumbSize) / 2).toPx() }
    val clampedFraction = sliderFraction.coerceIn(0f, 1f)

    fun updateFromPointerX(x: Float) {
        sliderFraction = (x / widthPx).coerceIn(0f, 1f)
    }

    fun commitSeek() {
        // seekFraction(), not seek(ms): both are equally reliable on the current mpv-backed
        // player (unlike the previous vlcj backend, where this distinction mattered) - kept as a
        // fraction here purely because that's what this slider already tracks internally, sparing
        // it a duration lookup to convert to an absolute position.
        setProgress((sliderFraction * currentMaxLengthMs).toLong())
        audioPlayer.seekFraction(sliderFraction)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thumbSize)
            .onSizeChanged { widthPx = it.width.toFloat() }
            .hoverable(hoverInteractionSource)
            // Hand-rolled via awaitEachGesture rather than detectDragGestures/detectTapGestures:
            // both of those convenience detectors wait for the pointer to cross Compose's touch-slop
            // threshold before recognizing anything, which is right for scroll-style gestures that
            // should ignore small movements, but wrong for a slider - a plain click never crosses
            // that threshold (so detectDragGestures alone never fires for one at all), and the
            // position a *real* drag first reports is adjusted by however much slop it consumed
            // getting there, not the raw press position (a momentary snap-to-wrong-place before
            // real onDrag events correct it). Reading the raw position on every event from the very
            // first press sidesteps both.
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    setDragging(true)
                    updateFromPointerX(down.position.x)

                    var pointerId = down.id
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId }
                            ?: break
                        if (!change.pressed) {
                            break
                        }
                        change.consume()
                        updateFromPointerX(change.position.x)
                        pointerId = change.id
                    }

                    setDragging(false)
                    commitSeek()
                }
            },
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(trackHeight)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(clampedFraction)
                    .background(MaterialTheme.colorScheme.tertiary),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset {
                    IntOffset(
                        x = (clampedFraction * (widthPx - thumbSizePx)).roundToInt(),
                        y = thumbOffsetYPx.roundToInt(),
                    )
                }
                .size(thumbSize)
                .graphicsLayer {
                    alpha = thumbAlpha
                }
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiary),
        )
    }

}
