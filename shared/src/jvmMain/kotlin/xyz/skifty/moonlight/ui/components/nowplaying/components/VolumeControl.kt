package xyz.skifty.moonlight.ui.components.nowplaying.components

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
import androidx.compose.runtime.LaunchedEffect
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
import moonlight.shared.generated.resources.Res
import moonlight.shared.generated.resources.cd_volume
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.moonlight.media.DesktopAudioPlayer
import xyz.skifty.moonlight.preferences.AppPreferences

private const val VOLUME_PREFERENCE_KEY = "moonlight_volume"

/** Mute toggle plus a [MiniVolumeSlider] revealed on hover. Owns its own volume/mute/hover
 *  state - nothing outside this control needs to read or drive it, beyond seeding/persisting
 *  the volume level via [appPreferences] so it survives app restarts. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun VolumeControl(audioPlayer: DesktopAudioPlayer, appPreferences: AppPreferences, modifier: Modifier = Modifier) {
    var isMuted by remember { mutableStateOf(false) }
    var volume by remember {
        mutableIntStateOf(
            appPreferences.get(VOLUME_PREFERENCE_KEY)
                ?.toIntOrNull()
                ?.coerceIn(0, 100)
                ?: 100,
        )
    } // last known UI volume, 0..100
    var isVolumeHovered by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        audioPlayer.setVolume(volume)
    }

    Row(
        modifier = modifier
            .onPointerEvent(PointerEventType.Enter) { isVolumeHovered = true }
            .onPointerEvent(PointerEventType.Exit) { isVolumeHovered = false },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(
            onClick = {
                isMuted = !isMuted
                audioPlayer.setVolume(if (isMuted) 0 else volume)
            },
        ) {
            Icon(
                imageVector = when {
                    isMuted || volume == 0 -> Icons.AutoMirrored.Filled.VolumeOff
                    volume < 50 -> Icons.AutoMirrored.Filled.VolumeMute
                    else -> Icons.AutoMirrored.Filled.VolumeUp
                },
                contentDescription = stringResource(Res.string.cd_volume),
            )
        }

        AnimatedVisibility(visible = isVolumeHovered) {
            val volumeFraction = if (isMuted) 0f else volume / 100f
            MiniVolumeSlider(
                fraction = volumeFraction,
                onFractionChange = { fraction ->
                    volume = (fraction * 100).toInt()
                        .coerceIn(0, 100)
                    isMuted = volume == 0
                    audioPlayer.setVolume(volume)
                    appPreferences.save(VOLUME_PREFERENCE_KEY, volume.toString())
                },
                modifier = Modifier.width(90.dp),
            )
        }
    }
}
