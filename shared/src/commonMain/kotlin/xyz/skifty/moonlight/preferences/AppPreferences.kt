package xyz.skifty.moonlight.preferences

/** Local storage for non-secret app settings (e.g. last-played song, volume) - unlike
 *  [xyz.skifty.moonlight.security.SecureStorage], nothing stored here needs an OS keyring. */
interface AppPreferences {

    fun save(key: String, value: String)
    fun get(key: String): String?

}
