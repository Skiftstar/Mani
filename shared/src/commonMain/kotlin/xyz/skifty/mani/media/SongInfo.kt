package xyz.skifty.mani.media

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class SongInfo {

    var songId by mutableStateOf<String?>(null)
        private set

    var songName by mutableStateOf<String?>(null)
        private set

    var songArtist by mutableStateOf<String?>(null)
        private set

    var songCoverArtUrl by mutableStateOf<String?>(null)
        private set

    var songPlaybackUrl by mutableStateOf<String?>(null)

    var songDurationSeconds by mutableStateOf<Int?>(null)
        private set

    var songBitRateKbps by mutableStateOf<Int?>(null)
        private set

    var songFormat by mutableStateOf<String?>(null)
        private set

    var songPlayCount by mutableStateOf<Int?>(null)

    var starred by mutableStateOf(false)

    // VibeNet stats (0-1 floats), from this app's own Navidrome fork - null on a stock server, or
    // any endpoint that doesn't return them. See ext/SongInfoExt.kt's toVibeProfileOrNull() and
    // PlaybackQueue's Autoplay feature, which averages these across manually-queued songs to seed
    // getVibeSimilarTracks.
    var songAcousticness by mutableStateOf<Double?>(null)
        private set

    var songDanceability by mutableStateOf<Double?>(null)
        private set

    var songEnergy by mutableStateOf<Double?>(null)
        private set

    var songInstrumentalness by mutableStateOf<Double?>(null)
        private set

    var songLiveness by mutableStateOf<Double?>(null)
        private set

    var songSpeechiness by mutableStateOf<Double?>(null)
        private set

    var songValence by mutableStateOf<Double?>(null)
        private set

    fun setSong(
        id: String,
        name: String,
        artist: String,
        coverArtUrl: String?,
        playbackUrl: String,
        durationSeconds: Int?,
        bitRateKbps: Int?,
        format: String?,
        playCount: Int?,
        starred: Boolean,
        acousticness: Double? = null,
        danceability: Double? = null,
        energy: Double? = null,
        instrumentalness: Double? = null,
        liveness: Double? = null,
        speechiness: Double? = null,
        valence: Double? = null,
    ) {
        songId = id
        songName = name
        songArtist = artist
        songCoverArtUrl = coverArtUrl
        songPlaybackUrl = playbackUrl
        songDurationSeconds = durationSeconds
        songBitRateKbps = bitRateKbps
        songFormat = format
        songPlayCount = playCount
        this.starred = starred
        songAcousticness = acousticness
        songDanceability = danceability
        songEnergy = energy
        songInstrumentalness = instrumentalness
        songLiveness = liveness
        songSpeechiness = speechiness
        songValence = valence
    }

    fun setSong(songInfo: SongInfo) {
        songId = songInfo.songId
        songName = songInfo.songName
        songArtist = songInfo.songArtist
        songCoverArtUrl = songInfo.songCoverArtUrl
        songPlaybackUrl = songInfo.songPlaybackUrl
        songDurationSeconds = songInfo.songDurationSeconds
        songBitRateKbps = songInfo.songBitRateKbps
        songFormat = songInfo.songFormat
        songPlayCount = songInfo.songPlayCount
        starred = songInfo.starred
        songAcousticness = songInfo.songAcousticness
        songDanceability = songInfo.songDanceability
        songEnergy = songInfo.songEnergy
        songInstrumentalness = songInfo.songInstrumentalness
        songLiveness = songInfo.songLiveness
        songSpeechiness = songInfo.songSpeechiness
        songValence = songInfo.songValence
    }

    fun clear() {
        songId = null
        songName = null
        songArtist = null
        songCoverArtUrl = null
        songPlaybackUrl = null
        songDurationSeconds = null
        songBitRateKbps = null
        songFormat = null
        songPlayCount = null
        starred = false
        songAcousticness = null
        songDanceability = null
        songEnergy = null
        songInstrumentalness = null
        songLiveness = null
        songSpeechiness = null
        songValence = null
    }
}
