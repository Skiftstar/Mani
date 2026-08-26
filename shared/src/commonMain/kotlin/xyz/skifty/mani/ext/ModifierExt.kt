package xyz.skifty.mani.ext

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import xyz.skifty.mani.ui.components.util.LocalTextFieldFocusTracker

/** Detects a right-click/secondary-click at this modifier's own local position, on platforms
 *  that have one - a no-op on platforms that don't (there's no touch equivalent, and adding one,
 *  e.g. long-press, is out of scope for now). See jvmMain's actual for the real detection logic. */
expect fun Modifier.detectSecondaryClick(onSecondaryClick: (positionInModifier: Offset) -> Unit): Modifier

/** Registers this composable's focus state with [LocalTextFieldFocusTracker] - add to every text
 *  field's modifier chain (the search box, the login fields, and any added later) so desktop's
 *  app-wide Space-to-pause keybind in JvmApp() can tell not to fire while text is being typed.
 *  Harmless on platforms without that keybind (nothing reads [LocalTextFieldFocusTracker] there). */
@Composable
fun Modifier.trackTextFieldFocus(): Modifier {
    val tracker = LocalTextFieldFocusTracker.current
    // A stable identity for this call site, kept across recompositions - the tracker keys focus
    // state off this rather than a plain count, so it can't drift (see TextFieldFocusTracker).
    val id = remember { Any() }
    return this.onFocusChanged { state -> tracker.setFocused(id, state.isFocused) }
}
