package xyz.skifty.moonlight.media.mpv

/**
 * Picks the [MpvIpcTransport] implementation for the current OS, mirroring
 * `SecureStorageFactory`'s `os.name`-based dispatch
 * (`xyz.skifty.moonlight.security.WindowsSecureStorage`).
 */
object MpvIpcTransportFactory {

    fun create(): MpvIpcTransport {
        val os = System.getProperty("os.name")
            .lowercase()
        return when {
            os.contains("win") -> WindowsMpvIpcTransport()
            os.contains("nux") || os.contains("nix") -> LinuxMpvIpcTransport()
            else -> throw UnsupportedOperationException(
                "Moonlight does not have an mpv IPC transport for this OS yet (os.name=$os).",
            )
        }
    }

}
