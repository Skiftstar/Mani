package xyz.skifty.mani.media

/** The 7 VibeNet stats (each a 0-1 float) this app's own Navidrome fork exposes per song, and
 *  that its `getVibeSimilarTracks` endpoint accepts directly as a taste-profile query - see
 *  [xyz.skifty.mani.api.ApiService.getVibeSimilarSongs]/[xyz.skifty.mani.ext.toVibeProfileOrNull]. */
data class VibeProfile(
    val acousticness: Double,

    val danceability: Double,

    val energy: Double,

    val instrumentalness: Double,

    val liveness: Double,

    val speechiness: Double,

    val valence: Double,
)
