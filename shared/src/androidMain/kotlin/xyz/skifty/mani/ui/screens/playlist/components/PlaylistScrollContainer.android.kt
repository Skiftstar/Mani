package xyz.skifty.mani.ui.screens.playlist.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.cd_clear_search
import mani.shared.generated.resources.cd_search
import mani.shared.generated.resources.playlist_search_placeholder
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.mani.ext.trackTextFieldFocus

// How far the user needs to pull down, while already at the top of the list, before it counts as
// a deliberate "reveal search" gesture rather than a small accidental overscroll bounce.
private val SEARCH_REVEAL_PULL_THRESHOLD = 32.dp

private val SEARCH_FIELD_HEIGHT = 40.dp
private val SEARCH_FIELD_ICON_SIZE = 18.dp

/** Takes over scrolling for `PlaylistScreen` on Android specifically, so it can react to scroll
 *  gestures at the boundary: a playlist-search field - hidden when the screen first opens -
 *  reveals itself only when the user is already at the very top of the list and pulls down
 *  further (an overscroll past the start, the same gesture pull-to-refresh uses) past
 *  [SEARCH_REVEAL_PULL_THRESHOLD], and hides again the moment real forward scrolling resumes.
 *
 *  A plain [androidx.compose.foundation.ScrollState] has no way to represent "tried to scroll
 *  past the top" at all - overscroll there simply doesn't change its `value` - so this needs a
 *  [NestedScrollConnection] observing the raw, not-yet-consumed drag amount instead, the same
 *  idiom `QueueScreen`'s own swipe-to-dismiss already uses (there via a `LazyListState`; here via
 *  this screen's plain `ScrollState`, since `PlaylistSongTable` isn't lazy).
 *
 *  Not auto-focused when revealed - it appeared from a scroll gesture, not an explicit
 *  tap-to-search action, so popping the keyboard unprompted would be surprising. Self-drawn on
 *  [BasicTextField] rather than Material3's `OutlinedTextField`, matching the same
 *  no-outline/static-border/fading-placeholder look as the global `SearchBar` (jvmMain-only, not
 *  visible to this file) and desktop's `PlaylistActionsRow` field - deliberately duplicated
 *  rather than shared across those, since neither lives somewhere this androidMain file can reach
 *  without a bigger, unrelated refactor. */
@Composable
actual fun PlaylistScrollContainer(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    content: @Composable () -> Unit,
) {
    val scrollState = rememberScrollState()
    var isSearchVisible by remember { mutableStateOf(false) }
    var pulledPx by remember { mutableFloatStateOf(0f) }
    val revealThresholdPx = with(LocalDensity.current) { SEARCH_REVEAL_PULL_THRESHOLD.toPx() }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val isAtTop = scrollState.value == 0
                if (isAtTop && available.y > 0f) {
                    // Dragging down while already at the top - an overscroll attempting to
                    // scroll further up than the list allows - is exactly the "at the top edge,
                    // then scroll up" gesture this reveals on.
                    pulledPx += available.y
                    if (pulledPx > revealThresholdPx) {
                        isSearchVisible = true
                    }
                } else {
                    pulledPx = 0f
                }
                if (available.y < 0f) {
                    // Real forward scrolling (content moving up) hides it again immediately.
                    isSearchVisible = false
                }
                // Never actually consumed - this only observes the gesture, the list itself
                // still handles (or clamps) it exactly as it would with no connection at all.
                return Offset.Zero
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = isSearchVisible) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(SEARCH_FIELD_HEIGHT)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(percent = 50),
                    )
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(Res.string.cd_search),
                    modifier = Modifier.size(SEARCH_FIELD_ICON_SIZE),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.playlist_search_placeholder),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .trackTextFieldFocus(),
                    )
                }

                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = stringResource(Res.string.cd_clear_search),
                            modifier = Modifier.size(SEARCH_FIELD_ICON_SIZE),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .nestedScroll(nestedScrollConnection)
                .verticalScroll(scrollState),
        ) {
            content()
        }
    }
}
