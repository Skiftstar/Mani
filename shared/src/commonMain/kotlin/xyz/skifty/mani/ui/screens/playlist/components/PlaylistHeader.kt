package xyz.skifty.mani.ui.screens.playlist.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.cd_playlist_cover
import mani.shared.generated.resources.playlist_badge
import mani.shared.generated.resources.playlist_runtime_hours_minutes
import mani.shared.generated.resources.playlist_runtime_minutes_only
import mani.shared.generated.resources.playlist_song_count
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.ext.toHoursAndMinutes
import xyz.skifty.mani.ext.totalRuntimeSeconds
import xyz.skifty.mani.media.PlaylistDetails

/** Cover art (or a heart fallback for the Liked Songs pseudo-playlist) + a "Playlist" badge,
 *  large title, and owner (if the server reports one)/song count. */
@Composable
fun PlaylistHeader(details: PlaylistDetails, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (details.coverArtUrl != null) {
            AsyncImage(
                model = details.coverArtUrl,
                contentDescription = stringResource(Res.string.cd_playlist_cover),
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(160.dp)
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
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = stringResource(Res.string.playlist_badge),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Text(
                text = details.name,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
            )

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
            )
        }
    }
}
