package xyz.skifty.mani.ui.screens.profile.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.recap_axis_acoustic
import mani.shared.generated.resources.recap_axis_dance
import mani.shared.generated.resources.recap_axis_energy
import mani.shared.generated.resources.recap_axis_instrumental
import mani.shared.generated.resources.recap_axis_live
import mani.shared.generated.resources.recap_axis_positive
import mani.shared.generated.resources.recap_axis_spoken
import mani.shared.generated.resources.recap_taste_profile_title
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.models.RecapTasteProfile
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val GRID_RING_FRACTIONS = listOf(0.33f, 0.66f, 1f)

@Composable
private fun tasteProfileAxisValues(tasteProfile: RecapTasteProfile): List<Pair<String, Double>> =
    listOf(
        stringResource(Res.string.recap_axis_acoustic) to (tasteProfile.acousticness ?: 0.0),
        stringResource(Res.string.recap_axis_dance) to (tasteProfile.danceability ?: 0.0),
        stringResource(Res.string.recap_axis_energy) to (tasteProfile.energy ?: 0.0),
        stringResource(Res.string.recap_axis_instrumental) to (tasteProfile.instrumentalness ?: 0.0),
        stringResource(Res.string.recap_axis_live) to (tasteProfile.liveness ?: 0.0),
        stringResource(Res.string.recap_axis_spoken) to (tasteProfile.speechiness ?: 0.0),
        stringResource(Res.string.recap_axis_positive) to (tasteProfile.valence ?: 0.0),
    )

/** The Recap tab's 7-axis "taste profile" radar (3b/6b in the design), averaged over the range's
 *  songs by the backend (see [RecapTasteProfile]). Renders nothing when [RecapTasteProfile.trackCount]
 *  is 0 - a stock server, or a range with no VibeNet-tagged songs, same fallback spirit as
 *  [xyz.skifty.mani.ui.components.nowplayingpanel.components.PanelMoodTags] elsewhere in the app.
 *  Null axes (partial VibeNet coverage) plot as 0 rather than hiding the axis entirely. On wide
 *  (desktop) layouts, [showLegend] also renders the percentage breakdown below the chart (3b) -
 *  mobile's stacked layout (6b) omits it. */
@Composable
fun TasteProfileRadar(tasteProfile: RecapTasteProfile, modifier: Modifier = Modifier, showLegend: Boolean = false) {
    if (tasteProfile.trackCount <= 0) {
        return
    }

    val axisValues = tasteProfileAxisValues(tasteProfile)
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val fillColor = MaterialTheme.colorScheme.primary

    Column(modifier = modifier) {
        SectionHeader(stringResource(Res.string.recap_taste_profile_title))
        Canvas(
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .aspectRatio(1f),
        ) {
            val axisCount = axisValues.size
            val center = Offset(size.width / 2f, size.height / 2f)
            val labelMargin = 28.dp.toPx()
            val radius = (minOf(size.width, size.height) / 2f) - labelMargin

            fun pointAt(index: Int, fraction: Float): Offset {
                val angle = -PI / 2 + (2 * PI * index / axisCount)
                return Offset(
                    x = center.x + (radius * fraction * cos(angle)).toFloat(),
                    y = center.y + (radius * fraction * sin(angle)).toFloat(),
                )
            }

            for (ringFraction in GRID_RING_FRACTIONS) {
                val ringPath = Path().apply {
                    for (i in 0 until axisCount) {
                        val point = pointAt(i, ringFraction)
                        if (i == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                    }
                    close()
                }
                drawPath(ringPath, color = gridColor, style = Stroke(width = 1.dp.toPx()))
            }
            for (i in 0 until axisCount) {
                drawLine(gridColor, center, pointAt(i, 1f), strokeWidth = 1.dp.toPx())
            }

            val valuePath = Path().apply {
                for (i in 0 until axisCount) {
                    val fraction = axisValues[i].second.toFloat().coerceIn(0f, 1f)
                    val point = pointAt(i, fraction)
                    if (i == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                }
                close()
            }
            drawPath(valuePath, color = fillColor.copy(alpha = 0.25f))
            drawPath(valuePath, color = fillColor, style = Stroke(width = 2.dp.toPx()))

            for (i in 0 until axisCount) {
                val labelPoint = pointAt(i, 1.18f)
                val measured = textMeasurer.measure(axisValues[i].first, labelStyle)
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(
                        x = labelPoint.x - measured.size.width / 2f,
                        y = labelPoint.y - measured.size.height / 2f,
                    ),
                )
            }
        }

        if (showLegend) {
            Column(modifier = Modifier.padding(top = 14.dp)) {
                // Sorted descending by value, not fixed axis order, so the legend's ranking
                // agrees with the shape's own biggest lobes - matches 3b's design note.
                for ((axisLabel, axisValue) in axisValues.sortedByDescending { (_, value) -> value }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = axisLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "${(axisValue * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}
