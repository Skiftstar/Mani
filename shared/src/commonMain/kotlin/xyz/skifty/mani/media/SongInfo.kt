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
        private set

    var starred by mutableStateOf(false)

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
    }
}
