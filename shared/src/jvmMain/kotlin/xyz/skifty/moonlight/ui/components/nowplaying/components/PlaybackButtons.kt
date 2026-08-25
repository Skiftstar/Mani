package xyz.skifty.moonlight.ui.components.nowplaying.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import moonlight.shared.generated.resources.Res
import moonlight.shared.generated.resources.cd_loop
import moonlight.shared.generated.resources.cd_next
import moonlight.shared.generated.resources.cd_pause
import moonlight.shared.generated.resources.cd_play
import moonlight.shared.generated.resources.cd_previous
import moonlight.shared.generated.resources.cd_shuffle
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.moonlight.media.DesktopAudioPlayer

/** Shuffle/previous/play-pause/next/loop transport controls plus the position/duration text. */
@Composable
fun PlaybackButtons(
    audioPlayer: DesktopAudioPlayer,
    positionMs: Long,
    durationMs: Long,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(onClick = { /* TODO: shuffle */ }) {
            Icon(
                imageVector = Icons.Filled.Shuffle,
                contentDescription = stringResource(Res.string.cd_shuffle),
            )
        }

        IconButton(onClick = { /* TODO: previous */ }) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = stringResource(Res.string.cd_previous),
            )
        }

        IconButton(onClick = { audioPlayer.pause() }) {
            Icon(
                imageVector = if (audioPlayer.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = stringResource(
                    if (audioPlayer.isPlaying) Res.string.cd_pause else Res.string.cd_play,
                ),
            )
        }

        IconButton(onClick = { /* TODO: next */ }) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = stringResource(Res.string.cd_next),
            )
        }

        IconButton(onClick = { /* TODO: loop */ }) {
            Icon(
                imageVector = Icons.Filled.Repeat,
                contentDescription = stringResource(Res.string.cd_loop),
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = "${formatDuration(positionMs)}/${formatDuration(durationMs)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(76.dp),
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${
        seconds.toString()
            .padStart(2, '0')
    }"
}
