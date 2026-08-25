package xyz.skifty.moonlight.media.mpv

import java.io.IOException
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.channels.ByteChannel
import java.nio.channels.SocketChannel
import java.nio.file.Path

private const val CONNECT_RETRY_DELAY_MS = 50L
private const val CONNECT_TIMEOUT_MS = 3_000L

/**
 * Connects to mpv's `--input-ipc-server` endpoint via a Unix domain socket, using
 * [java.net.UnixDomainSocketAddress] (JDK 16+ standard library - no native library needed, unlike
 * the vlcj/JNA-backed integrations elsewhere in this codebase).
 */
class LinuxMpvIpcTransport : MpvIpcTransport {

    override fun connect(
        socketPath: String,
    ): ByteChannel {
        val address = UnixDomainSocketAddress.of(Path.of(socketPath))
        val deadline = System.currentTimeMillis() + CONNECT_TIMEOUT_MS

        while (true) {
            try {
                return SocketChannel.open(StandardProtocolFamily.UNIX)
                    .also { channel -> channel.connect(address) }
            } catch (e: IOException) {
                if (System.currentTimeMillis() >= deadline) {
                    throw IllegalStateException(
                        "Could not connect to mpv's IPC socket at $socketPath within " +
                            "${CONNECT_TIMEOUT_MS}ms - mpv may have failed to start.",
                        e,
                    )
                }
                Thread.sleep(CONNECT_RETRY_DELAY_MS)
            }
        }
    }

}
