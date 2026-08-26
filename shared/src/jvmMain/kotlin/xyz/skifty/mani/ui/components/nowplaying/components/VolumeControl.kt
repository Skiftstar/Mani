package xyz.skifty.mani.ui.components.nowplaying.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.cd_volume
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.media.DesktopAudioPlayer

/** Mute toggle plus a [MiniVolumeSlider] revealed on hover. The volume level itself is owned by
 *  [audioPlayer] (so it's a single source of truth reachable from MPRIS too) - this control only
 *  owns its own mute/hover UI state, remembering the pre-mute level to restore on unmute. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun VolumeControl(audioPlayer: DesktopAudioPlayer, modifier: Modifier = Modifier) {
    val volume = audioPlayer.volume
    val isMuted = volume == 0
    var preMuteVolume by remember { mutableIntStateOf(100) }
    var isVolumeHovered by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .onPointerEvent(PointerEventType.Enter) { isVolumeHovered = true }
            .onPointerEvent(PointerEventType.Exit) { isVolumeHovered = false },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(
            onClick = {
                if (isMuted) {
                    audioPlayer.setVolume(preMuteVolume)
                } else {
                    preMuteVolume = volume
                    audioPlayer.setVolume(0)
                }
            },
        ) {
            Icon(
                imageVector = when {
                    isMuted -> Icons.AutoMirrored.Filled.VolumeOff
                    volume < 50 -> Icons.AutoMirrored.Filled.VolumeMute
                    else -> Icons.AutoMirrored.Filled.VolumeUp
                },
                contentDescription = stringResource(Res.string.cd_volume),
            )
        }

        AnimatedVisibility(visible = isVolumeHovered) {
            MiniVolumeSlider(
                fraction = volume / 100f,
                onFractionChange = { fraction ->
                    audioPlayer.setVolume(
                        (fraction * 100).toInt()
                            .coerceIn(0, 100),
                    )
                },
                modifier = Modifier.width(90.dp),
            )
        }
    }
}
