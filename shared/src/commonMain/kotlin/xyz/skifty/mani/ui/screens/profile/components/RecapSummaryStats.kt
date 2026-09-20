package xyz.skifty.mani.ui.screens.profile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.recap_stat_artists
import mani.shared.generated.resources.recap_stat_listened
import mani.shared.generated.resources.recap_stat_plays
import mani.shared.generated.resources.recap_stat_songs
import org.jetbrains.compose.resources.stringResource

private val NARROW_WIDTH_BREAKPOINT = 420.dp
private val WIDE_VALUE_FONT_SIZE = 30.sp
private val NARROW_VALUE_FONT_SIZE = 25.sp
private val STAT_UNIT_FONT_SIZE = 15.sp
private const val VALUE_LINE_HEIGHT_RATIO = 1.15f

/** The 4-stat row at the top of the Recap tab - Plays / Listened / Songs / Artists. Desktop's
 *  `.rc-stats` is a content-hugging flex row with a fixed 52dp gap between items; mobile's
 *  `.m-stats` is a strict 2-column, 50/50 CSS grid instead - not just tighter spacing, an actually
 *  different layout (each item fills its half exactly, rather than sitting flush left with empty
 *  space on the right), so the two are built as genuinely separate layouts rather than one
 *  spacing-only responsive tweak.
 *
 *  Listened's value mixes two font sizes ("26" at the full stat size, "h"/"m" smaller) while the
 *  other three are a single size - relying on both to happen to *measure* out to the same height
 *  turned out not to hold reliably, throwing Listened's label out of line with the other three's.
 *  Each value now renders inside a [valueSlotHeight]-tall, bottom-aligned [Box] instead, so every
 *  label starts at the same y by construction, regardless of what the value's own content measures
 *  to internally. */
@Composable
fun RecapSummaryStats(playCount: Long, listenedMinutes: Double, uniqueSongs: Long, uniqueArtists: Long, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier) {
        val isWide = maxWidth >= NARROW_WIDTH_BREAKPOINT
        val valueFontSize = if (isWide) WIDE_VALUE_FONT_SIZE else NARROW_VALUE_FONT_SIZE
        val valueLineHeight = valueFontSize * VALUE_LINE_HEIGHT_RATIO
        val valueStyle = MaterialTheme.typography.headlineSmall.copy(
            fontSize = valueFontSize,
            lineHeight = valueLineHeight,
        )
        val valueSlotHeight = with(LocalDensity.current) { valueLineHeight.toDp() }

        val valueContents: List<@Composable () -> Unit> = listOf(
            { PlainStatValue(playCount.toString(), valueStyle) },
            { ListenedStatValue(listenedMinutes, valueStyle) },
            { PlainStatValue(uniqueSongs.toString(), valueStyle) },
            { PlainStatValue(uniqueArtists.toString(), valueStyle) },
        )
        val labels = listOf(
            stringResource(Res.string.recap_stat_plays),
            stringResource(Res.string.recap_stat_listened),
            stringResource(Res.string.recap_stat_songs),
            stringResource(Res.string.recap_stat_artists),
        )

        if (isWide) {
            Row(horizontalArrangement = Arrangement.spacedBy(52.dp)) {
                for (i in valueContents.indices) {
                    StatItem(valueContents[i], labels[i], valueSlotHeight)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                for (rowIndices in valueContents.indices.chunked(2)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        for (i in rowIndices) {
                            StatItem(valueContents[i], labels[i], valueSlotHeight, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(valueContent: @Composable () -> Unit, label: String, valueSlotHeight: Dp, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier.height(valueSlotHeight),
            contentAlignment = Alignment.BottomStart,
        ) {
            valueContent()
        }
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.08.em),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PlainStatValue(value: String, style: TextStyle) {
    Text(text = value, style = style, fontWeight = FontWeight.SemiBold)
}

// Within this one line, "26"/"m" (the numbers) and "h"/"m" (the units) do need their own
// alignByBaseline() - that's legitimate, ordinary same-line baseline alignment (CSS's default
// inline vertical-align: baseline), unrelated to the cross-item height problem the outer Box in
// StatItem now handles.
@Composable
private fun ListenedStatValue(totalMinutes: Double, style: TextStyle) {
    val roundedMinutes = totalMinutes.toLong().coerceAtLeast(0)
    val hours = roundedMinutes / 60
    val minutes = roundedMinutes % 60
    val unitStyle = style.copy(fontSize = STAT_UNIT_FONT_SIZE)
    Row {
        if (hours > 0) {
            Text(text = hours.toString(), style = style, fontWeight = FontWeight.SemiBold, modifier = Modifier.alignByBaseline())
            Text(text = "h ", style = unitStyle, fontWeight = FontWeight.SemiBold, modifier = Modifier.alignByBaseline())
        }
        Text(text = minutes.toString(), style = style, fontWeight = FontWeight.SemiBold, modifier = Modifier.alignByBaseline())
        Text(text = "m", style = unitStyle, fontWeight = FontWeight.SemiBold, modifier = Modifier.alignByBaseline())
    }
}
