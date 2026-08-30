package xyz.skifty.mani.ui.screens

sealed class Screen {
    object Home : Screen()
    object Login : Screen()
    object LikedSongs : Screen()
    object Search : Screen()
    data class Playlist(val playlistId: String, val playlistName: String) : Screen()

    // Android-only destinations - desktop's Sidebar never produces these (see JvmApp's own
    // now-non-exhaustive `when` over Screen for how it tolerates that).
    object Library : Screen()
    object Profile : Screen()
    object NowPlaying : Screen()
    object Queue : Screen()
}
