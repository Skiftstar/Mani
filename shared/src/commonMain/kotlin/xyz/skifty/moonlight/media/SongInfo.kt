package xyz.skifty.moonlight.media

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

    fun setSong(
        id: String,
        name: String,
        artist: String,
        coverArtUrl: String?,
        playbackUrl: String,
        durationSeconds: Int?,
        bitRateKbps: Int?,
        format: String?
    ) {
        songId = id
        songName = name
        songArtist = artist
        songCoverArtUrl = coverArtUrl
        songPlaybackUrl = playbackUrl
        songDurationSeconds = durationSeconds
        songBitRateKbps = bitRateKbps
        songFormat = format
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
    }
}
