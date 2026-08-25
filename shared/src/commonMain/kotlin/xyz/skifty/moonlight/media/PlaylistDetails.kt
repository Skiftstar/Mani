package xyz.skifty.moonlight.media

/** [id] is null for the Liked (Starred) Songs pseudo-playlist, which has no Subsonic playlist id. */
data class PlaylistDetails(
    val id: String?,

    val name: String,

    val coverArtUrl: String?,

    val songs: List<SongInfo>,
)
