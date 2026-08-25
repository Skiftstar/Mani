package xyz.skifty.moonlight.models

import kotlinx.serialization.Serializable

@Serializable
data class ResponseSongInfo(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val duration: Int? = null,
    val coverArt: String? = null,
    val bitRate: Int? = null,
    val suffix: String? = null
)
