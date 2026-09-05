package xyz.skifty.mani.ui.screens.nowplaying.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import xyz.skifty.mani.media.VisualizerState
import xyz.skifty.mani.ui.theme.MainText
import xyz.skifty.mani.ui.theme.VisualizerAccent

// Ring radii, as a fraction of the canvas's own half-size (min(width, height) / 2), largest first -
// fixed, not audio-reactive (only each ring's own bars extend/retract - see drawSpectrumRing()).
// Gaps between rings are kept equal (0.22 apart) while scaling the whole trio up.
private const val OUTER_RING_RADIUS_FRACTION = 0.68f
private const val MIDDLE_RING_RADIUS_FRACTION = 0.46f
private const val INNER_RING_RADIUS_FRACTION = 0.24f

// A bar's half-length at zero energy (kept a flat, near-zero dp value - negligible on every ring
// either way, so it doesn't need to scale) and the extra half-length added at full (1f) energy - a
// bar spans [baseRadius - halfLength, baseRadius + halfLength] along its own radial direction, so
// it grows/shrinks symmetrically around the ring's own radius. That growth is expressed as a
// fraction of the ring's OWN radius, not a flat dp value - same reasoning as the bar-width
// constants below: a flat dp growth reads as a huge change on the (much smaller-radius) inner ring
// but barely registers on the outer ring. 0.19 was chosen to stay safely under the 0.22 gap between
// rings even for the outer ring (the largest radius), so growing bars can't reach into a
// neighboring ring even at full (1f) energy. Middle and outer each get their own fraction,
// deliberately different from inner's (rather than derived from a shared value) - middle should
// read as growing less than inner despite its own radius being larger, and outer's was toned down
// 10% on top of that same reasoning.
private const val BASE_BAR_HALF_LENGTH_DP = 0.19f
private const val INNER_MAX_EXTRA_HALF_LENGTH_FRACTION_OF_RADIUS = 0.19f
private const val MIDDLE_MAX_EXTRA_HALF_LENGTH_FRACTION_OF_RADIUS = 0.08f
private const val OUTER_MAX_EXTRA_HALF_LENGTH_FRACTION_OF_RADIUS = 0.171f

// Bar width is expressed as a fraction of each ring's OWN per-segment arc length (radius * angle
// between bars), not a flat dp value - the three rings have very different circumferences (the
// inner ring's is much smaller), so the same flat width would look cramped/overlapping on the
// inner ring while barely registering as a change on the much roomier outer ring. Expressing it
// relative to each ring's own available space keeps the bar-to-gap ratio (and how noticeable the
// energy-driven widening is) consistent across all three rings.
private const val BASE_STROKE_WIDTH_FRACTION_OF_ARC = 0.45f
private const val MAX_EXTRA_STROKE_WIDTH_FRACTION_OF_ARC = 0.35f

// Corner radius on each bar, as a fraction of that bar's own (energy-dependent) width - scales
// with it rather than being a flat dp value, for the same reason width/length already do. Well
// under 0.5 (which would make it a full pill/capsule, same as the StrokeCap.Round look this was
// deliberately moved away from) - just enough to soften the otherwise sharp rectangle corners.
private const val BAR_CORNER_RADIUS_FRACTION_OF_WIDTH = 0.3f

// One full rotation, in either direction - deliberately constant/idle, not audio-reactive (only bar
// length is - see the reference screenshot this was built from).
private const val ROTATION_PERIOD_MILLIS = 32_000

private const val TWO_PI = (2.0 * PI).toFloat()
private const val RADIANS_TO_DEGREES = (180.0 / PI).toFloat()

/** The Now Playing screen's audio visualizer - an alternative to [AlbumArt], toggled from
 *  ProfileScreen's "Show audio visualizer" switch. Three concentric rings of independent bars, all
 *  three drawing the same [visualizerState] spectrum (see
 *  [xyz.skifty.mani.media.AudioSessionVisualizer] for how each bar's length is derived from the
 *  actual audio being played) rather than each ring being restricted to its own frequency slice.
 *  Outer and inner rings are orange, the middle ring is white. Inner and middle rotate
 *  counter-clockwise; outer rotates clockwise - all three at the same constant speed, always running
 *  regardless of whether real capture data is flowing (bars simply stay flat/near-zero if it isn't)
 *  rather than falling back to
 *  cover art - a silent revert would be a more jarring, more confusing UI change than rings whose
 *  bars are just quiet. */
@Composable
fun AudioVisualizer(
    visualizerState: VisualizerState,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "audioVisualizerRotation")
    val rotationRadians by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = TWO_PI,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = ROTATION_PERIOD_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rotationRadians",
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val halfSize = minOf(size.width, size.height) / 2f
        val baseHalfLengthPx = BASE_BAR_HALF_LENGTH_DP.dp.toPx()

        drawSpectrumRing(
            center = center,
            baseRadius = halfSize * OUTER_RING_RADIUS_FRACTION,
            color = VisualizerAccent,
            baseHalfLengthPx = baseHalfLengthPx,
            maxExtraHalfLengthFraction = OUTER_MAX_EXTRA_HALF_LENGTH_FRACTION_OF_RADIUS,
            rotationRadians = rotationRadians,
            magnitudes = visualizerState.bars,
        )
        drawSpectrumRing(
            center = center,
            baseRadius = halfSize * MIDDLE_RING_RADIUS_FRACTION,
            color = MainText,
            baseHalfLengthPx = baseHalfLengthPx,
            maxExtraHalfLengthFraction = MIDDLE_MAX_EXTRA_HALF_LENGTH_FRACTION_OF_RADIUS,
            rotationRadians = -rotationRadians,
            magnitudes = visualizerState.bars,
        )
        drawSpectrumRing(
            center = center,
            baseRadius = halfSize * INNER_RING_RADIUS_FRACTION,
            color = VisualizerAccent,
            baseHalfLengthPx = baseHalfLengthPx,
            maxExtraHalfLengthFraction = INNER_MAX_EXTRA_HALF_LENGTH_FRACTION_OF_RADIUS,
            rotationRadians = -rotationRadians,
            magnitudes = visualizerState.bars,
        )
    }
}

private fun DrawScope.drawSpectrumRing(
    center: Offset,
    baseRadius: Float,
    color: Color,
    baseHalfLengthPx: Float,
    maxExtraHalfLengthFraction: Float,
    rotationRadians: Float,
    magnitudes: FloatArray,
) {
    val segmentCount = magnitudes.size
    if (segmentCount == 0) {
        return
    }
    val angleStep = TWO_PI / segmentCount

    // This ring's own available space - arc length per bar (radius * angle) for width, and the
    // radius itself for length growth - see the constants' own doc comments for why both are
    // derived from each ring's own dimensions instead of a flat dp value.
    val segmentArcLengthPx = baseRadius * angleStep
    val baseStrokeWidthPx = segmentArcLengthPx * BASE_STROKE_WIDTH_FRACTION_OF_ARC
    val maxExtraStrokeWidthPx = segmentArcLengthPx * MAX_EXTRA_STROKE_WIDTH_FRACTION_OF_ARC
    val maxExtraHalfLengthPx = baseRadius * maxExtraHalfLengthFraction

    for (index in 0 until segmentCount) {
        val angleRadians = rotationRadians + index * angleStep
        val magnitude = magnitudes[index]
        val halfLength = baseHalfLengthPx + magnitude * maxExtraHalfLengthPx
        val strokeWidthPx = baseStrokeWidthPx + magnitude * maxExtraStrokeWidthPx
        val cornerRadiusPx = strokeWidthPx * BAR_CORNER_RADIUS_FRACTION_OF_WIDTH

        // Each bar is a small axis-aligned rounded rect, drawn in its own rotated frame - rotating
        // the canvas per bar (rather than hand-computing a rotated rectangle's corners) is what lets
        // it stay simple. A stroked line's cap only offers fully flat (Butt), fully round (Round -
        // what made even a barely-there resting bar look like a big solid dot, which is why this
        // moved away from drawLine in the first place), or Square (flat but extended); a rounded
        // rect is the only way to get an actual small, adjustable corner radius.
        rotate(degrees = angleRadians * RADIANS_TO_DEGREES, pivot = center) {
            drawRoundRect(
                color = color,
                topLeft = Offset(center.x + baseRadius - halfLength, center.y - strokeWidthPx / 2f),
                size = Size(halfLength * 2f, strokeWidthPx),
                cornerRadius = CornerRadius(cornerRadiusPx),
            )
        }
    }
}
