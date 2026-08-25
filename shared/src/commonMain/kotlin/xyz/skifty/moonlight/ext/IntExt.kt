package xyz.skifty.moonlight.ext

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
