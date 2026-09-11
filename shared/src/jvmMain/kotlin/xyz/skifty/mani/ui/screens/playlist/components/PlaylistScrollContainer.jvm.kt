package xyz.skifty.mani.ui.screens.playlist.components

import androidx.compose.runtime.Composable

// Desktop already scrolls whatever screen is showing via JvmApp's own shared, app-wide
// ScrollState (also what AutoHidingScrollbar reads) - PlaylistScreen has never owned scrolling
// itself here, and the reveal-on-scroll-up search field is an Android-only mechanism (desktop
// already has its own always-available field in PlaylistActionsRow), so this is a no-op.
@Composable
actual fun PlaylistScrollContainer(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    content: @Composable () -> Unit,
) {
    content()
}
