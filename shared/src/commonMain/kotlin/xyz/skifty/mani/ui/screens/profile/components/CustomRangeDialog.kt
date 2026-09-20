package xyz.skifty.mani.ui.screens.profile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.custom_range_cancel_button
import mani.shared.generated.resources.custom_range_placeholder
import mani.shared.generated.resources.custom_range_select_button
import mani.shared.generated.resources.custom_range_title
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.ext.popupContainer
import kotlin.time.Clock
import kotlin.time.Instant

private val HEADER_START_PADDING = 24.dp

/** "Custom range…"'s date picker (5b in the design) - a real [DateRangePicker] (its own From/To
 *  header + one calendar) rather than a hand-drawn grid, wrapped in a plain [Dialog] + [Surface]
 *  instead of [androidx.compose.material3.AlertDialog] since DateRangePicker wants more room than
 *  AlertDialog's slots give it (matches Material3's own DateRangePicker-in-dialog samples), styled
 *  to match [xyz.skifty.mani.ui.components.CreatePlaylistDialog]'s [popupContainer]/[TextButton]
 *  convention. Future days are disabled. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomRangeDialog(
    initialFrom: Instant?,
    initialTo: Instant?,
    onDismissRequest: () -> Unit,
    onRangeSelected: (from: Instant, to: Instant) -> Unit,
) {
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialFrom?.toEpochMilliseconds(),
        initialSelectedEndDateMillis = initialTo?.toEpochMilliseconds(),
        selectableDates = remember {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis <= Clock.System.now().toEpochMilliseconds()
            }
        },
    )

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.size(width = 360.dp, height = 568.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.popupContainer,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                DateRangePicker(
                    state = state,
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 12.dp),
                    colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.popupContainer),
                    // Both title and headline are overridden together so they share the same left
                    // edge (HEADER_START_PADDING) - Material3's own defaults don't align with each
                    // other once the headline below is replaced. The default headline also lays
                    // out "Start date - End date" in a way that wraps to two lines at this
                    // dialog's (deliberately not full-screen, see the class doc comment) 360dp
                    // width; a single left-aligned, single-line Text using our own short date
                    // formatting (already used elsewhere, e.g. RangeControl) fits comfortably
                    // instead.
                    title = {
                        Text(
                            text = stringResource(Res.string.custom_range_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = HEADER_START_PADDING, top = 16.dp),
                        )
                    },
                    headline = {
                        val startMillis = state.selectedStartDateMillis
                        val endMillis = state.selectedEndDateMillis
                        Text(
                            text = if (startMillis != null && endMillis != null) {
                                formatDateSpan(
                                    Instant.fromEpochMilliseconds(startMillis),
                                    Instant.fromEpochMilliseconds(endMillis),
                                )
                            } else {
                                stringResource(Res.string.custom_range_placeholder)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = HEADER_START_PADDING, end = 24.dp, bottom = 12.dp),
                        )
                    },
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(stringResource(Res.string.custom_range_cancel_button))
                    }
                    TextButton(
                        onClick = {
                            val startMillis = state.selectedStartDateMillis
                            val endMillis = state.selectedEndDateMillis
                            if (startMillis != null && endMillis != null) {
                                onRangeSelected(
                                    Instant.fromEpochMilliseconds(startMillis),
                                    Instant.fromEpochMilliseconds(endMillis),
                                )
                            }
                        },
                        enabled = state.selectedStartDateMillis != null && state.selectedEndDateMillis != null,
                    ) {
                        Text(stringResource(Res.string.custom_range_select_button))
                    }
                }
            }
        }
    }
}
