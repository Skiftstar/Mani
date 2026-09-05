package xyz.skifty.mani.ui.screens.profile

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.profile_logout_button
import mani.shared.generated.resources.profile_show_visualizer_label
import org.jetbrains.compose.resources.stringResource

/** Also doubles as the app's only settings surface for now - just the audio visualizer toggle
 *  below, next to the existing logout button. */
@Composable
fun ProfileScreen(
    showVisualizer: Boolean,
    onShowVisualizerChange: (Boolean) -> Unit,
    onLogout: () -> Unit,
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(Res.string.profile_show_visualizer_label))
            Switch(
                checked = showVisualizer,
                onCheckedChange = ::onVisualizerToggled,
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(onClick = onLogout) {
            Text(stringResource(Res.string.profile_logout_button))
        }
    }
}
