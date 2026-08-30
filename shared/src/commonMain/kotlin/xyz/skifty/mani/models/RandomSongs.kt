package xyz.skifty.mani.models

import kotlinx.serialization.Serializable

@Serializable
data class RandomSongs(
    val song: List<ResponseSongInfo> = emptyList(),
)
