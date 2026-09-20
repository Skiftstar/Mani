package xyz.skifty.mani.ui.screens.profile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.recap_playlist_name_format
import mani.shared.generated.resources.recap_save_as_image
import mani.shared.generated.resources.recap_save_as_playlist
import mani.shared.generated.resources.recap_save_as_playlist_error
import mani.shared.generated.resources.recap_save_as_playlist_success
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.api.ApiService

/** The Recap tab's two save actions (3b/6b in the design). "Save as playlist" takes the range's
 *  top [songIds] and hands off to the same [apiService] endpoints
 *  [xyz.skifty.mani.ui.components.CreatePlaylistDialog] uses, prefilled with a name derived from
 *  [rangeLabel]. "Save recap as image" is intentionally disabled - composable-to-image export plus
 *  a platform share/save flow is new infra with no existing precedent in the app; deferred rather
 *  than built in this pass. */
@Composable
fun SaveRecapActions(rangeLabel: String, songIds: List<String>, apiService: ApiService, modifier: Modifier = Modifier) {
    var isSaving by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun saveAsPlaylist() {
        if (isSaving || songIds.isEmpty()) {
            return
        }
        isSaving = true
        resultMessage = null
        coroutineScope.launch {
            val playlistName = getString(Res.string.recap_playlist_name_format, rangeLabel)
            apiService.createPlaylist(playlistName)
                .onSuccess { playlist ->
                    for (songId in songIds) {
                        apiService.addSongToPlaylist(playlist.id, songId)
                    }
                    resultMessage = getString(Res.string.recap_save_as_playlist_success)
                }
                .onFailure {
                    resultMessage = getString(Res.string.recap_save_as_playlist_error)
                }
            isSaving = false
        }
    }

    Column(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = ::saveAsPlaylist,
                enabled = !isSaving && songIds.isNotEmpty(),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(Res.string.recap_save_as_playlist))
                }
            }
            // TODO: composable-to-bitmap export + platform share/save - deferred, see the Profile
            //  screen implementation plan.
            OutlinedButton(
                onClick = {},
                enabled = false,
            ) {
                Text(stringResource(Res.string.recap_save_as_image))
            }
        }
        resultMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
