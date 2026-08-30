package xyz.skifty.mani.ui.components.nowplayingpanel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** How wide the whole window needs to be for the panel to fit alongside the sidebar and main
 *  content without cramping either - see JvmApp, which hides the panel entirely below this
 *  (except when the Queue view is forced open). */
val NOW_PLAYING_PANEL_WIDTH = 320.dp

/** The right-side panel's shared shell - fixed-width `Column` plus a leading `VerticalDivider`,
 *  mirroring Sidebar's own structure but anchored to the trailing edge instead of the leading one.
 *  Shared by [NowPlayingPanel] and `QueuePanel` (the two contents that swap into this same slot),
 *  so the divider/width/background/padding treatment can't drift between them. */
@Composable
fun NowPlayingSidePanelShell(content: @Composable ColumnScope.() -> Unit) {
    Row(modifier = Modifier.fillMaxHeight()) {
        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(
            modifier = Modifier
                .width(NOW_PLAYING_PANEL_WIDTH)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(20.dp),
            content = content,
        )
    }
}
