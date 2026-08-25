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

    fun setSong(
        id: String,
        name: String,
        artist: String,
        coverArtUrl: String?,
        playbackUrl: String
    ) {
        songId = id
        songName = name
        songArtist = artist
        songCoverArtUrl = coverArtUrl
        songPlaybackUrl = playbackUrl
    }

    fun setSong(songInfo: SongInfo) {
        songId = songInfo.songId
        songName = songInfo.songName
        songArtist = songInfo.songArtist
        songCoverArtUrl = songInfo.songCoverArtUrl
        songPlaybackUrl = songInfo.songPlaybackUrl
    }

    fun clear() {
        songId = null
        songName = null
        songArtist = null
        songCoverArtUrl = null
        songPlaybackUrl = null
    }
}
