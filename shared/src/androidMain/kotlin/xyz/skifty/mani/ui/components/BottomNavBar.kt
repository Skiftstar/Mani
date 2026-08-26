package xyz.skifty.mani.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.nav_home
import mani.shared.generated.resources.nav_playlists
import mani.shared.generated.resources.nav_profile
import mani.shared.generated.resources.nav_search
import mani.shared.generated.resources.playlist_liked_songs_title
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.ui.screens.Screen

/** The app-wide bottom navigation - Home, Search, Liked Songs, Playlists, Profile, in that order -
 *  shown on every screen except [Screen.NowPlaying], where it's replaced by that screen's own
 *  full-screen player. */
@Composable
fun BottomNavBar(selected: Screen, onSelect: (Screen) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            selected = selected == Screen.Home,
            onClick = { onSelect(Screen.Home) },
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            label = { Text(stringResource(Res.string.nav_home)) },
        )
        NavigationBarItem(
            selected = selected == Screen.Search,
            onClick = { onSelect(Screen.Search) },
            icon = { Icon(Icons.Filled.Search, contentDescription = null) },
            label = { Text(stringResource(Res.string.nav_search)) },
        )
        NavigationBarItem(
            selected = selected == Screen.LikedSongs,
            onClick = { onSelect(Screen.LikedSongs) },
            icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
            label = { Text(stringResource(Res.string.playlist_liked_songs_title)) },
        )
        NavigationBarItem(
            selected = selected == Screen.Library,
            onClick = { onSelect(Screen.Library) },
            icon = { Icon(Icons.Filled.LibraryMusic, contentDescription = null) },
            label = { Text(stringResource(Res.string.nav_playlists)) },
        )
        NavigationBarItem(
            selected = selected == Screen.Profile,
            onClick = { onSelect(Screen.Profile) },
            icon = { Icon(Icons.Filled.Person, contentDescription = null) },
            label = { Text(stringResource(Res.string.nav_profile)) },
        )
    }
}
