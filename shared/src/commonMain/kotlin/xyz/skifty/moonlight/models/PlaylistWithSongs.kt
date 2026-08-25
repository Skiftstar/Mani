package xyz.skifty.moonlight.models

import kotlinx.serialization.Serializable

@Serializable
data class PlaylistWithSongs(
    val id: String,

    val name: String,

    val songCount: Int = 0,

    val coverArt: String? = null,

    val entry: List<ResponseSongInfo> = emptyList(),
)
