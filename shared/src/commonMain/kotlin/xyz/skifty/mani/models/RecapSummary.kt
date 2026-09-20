package xyz.skifty.mani.models

import kotlinx.serialization.Serializable

@Serializable
data class RecapSummary(
    val playCount: Long = 0,

    val totalMinutes: Double = 0.0,

    val uniqueSongs: Long = 0,

    val uniqueArtists: Long = 0,
)
