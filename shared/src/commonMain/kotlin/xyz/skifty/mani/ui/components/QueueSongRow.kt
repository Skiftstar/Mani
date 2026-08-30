package xyz.skifty.mani.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.cd_remove_from_queue
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.media.SongInfo
import xyz.skifty.mani.ui.components.nowplaying.components.TrackInfo

/** One row in a Queue view (desktop's QueuePanel, Android's QueueScreen) - [songInfo]'s
 *  cover/title/artist (via the shared [TrackInfo]), clickable to skip straight to it, plus a
 *  trailing remove button. */
@Composable
fun QueueSongRow(
    songInfo: SongInfo,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TrackInfo(
            songInfo = songInfo,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick),
        )
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(Res.string.cd_remove_from_queue),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
