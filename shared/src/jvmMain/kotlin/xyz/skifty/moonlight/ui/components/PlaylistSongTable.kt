package xyz.skifty.moonlight.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import moonlight.shared.generated.resources.Res
import moonlight.shared.generated.resources.cd_duration
import moonlight.shared.generated.resources.playlist_column_quality
import moonlight.shared.generated.resources.playlist_column_title
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.moonlight.api.ApiService
import xyz.skifty.moonlight.media.DesktopAudioPlayer
import xyz.skifty.moonlight.media.SongInfo

/** Column headers followed by one [PlaylistSongRow] per song. Owns the star/unstar toggle
 *  (calling [apiService] and optimistically flipping [SongInfo.starred], reverting it if the
 *  request fails) so every screen using this table gets identical behavior for free. */
@Composable
fun PlaylistSongTable(
    songs: List<SongInfo>,
    audioPlayer: DesktopAudioPlayer,
    activeSongInfo: SongInfo,
    apiService: ApiService,
    onSongClick: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()

    fun toggleStar(songInfo: SongInfo) {
        val songId = songInfo.songId
            ?: return
        val wasStarred = songInfo.starred
        songInfo.starred = !wasStarred
        coroutineScope.launch {
            val result = if (wasStarred) {
                apiService.unstar(songId)
            } else {
                apiService.star(songId)
            }
            if (result.isFailure) {
                songInfo.starred = wasStarred
            }
        }
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
                Text(
                    text = "#",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(modifier = Modifier.size(48.dp))
            Text(
                text = stringResource(Res.string.playlist_column_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(Res.string.playlist_column_quality),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(80.dp),
            )
            Box(
                modifier = Modifier.width(48.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = Icons.Filled.Schedule,
                    contentDescription = stringResource(Res.string.cd_duration),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(modifier = Modifier.size(32.dp))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        for ((index, songInfo) in songs.withIndex()) {
            PlaylistSongRow(
                index = index + 1,
                songInfo = songInfo,
                audioPlayer = audioPlayer,
                activeSongInfo = activeSongInfo,
                onClick = {
                    onSongClick(index)
                },
                onToggleStar = {
                    toggleStar(songInfo)
                },
            )
        }
    }
}
