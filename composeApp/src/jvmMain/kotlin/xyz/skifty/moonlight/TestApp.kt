package xyz.skifty.moonlight

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview

import xyz.skifty.moonlight.media.DesktopAudioPlayer

@Composable
@Preview
fun TestApp() {

    val audioPlayer = remember { DesktopAudioPlayer() }

    MaterialTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = {
                audioPlayer.play("yourUrl")
            }) {
                Text("Test")
            }


            Button(onClick = { audioPlayer.pause() }) {
                Text("Pause")
            }

            Button(onClick = { audioPlayer.stop() }) {
                Text("Stop")
            }

            Button(onClick = { audioPlayer.seek(60_000) }) { // 1 min
                Text("Seek to 1 min")
            }
        }
    }
}