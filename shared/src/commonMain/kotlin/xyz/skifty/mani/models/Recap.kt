package xyz.skifty.mani.models

import kotlinx.serialization.Serializable

@Serializable
data class Recap(
    val topSong: List<RecapTopSongEntry> = emptyList(),
)
