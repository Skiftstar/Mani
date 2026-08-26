package xyz.skifty.mani.ui.components.util

import androidx.compose.runtime.compositionLocalOf

/** Tracks which text fields currently have focus, app-wide, by identity rather than a plain
 *  count - a count would drift if a field's focus-changed callback ever fires an extra or
 *  out-of-order event for the same field (e.g. Compose reattaching the callback on recomposition,
 *  which for a search box happens on every keystroke) - one stray decrement without its matching
 *  increment would permanently misreport nothing as focused. Adding/removing the same field's id
 *  twice is a harmless no-op instead, so this can't desync the same way.
 *
 *  Reached via [LocalTextFieldFocusTracker] rather than a threaded parameter, so JvmApp's global
 *  Space-to-pause keybind can tell not to fire while text is being typed, from anywhere in the
 *  tree, without every screen needing to plumb it through. */
class TextFieldFocusTracker {

    private val focusedFieldIds = mutableSetOf<Any>()

    val isAnyFieldFocused: Boolean get() = focusedFieldIds.isNotEmpty()

    fun setFocused(id: Any, focused: Boolean) {
        if (focused) {
            focusedFieldIds.add(id)
        } else {
            focusedFieldIds.remove(id)
        }
    }

}

val LocalTextFieldFocusTracker = compositionLocalOf { TextFieldFocusTracker() }
