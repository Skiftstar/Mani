package xyz.skifty.moonlight.ui.screens.playlist.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import moonlight.shared.generated.resources.Res
import moonlight.shared.generated.resources.cd_album_art
import moonlight.shared.generated.resources.cd_pause
import moonlight.shared.generated.resources.cd_play
import moonlight.shared.generated.resources.unknown_artist
import moonlight.shared.generated.resources.unknown_title
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.moonlight.ext.toDurationLabel
import xyz.skifty.moonlight.media.DesktopAudioPlayer
import xyz.skifty.moonlight.media.SongInfo

/** One row of [PlaylistSongTable] - row number (swaps to a play/pause icon on hover), cover
 *  thumbnail + title/artist, quality, and duration. Whenever this row's song is the one currently
 *  loaded in [audioPlayer] (per [activeSongInfo]), its text is tinted the accent color and
 *  clicking it toggles play/pause instead of restarting the track from the beginning. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PlaylistSongRow(
    index: Int,
    songInfo: SongInfo,
    audioPlayer: DesktopAudioPlayer,
    activeSongInfo: SongInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isHovered by remember { mutableStateOf(false) }

    val isActive = songInfo.songId != null && songInfo.songId == activeSongInfo.songId
    val isPlaying = isActive && audioPlayer.isPlaying
    val contentColor = if (isActive) MaterialTheme.colorScheme.primary else LocalContentColor.current
    val secondaryColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClick = {
                    if (isActive) {
                        audioPlayer.togglePlayPause()
                    } else {
                        onClick()
                    }
                },
            )
            .onPointerEvent(PointerEventType.Enter) { isHovered = true }
            .onPointerEvent(PointerEventType.Exit) { isHovered = false }
            .padding(
                horizontal = 16.dp,
                vertical = 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.width(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isHovered) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(
                        if (isPlaying) Res.string.cd_pause else Res.string.cd_play,
                    ),
                    modifier = Modifier.size(16.dp),
                    tint = contentColor,
                )
            } else {
                Text(
                    text = index.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryColor,
                )
            }
        }

        AsyncImage(
            model = songInfo.songCoverArtUrl,
            contentDescription = stringResource(Res.string.cd_album_art),
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = songInfo.songName ?: stringResource(Res.string.unknown_title),
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = songInfo.songArtist ?: stringResource(Res.string.unknown_artist),
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Text(
            text = qualityLabel(songInfo),
            style = MaterialTheme.typography.bodyMedium,
            color = secondaryColor,
            modifier = Modifier.width(80.dp),
        )

        Text(
            text = songInfo.songDurationSeconds?.toDurationLabel() ?: "--:--",
            style = MaterialTheme.typography.bodyMedium,
            color = secondaryColor,
            textAlign = TextAlign.End,
            modifier = Modifier.width(48.dp),
        )
    }
}

private fun qualityLabel(songInfo: SongInfo): String {
    songInfo.songBitRateKbps?.let { bitRateKbps ->
        return "$bitRateKbps kbps"
    }
    return songInfo.songFormat?.uppercase() ?: "--"
}
