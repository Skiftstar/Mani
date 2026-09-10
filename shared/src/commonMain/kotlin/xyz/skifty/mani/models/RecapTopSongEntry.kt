package xyz.skifty.mani.models

import kotlinx.serialization.Serializable

@Serializable
data class RecapTopSongEntry(
    val entry: ResponseSongInfo,
)
