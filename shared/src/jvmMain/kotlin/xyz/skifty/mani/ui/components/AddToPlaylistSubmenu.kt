package xyz.skifty.mani.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.context_menu_no_playlists
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.api.ApiService
import xyz.skifty.mani.ext.contextMenuContainer
import xyz.skifty.mani.media.PlaylistInfo
import xyz.skifty.mani.media.PlaylistLibrary
import xyz.skifty.mani.ui.components.util.FixedPositionProvider

private val SUBMENU_MAX_HEIGHT = 320.dp

/** Flyout shown while hovering [SongContextMenu]'s "Add to Playlist" row, opened at
 *  [positionInWindow] (the hovered row's own top-right corner, in window coordinates - see
 *  [FixedPositionProvider] for why this is passed explicitly rather than resolved automatically),
 *  scrollable if the playlist list runs long. Triggers [PlaylistLibrary.ensureLoaded] the first
 *  time it's actually opened, so the underlying fetch only ever happens once per session, not
 *  once per hover. [onHoverChanged] reports whether the cursor is over this flyout itself, so
 *  [SongContextMenu] can keep it open while the cursor is here even after it's left the trigger
 *  row that opened it. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AddToPlaylistSubmenu(
    positionInWindow: Offset,
    apiService: ApiService,
    playlistLibrary: PlaylistLibrary,
    onHoverChanged: (Boolean) -> Unit,
    onSelectPlaylist: (PlaylistInfo) -> Unit,
) {
    LaunchedEffect(Unit) {
        playlistLibrary.ensureLoaded(apiService)
    }

    Popup(
        popupPositionProvider = remember(positionInWindow) {
            FixedPositionProvider(IntOffset(positionInWindow.x.toInt(), positionInWindow.y.toInt()))
        },
    ) {
        Surface(
            modifier = Modifier
                // Same reasoning as SongContextMenu - without a bounded width, this would
                // stretch to the full window width instead of hugging its content.
                .width(IntrinsicSize.Max)
                .onPointerEvent(PointerEventType.Enter) { onHoverChanged(true) }
                .onPointerEvent(PointerEventType.Exit) { onHoverChanged(false) },
            shape = MaterialTheme.shapes.extraSmall,
            color = MaterialTheme.colorScheme.contextMenuContainer,
            tonalElevation = 3.dp,
            shadowElevation = 3.dp,
        ) {
            val playlists = playlistLibrary.playlists
            when {
                playlists == null -> Column(modifier = Modifier.padding(16.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                }

                playlists.isEmpty() -> DropdownMenuItem(
                    text = { Text(stringResource(Res.string.context_menu_no_playlists)) },
                    onClick = {},
                    enabled = false,
                )

                else -> Column(
                    modifier = Modifier
                        .heightIn(max = SUBMENU_MAX_HEIGHT)
                        .verticalScroll(rememberScrollState()),
                ) {
                    for (playlist in playlists) {
                        DropdownMenuItem(
                            text = { Text(playlist.name) },
                            onClick = { onSelectPlaylist(playlist) },
                        )
                    }
                }
            }
        }
    }
}
