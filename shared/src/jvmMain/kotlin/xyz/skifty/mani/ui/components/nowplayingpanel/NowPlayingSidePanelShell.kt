package xyz.skifty.mani.ui.components.nowplayingpanel

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import java.awt.Cursor
import org.koin.compose.koinInject
import xyz.skifty.mani.preferences.AppPreferences

private const val PANEL_WIDTH_PREFERENCE_KEY = "mani_now_playing_panel_width"

/** Default width - before any saved [PANEL_WIDTH_PREFERENCE_KEY] preference is loaded, or if the
 *  user has never dragged the divider. Also what JvmApp's own "is the window wide enough to show
 *  this panel at all" decision (`MIN_WINDOW_WIDTH_FOR_NOW_PLAYING_PANEL`) is sized against -
 *  independent of whatever the user has since resized the actual panel to. */
val NOW_PLAYING_PANEL_WIDTH = 320.dp

private val MIN_NOW_PLAYING_PANEL_WIDTH = 240.dp
private val MAX_NOW_PLAYING_PANEL_WIDTH = 480.dp

/** The right-side panel's shared shell - a `Column` plus a leading `VerticalDivider`, mirroring
 *  Sidebar's own structure but anchored to the trailing edge instead of the leading one. The
 *  divider is itself draggable to resize the panel (clamped to
 *  [MIN_NOW_PLAYING_PANEL_WIDTH]/[MAX_NOW_PLAYING_PANEL_WIDTH], persisted across restarts via
 *  [AppPreferences] the same way [xyz.skifty.mani.AppShellState] persists volume/last-song). Shared
 *  by [NowPlayingPanel] and `QueuePanel` (the two contents that swap into this same slot), so the
 *  divider/width/background/padding treatment - and the resized width itself - can't drift between
 *  them. */
@Composable
fun NowPlayingSidePanelShell(content: @Composable ColumnScope.() -> Unit) {
    val appPreferences = koinInject<AppPreferences>()
    var panelWidth by remember { mutableStateOf(NOW_PLAYING_PANEL_WIDTH) }

    LaunchedEffect(Unit) {
        runCatching {
            appPreferences.get(PANEL_WIDTH_PREFERENCE_KEY)
                ?.toFloatOrNull()
                ?.dp
                ?.coerceIn(MIN_NOW_PLAYING_PANEL_WIDTH, MAX_NOW_PLAYING_PANEL_WIDTH)
                ?.let { savedWidth -> panelWidth = savedWidth }
        }
    }

    LaunchedEffect(panelWidth) {
        runCatching { appPreferences.save(PANEL_WIDTH_PREFERENCE_KEY, panelWidth.value.toString()) }
    }

    val density = LocalDensity.current
    val dragState = rememberDraggableState { deltaPx ->
        // The divider sits on the panel's *leading* (left) edge, but the panel itself hugs the
        // window's trailing edge - so dragging left (negative delta) grows the panel and dragging
        // right shrinks it, the opposite sign from the raw delta.
        val deltaDp = with(density) { deltaPx.toDp() }
        panelWidth = (panelWidth - deltaDp).coerceIn(MIN_NOW_PLAYING_PANEL_WIDTH, MAX_NOW_PLAYING_PANEL_WIDTH)
    }

    Row(modifier = Modifier.fillMaxHeight()) {
        VerticalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier
                .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)))
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                ),
        )
        Column(
            modifier = Modifier
                .width(panelWidth)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(20.dp),
            content = content,
        )
    }
}
