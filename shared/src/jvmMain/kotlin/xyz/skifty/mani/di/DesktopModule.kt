package xyz.skifty.mani.di

import org.koin.dsl.module
import xyz.skifty.mani.media.AudioPlayer
import xyz.skifty.mani.media.DesktopAudioPlayer
import xyz.skifty.mani.preferences.AppPreferences
import xyz.skifty.mani.preferences.JvmAppPreferences
import xyz.skifty.mani.security.LinuxSecureStorage
import xyz.skifty.mani.security.SecureStorage
import xyz.skifty.mani.security.WindowsSecureStorage

// MprisService is deliberately NOT bound here - Koin's single {} throws if its factory lambda
// returns null (confirmed by hand: IllegalStateException "Single instance created couldn't return
// value" at composition time), which is exactly what constructing it needs to be able to do (no
// reachable D-Bus session bus is an expected, tolerated outcome, not a startup failure - see
// JvmApp's own construction of it for the full reasoning). It stays a plain remember{}-scoped
// construct in JvmApp.kt instead, built from these other singletons via koinInject().
val desktopModule = module {
    single<AudioPlayer> { DesktopAudioPlayer() }
    single<AppPreferences> { JvmAppPreferences() }
    single<SecureStorage> {
        val os = System.getProperty("os.name")
            .lowercase()
        when {
            os.contains("win") -> WindowsSecureStorage()
            os.contains("nux") || os.contains("nix") -> LinuxSecureStorage()
            else -> throw UnsupportedOperationException(
                "Mani does not have a secure storage backend for this OS yet (os.name=$os).",
            )
        }
    }
}
