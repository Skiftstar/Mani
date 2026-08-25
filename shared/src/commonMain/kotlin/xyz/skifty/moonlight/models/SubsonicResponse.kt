package xyz.skifty.moonlight.models

import kotlinx.serialization.Serializable

@Serializable
data class SubsonicResponse(
    val status: String,
    val starred2: Starred2? = null,
    val playlists: PlaylistsWrapper? = null,
    val playlist: PlaylistWithSongs? = null,
    val song: ResponseSongInfo? = null,
    val error: SubsonicError? = null
)
