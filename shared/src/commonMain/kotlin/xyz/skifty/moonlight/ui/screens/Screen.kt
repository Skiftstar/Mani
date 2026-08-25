package xyz.skifty.moonlight.ui.screens

sealed class Screen {
    object Home : Screen()
    object Login : Screen()
    object LikedSongs : Screen()
    object Search : Screen()
    data class Playlist(val playlistId: String, val playlistName: String) : Screen()
}
