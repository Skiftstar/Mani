package xyz.skifty.mani.models

import kotlinx.serialization.Serializable

@Serializable
data class Starred2(
    val song: List<ResponseSongInfo> = emptyList()
)