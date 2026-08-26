package xyz.skifty.mani.ui.components

import androidx.compose.ui.unit.dp

actual val showExtendedSongColumns: Boolean = false

// Smaller than desktop's 24dp - PlaylistSongRow's own 16dp internal padding already carries most
// of the margin here, so stacking the full header margin on top of it was making rows narrower
// than they needed to be.
actual val playlistTableHorizontalPadding = 8.dp
