package xyz.skifty.mani.ui.screens.playlist.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.cd_pause
import mani.shared.generated.resources.cd_play
import mani.shared.generated.resources.cd_playlist_cover
import mani.shared.generated.resources.playlist_runtime_hours_minutes
import mani.shared.generated.resources.playlist_runtime_minutes_only
import mani.shared.generated.resources.playlist_song_count
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.ext.toHoursAndMinutes
import xyz.skifty.mani.ext.totalRuntimeSeconds
import xyz.skifty.mani.media.AudioPlayer
import xyz.skifty.mani.media.PlaybackQueue
import xyz.skifty.mani.media.PlaylistDetails

/** Large centered cover, then title/owner/song-count sharing a row with the play button - the
 *  mobile layout convention (Spotify's playlist screen in particular), vs. desktop's more compact
 *  side-by-side [PlaylistHeader]. */
@Composable
actual fun PlaylistHeaderBlock(
    details: PlaylistDetails,
    audioPlayer: AudioPlayer,
    playbackQueue: PlaybackQueue,
    playlistId: String?,
    modifier: Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (details.coverArtUrl != null) {
            AsyncImage(
                model = details.coverArtUrl,
                contentDescription = stringResource(Res.string.cd_playlist_cover),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = stringResource(Res.string.cd_playlist_cover),
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val isThisPlaylistActive = playbackQueue.songs.isNotEmpty() && playbackQueue.currentSourceId == playlistId
        val isPlaying = isThisPlaylistActive && audioPlayer.isPlaying

        // Title/metadata and the play button share one row, vertically centered against both
        // lines together (not either line alone) - same shape as NowPlayingTitleRow's title/like
        // pairing.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Left-aligned, unlike the centered cover above - fillMaxWidth so Start alignment
                // has the full row to align against, rather than just centering a wrap-content-
                // sized text block.
                Text(
                    text = details.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(8.dp))

                val songCount = pluralStringResource(
                    Res.plurals.playlist_song_count,
                    details.songs.size,
                    details.songs.size,
                )
                val (runtimeHours, runtimeMinutes) = details.totalRuntimeSeconds().toHoursAndMinutes()
                val runtimeLabel = if (runtimeHours > 0) {
                    stringResource(Res.string.playlist_runtime_hours_minutes, runtimeHours, runtimeMinutes)
                } else {
                    stringResource(Res.string.playlist_runtime_minutes_only, runtimeMinutes)
                }
                val metadataLine = listOfNotNull(details.ownerName, songCount, runtimeLabel)
                    .joinToString(" • ")
                Text(
                    text = metadataLine,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            FilledIconButton(
                onClick = {
                    if (isThisPlaylistActive) {
                        audioPlayer.togglePlayPause()
                    } else {
                        playbackQueue.start(details.songs, 0, playlistId)
                    }
                },
                modifier = Modifier.size(64.dp),
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(
                        if (isPlaying) Res.string.cd_pause else Res.string.cd_play,
                    ),
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}
