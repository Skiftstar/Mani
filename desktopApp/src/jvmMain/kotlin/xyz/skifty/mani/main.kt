package xyz.skifty.mani

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.koin.core.context.startKoin
import xyz.skifty.mani.di.commonModule
import xyz.skifty.mani.di.desktopModule
import xyz.skifty.mani.ui.components.TitleBar
import xyz.skifty.mani.ui.theme.ManiTheme

fun main() {
    startKoin {
        modules(commonModule, desktopModule)
    }

    application {
        val windowState = rememberWindowState()
        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "Mani",
            // The classic desktop-only painterResource(String) (reads straight from
            // src/jvmMain/resources/) rather than the typed Compose Resources library every other
            // module here uses - that one's codegen never activates for this module (its
            // generateResourceAccessorsForJvmMain task's onlyIf stays false even with a
            // composeResources/ dir in place, confirmed by hand), and this module has exactly one
            // icon to load, not enough to justify chasing that down. Deprecated, but still fully
            // functional - just a compile-time warning.
            icon = painterResource("mani-icon.svg"),
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
}
