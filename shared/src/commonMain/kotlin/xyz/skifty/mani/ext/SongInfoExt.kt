package xyz.skifty.mani.ext

import xyz.skifty.mani.media.SongInfo
import xyz.skifty.mani.media.VibeProfile

/** A short human-readable label for this song's audio quality - bit rate when known ("320 kbps"),
 *  falling back to the file format ("FLAC"), or "--" when neither is known. */
fun SongInfo.qualityLabel(): String {
    songBitRateKbps?.let { bitRateKbps ->
        return "$bitRateKbps kbps"
    }
    return songFormat?.uppercase() ?: "--"
}

/** This song's own [VibeProfile], or null if any of the 7 stats is missing - e.g. a song from a
 *  stock server, or any endpoint this fork doesn't attach VibeNet stats to. */
fun SongInfo.toVibeProfileOrNull(): VibeProfile? {
    val acousticness = songAcousticness
        ?: return null
    val danceability = songDanceability
        ?: return null
    val energy = songEnergy
        ?: return null
    val instrumentalness = songInstrumentalness
        ?: return null
    val liveness = songLiveness
        ?: return null
    val speechiness = songSpeechiness
        ?: return null
    val valence = songValence
        ?: return null
    return VibeProfile(
        acousticness = acousticness,
        danceability = danceability,
        energy = energy,
        instrumentalness = instrumentalness,
        liveness = liveness,
        speechiness = speechiness,
        valence = valence,
    )
}
