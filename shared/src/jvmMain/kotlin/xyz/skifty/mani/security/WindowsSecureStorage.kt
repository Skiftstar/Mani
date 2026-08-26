package xyz.skifty.mani.security

import xyz.skifty.mani.security.SecureStorage
import java.io.File

class WindowsSecureStorage : SecureStorage {
    override fun save(key: String, secret: String) {
        val encrypted = DPAPI.protect(secret.toByteArray(Charsets.UTF_8))
        File("${System.getProperty("user.home")}/.secure/$key").apply {
            parentFile.mkdirs()
            writeBytes(encrypted)
        }
    }

    override fun get(key: String): String? {
        val file = File("${System.getProperty("user.home")}/.secure/$key")
        return if (file.exists()) {
            val decrypted = DPAPI.unprotect(file.readBytes())
            String(decrypted, Charsets.UTF_8)
        } else null
    }

    override fun delete(key: String) {
        File("${System.getProperty("user.home")}/.secure/$key").delete()
    }
}
