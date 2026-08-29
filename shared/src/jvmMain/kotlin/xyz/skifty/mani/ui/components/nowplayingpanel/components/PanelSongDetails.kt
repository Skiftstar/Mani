package xyz.skifty.mani.ui.components.nowplayingpanel.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import xyz.skifty.mani.media.SongInfo

/** Quality (bit rate/format) and play count - the panel's "song info" section. A server that
 *  omits playCount entirely for a never-played song is indistinguishable from one that just
 *  doesn't report it at all, so [SongInfo.songPlayCount] being null is treated the same as 0
 *  rather than hiding the row. */
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
    }
}
