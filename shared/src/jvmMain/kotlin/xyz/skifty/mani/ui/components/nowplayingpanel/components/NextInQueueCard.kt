package xyz.skifty.mani.ui.components.nowplayingpanel.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.now_playing_panel_next_in_queue
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.media.SongInfo
import xyz.skifty.mani.ui.components.nowplaying.components.TrackInfo

/** "Next in Queue" header + a compact preview of [songInfo] (the queue's upcoming track, reusing
 *  the bottom playback bar's own cover+title/artist layout), pinned to the bottom of the
 *  now-playing panel. Clicking the preview skips straight to that song, via [onClick]. */
@Composable
fun NextInQueueCard(songInfo: SongInfo, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(Res.string.now_playing_panel_next_in_queue),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TrackInfo(
            songInfo = songInfo,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clickable(onClick = onClick),
        )
    }
}
