package xyz.skifty.moonlight.ui.components.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
    var lastSeekCount by remember { mutableIntStateOf(audioPlayer.seekCount) }
    var lastSeekAtMs by remember { mutableLongStateOf(0L) }

    // A freshly (re)started track's real duration hasn't arrived from mpv yet at this instant -
    // reset immediately rather than waiting for the polling loop below to notice, which otherwise
    // keeps showing (and, via ProgressSlider's seekFraction() call, seeking against) the *previous*
    // track's duration until that update lands.
    LaunchedEffect(audioPlayer.playbackStartedCount) {
        if (audioPlayer.playbackStartedCount > 0) {
            positionMs = audioPlayer.lastConfirmedStartPositionMs
            durationMs = 1L // avoid /0
        }
    }

    LaunchedEffect(audioPlayer) {
        while (true) {
            if (audioPlayer.seekCount != lastSeekCount) {
                lastSeekCount = audioPlayer.seekCount
                lastSeekAtMs = System.currentTimeMillis()
            }
            if (!isDragging) {
                val freshPosition = audioPlayer.currentPosition()
                // libVLC can transiently report a position near 0 while briefly re-buffering right
                // after a manual seek elsewhere in the track - not reliably bounded to a single poll
                // cycle, so waiting a fixed short delay before trusting a fresh read isn't enough to
                // reliably outlast it (tried that first - it just held the *wrong* value for longer
                // instead of the right one). Instead of guessing a wait time, don't trust an
                // implausible snap back to (near) 0 shortly after we know we just seeked well past
                // that - keep the last known-good value and let a later poll, once the position has
                // genuinely moved on, take over.
                val looksLikeTransientResetDuringSeek = freshPosition < 500 &&
                    positionMs > 2000 &&
                    System.currentTimeMillis() - lastSeekAtMs < 2000
                if (!looksLikeTransientResetDuringSeek) {
                    positionMs = freshPosition
                }
                val d = audioPlayer.length()
                if (d > 0) durationMs = d
            }
            delay(200) // 5 updates/sec, smooth enough
        }
    }

    // A plain background rather than a Surface deliberately - Surface clips its content to its
    // own bounds, which would cut off the top of ProgressSlider's hover thumb (it intentionally
    // overflows slightly above its own top edge to stay centered on a track that sits flush at
    // this widget's top edge, with zero gap above it).
    Column(
        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
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
