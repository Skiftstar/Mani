package xyz.skifty.mani.ui.screens.profile.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.profile_visualizer_description
import mani.shared.generated.resources.profile_show_visualizer_label
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun ProfileVisualizerRow(
    showVisualizer: Boolean,
    onShowVisualizerChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current

    // Only ever flips the preference on an actual grant/denial result - a denial simply leaves the
    // switch off, rather than optimistically turning it on before the user has actually granted
    // anything.
    val requestRecordAudioPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        onShowVisualizerChange(granted)
    }

    fun onVisualizerToggled(
        enabled: Boolean,
    ) {
        if (!enabled) {
            onShowVisualizerChange(false)
            return
        }
        val alreadyGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (alreadyGranted) {
            onShowVisualizerChange(true)
        } else {
            requestRecordAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    SettingsRow(
        label = stringResource(Res.string.profile_show_visualizer_label),
        description = stringResource(Res.string.profile_visualizer_description),
        trailing = {
            Switch(
                checked = showVisualizer,
                onCheckedChange = ::onVisualizerToggled,
            )
        },
    )
}
