package xyz.skifty.mani.ext

import xyz.skifty.mani.media.SongInfo

/** Sum of every song's duration, in seconds - songs with no known duration count as 0. Works on
 *  any song list, not just a whole playlist's - lets the playlist header compute this against a
 *  search-filtered subset as easily as the full list. */
fun List<SongInfo>.totalRuntimeSeconds(): Int =
    sumOf { songInfo -> songInfo.songDurationSeconds ?: 0 }
