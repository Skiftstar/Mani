package xyz.skifty.moonlight.models

import kotlinx.serialization.Serializable

@Serializable
data class SearchResult3(
    val song: List<ResponseSongInfo> = emptyList(),
)
