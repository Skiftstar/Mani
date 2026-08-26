package xyz.skifty.moonlight.ui.components.util

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider

/** Places a [androidx.compose.ui.window.Popup] at an exact, already-known window position,
 *  ignoring the `anchorBounds` Compose would otherwise try to resolve automatically - necessary
 *  once a Popup is nested inside another Popup's content (as both the song context menu and its
 *  "Add to Playlist" flyout are), since automatic anchor-bounds resolution doesn't reliably see
 *  through that nesting and can resolve to something degenerate (observed: menus anchoring near
 *  the window's top-left corner instead of their real on-screen position). [position] should be
 *  captured via `Modifier.onGloballyPositioned { it.positionInWindow() }` on the real anchor. */
class FixedPositionProvider(private val position: IntOffset) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = position
}
