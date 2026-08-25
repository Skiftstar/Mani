package xyz.skifty.moonlight.ui.components.nowplaying

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import moonlight.shared.generated.resources.Res
import moonlight.shared.generated.resources.cd_fullscreen
import moonlight.shared.generated.resources.cd_queue
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.moonlight.media.DesktopAudioPlayer
import xyz.skifty.moonlight.media.PlaybackQueue
import xyz.skifty.moonlight.media.SongInfo
import xyz.skifty.moonlight.ui.components.nowplaying.components.PlaybackButtons
import xyz.skifty.moonlight.ui.components.nowplaying.components.ProgressSlider
import xyz.skifty.moonlight.ui.components.nowplaying.components.TrackInfo
import xyz.skifty.moonlight.ui.components.nowplaying.components.VolumeControl

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingBottomWidget(audioPlayer: DesktopAudioPlayer, activeSongInfo: SongInfo, playbackQueue: PlaybackQueue) {

    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(1L) } // avoid /0
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(audioPlayer) {
        while (true) {
            if (!isDragging) {
                positionMs = audioPlayer.currentPosition()
                val d = audioPlayer.length()
                if (d > 0) durationMs = d
            }
            delay(200) // 5 updates/sec, smooth enough
        }
    }

    Surface(
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            ProgressSlider(
                audioPlayer = audioPlayer,
                positionMs = positionMs,
                maxLengthMs = durationMs,
                isDragging = isDragging,
                setDragging = { dragging -> isDragging = dragging },
                setProgress = { newPos -> positionMs = newPos },
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TrackInfo(songInfo = activeSongInfo, modifier = Modifier.weight(1f))

                PlaybackButtons(
                    audioPlayer = audioPlayer,
                    playbackQueue = playbackQueue,
                    positionMs = positionMs,
                    durationMs = durationMs,
                )

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    VolumeControl(audioPlayer = audioPlayer)

                    IconButton(onClick = { /* TODO: queue */ }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = stringResource(Res.string.cd_queue),
                        )
                    }

                    IconButton(onClick = { /* TODO: fullscreen */ }) {
                        Icon(
                            imageVector = Icons.Filled.Fullscreen,
                            contentDescription = stringResource(Res.string.cd_fullscreen),
                        )
                    }
                }
            }
        }
    }
}
