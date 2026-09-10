package xyz.skifty.mani.ui.screens.home.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

actual val showHomeShelfPageArrows: Boolean = false

@Composable
actual fun homeShelfTitleStyle(): TextStyle =
    MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
