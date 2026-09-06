package xyz.skifty.mani.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.create_playlist_cancel_button
import mani.shared.generated.resources.create_playlist_confirm_button
import mani.shared.generated.resources.create_playlist_dialog_title
import mani.shared.generated.resources.create_playlist_error
import mani.shared.generated.resources.create_playlist_name_label
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.api.ApiService
import xyz.skifty.mani.ext.popupContainer
import xyz.skifty.mani.ext.trackTextFieldFocus
import xyz.skifty.mani.media.PlaylistLibrary

private val NAME_FIELD_HEIGHT = 48.dp

/** Dialog for creating a brand new, empty playlist - the same composable is used from both
 *  desktop's Sidebar ("+" below Liked Songs) and Android's PlaylistLibraryListScreen ("+" in the
 *  top right), since an [AlertDialog] behaves identically on both targets and there's nothing
 *  platform-specific about a name prompt (unlike the song context menu, which genuinely differs
 *  between right-click and long-press). On success, refreshes [playlistLibrary] so every screen
 *  reading its `playlists` state picks up the new playlist automatically. */
@Composable
fun CreatePlaylistDialog(
    apiService: ApiService,
    playlistLibrary: PlaylistLibrary,
    onDismissRequest: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun createPlaylist() {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty() || isLoading) {
            return
        }
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            apiService.createPlaylist(trimmedName)
                .onSuccess {
                    playlistLibrary.refreshPlaylists(apiService)
                    onDismissRequest()
                }
                .onFailure {
                    isLoading = false
                    errorMessage = getString(Res.string.create_playlist_error)
                }
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.popupContainer,
        title = { Text(stringResource(Res.string.create_playlist_dialog_title)) },
        text = {
            Column {
                // Self-drawn on BasicTextField rather than Material3's OutlinedTextField, same
                // approach as SearchBar and for the same reason - a static border and a
                // placeholder that just fades out (instead of animating into the border as a
                // floating label) reads as calmer than OutlinedTextField's focus-color-changing
                // outline, and matches the rest of the app's text input everywhere else.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(NAME_FIELD_HEIGHT)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 16.dp),
                ) {
                    if (name.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.create_playlist_name_label),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterStart),
                        )
                    }
                    BasicTextField(
                        value = name,
                        onValueChange = { value ->
                            name = value
                            errorMessage = null
                        },
                        singleLine = true,
                        enabled = !isLoading,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.CenterStart)
                            .trackTextFieldFocus(),
                    )
                }
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
                onClick = ::createPlaylist,
                enabled = name.isNotBlank() && !isLoading,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(Res.string.create_playlist_confirm_button))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                enabled = !isLoading,
            ) {
                Text(stringResource(Res.string.create_playlist_cancel_button))
            }
        },
    )
}
