package xyz.skifty.moonlight.ui.components.nowplaying.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * A minimal, self-contained slider for the volume control.
 *
 * Material3's [androidx.compose.material3.Slider] positions its custom `thumb`/`track` slots via
 * internal layout logic that doesn't line them up on their shared axis when given a small,
 * non-default thumb/track size - the thumb consistently rendered a couple of pixels off from the
 * track's center no matter how the slots were sized or aligned. Rather than fight that, this
 * places both directly ourselves in one Box we fully control, so centering is exact by construction.
 */
@Composable
fun MiniVolumeSlider(
    fraction: Float,
    onFractionChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    trackHeight: Dp = 4.dp,
    thumbSize: Dp = 12.dp,
) {
    var widthPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val thumbSizePx = with(density) { thumbSize.toPx() }
    val clampedFraction = fraction.coerceIn(0f, 1f)

    fun updateFromPointerX(x: Float) {
        val travel = widthPx - thumbSizePx
        if (travel > 0f) {
            onFractionChange(((x - thumbSizePx / 2f) / travel).coerceIn(0f, 1f))
        }
    }

    Box(
        modifier = modifier
            .height(thumbSize)
            .onSizeChanged { widthPx = it.width.toFloat() }
            .pointerInput(Unit) {
                detectTapGestures { offset -> updateFromPointerX(offset.x) }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    updateFromPointerX(change.position.x)
                }
            },
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(trackHeight)
                .clip(RoundedCornerShape(trackHeight / 2))
                .background(MaterialTheme.colorScheme.outlineVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(clampedFraction)
                    .clip(RoundedCornerShape(trackHeight / 2))
                    .background(MaterialTheme.colorScheme.tertiary),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset {
                    IntOffset(
                        x = (clampedFraction * (widthPx - thumbSizePx)).roundToInt(),
                        y = 0,
                    )
                }
                .size(thumbSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiary),
        )
    }
}
