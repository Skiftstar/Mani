package xyz.skifty.mani.models

import kotlinx.serialization.Serializable

@Serializable
data class VibeSimilarTrackEntry(
    val entry: ResponseSongInfo,

    val distance: Double? = null,
)
