package xyz.skifty.mani.media

import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/** Which window the Profile screen's Recap tab is showing - one of three fixed presets, or a
 *  user-picked [Custom] range from the "Custom range…" dialog. Deliberately doesn't carry [from]/
 *  [to] itself for the presets (see [resolve]) so a long-lived selection always reflects "as of
 *  now" rather than freezing at the moment it was first picked.
 *
 *  [Custom.to] is the *day* the user picked as the end of the range, not an instant - it comes
 *  straight from [androidx.compose.material3.DateRangePickerState], which reports both endpoints
 *  as UTC midnight of their respective day (so picking the same day for both fields would
 *  otherwise resolve to a zero-width, midnight-to-that-same-midnight window). [resolve] is where
 *  that day gets turned into an actual query boundary, one day later, so the selected end day is
 *  included in full - see its own doc comment. */
sealed class RecapRange {
    data object Last30Days : RecapRange()
    data object Last6Months : RecapRange()
    data object LastYear : RecapRange()
    data class Custom(val from: Instant, val to: Instant) : RecapRange()
}

/** Resolves [range] to a concrete `from`/`to` window, evaluated against [now] - matches
 *  [xyz.skifty.mani.api.ApiService.getRecap]'s params, and mirrors the backend's own UTC-based
 *  window math (see the fork's `recapDateRange`). A [RecapRange.Custom]'s `to` is pushed one day
 *  past [RecapRange.Custom.to] (the selected end *day*'s own midnight) so the window actually
 *  covers that whole day, through to the following midnight - e.g. picking the same day for both
 *  fields resolves to that day's midnight through the next day's midnight, not a zero-width range. */
fun RecapRange.resolve(
    now: Instant = Clock.System.now(),
): Pair<Instant, Instant> =
    when (this) {
        RecapRange.Last30Days -> now.minus(DateTimePeriod(days = 30), TimeZone.UTC) to now
        RecapRange.Last6Months -> now.minus(DateTimePeriod(months = 6), TimeZone.UTC) to now
        RecapRange.LastYear -> now.minus(DateTimePeriod(years = 1), TimeZone.UTC) to now
        is RecapRange.Custom -> from to (to + 1.days)
    }
