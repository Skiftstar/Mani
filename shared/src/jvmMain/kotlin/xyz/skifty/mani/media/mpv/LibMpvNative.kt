package xyz.skifty.mani.media.mpv

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.StringArray
import com.sun.jna.Structure

// Numeric values confirmed by hand against libmpv's own client.h (shinchiro/mpv-winbuild-cmake's
// mpv-dev package) rather than guessed - the client API is a stable ABI, but these are exactly the
// subset this codebase actually uses (see MPV_FORMAT/MPV_EVENT below for the full list).
internal const val MPV_FORMAT_FLAG = 3
internal const val MPV_FORMAT_DOUBLE = 5

internal const val MPV_EVENT_SHUTDOWN = 1
internal const val MPV_EVENT_END_FILE = 7
internal const val MPV_EVENT_FILE_LOADED = 8
internal const val MPV_EVENT_PROPERTY_CHANGE = 22

internal const val MPV_END_FILE_REASON_EOF = 0

/** Mirrors libmpv's `mpv_event` struct (client.h) - the top-level event envelope returned by
 *  [LibMpv.mpv_wait_event]. [data] is a tagged-union pointer whose real struct type depends on
 *  [eventId] - [MpvEventProperty]/[MpvEventEndFile] read it for the two event kinds this class
 *  cares about ([MPV_EVENT_PROPERTY_CHANGE]/[MPV_EVENT_END_FILE]). */
@Structure.FieldOrder("eventId", "error", "replyUserdata", "data")
internal class MpvEvent : Structure() {
    @JvmField var eventId: Int = 0
    @JvmField var error: Int = 0
    @JvmField var replyUserdata: Long = 0
    @JvmField var data: Pointer? = null
}

/** Mirrors `mpv_event_property` - the `data` payload of an [MPV_EVENT_PROPERTY_CHANGE] event. */
@Structure.FieldOrder("name", "format", "data")
internal class MpvEventProperty(pointer: Pointer) : Structure(pointer) {
    @JvmField var name: String? = null
    @JvmField var format: Int = 0
    @JvmField var data: Pointer? = null

    init {
        read()
    }
}

/** Mirrors `mpv_event_end_file` - the `data` payload of an [MPV_EVENT_END_FILE] event. Only
 *  [reason] is used here (matching [MpvIpcClient]'s JSON-IPC equivalent, which only ever checks
 *  `end-file`'s own `reason == "eof"`), but the struct's full field order still has to be declared
 *  correctly for JNA to read [reason] itself at the right offset. */
@Structure.FieldOrder("reason", "error", "playlistEntryId", "playlistInsertId", "playlistInsertNumEntries")
internal class MpvEventEndFile(pointer: Pointer) : Structure(pointer) {
    @JvmField var reason: Int = 0
    @JvmField var error: Int = 0
    @JvmField var playlistEntryId: Long = 0
    @JvmField var playlistInsertId: Long = 0
    @JvmField var playlistInsertNumEntries: Int = 0

    init {
        read()
    }
}

/** JNA binding for libmpv's client API (client.h) - the subset [LibMpvAudioPlayer] actually calls.
 *  Loaded from `libmpv-2.dll`, bundled the same way `mpv.exe` already is (see
 *  `desktopApp/build.gradle.kts`'s `downloadMpvForWindows`) - resolved via [resolveLibraryPath]
 *  below, mirroring `MpvIpcClient.kt`'s own `resolveMpvExecutable()`. */
internal interface LibMpv : Library {

    fun mpv_create(): Pointer?
    fun mpv_initialize(ctx: Pointer): Int
    fun mpv_terminate_destroy(ctx: Pointer)
    fun mpv_set_option_string(ctx: Pointer, name: String, data: String): Int
    fun mpv_command(ctx: Pointer, args: StringArray): Int
    fun mpv_set_property_string(ctx: Pointer, name: String, data: String): Int
    fun mpv_observe_property(ctx: Pointer, replyUserdata: Long, name: String, format: Int): Int

    // Declared returning the Structure type directly (not Pointer) - JNA's documented convention
    // for a native function that returns a pointer to a struct it owns: the returned pointer is
    // auto-wrapped and read into a fresh MpvEvent for each call, which is exactly right here since
    // mpv_wait_event's own docs say the returned mpv_event* is only valid until the *next*
    // mpv_wait_event() call anyway - nothing here needs to keep it alive any longer than that.
    fun mpv_wait_event(ctx: Pointer, timeout: Double): MpvEvent

    fun mpv_error_string(error: Int): String

    companion object {

        val INSTANCE: LibMpv by lazy {
            Native.load(resolveLibraryPath(), LibMpv::class.java)
        }

        // Same resolution shape as MpvIpcClient.kt's resolveMpvExecutable(): a bundled copy under
        // the packaged app's own resources directory if one's there, otherwise a bare library name
        // for Native.load() to resolve via jna.library.path/java.library.path/the OS's own DLL
        // search path (covers plain `./gradlew :desktopApp:run`, where nothing is bundled).
        private fun resolveLibraryPath(): String {
            val resourcesDir = System.getProperty("compose.application.resources.dir")
                ?: return "libmpv-2"
            val bundled = java.io.File(resourcesDir, "libmpv-2.dll")
            return if (bundled.exists()) bundled.absolutePath else "libmpv-2"
        }

    }

}
