package xyz.skifty.mani.ui.screens.playlist.components

import androidx.compose.runtime.Composable

/** Wraps `PlaylistScreen`'s content, owning whatever scrolling mechanism is appropriate per
 *  platform. Desktop's actual is a pure passthrough - it already scrolls externally via
 *  `JvmApp`'s own shared, app-wide scroll state, same as every other screen there. Android's
 *  actual takes over scrolling itself so it can react to scroll direction: it reveals a
 *  playlist-search field (hidden initially) when the user scrolls up, and hides it again on
 *  scrolling down - see that actual's own doc comment. */
@Composable
expect fun PlaylistScrollContainer(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    content: @Composable () -> Unit,
)
