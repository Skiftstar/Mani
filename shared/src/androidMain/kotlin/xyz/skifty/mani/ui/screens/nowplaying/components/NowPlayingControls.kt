package xyz.skifty.mani.ui.screens.nowplaying.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.cd_loop
import mani.shared.generated.resources.cd_next
import mani.shared.generated.resources.cd_pause
import mani.shared.generated.resources.cd_play
import mani.shared.generated.resources.cd_previous
import mani.shared.generated.resources.cd_shuffle
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.media.AudioPlayer
import xyz.skifty.mani.media.LoopMode
import xyz.skifty.mani.media.PlaybackQueue

/** The Now Playing screen's own, larger transport control row - shuffle/previous/play-pause/
 *  next/loop, the same set as desktop's [PlaybackButtons] just laid out for a full-width touch
 *  target with a bigger center play/pause button. */
@Composable
fun NowPlayingControls(audioPlayer: AudioPlayer, playbackQueue: PlaybackQueue) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { playbackQueue.setShuffle(!playbackQueue.shuffleEnabled) }) {
            Icon(
                imageVector = Icons.Filled.Shuffle,
                contentDescription = stringResource(Res.string.cd_shuffle),
                tint = if (playbackQueue.shuffleEnabled) MaterialTheme.colorScheme.primary else LocalContentColor.current,
            )
        }

        IconButton(
            onClick = { playbackQueue.previous() },
            enabled = playbackQueue.hasPrevious,
        ) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = stringResource(Res.string.cd_previous),
                modifier = Modifier.size(32.dp),
            )
        }

        IconButton(
            onClick = { audioPlayer.togglePlayPause() },
            modifier = Modifier.size(64.dp),
        ) {
            Icon(
                imageVector = if (audioPlayer.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = stringResource(
                    if (audioPlayer.isPlaying) Res.string.cd_pause else Res.string.cd_play,
                ),
                modifier = Modifier.size(40.dp),
            )
        }

        IconButton(
            onClick = { playbackQueue.next() },
            enabled = playbackQueue.hasNext,
        ) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = stringResource(Res.string.cd_next),
                modifier = Modifier.size(32.dp),
            )
        }

        IconButton(onClick = { playbackQueue.cycleLoopMode() }) {
            Icon(
                imageVector = if (playbackQueue.loopMode == LoopMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                contentDescription = stringResource(Res.string.cd_loop),
                tint = if (playbackQueue.loopMode != LoopMode.OFF) MaterialTheme.colorScheme.primary else LocalContentColor.current,
            )
        }
    }
}
