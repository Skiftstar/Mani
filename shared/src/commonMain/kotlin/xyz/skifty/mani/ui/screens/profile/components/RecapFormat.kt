package xyz.skifty.mani.ui.screens.profile.components

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** "9h 38m" (or just "38m" under an hour) from a recap total, e.g. [xyz.skifty.mani.models.RecapSummary.totalMinutes]. */
fun formatListenedMinutes(totalMinutes: Double): String {
    val roundedMinutes = totalMinutes.toLong().coerceAtLeast(0)
    val hours = roundedMinutes / 60
    val minutes = roundedMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

/** "20 Mar 2026" - day, short month name, year - for a range's resolved endpoints. */
fun formatDateLabel(instant: Instant): String {
    val date = instant.toLocalDateTime(TimeZone.UTC).date
    val monthAbbreviation = date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    return "${date.day} $monthAbbreviation ${date.year}"
}

/** "20 Mar – 20 Sep 2026" - a resolved range's endpoints, joined with an en dash. */
fun formatDateSpan(from: Instant, to: Instant): String =
    "${formatDateLabel(from)} – ${formatDateLabel(to)}"

/** "111 days" - a custom range's applied span, once picked (5c in the design: the label switches
 *  from the resolved dates to this day-count). */
fun formatDayCount(from: Instant, to: Instant): String {
    val days = (to - from).inWholeDays
    return "$days days"
}
