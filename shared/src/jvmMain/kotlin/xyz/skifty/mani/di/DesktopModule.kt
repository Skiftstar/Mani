package xyz.skifty.mani.di

import org.koin.dsl.module
import xyz.skifty.mani.media.AudioPlayer
import xyz.skifty.mani.media.DesktopAudioPlayer
import xyz.skifty.mani.media.WindowsLibMpvAudioPlayer
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
    // Windows gets a libmpv-backed AudioPlayer instead of DesktopAudioPlayer's subprocess-plus-
    // JSON-IPC one Linux keeps - mpv's old hand-rolled Win32 named-pipe IPC transport (since
    // deleted) was a disproportionate source of Windows-only playback errors compared to Linux's
    // plain Unix-domain-socket transport, so this replaces that transport with a direct in-process
    // JNA binding to libmpv-2.dll on Windows specifically, while keeping mpv's own proven
    // decode/seek engine unchanged - see WindowsLibMpvAudioPlayer's own doc comment. Same os.name
    // dispatch idiom as SecureStorage below.
    single<AudioPlayer> {
        val os = System.getProperty("os.name")
            .lowercase()
        if (os.contains("win")) {
            WindowsLibMpvAudioPlayer()
        } else {
            DesktopAudioPlayer()
        }
    }
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
