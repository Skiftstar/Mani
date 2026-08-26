package xyz.skifty.mani

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import xyz.skifty.mani.ui.components.TitleBar
import xyz.skifty.mani.ui.theme.ManiTheme

fun main() = application {
    val windowState = rememberWindowState()
    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Mani",
        undecorated = true,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TitleBar composes outside JvmApp's own ManiTheme{} wrapper, so it needs its
            // own here to pick up the app's color scheme instead of Compose's un-themed default.
            // fillContainer = false: TitleBar already sets every color explicitly and shouldn't
            // fill the whole window (it'd fight JvmApp's Box below for space).
            ManiTheme(fillContainer = false) {
                TitleBar(
                    windowState = windowState,
                    onCloseClick = ::exitApplication,
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                JvmApp()
            }
        }
    }
}
