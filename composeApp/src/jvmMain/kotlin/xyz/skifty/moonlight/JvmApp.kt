package xyz.skifty.moonlight

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.skifty.moonlight.api.ApiService
import xyz.skifty.moonlight.media.SongInfo
import xyz.skifty.moonlight.security.SecureStorageFactory

import androidx.compose.foundation.layout.Row
import xyz.skifty.moonlight.media.DesktopAudioPlayer
import xyz.skifty.moonlight.media.nowPlayingBottomWidget.NowPlayingBottomWidget
import xyz.skifty.moonlight.screens.HomeScreen
import xyz.skifty.moonlight.screens.LoginScreen
import xyz.skifty.moonlight.screens.Screen
import xyz.skifty.moonlight.screens.Sidebar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun JvmApp() {

    val audioPlayer = remember { DesktopAudioPlayer() }
    val activeSongInfo = remember { SongInfo() }
    val apiService = remember { ApiService() }
    val secureStorage = remember { SecureStorageFactory.create() }

    remember {
        SingletonImageLoader.setSafe { context ->
            ImageLoader.Builder(context)
                .components { add(KtorNetworkFetcherFactory()) }
                .build()
        }
    }

    // null = still checking for a saved session
    var screen by remember { mutableStateOf<Screen?>(null) }

    LaunchedEffect(Unit) {
        val url = secureStorage.get("moonlight_api_url")
        val user = secureStorage.get("moonlight_username")
        val token = secureStorage.get("moonlight_token")
        val salt = secureStorage.get("moonlight_salt")

        screen = if (url != null && user != null && token != null && salt != null) {
            apiService.restoreSession(url, user, token, salt)
            if (apiService.ping().isSuccess) Screen.Home else Screen.Login
        } else {
            Screen.Login
        }
    }

    MaterialTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize()
        ) {
            // Main content takes all available space
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (screen == Screen.Home) {
                    Sidebar(onHomeClick = { screen = Screen.Home })
                }

                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(scrollState).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (val currentScreen = screen) {
                        null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        Screen.Home -> HomeScreen(apiService, audioPlayer, activeSongInfo)
                        Screen.Login -> LoginScreen(apiService, onLoginSuccess = { screen = Screen.Home })
                    }
                }
            }

            if (activeSongInfo.songId != null) {
                NowPlayingBottomWidget(audioPlayer = audioPlayer, activeSongInfo = activeSongInfo)
            }
        }
    }
}
