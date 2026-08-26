package xyz.skifty.mani.ui.components.nowplaying.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Text
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
import xyz.skifty.mani.ext.toDurationLabel
import xyz.skifty.mani.media.AudioPlayer
import xyz.skifty.mani.media.LoopMode
import xyz.skifty.mani.media.PlaybackQueue

/** Shuffle/previous/play-pause/next/loop transport controls plus the position/duration text. */
@Composable
fun PlaybackButtons(
    audioPlayer: AudioPlayer,
    playbackQueue: PlaybackQueue,
    positionMs: Long,
    durationMs: Long,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(onClick = { playbackQueue.setShuffle(!playbackQueue.shuffleEnabled) }) {
            Icon(
                imageVector = Icons.Filled.Shuffle,
                contentDescription = stringResource(Res.string.cd_shuffle),
                tint = if (playbackQueue.shuffleEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    LocalContentColor.current
                },
            )
        }

        IconButton(
            onClick = { playbackQueue.previous() },
            enabled = playbackQueue.hasPrevious,
        ) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = stringResource(Res.string.cd_previous),
            )
        }

        IconButton(onClick = { audioPlayer.togglePlayPause() }) {
            Icon(
                imageVector = if (audioPlayer.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = stringResource(
                    if (audioPlayer.isPlaying) Res.string.cd_pause else Res.string.cd_play,
                ),
            )
        }

        IconButton(
            onClick = { playbackQueue.next() },
            enabled = playbackQueue.hasNext,
        ) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = stringResource(Res.string.cd_next),
            )
        }

        IconButton(onClick = { playbackQueue.cycleLoopMode() }) {
            Icon(
                imageVector = if (playbackQueue.loopMode == LoopMode.ONE) {
                    Icons.Filled.RepeatOne
                } else {
                    Icons.Filled.Repeat
                },
                contentDescription = stringResource(Res.string.cd_loop),
                tint = if (playbackQueue.loopMode != LoopMode.OFF) {
                    MaterialTheme.colorScheme.primary
                } else {
                    LocalContentColor.current
                },
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = "${(positionMs / 1000).toInt().toDurationLabel()}/${(durationMs / 1000).toInt().toDurationLabel()}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(76.dp),
        )
    }
}
