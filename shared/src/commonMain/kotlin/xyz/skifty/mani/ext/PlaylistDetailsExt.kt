package xyz.skifty.mani.ext

import xyz.skifty.mani.media.PlaylistDetails

/** Sum of every song's duration, in seconds - songs with no known duration count as 0. */
fun PlaylistDetails.totalRuntimeSeconds(): Int =
    songs.sumOf { songInfo -> songInfo.songDurationSeconds ?: 0 }
