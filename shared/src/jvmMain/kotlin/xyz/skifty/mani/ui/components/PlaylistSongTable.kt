package xyz.skifty.mani.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.cd_duration
import mani.shared.generated.resources.playlist_column_quality
import mani.shared.generated.resources.playlist_column_title
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.api.ApiService
import xyz.skifty.mani.media.DesktopAudioPlayer
import xyz.skifty.mani.media.PlaybackQueue
import xyz.skifty.mani.media.PlaylistInfo
import xyz.skifty.mani.media.PlaylistLibrary
import xyz.skifty.mani.media.SongInfo

/** Column headers followed by one [PlaylistSongRow] per song. Owns the star/unstar toggle
 *  (calling [apiService] and optimistically flipping [SongInfo.starred], reverting it if the
 *  request fails) so every screen using this table gets identical behavior for free - likewise
 *  for each row's context-menu actions (Play/Add to Queue/Add to Playlist), all constructed here
 *  from [playbackQueue]/[apiService] rather than threading those services into [PlaylistSongRow]
 *  itself. [onRemoveFromPlaylist] is left null by screens where it doesn't apply (Liked Songs,
 *  search results) - [PlaylistSongRow] hides that menu item entirely when it's null. */
@Composable
fun PlaylistSongTable(
    songs: List<SongInfo>,
    audioPlayer: DesktopAudioPlayer,
    activeSongInfo: SongInfo,
    apiService: ApiService,
    playbackQueue: PlaybackQueue,
    playlistLibrary: PlaylistLibrary,
    onSongClick: (index: Int) -> Unit,
    onRemoveFromPlaylist: ((index: Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()

    fun toggleStar(songInfo: SongInfo) {
        val songId = songInfo.songId
            ?: return
        val wasStarred = songInfo.starred
        songInfo.starred = !wasStarred
        coroutineScope.launch {
            val result = if (wasStarred) {
                apiService.unstar(songId)
            } else {
                apiService.star(songId)
            }
            if (result.isFailure) {
                songInfo.starred = wasStarred
            }
        }
    }

    fun addToPlaylist(songInfo: SongInfo, playlist: PlaylistInfo) {
        val songId = songInfo.songId
            ?: return
        coroutineScope.launch {
            val alreadyInPlaylist = playlistLibrary.containsSong(apiService, playlist.id, songId)
            if (!alreadyInPlaylist) {
                val result = apiService.addSongToPlaylist(playlist.id, songId)
                if (result.isSuccess) {
                    playlistLibrary.recordSongAdded(playlist.id, songId)
                    // Picked up by every reader of playlistLibrary.playlists automatically
                    // (Sidebar in particular) - a playlist's cover art can be auto-derived from
                    // its songs, so adding one can change what should be shown for it.
                    playlistLibrary.refreshPlaylists(apiService)
                }
            }
        }
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 8.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.width(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "#",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(modifier = Modifier.size(48.dp))
            Text(
                text = stringResource(Res.string.playlist_column_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(Res.string.playlist_column_quality),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(80.dp),
            )
            Box(
                modifier = Modifier.width(48.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = Icons.Filled.Schedule,
                    contentDescription = stringResource(Res.string.cd_duration),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(modifier = Modifier.size(32.dp))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        for ((index, songInfo) in songs.withIndex()) {
            PlaylistSongRow(
                index = index + 1,
                songInfo = songInfo,
                audioPlayer = audioPlayer,
                activeSongInfo = activeSongInfo,
                apiService = apiService,
                playlistLibrary = playlistLibrary,
                onClick = {
                    onSongClick(index)
                },
                onToggleStar = {
                    toggleStar(songInfo)
                },
                onPlay = {
                    playbackQueue.start(listOf(songInfo), 0, sourceId = null)
                },
                onAddToQueue = {
                    playbackQueue.addToEnd(songInfo)
                },
                onAddToPlaylist = { playlist ->
                    addToPlaylist(songInfo, playlist)
                },
                onRemoveFromPlaylist = onRemoveFromPlaylist?.let { callback ->
                    { callback(index) }
                },
            )
        }
    }
}
