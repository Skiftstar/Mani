package xyz.skifty.moonlight.security

import xyz.skifty.moonlight.security.SecureStorage
import java.io.File

actual object SecureStorageFactory {
    actual fun create(): SecureStorage {
        val os = System.getProperty("os.name")
            .lowercase()
        return when {
            os.contains("win") -> WindowsSecureStorage()
            os.contains("nux") || os.contains("nix") -> LinuxSecureStorage()
            else -> throw UnsupportedOperationException(
                "Moonlight does not have a secure storage backend for this OS yet (os.name=$os).",
            )
        }
    }
}

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
