package xyz.skifty.mani.ui.components.nowplaying

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.cd_pause
import mani.shared.generated.resources.cd_play
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.media.AudioPlayer
import xyz.skifty.mani.media.SongInfo
import xyz.skifty.mani.ui.components.nowplaying.components.TrackInfo

/** The widget shown above [xyz.skifty.mani.ui.components.BottomNavBar] whenever a song is
 *  playing - title/cover art, a (non-draggable, unlike [NowPlayingBottomWidget]'s) progress bar,
 *  and a pause/resume button. Tapping anywhere else on it expands into the Now Playing screen. */
@Composable
fun MiniPlayerBar(
    audioPlayer: AudioPlayer,
    activeSongInfo: SongInfo,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {

    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(1L) } // avoid /0

    LaunchedEffect(audioPlayer.playbackStartedCount) {
        if (audioPlayer.playbackStartedCount > 0) {
            positionMs = audioPlayer.lastConfirmedStartPositionMs
            durationMs = 1L
        }
    }

    LaunchedEffect(audioPlayer) {
        while (true) {
            positionMs = audioPlayer.currentPosition()
            val d = audioPlayer.length()
            if (d > 0) durationMs = d
            delay(200)
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onExpand),
    ) {
        Column {
            LinearProgressIndicator(
                progress = { (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                drawStopIndicator = {}, // suppresses the trailing "stop" dot Material3 draws by default
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TrackInfo(songInfo = activeSongInfo, modifier = Modifier.weight(1f))

                IconButton(onClick = { audioPlayer.togglePlayPause() }) {
                    Icon(
                        imageVector = if (audioPlayer.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = stringResource(
                            if (audioPlayer.isPlaying) Res.string.cd_pause else Res.string.cd_play,
                        ),
                    )
                }
            }
        }
    }
}
