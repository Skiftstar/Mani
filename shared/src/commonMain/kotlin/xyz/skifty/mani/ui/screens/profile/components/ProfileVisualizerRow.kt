package xyz.skifty.mani.ui.screens.profile.components

import androidx.compose.runtime.Composable

/** The Settings tab's "Show audio visualizer" row - Android-only, since
 *  [xyz.skifty.mani.media.AudioSessionVisualizer] is androidMain-only. The jvm actual renders
 *  nothing. Turning this on requests RECORD_AUDIO at runtime (see the android actual). */
@Composable
expect fun ProfileVisualizerRow(
    showVisualizer: Boolean,
    onShowVisualizerChange: (Boolean) -> Unit,
)
