package xyz.skifty.moonlight.preferences

import java.util.prefs.Preferences

actual object AppPreferencesFactory {
    actual fun create(): AppPreferences = JvmAppPreferences()
}

/** Backed by the plain JDK preferences store (registry on Windows, a file tree on Linux/macOS) -
 *  no external daemon/CLI dependency, unlike [xyz.skifty.moonlight.security.LinuxSecureStorage]. */
class JvmAppPreferences : AppPreferences {

    private val preferences = Preferences.userRoot()
        .node("xyz/skifty/moonlight")

    override fun save(key: String, value: String) {
        preferences.put(key, value)
    }

    override fun get(key: String): String? {
        return preferences.get(key, null)
    }

}
