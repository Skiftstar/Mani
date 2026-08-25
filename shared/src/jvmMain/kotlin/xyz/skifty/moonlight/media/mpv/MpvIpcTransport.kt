package xyz.skifty.moonlight.media.mpv

import java.nio.channels.ByteChannel

/**
 * Abstracts how a byte-stream connection to mpv's IPC endpoint at [connect]'s `socketPath` is
 * opened - a Unix domain socket on Linux (see [LinuxMpvIpcTransport]), a named pipe on Windows
 * (not yet implemented - this interface is the extension point for that). [MpvIpcClient] only
 * depends on this, never on socket/pipe specifics directly.
 */
interface MpvIpcTransport {

    /** Connects to mpv's IPC endpoint at [socketPath], retrying for a short window since mpv
     *  needs a moment after being spawned to create the endpoint. */
    fun connect(
        socketPath: String,
    ): ByteChannel

}
