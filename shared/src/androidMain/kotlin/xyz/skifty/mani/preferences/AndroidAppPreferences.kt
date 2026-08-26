package xyz.skifty.mani.preferences

import android.content.Context

/** Backed by plain SharedPreferences - AppPreferences' get/save contract is synchronous, and
 *  DataStore (Google's now-recommended replacement) is Flow/suspend-based, which would need
 *  runBlocking at every call site to fit this interface - worse, not better, for the handful of
 *  tiny string values (last-played song id, volume) actually stored here. */
class AndroidAppPreferences(context: Context) : AppPreferences {

    private val preferences = context.getSharedPreferences("xyz.skifty.mani.prefs", Context.MODE_PRIVATE)

    override fun save(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }

    override fun get(key: String): String? {
        return preferences.getString(key, null)
    }

}
