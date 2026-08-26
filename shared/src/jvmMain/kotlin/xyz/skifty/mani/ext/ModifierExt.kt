package xyz.skifty.mani.ext

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import xyz.skifty.mani.ui.components.util.LocalTextFieldFocusTracker

/** Registers this composable's focus state with [LocalTextFieldFocusTracker] - add to every text
 *  field's modifier chain (the search box, the login fields, and any added later) so the app-wide
 *  Space-to-pause keybind in JvmApp() can tell not to fire while text is being typed. */
@Composable
fun Modifier.trackTextFieldFocus(): Modifier {
    val tracker = LocalTextFieldFocusTracker.current
    // A stable identity for this call site, kept across recompositions - the tracker keys focus
    // state off this rather than a plain count, so it can't drift (see TextFieldFocusTracker).
    val id = remember { Any() }
    return this.onFocusChanged { state -> tracker.setFocused(id, state.isFocused) }
}
