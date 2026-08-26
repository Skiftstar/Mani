package xyz.skifty.moonlight.ui.screens.playlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import xyz.skifty.moonlight.api.ApiService
import xyz.skifty.moonlight.media.DesktopAudioPlayer
import xyz.skifty.moonlight.media.PlaybackQueue
import xyz.skifty.moonlight.media.PlaylistDetails
import xyz.skifty.moonlight.media.PlaylistLibrary
import xyz.skifty.moonlight.media.SongInfo
import xyz.skifty.moonlight.ui.components.PlaylistSongTable
import xyz.skifty.moonlight.ui.screens.playlist.components.PlaylistActionsRow
import xyz.skifty.moonlight.ui.screens.playlist.components.PlaylistHeader

@Composable
fun PlaylistScreen(
    apiService: ApiService,
    audioPlayer: DesktopAudioPlayer,
    activeSongInfo: SongInfo,
    playbackQueue: PlaybackQueue,
    playlistLibrary: PlaylistLibrary,
    playlistId: String?,
    playlistName: String,
) {

    var details by remember(playlistId) { mutableStateOf<PlaylistDetails?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(playlistId) {
        details = if (playlistId == null) {
            PlaylistDetails(
                id = null,
                name = playlistName,
                coverArtUrl = null,
                ownerName = apiService.currentSession?.username,
                songs = apiService.getStarredSongs(),
            )
        } else {
            apiService.getPlaylist(playlistId)
        }
    }

    val currentDetails = details
    if (currentDetails == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            PlaylistHeader(details = currentDetails)
            PlaylistActionsRow(
                audioPlayer = audioPlayer,
                playbackQueue = playbackQueue,
                playlistId = playlistId,
                songs = currentDetails.songs,
            )
            PlaylistSongTable(
                songs = currentDetails.songs,
                audioPlayer = audioPlayer,
                activeSongInfo = activeSongInfo,
                apiService = apiService,
                playbackQueue = playbackQueue,
                playlistLibrary = playlistLibrary,
                onSongClick = { index ->
                    playbackQueue.start(currentDetails.songs, index, playlistId)
                },
                // Never shown for Liked Songs (playlistId == null) - there's no Subsonic
                // playlist id to remove a song from there; unstarring is the equivalent action,
                // already covered by the Like/Unlike item.
                onRemoveFromPlaylist = playlistId?.let { pid ->
                    { index: Int ->
                        val song = currentDetails.songs.getOrNull(index)
                        val songId = song?.songId
                        if (song != null && songId != null) {
                            val previous = currentDetails
                            // Optimistic - rolled back wholesale on failure rather than trying to
                            // re-insert at a possibly-now-stale index.
                            details = currentDetails.copy(songs = currentDetails.songs - song)
                            scope.launch {
                                val result = apiService.removeSongFromPlaylist(pid, index)
                                if (result.isFailure) {
                                    details = previous
                                } else {
                                    playlistLibrary.recordSongRemoved(pid, songId)
                                    // Picked up by every reader of playlistLibrary.playlists
                                    // automatically (Sidebar in particular) - same reasoning as
                                    // the add side: a playlist's cover art can be auto-derived
                                    // from its songs, so removing one can change it too.
                                    playlistLibrary.refreshPlaylists(apiService)
                                }
                            }
                        }
                    }
                },
            )
        }
    }

}
