package xyz.skifty.moonlight.media.mpv

import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinBase
import com.sun.jna.platform.win32.WinError
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.ptr.IntByReference
import com.sun.jna.win32.W32APIOptions
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.ByteChannel
import java.util.UUID

private const val CONNECT_RETRY_DELAY_MS = 50L
private const val CONNECT_TIMEOUT_MS = 3_000L

// Not exposed as a named constant by this version of jna-platform's WinBase/WinNT - value per the
// Win32 API docs (https://learn.microsoft.com/windows/win32/fileio/file-attribute-constants).
private const val FILE_FLAG_OVERLAPPED = 0x40000000

/**
 * Connects to mpv's `--input-ipc-server` endpoint via a Windows named pipe, driven through the
 * Win32 `CreateFile`/`ReadFile`/`WriteFile` API via JNA's `Kernel32` (already a dependency here
 * for [xyz.skifty.moonlight.security.DPAPI]) using overlapped (asynchronous) I/O, rather than
 * [java.io.RandomAccessFile]'s `FileChannel` or plain synchronous `ReadFile`/`WriteFile` calls.
 *
 * Two simpler options were ruled out, both confirmed by hand while wiring this transport up:
 * - `FileChannel` serializes reads and writes through one shared internal lock, built for a
 *   single-position seekable file (unlike [LinuxMpvIpcTransport]'s `SocketChannel`, which keeps
 *   independent locks per direction - see [MpvIpcClient]'s writeLine()/readLoop() doc comment). A
 *   blocking read parked waiting for mpv's next line holds that lock for its entire duration,
 *   deadlocking any concurrent write - exactly what [MpvIpcClient] needs to do on every command.
 * - Even raw synchronous (non-overlapped) `ReadFile`/`WriteFile` calls on the same HANDLE don't
 *   work either: Windows only supports one pending synchronous I/O operation per handle at a
 *   time, so a `WriteFile` issued while a `ReadFile` is already pending on the same handle breaks
 *   the pipe (the pending `ReadFile` fails with `ERROR_BROKEN_PIPE`, the `WriteFile` with
 *   `ERROR_NO_DATA`).
 *
 * Overlapped I/O is what actually supports a pending read and a pending write on the same handle
 * at once: each call gets its own [WinBase.OVERLAPPED] (backed by its own event), `ReadFile`/
 * `WriteFile` return immediately with `ERROR_IO_PENDING`, and [Kernel32Ext.GetOverlappedResult]
 * blocks the calling thread until that specific operation completes - independent per call, same
 * as `SocketChannel`'s independent per-direction locks.
 *
 * The buffer for each call is a [Memory] (raw native memory), not a JVM `byte[]`, and deliberately
 * so: JNA only copies a `byte[]` parameter's contents back into the Java array immediately after
 * the native call returns - fine for a call that completes synchronously, but a `ReadFile` that
 * returns `ERROR_IO_PENDING` completes *later*, once mpv actually has data, at which point nothing
 * copies what the OS wrote into that `byte[]` anymore (confirmed by hand: reads that had to
 * genuinely wait came back with the first several bytes blank). [Memory] is stable, addressable
 * native memory the OS can write into whenever the read actually completes, which we then copy out
 * explicitly (see [WindowsNamedPipeByteChannel.read]) only once [Kernel32Ext.GetOverlappedResult]
 * confirms it's ready.
 */
class WindowsMpvIpcTransport : MpvIpcTransport {

    override fun createEndpointPath(): String =
        "\\\\.\\pipe\\moonlight-mpv-${UUID.randomUUID()}"

    override fun connect(
        socketPath: String,
    ): ByteChannel {
        val deadline = System.currentTimeMillis() + CONNECT_TIMEOUT_MS

        while (true) {
            val handle = Kernel32.INSTANCE.CreateFile(
                socketPath,
                WinNT.GENERIC_READ or WinNT.GENERIC_WRITE,
                0,
                null,
                WinNT.OPEN_EXISTING,
                WinNT.FILE_ATTRIBUTE_NORMAL or FILE_FLAG_OVERLAPPED,
                null,
            )
            if (handle != WinBase.INVALID_HANDLE_VALUE) {
                return WindowsNamedPipeByteChannel(handle)
            }

            // ERROR_FILE_NOT_FOUND: mpv hasn't created the pipe yet. ERROR_PIPE_BUSY: it has, but
            // hasn't called ConnectNamedPipe for this instance yet. Both are expected transiently
            // right after spawning mpv - anything else is a real failure.
            val error = Kernel32.INSTANCE.GetLastError()
            if (error != WinError.ERROR_FILE_NOT_FOUND && error != WinError.ERROR_PIPE_BUSY) {
                throw IOException(
                    "CreateFile on mpv's IPC pipe at $socketPath failed (error $error)",
                )
            }
            if (System.currentTimeMillis() >= deadline) {
                throw IllegalStateException(
                    "Could not connect to mpv's IPC pipe at $socketPath within " +
                        "${CONNECT_TIMEOUT_MS}ms - mpv may have failed to start.",
                )
            }
            Thread.sleep(CONNECT_RETRY_DELAY_MS)
        }
    }

}

/** The pieces of Win32's overlapped-I/O API that jna-platform's [Kernel32] either doesn't expose
 *  at all ([GetOverlappedResult]) or only exposes with a `byte[]` buffer parameter unsuitable for
 *  overlapped calls ([ReadFile]/[WriteFile] - see [WindowsMpvIpcTransport]'s doc comment) - loaded
 *  the same way jna-platform loads `Kernel32` itself. */
private interface Kernel32Ext : Kernel32 {

    companion object {
        val INSTANCE: Kernel32Ext = Native.load(
            "kernel32",
            Kernel32Ext::class.java,
            W32APIOptions.DEFAULT_OPTIONS,
        )
    }

    fun GetOverlappedResult(
        hFile: WinNT.HANDLE,
        lpOverlapped: WinBase.OVERLAPPED,
        lpNumberOfBytesTransferred: IntByReference,
        bWait: Boolean,
    ): Boolean

    fun ReadFile(
        hFile: WinNT.HANDLE,
        lpBuffer: Memory,
        nNumberOfBytesToRead: Int,
        lpNumberOfBytesRead: IntByReference,
        lpOverlapped: WinBase.OVERLAPPED,
    ): Boolean

    fun WriteFile(
        hFile: WinNT.HANDLE,
        lpBuffer: Memory,
        nNumberOfBytesToWrite: Int,
        lpNumberOfBytesWritten: IntByReference,
        lpOverlapped: WinBase.OVERLAPPED,
    ): Boolean

}

/** A minimal [ByteChannel] over a Win32 named-pipe [handle] opened with `FILE_FLAG_OVERLAPPED` -
 *  see [WindowsMpvIpcTransport]'s doc comment for why plain synchronous `ReadFile`/`WriteFile`
 *  calls aren't enough here. */
private class WindowsNamedPipeByteChannel(
    private val handle: WinNT.HANDLE,
) : ByteChannel {

    @Volatile
    private var open = true

    override fun isOpen(): Boolean = open

    override fun close() {
        if (open) {
            open = false
            Kernel32.INSTANCE.CloseHandle(handle)
        }
    }

    override fun read(
        destination: ByteBuffer,
    ): Int {
        val length = destination.remaining()
        val buffer = Memory(length.toLong())
        val bytesTransferred = runOverlapped { overlapped, count ->
            Kernel32Ext.INSTANCE.ReadFile(handle, buffer, length, count, overlapped)
        }
            ?: return -1 // mpv closed its end - the same end-of-stream signal SocketChannel gives.
        destination.put(buffer.getByteArray(0, bytesTransferred))
        return bytesTransferred
    }

    override fun write(
        source: ByteBuffer,
    ): Int {
        val length = source.remaining()
        val bytes = ByteArray(length)
        source.get(bytes)
        val buffer = Memory(length.toLong())
        buffer.write(0, bytes, 0, length)
        return runOverlapped { overlapped, count ->
            Kernel32Ext.INSTANCE.WriteFile(handle, buffer, length, count, overlapped)
        }
            ?: throw IOException("WriteFile on mpv's IPC pipe failed: the pipe is closed")
    }

    /** Runs a `ReadFile`/`WriteFile` call ([issue], taking the overlapped struct and byte-count
     *  out-param it's meant to be called with) against a fresh [WinBase.OVERLAPPED]/event pair,
     *  blocking this thread until it completes - independently of any other pending overlapped
     *  call on [handle] from another thread. Returns the transferred byte count, or `null` if the
     *  other end of the pipe has closed. */
    private fun runOverlapped(
        issue: (WinBase.OVERLAPPED, IntByReference) -> Boolean,
    ): Int? {
        val event = Kernel32.INSTANCE.CreateEvent(null, true, false, null)
        try {
            val overlapped = WinBase.OVERLAPPED()
            overlapped.hEvent = event
            overlapped.write()

            val transferred = IntByReference()
            if (!issue(overlapped, transferred)) {
                val error = Kernel32.INSTANCE.GetLastError()
                if (error != WinError.ERROR_IO_PENDING) {
                    return endOfStreamOrThrow(error)
                }
                if (!Kernel32Ext.INSTANCE.GetOverlappedResult(handle, overlapped, transferred, true)) {
                    return endOfStreamOrThrow(Kernel32.INSTANCE.GetLastError())
                }
            }
            return transferred.value
        } finally {
            Kernel32.INSTANCE.CloseHandle(event)
        }
    }

    private fun endOfStreamOrThrow(
        error: Int,
    ): Int? {
        if (error == WinError.ERROR_BROKEN_PIPE || error == WinError.ERROR_PIPE_NOT_CONNECTED) {
            return null
        }
        throw IOException("mpv's IPC pipe I/O failed (error $error)")
    }

}
