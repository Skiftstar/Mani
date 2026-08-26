package xyz.skifty.mani.models

import kotlinx.serialization.Serializable

@Serializable
data class PlaylistsWrapper(
    val playlist: List<Playlist> = emptyList(),
)
