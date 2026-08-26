package xyz.skifty.mani.ext

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset

// No secondary-click concept on touch (no Android long-press equivalent is in scope yet - see
// PlaylistSongRow's own doc comment).
actual fun Modifier.detectSecondaryClick(onSecondaryClick: (positionInModifier: Offset) -> Unit): Modifier = this
