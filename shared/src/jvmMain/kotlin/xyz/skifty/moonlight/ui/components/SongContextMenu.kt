package xyz.skifty.moonlight.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import moonlight.shared.generated.resources.Res
import moonlight.shared.generated.resources.context_menu_add_to_playlist
import moonlight.shared.generated.resources.context_menu_add_to_queue
import moonlight.shared.generated.resources.context_menu_like
import moonlight.shared.generated.resources.context_menu_play
import moonlight.shared.generated.resources.context_menu_remove_from_playlist
import moonlight.shared.generated.resources.context_menu_unlike
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.moonlight.api.ApiService
import xyz.skifty.moonlight.ext.contextMenuContainer
import xyz.skifty.moonlight.media.PlaylistInfo
import xyz.skifty.moonlight.media.PlaylistLibrary
import xyz.skifty.moonlight.media.SongInfo
import xyz.skifty.moonlight.ui.components.util.FixedPositionProvider

// How long the "Add to Playlist" flyout stays open after the cursor leaves both it and its
// trigger row, before actually closing - long enough to cross the small gap between them (moving
// diagonally, not perfectly horizontally, briefly leaves both) without the flyout vanishing
// mid-transition, short enough not to feel like it lingers.
private const val SUBMENU_CLOSE_DELAY_MS = 200L

/** Right-click menu for a song row - Play (replace the active queue with just this song), Add to
 *  Queue, Like/Unlike, Add to Playlist (a hover-out flyout, see [AddToPlaylistSubmenu]), and -
 *  only when [onRemoveFromPlaylist] is non-null - Remove from Playlist. Every action dismisses
 *  the menu afterward except hovering Add to Playlist, which only opens its flyout.
 *
 *  A raw [Popup] rather than Material3's [androidx.compose.material3.DropdownMenu] - its `offset`
 *  param is added on top of a default anchor-relative position (below the anchor's bounds), which
 *  isn't what's wanted here (the menu should open exactly at the cursor) and is awkward to cancel
 *  out reliably; [FixedPositionProvider] instead places it at [positionInWindow] directly. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SongContextMenu(
    positionInWindow: Offset,
    onDismissRequest: () -> Unit,
    songInfo: SongInfo,
    apiService: ApiService,
    playlistLibrary: PlaylistLibrary,
    onPlay: () -> Unit,
    onAddToQueue: () -> Unit,
    onToggleStar: () -> Unit,
    onAddToPlaylist: (PlaylistInfo) -> Unit,
    onRemoveFromPlaylist: (() -> Unit)?,
) {
    Popup(
        popupPositionProvider = remember(positionInWindow) {
            FixedPositionProvider(IntOffset(positionInWindow.x.toInt(), positionInWindow.y.toInt()))
        },
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        Surface(
            // Popups otherwise get unbounded width from their default measurement, and
            // DropdownMenuItem fills whatever width it's given - without this, the menu would
            // stretch to the full window width instead of hugging its content.
            modifier = Modifier.width(IntrinsicSize.Max),
            shape = MaterialTheme.shapes.extraSmall,
            color = MaterialTheme.colorScheme.contextMenuContainer,
            tonalElevation = 3.dp,
            shadowElevation = 3.dp,
        ) {
            Column {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.context_menu_play)) },
                    onClick = {
                        onPlay()
                        onDismissRequest()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.context_menu_add_to_queue)) },
                    onClick = {
                        onAddToQueue()
                        onDismissRequest()
                    },
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                if (songInfo.starred) Res.string.context_menu_unlike else Res.string.context_menu_like,
                            ),
                        )
                    },
                    onClick = {
                        onToggleStar()
                        onDismissRequest()
                    },
                )

                // Two separate hover flags, not one shared between the trigger row and the
                // flyout itself: moving the cursor from one to the other crosses a small real
                // gap where *neither* reports hovered for a moment, so a single flag (or acting
                // on Exit immediately) closes the flyout mid-transition. isSubmenuVisible instead
                // debounces going to fully-unhovered by SUBMENU_CLOSE_DELAY_MS, cancelled the
                // instant either side is hovered again.
                var isTriggerHovered by remember { mutableStateOf(false) }
                var isSubmenuHovered by remember { mutableStateOf(false) }
                var isSubmenuVisible by remember { mutableStateOf(false) }
                var addToPlaylistTopRight by remember { mutableStateOf(Offset.Zero) }

                LaunchedEffect(isTriggerHovered, isSubmenuHovered) {
                    if (isTriggerHovered || isSubmenuHovered) {
                        isSubmenuVisible = true
                    } else {
                        delay(SUBMENU_CLOSE_DELAY_MS)
                        isSubmenuVisible = false
                    }
                }

                Box(
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        addToPlaylistTopRight = coordinates.positionInWindow() +
                            Offset(coordinates.size.width.toFloat(), 0f)
                    },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.context_menu_add_to_playlist)) },
                        onClick = {},
                        modifier = Modifier
                            .onPointerEvent(PointerEventType.Enter) { isTriggerHovered = true }
                            .onPointerEvent(PointerEventType.Exit) { isTriggerHovered = false },
                    )
                    if (isSubmenuVisible) {
                        AddToPlaylistSubmenu(
                            positionInWindow = addToPlaylistTopRight,
                            apiService = apiService,
                            playlistLibrary = playlistLibrary,
                            onHoverChanged = { hovered -> isSubmenuHovered = hovered },
                            onSelectPlaylist = { playlist ->
                                onAddToPlaylist(playlist)
                                onDismissRequest()
                            },
                        )
                    }
                }

                onRemoveFromPlaylist?.let { remove ->
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.context_menu_remove_from_playlist)) },
                        onClick = {
                            remove()
                            onDismissRequest()
                        },
                    )
                }
            }
        }
    }
}
