package xyz.skifty.moonlight.media.mpris

import org.freedesktop.dbus.annotations.DBusBoundProperty
import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.interfaces.DBusInterface

/** The MPRIS2 root interface - see https://specifications.freedesktop.org/mpris-spec/latest/Media_Player.html */
@Suppress("FunctionName")
@DBusInterfaceName("org.mpris.MediaPlayer2")
interface MprisRoot : DBusInterface {

    fun Raise()
    fun Quit()

    @DBusBoundProperty
    fun isCanQuit(): Boolean

    @DBusBoundProperty
    fun isCanRaise(): Boolean

    @DBusBoundProperty
    fun isHasTrackList(): Boolean

    @DBusBoundProperty
    fun getIdentity(): String

    @DBusBoundProperty
    fun getSupportedUriSchemes(): List<String>

    @DBusBoundProperty
    fun getSupportedMimeTypes(): List<String>

}
