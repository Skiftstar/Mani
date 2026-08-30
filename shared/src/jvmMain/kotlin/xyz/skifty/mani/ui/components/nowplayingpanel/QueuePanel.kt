package xyz.skifty.mani.ui.components.nowplayingpanel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.cd_close_queue
import mani.shared.generated.resources.queue_empty_state
import mani.shared.generated.resources.queue_title
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.media.PlaybackQueue
import xyz.skifty.mani.ui.components.AutoHidingScrollbar
import xyz.skifty.mani.ui.components.QueueSongRow

/** Replaces [NowPlayingPanel] in the same side-panel slot while the queue view is open - a title
 *  row with a close (`X`) button to switch back, then every [PlaybackQueue.upcoming] song, each
 *  clickable to skip straight to it ([PlaybackQueue.skipTo]) with its own remove button
 *  ([PlaybackQueue.removeAt]) - or an empty-state message when nothing's queued. */
@Composable
fun QueuePanel(
    playbackQueue: PlaybackQueue,
    onClose: () -> Unit,
) {
    NowPlayingSidePanelShell {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.queue_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(Res.string.cd_close_queue),
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        val upcoming = playbackQueue.upcoming
        if (upcoming.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.queue_empty_state),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            val scrollState = rememberScrollState()
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    for (entry in upcoming) {
                        QueueSongRow(
                            songInfo = entry.song,
                            onClick = {
                                playbackQueue.skipTo(entry.position)
                            },
                            onRemove = {
                                playbackQueue.removeAt(entry.position)
                            },
                        )
                    }
                }
                AutoHidingScrollbar(
                    scrollState = scrollState,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight(),
                )
            }
        }
    }
}
