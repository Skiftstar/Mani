package xyz.skifty.mani.ui.screens.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.recap_range_custom
import mani.shared.generated.resources.recap_range_last_30_days
import mani.shared.generated.resources.recap_range_last_6_months
import mani.shared.generated.resources.recap_range_last_year
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.media.RecapRange
import xyz.skifty.mani.media.resolve

/** The range dropdown at the top of the Recap tab (5a in the design) - a fixed list of presets,
 *  then a divider, then "Custom range…" which opens [CustomRangeDialog] instead of applying
 *  directly. The trailing line switches from the resolved dates to a day-count once a custom
 *  range is applied (5a -> 5c). Box-wrapped for the same reason [xyz.skifty.mani.ui.components.LanguageDropdown]
 *  is - DropdownMenu anchors to its nearest enclosing Box, not whatever precedes it. */
@Composable
fun RangeControl(range: RecapRange, onRangeSelected: (RecapRange) -> Unit, modifier: Modifier = Modifier) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showCustomDialog by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box {
            Row(
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                    .clickable { menuExpanded = true }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = rangeButtonLabel(range),
                    style = MaterialTheme.typography.labelLarge,
                )
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                )
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.recap_range_last_30_days)) },
                    onClick = {
                        menuExpanded = false
                        onRangeSelected(RecapRange.Last30Days)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.recap_range_last_6_months)) },
                    onClick = {
                        menuExpanded = false
                        onRangeSelected(RecapRange.Last6Months)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.recap_range_last_year)) },
                    onClick = {
                        menuExpanded = false
                        onRangeSelected(RecapRange.LastYear)
                    },
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.recap_range_custom)) },
                    onClick = {
                        menuExpanded = false
                        showCustomDialog = true
                    },
                )
            }
        }
        Text(
            text = rangeSubtitle(range),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (showCustomDialog) {
        val currentCustom = range as? RecapRange.Custom
        CustomRangeDialog(
            initialFrom = currentCustom?.from,
            initialTo = currentCustom?.to,
            onDismissRequest = { showCustomDialog = false },
            onRangeSelected = { from, to ->
                showCustomDialog = false
                onRangeSelected(RecapRange.Custom(from, to))
            },
        )
    }
}

/** The current selection's own short label - a preset's name, or a custom range's formatted date
 *  span - reused as-is for [SaveRecapActions]' generated playlist name. */
@Composable
fun rangeButtonLabel(range: RecapRange): String =
    when (range) {
        RecapRange.Last30Days -> stringResource(Res.string.recap_range_last_30_days)
        RecapRange.Last6Months -> stringResource(Res.string.recap_range_last_6_months)
        RecapRange.LastYear -> stringResource(Res.string.recap_range_last_year)
        is RecapRange.Custom -> formatDateSpan(range.from, range.to)
    }

// Presets show their resolved date span; a custom range shows its day-count instead, once
// applied - matches 5c's "the dates become the label" behavior.
private fun rangeSubtitle(range: RecapRange): String {
    val (from, to) = range.resolve()
    return if (range is RecapRange.Custom) formatDayCount(from, to) else formatDateSpan(from, to)
}
