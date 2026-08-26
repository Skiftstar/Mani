package xyz.skifty.mani.ext

/** Formats a duration in seconds (this receiver) as `m:ss`. */
fun Int.toDurationLabel(): String {
    val totalSeconds = this.coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${
        seconds.toString()
            .padStart(2, '0')
    }"
}

/** Hours and minutes (rounded down to the minute) from a duration in seconds (this receiver) -
 *  for a playlist's total runtime, unlike [toDurationLabel]'s per-song `m:ss`. */
fun Int.toHoursAndMinutes(): Pair<Int, Int> {
    val totalMinutes = this.coerceAtLeast(0) / 60
    return (totalMinutes / 60) to (totalMinutes % 60)
}
