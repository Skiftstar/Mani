package xyz.skifty.mani.models

import kotlinx.serialization.Serializable

@Serializable
data class Recap(
    val summary: RecapSummary = RecapSummary(),

    val topSong: List<RecapTopSongEntry> = emptyList(),

    val topArtist: List<RecapTopArtistEntry> = emptyList(),

    val tasteProfile: RecapTasteProfile = RecapTasteProfile(),
)
