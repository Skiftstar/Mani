package xyz.skifty.mani.ui.screens.playlist.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.skifty.mani.media.AudioPlayer
import xyz.skifty.mani.media.PlaybackQueue
import xyz.skifty.mani.media.PlaylistDetails

@Composable
actual fun PlaylistHeaderBlock(
    details: PlaylistDetails,
    audioPlayer: AudioPlayer,
    playbackQueue: PlaybackQueue,
    playlistId: String?,
    modifier: Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        PlaylistHeader(details = details)
        PlaylistActionsRow(
            audioPlayer = audioPlayer,
            playbackQueue = playbackQueue,
            playlistId = playlistId,
            songs = details.songs,
        )
    }
}
