package xyz.skifty.mani.ui.screens.login.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import xyz.skifty.mani.i18n.AppLanguage

@Composable
fun LanguageDropdown(selected: AppLanguage, onSelect: (AppLanguage) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    // DropdownMenu anchors to its nearest enclosing Box, not to whatever composable happens to
    // precede it - without this wrapper it was anchoring to this composable's parent Row (which
    // spans the full window width), not the icon button, hence appearing top-left instead of
    // under the icon.
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(imageVector = Icons.Filled.Language, contentDescription = selected.displayName)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            for (language in AppLanguage.entries) {
                DropdownMenuItem(
                    text = { Text(language.displayName) },
                    onClick = {
                        onSelect(language)
                        expanded = false
                    },
                )
            }
        }
    }
}
