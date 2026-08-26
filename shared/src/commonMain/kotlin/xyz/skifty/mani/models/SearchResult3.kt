package xyz.skifty.mani.models

import kotlinx.serialization.Serializable

@Serializable
data class SearchResult3(
    val song: List<ResponseSongInfo> = emptyList(),
)
