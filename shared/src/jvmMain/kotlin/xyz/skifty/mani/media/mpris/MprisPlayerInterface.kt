package xyz.skifty.mani.media.mpris

import org.freedesktop.dbus.DBusPath
import org.freedesktop.dbus.annotations.DBusBoundProperty
import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.annotations.PropertiesEmitsChangedSignal.EmitChangeSignal
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.messages.DBusSignal
import org.freedesktop.dbus.types.Variant

/** The MPRIS2 player interface - see https://specifications.freedesktop.org/mpris-spec/latest/Player_Interface.html */
@Suppress("FunctionName")
@DBusInterfaceName("org.mpris.MediaPlayer2.Player")
interface MprisPlayerInterface : DBusInterface {

    fun Next()
    fun Previous()
    fun Pause()
    fun PlayPause()
    fun Stop()
    fun Play()
    fun Seek(offsetMicroseconds: Long)
    fun SetPosition(trackId: DBusPath, positionMicroseconds: Long)
    fun OpenUri(uri: String)

    @DBusBoundProperty
    fun getPlaybackStatus(): String

    @DBusBoundProperty
    fun getLoopStatus(): String

    @DBusBoundProperty
    fun setLoopStatus(value: String)

    @DBusBoundProperty(emitChangeSignal = EmitChangeSignal.CONST)
    fun getRate(): Double

    @DBusBoundProperty
    fun isShuffle(): Boolean

    @DBusBoundProperty
    fun setShuffle(shuffle: Boolean)

    @DBusBoundProperty
    fun getMetadata(): Map<String, Variant<*>>

    @DBusBoundProperty
    fun getVolume(): Double

    @DBusBoundProperty
    fun setVolume(volume: Double)

    // Per spec, Position should not be reported as emitting PropertiesChanged - clients poll it
    // (or listen for Seeked) instead.
    @DBusBoundProperty(emitChangeSignal = EmitChangeSignal.FALSE)
    fun getPosition(): Long

    @DBusBoundProperty(emitChangeSignal = EmitChangeSignal.CONST)
    fun getMinimumRate(): Double

    @DBusBoundProperty(emitChangeSignal = EmitChangeSignal.CONST)
    fun getMaximumRate(): Double

    // Reflects whether the playback queue actually has a next/previous track.
    @DBusBoundProperty
    fun isCanGoNext(): Boolean

    @DBusBoundProperty
    fun isCanGoPrevious(): Boolean

    @DBusBoundProperty(emitChangeSignal = EmitChangeSignal.CONST)
    fun isCanPlay(): Boolean

    @DBusBoundProperty(emitChangeSignal = EmitChangeSignal.CONST)
    fun isCanPause(): Boolean

    @DBusBoundProperty(emitChangeSignal = EmitChangeSignal.CONST)
    fun isCanSeek(): Boolean

    @DBusBoundProperty(emitChangeSignal = EmitChangeSignal.CONST)
    fun isCanControl(): Boolean

    /**
     * Per spec, clients are expected to interpolate track position locally between updates rather
     * than poll it - this signal is their cue for *when* to (re-)anchor that interpolation to a
     * known position, both after an explicit seek and whenever playback starts/resumes (dbus-java
     * requires signal classes to be nested directly in the [DBusInterface] they belong to, since it
     * derives the D-Bus interface name from the enclosing class).
     */
    class Seeked(path: String, positionMicroseconds: Long) : DBusSignal(path, positionMicroseconds)

}
