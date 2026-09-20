package xyz.skifty.mani.models

import kotlinx.serialization.Serializable

/** The 7 VibeNet stats averaged over a recap range, each null when the range's songs simply have
 *  no VibeNet data (stock server, or nothing analyzed yet) - see [xyz.skifty.mani.media.VibeProfile]
 *  for the per-song equivalent. [trackCount] is how many songs the average was computed from; 0
 *  means the radar has nothing to draw. */
@Serializable
data class RecapTasteProfile(
    val acousticness: Double? = null,

    val danceability: Double? = null,

    val energy: Double? = null,

    val instrumentalness: Double? = null,

    val liveness: Double? = null,

    val speechiness: Double? = null,

    val valence: Double? = null,

    val trackCount: Long = 0,
)
