package xyz.skifty.mani.models

import kotlinx.serialization.Serializable

@Serializable
data class RecapTopArtistEntry(
    val artistId: String,

    val name: String,

    val playCount: Long,

    val totalMinutes: Double,
)
