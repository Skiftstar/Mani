package xyz.skifty.mani.security

import java.io.IOException
import java.nio.charset.StandardCharsets

/**
 * Secure storage backed by the freedesktop Secret Service API (GNOME Keyring /
 * KWallet), via the `secret-tool` CLI from libsecret-tools/libsecret-utils.
 *
 * Each secret is stored under the attribute pair `application=mani key=<key>`.
 * Secrets are passed to `secret-tool store` via stdin, never as a process argument,
 * so they never show up in `ps`/process listings.
 */
class LinuxSecureStorage : SecureStorage {

    init {
        if (!isSecretToolAvailable()) {
            throw IllegalStateException(
                "Mani needs the 'secret-tool' command to store credentials securely on " +
                    "Linux, but it wasn't found on PATH. Install it via your package manager, e.g.:\n" +
                    "  Debian/Ubuntu: sudo apt install libsecret-tools\n" +
                    "  Fedora:        sudo dnf install libsecret\n" +
                    "  Arch:          sudo pacman -S libsecret\n" +
                    "then restart Mani.",
            )
        }
    }

    override fun save(key: String, value: String) {
        val process = ProcessBuilder(
            "secret-tool", "store", "--label=Mani ($key)",
            "application", "mani", "key", key,
        ).redirectErrorStream(false)
            .start()

        process.outputStream.use { it.write(value.toByteArray(StandardCharsets.UTF_8)) }
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            val stderr = process.errorStream.readBytes()
                .toString(StandardCharsets.UTF_8)
                .trim()
            throw IllegalStateException(
                "Failed to store '$key' in the system keyring (secret-tool exited $exitCode)." +
                    if (stderr.isNotEmpty()) " $stderr" else "",
            )
        }
    }

    override fun get(key: String): String? {
        val process = ProcessBuilder(
            "secret-tool", "lookup", "application", "mani", "key", key,
        ).redirectErrorStream(false)
            .start()

        val stdout = process.inputStream.readBytes()
            .toString(StandardCharsets.UTF_8)
        val exitCode = process.waitFor()
        if (exitCode == 0) {
            return stdout.trim()
                .ifEmpty { null }
        }

        val stderr = process.errorStream.readBytes()
            .toString(StandardCharsets.UTF_8)
            .trim()
        if (stderr.isEmpty()) {
            // Normal "no such secret" case (e.g. first run, nothing saved yet).
            return null
        }
        throw IllegalStateException(
            "Failed to look up '$key' in the system keyring (secret-tool exited $exitCode). $stderr",
        )
    }

    override fun delete(key: String) {
        // Not fatal if the key is already absent or the clear otherwise fails.
        runCatching {
            ProcessBuilder(
                "secret-tool", "clear", "application", "mani", "key", key,
            ).start()
                .waitFor()
        }
    }

    private fun isSecretToolAvailable(): Boolean {
        // We only care whether the executable resolves on PATH; secret-tool has no
        // --version flag, and any subcommand would require a reachable Secret Service,
        // so a non-zero exit here doesn't mean the binary is missing.
        return try {
            ProcessBuilder("secret-tool", "--help").start()
                .waitFor()
            true
        } catch (_: IOException) {
            false
        }
    }
}
