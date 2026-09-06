package xyz.skifty.mani.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import mani.shared.generated.resources.delete_playlist_cancel_button
import mani.shared.generated.resources.delete_playlist_confirm_button
import mani.shared.generated.resources.delete_playlist_dialog_title
import mani.shared.generated.resources.delete_playlist_error
import mani.shared.generated.resources.delete_playlist_message
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.api.ApiService
import xyz.skifty.mani.ext.popupContainer
import xyz.skifty.mani.media.PlaylistInfo
import xyz.skifty.mani.media.PlaylistLibrary

/** Confirmation dialog for permanently deleting [playlist] - same shared-across-both-platforms
 *  approach as [CreatePlaylistDialog], for the same reason (an [AlertDialog] behaves identically
 *  on desktop and Android). [onDeleted] fires only on a *successful* delete, separate from
 *  [onDismissRequest] (which fires on cancel too) - desktop uses it to navigate away if the
 *  deleted playlist was the one currently open; Android has no such case, so it leaves it at its
 *  default no-op (playlist deletion there only ever happens from the library list screen, never
 *  from within the playlist's own detail view). */
@Composable
fun DeletePlaylistDialog(
    apiService: ApiService,
    playlistLibrary: PlaylistLibrary,
    playlist: PlaylistInfo,
    onDismissRequest: () -> Unit,
    onDeleted: () -> Unit = {},
) {
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun deletePlaylist() {
        if (isLoading) {
            return
        }
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            apiService.deletePlaylist(playlist.id)
                .onSuccess {
                    playlistLibrary.refreshPlaylists(apiService)
                    onDeleted()
                    onDismissRequest()
                }
                .onFailure {
                    isLoading = false
                    errorMessage = getString(Res.string.delete_playlist_error)
                }
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.popupContainer,
        title = { Text(stringResource(Res.string.delete_playlist_dialog_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(Res.string.delete_playlist_message, playlist.name),
                    style = MaterialTheme.typography.bodyMedium,
                )
                errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = ::deletePlaylist,
                enabled = !isLoading,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(Res.string.delete_playlist_confirm_button))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                enabled = !isLoading,
            ) {
                Text(stringResource(Res.string.delete_playlist_cancel_button))
            }
        },
    )
}
