package xyz.skifty.moonlight.ui.components.nowplaying.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import xyz.skifty.moonlight.media.DesktopAudioPlayer

/**
 * A self-drawn seek bar, same approach as [MiniVolumeSlider] and for the same reason: Material3's
 * [androidx.compose.material3.Slider] doesn't let a consumer reliably control where its custom
 * thumb/track slots sit within its own bounds, which matters here since the track needs to sit
 * flush at the very top of the now-playing widget with no gap above it. The track sits at the top
 * of this composable's own bounds; the hover thumb is centered on the track's vertical midpoint,
 * which means it overflows slightly above this composable's own top edge (harmless - Compose
 * doesn't clip a Box's children to its own bounds by default).
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ProgressSlider(
    audioPlayer: DesktopAudioPlayer,
    positionMs: Long,
    maxLengthMs: Long,
    isDragging: Boolean,
    setProgress: (Long) -> Unit,
    setDragging: (Boolean) -> Unit,
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

    var isHovered by remember { mutableStateOf(false) }

    val thumbAlpha by animateFloatAsState(
        targetValue = if (isHovered) 1f else 0f,
        label = "progressThumbAlpha",
    )

    var widthPx by remember { mutableFloatStateOf(0f) }
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
        // seekFraction(), not seek(ms): switching this to an absolute-time seek was tried and made
        // seeking stop working entirely (always reset to the start) - reverted back to fraction-based,
        // which is confirmed to actually work for this stream even if the position display can
        // still glitch briefly (see the transient-reset guard in NowPlayingBottomWidget).
        setProgress((sliderFraction * maxLengthMs).toLong())
        audioPlayer.seekFraction(sliderFraction)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(thumbSize)
            .onSizeChanged { widthPx = it.width.toFloat() }
            .onPointerEvent(PointerEventType.Enter) { isHovered = true }
            .onPointerEvent(PointerEventType.Exit) { isHovered = false }
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
