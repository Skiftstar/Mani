package xyz.skifty.mani.security

import android.content.Context
import android.util.Base64

/** Ciphertext (base64, via [AndroidKeystoreCipher]) stored in a plain SharedPreferences file - the
 *  encryption already happens at the AndroidKeystoreCipher layer, so the store itself doesn't need
 *  to be anything special, same division of responsibility as [WindowsSecureStorage] (DPAPI
 *  encrypts, a plain File holds the bytes). */
class AndroidSecureStorage(context: Context) : SecureStorage {

    private val preferences = context.getSharedPreferences("xyz.skifty.mani.secure", Context.MODE_PRIVATE)

    override fun save(key: String, value: String) {
        val encrypted = AndroidKeystoreCipher.encrypt(value.toByteArray(Charsets.UTF_8))
        preferences.edit().putString(key, Base64.encodeToString(encrypted, Base64.NO_WRAP)).apply()
    }

    override fun get(key: String): String? {
        val stored = preferences.getString(key, null)
            ?: return null
        return AndroidKeystoreCipher.decrypt(Base64.decode(stored, Base64.NO_WRAP)).toString(Charsets.UTF_8)
    }

    override fun delete(key: String) {
        preferences.edit().remove(key).apply()
    }

}
