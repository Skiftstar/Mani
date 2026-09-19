package xyz.skifty.mani.ui.components.nowplayingpanel.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.now_playing_panel_plays_count
import mani.shared.generated.resources.playlist_column_quality
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.ext.qualityLabel
import xyz.skifty.mani.ext.toVibeProfileOrNull
import xyz.skifty.mani.media.SongInfo

/** Quality (bit rate/format), play count, and (when the server provides them) VibeNet mood tags -
 *  the panel's "song info" section. A server that omits playCount entirely for a never-played song
 *  is indistinguishable from one that just doesn't report it at all, so [SongInfo.songPlayCount]
 *  being null is treated the same as 0 rather than hiding the row. Mood tags are all-or-nothing
 *  instead - see [toVibeProfileOrNull]/[PanelMoodTags] - a stock server (or any song this app's own
 *  Navidrome fork hasn't tagged) just doesn't show that row at all. */
@Composable
fun PanelSongDetails(songInfo: SongInfo, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "${stringResource(Res.string.playlist_column_quality)}: ${songInfo.qualityLabel()}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(Res.string.now_playing_panel_plays_count, songInfo.songPlayCount ?: 0),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        songInfo.toVibeProfileOrNull()?.let { vibeProfile ->
            PanelMoodTags(
                vibeProfile = vibeProfile,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
