package xyz.skifty.mani.ui.screens

sealed class Screen {
    object Home : Screen()
    object Login : Screen()
    object LikedSongs : Screen()
    object Search : Screen()
    data class Playlist(val playlistId: String, val playlistName: String) : Screen()

    // Android-only destinations
    object Library : Screen()
    object Profile : Screen()
    object NowPlaying : Screen()
    object Queue : Screen()
}
