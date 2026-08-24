package xyz.skifty.moonlight.media.nowPlayingBottomWidget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import xyz.skifty.moonlight.media.SongInfo
import xyz.skifty.moonlight.media.DesktopAudioPlayer

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingBottomWidget(audioPlayer: DesktopAudioPlayer, activeSongInfo: SongInfo) {

    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(1L) } // avoid /0
    var isDragging by remember { mutableStateOf(false) }

    var isMuted by remember { mutableStateOf(false) }
    var volume by remember { mutableIntStateOf(100) } // last known UI volume, 0..100
    var isVolumeHovered by remember { mutableStateOf(false) }

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
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            ProgressSlider(
                audioPlayer = audioPlayer,
                positionMs = positionMs,
                maxLengthMs = durationMs,
                isDragging = isDragging,
                setDragging = { dragging -> isDragging = dragging },
                setProgress = { newPos -> positionMs = newPos }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ---- Left zone: cover art + title/artist ----
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AsyncImage(
                        model = activeSongInfo.songCoverArtUrl,
                        contentDescription = "Album art",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                    Column {
                        Text(
                            text = activeSongInfo.songName ?: "Unknown title",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = activeSongInfo.songArtist ?: "Unknown artist",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // ---- Center zone: transport controls + time ----
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = { /* TODO: shuffle */ }) {
                        Icon(imageVector = Icons.Filled.Shuffle, contentDescription = "Shuffle")
                    }

                    IconButton(onClick = { /* TODO: previous */ }) {
                        Icon(imageVector = Icons.Filled.SkipPrevious, contentDescription = "Previous")
                    }

                    IconButton(onClick = { audioPlayer.pause() }) {
                        Icon(
                            imageVector = if (audioPlayer.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (audioPlayer.isPlaying) "Pause" else "Play"
                        )
                    }

                    IconButton(onClick = { /* TODO: next */ }) {
                        Icon(imageVector = Icons.Filled.SkipNext, contentDescription = "Next")
                    }

                    IconButton(onClick = { /* TODO: loop */ }) {
                        Icon(imageVector = Icons.Filled.Repeat, contentDescription = "Loop")
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = "${formatDuration(positionMs)}/${formatDuration(durationMs)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(76.dp)
                    )
                }

                // ---- Right zone: volume, queue, fullscreen ----
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .onPointerEvent(PointerEventType.Enter) { isVolumeHovered = true }
                            .onPointerEvent(PointerEventType.Exit) { isVolumeHovered = false },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(onClick = {
                            isMuted = !isMuted
                            audioPlayer.setVolume(if (isMuted) 0 else volume)
                        }) {
                            Icon(
                                imageVector = when {
                                    isMuted || volume == 0 -> Icons.AutoMirrored.Filled.VolumeOff
                                    volume < 50 -> Icons.AutoMirrored.Filled.VolumeMute
                                    else -> Icons.AutoMirrored.Filled.VolumeUp
                                },
                                contentDescription = "Volume"
                            )
                        }

                        AnimatedVisibility(visible = isVolumeHovered) {
                            val volumeFraction = if (isMuted) 0f else volume / 100f
                            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                                Slider(
                                    value = volumeFraction,
                                    onValueChange = { fraction ->
                                        volume = (fraction * 100).toInt().coerceIn(0, 100)
                                        isMuted = volume == 0
                                        audioPlayer.setVolume(volume)
                                    },
                                    modifier = Modifier.width(90.dp),
                                    thumb = {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                    },
                                    track = {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .fillMaxWidth(volumeFraction)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(MaterialTheme.colorScheme.primary)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }

                    IconButton(onClick = { /* TODO: queue */ }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Queue")
                    }

                    IconButton(onClick = { /* TODO: fullscreen */ }) {
                        Icon(imageVector = Icons.Filled.Fullscreen, contentDescription = "Fullscreen")
                    }
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
