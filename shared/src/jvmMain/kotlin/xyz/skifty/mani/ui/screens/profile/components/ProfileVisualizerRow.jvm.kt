package xyz.skifty.mani.ui.screens.profile.components

import androidx.compose.runtime.Composable

// No audio visualizer on desktop - AudioSessionVisualizer is androidMain-only.
@Composable
actual fun ProfileVisualizerRow(
    showVisualizer: Boolean,
    onShowVisualizerChange: (Boolean) -> Unit,
) {
}
