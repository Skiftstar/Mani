package xyz.skifty.mani.ui.screens.playlist.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.cd_pause
import mani.shared.generated.resources.cd_play
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.media.AudioPlayer
import xyz.skifty.mani.media.PlaybackQueue
import xyz.skifty.mani.media.SongInfo

/** Transport actions for the whole playlist, sitting in their own row below the header. Shows a
 *  pause icon instead of play whenever *this* playlist is the one currently playing. */
@Composable
fun PlaylistActionsRow(
    audioPlayer: AudioPlayer,
    playbackQueue: PlaybackQueue,
    playlistId: String?,
    songs: List<SongInfo>,
    modifier: Modifier = Modifier,
) {
    val isThisPlaylistActive = playbackQueue.songs.isNotEmpty() && playbackQueue.currentSourceId == playlistId
    val isPlaying = isThisPlaylistActive && audioPlayer.isPlaying

    Row(modifier = modifier) {
        FilledIconButton(
            onClick = {
                if (isThisPlaylistActive) {
                    audioPlayer.togglePlayPause()
                } else {
                    playbackQueue.start(songs, 0, playlistId)
                }
            },
            modifier = Modifier.size(56.dp),
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = stringResource(if (isPlaying) Res.string.cd_pause else Res.string.cd_play),
            )
        }
    }
}
