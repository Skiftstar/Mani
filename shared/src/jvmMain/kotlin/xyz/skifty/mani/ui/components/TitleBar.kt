package xyz.skifty.mani.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.WindowState
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.app_name
import mani.shared.generated.resources.cd_close_window
import mani.shared.generated.resources.cd_maximize_window
import mani.shared.generated.resources.cd_minimize_window
import mani.shared.generated.resources.cd_restore_window
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.ui.theme.BottomBarBackground

private val TITLE_BAR_HEIGHT = 36.dp
private val TITLE_BAR_BUTTON_SIZE = 36.dp
private val TITLE_BAR_ICON_SIZE = 16.dp

/** Custom-drawn replacement for the OS title bar - `main()` creates its Window as
 *  `undecorated = true`, so there's no native chrome left to drag or minimize/maximize/close by.
 *  [WindowDraggableArea] (the standard mechanism for undecorated windows, e.g. Discord/Spotify's
 *  own title bars) makes the whole bar draggable to move the window; the three buttons on the
 *  right drive [windowState] directly (minimize, toggle maximize) or call [onCloseClick]. Painted
 *  the same [BottomBarBackground] tone as NowPlayingBottomWidget - a shade darker than the
 *  rest of the app's background - so it reads as a distinct surface bracketing the window at both
 *  top and bottom, with a divider marking where it ends. Requires a
 *  [xyz.skifty.mani.ui.theme.ManiTheme] ancestor to pick up the theme's other colors (text,
 *  divider) correctly, same as everything JvmApp renders (main() wraps this call in one itself,
 *  since this composes outside JvmApp's own).
 *
 *  Deliberately doesn't attempt edge-drag resizing or Aero Snap - undecorated windows lose both,
 *  and reimplementing them needs lower-level platform hooks outside Compose Desktop's own API. */
@Composable
fun WindowScope.TitleBar(
    windowState: WindowState,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isMaximized = windowState.placement == WindowPlacement.Maximized

    Column(modifier = modifier) {
        WindowDraggableArea(
            modifier = Modifier
                .fillMaxWidth()
                .height(TITLE_BAR_HEIGHT)
                .background(BottomBarBackground),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.app_name),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )

                TitleBarIconButton(
                    onClick = { windowState.isMinimized = true },
                    icon = Icons.Filled.Minimize,
                    contentDescription = stringResource(Res.string.cd_minimize_window),
                )

                TitleBarIconButton(
                    onClick = {
                        windowState.placement = if (isMaximized) {
                            WindowPlacement.Floating
                        } else {
                            WindowPlacement.Maximized
                        }
                    },
                    icon = if (isMaximized) Icons.Filled.FilterNone else Icons.Filled.CropSquare,
                    contentDescription = stringResource(
                        if (isMaximized) Res.string.cd_restore_window else Res.string.cd_maximize_window,
                    ),
                )

                TitleBarIconButton(
                    onClick = onCloseClick,
                    icon = Icons.Filled.Close,
                    contentDescription = stringResource(Res.string.cd_close_window),
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/** One title bar button - a plain white [icon], with a flat [MaterialTheme.colorScheme.outlineVariant]
 *  background (the same color used for dividers, including the one right below this whole bar)
 *  while hovered. Hover is tracked explicitly via [onPointerEvent], the same idiom this codebase
 *  already uses elsewhere (e.g. PlaylistSongRow's play/pause hover swap) rather than relying on
 *  IconButton's default ripple/indication, which doesn't read as a hover *background* the way a
 *  native title bar button's does. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun TitleBarIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
) {
    var isHovered by remember { mutableStateOf(false) }

    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(TITLE_BAR_BUTTON_SIZE)
            .background(if (isHovered) MaterialTheme.colorScheme.outlineVariant else Color.Transparent)
            .onPointerEvent(PointerEventType.Enter) { isHovered = true }
            .onPointerEvent(PointerEventType.Exit) { isHovered = false },
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(TITLE_BAR_ICON_SIZE),
            tint = Color.White,
        )
    }
}
