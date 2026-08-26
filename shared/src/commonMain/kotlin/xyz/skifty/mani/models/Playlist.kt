package xyz.skifty.mani.models

import kotlinx.serialization.Serializable

@Serializable
data class Playlist(
    val id: String,

    val name: String,

    val songCount: Int = 0,

    val coverArt: String? = null,
)
