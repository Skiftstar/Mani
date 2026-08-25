package xyz.skifty.moonlight.media.mpv

import java.nio.channels.ByteChannel

/**
 * Abstracts how a byte-stream connection to mpv's IPC endpoint at [connect]'s `socketPath` is
 * opened - a Unix domain socket on Linux (see [LinuxMpvIpcTransport]), a named pipe on Windows
 * (see [WindowsMpvIpcTransport]). [MpvIpcClient] only depends on this, never on socket/pipe
 * specifics directly.
 */
interface MpvIpcTransport {

    /** Generates a fresh, OS-appropriate identifier for mpv's `--input-ipc-server` endpoint - a
     *  temp-file path on Linux (mpv creates the actual socket node itself), a `\\.\pipe\...` name
     *  on Windows (mpv creates the actual pipe itself too - this only reserves the name). */
    fun createEndpointPath(): String

    /** Connects to mpv's IPC endpoint at [socketPath], retrying for a short window since mpv
     *  needs a moment after being spawned to create the endpoint. */
    fun connect(
        socketPath: String,
    ): ByteChannel

}
