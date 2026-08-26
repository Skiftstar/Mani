package xyz.skifty.mani.ext

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/** A menu/flyout container color, distinct from [ColorScheme.surfaceContainer] - this theme sets
 *  that equal to `background` (see Theme.kt's flat-surface reasoning), so a menu using it would
 *  be invisible against the page behind it. Blends most of the way toward `surfaceContainerHigh`
 *  (a genuinely brighter, auto-derived tone) while staying a little darker than it, for contrast
 *  without feeling too bright against the rest of the app's flat look. */
val ColorScheme.contextMenuContainer: Color
    get() = lerp(surfaceContainer, surfaceContainerHigh, 0.85f)
