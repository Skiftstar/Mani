package xyz.skifty.mani.ext

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/** A menu/dialog/popup container color, distinct from [ColorScheme.surfaceContainer] - this
 *  theme sets that equal to `background` (see Theme.kt's flat-surface reasoning), so a popup
 *  using it would be invisible against the page behind it. Reuses `surfaceVariant` - the app's
 *  own next-brightest tone above the flat background - rather than an auto-derived Material You
 *  tonal color (e.g. `surfaceContainerHigh`, left unset in ManiDarkColorScheme), so every popup
 *  (the desktop right-click menu, its Add to Playlist flyout, the create-playlist dialog) reads
 *  as part of the same deliberately flat palette instead of an off-theme algorithmic tone. */
val ColorScheme.popupContainer: Color
    get() = surfaceVariant
