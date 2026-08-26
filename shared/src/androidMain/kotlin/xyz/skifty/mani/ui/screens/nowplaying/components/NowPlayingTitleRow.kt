package xyz.skifty.mani.ui.screens.nowplaying.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.cd_star
import mani.shared.generated.resources.cd_unstar
import mani.shared.generated.resources.unknown_artist
import mani.shared.generated.resources.unknown_title
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.api.ApiService
import xyz.skifty.mani.media.PlaylistLibrary
import xyz.skifty.mani.media.SongInfo

/** Title + artist (stacked), with a like toggle to the right, vertically centered against both
 *  lines together rather than either one alone. */
@Composable
fun NowPlayingTitleRow(activeSongInfo: SongInfo, apiService: ApiService, playlistLibrary: PlaylistLibrary) {
    val coroutineScope = rememberCoroutineScope()

    fun toggleStar() {
        val songId = activeSongInfo.songId
            ?: return
        val wasStarred = activeSongInfo.starred
        activeSongInfo.starred = !wasStarred
        coroutineScope.launch {
            val result = if (wasStarred) apiService.unstar(songId) else apiService.star(songId)
            if (result.isFailure) {
                activeSongInfo.starred = wasStarred
            } else {
                // See PlaylistSongTable's identical toggleStar - Liked Songs is a pseudo-playlist,
                // so starring/unstarring needs the same cache invalidation add/remove does.
                playlistLibrary.refreshPlaylists(apiService)
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = activeSongInfo.songName ?: stringResource(Res.string.unknown_title),
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = activeSongInfo.songArtist ?: stringResource(Res.string.unknown_artist),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        IconButton(onClick = { toggleStar() }) {
            Icon(
                imageVector = if (activeSongInfo.starred) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = stringResource(
                    if (activeSongInfo.starred) Res.string.cd_unstar else Res.string.cd_star,
                ),
                tint = if (activeSongInfo.starred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
