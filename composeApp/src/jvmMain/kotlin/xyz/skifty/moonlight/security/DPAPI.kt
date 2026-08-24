package xyz.skifty.moonlight.security

import com.sun.jna.platform.win32.Crypt32
import com.sun.jna.platform.win32.WinCrypt

object DPAPI {
    fun protect(data: ByteArray): ByteArray {
        val out = WinCrypt.DATA_BLOB(data)
        val pOut = WinCrypt.DATA_BLOB()
        Crypt32.INSTANCE.CryptProtectData(
            out, null, null, null, null, 0, pOut
        )
        return pOut.data
    }

    fun unprotect(data: ByteArray): ByteArray {
        val inBlob = WinCrypt.DATA_BLOB(data)
        val pOut = WinCrypt.DATA_BLOB()
        Crypt32.INSTANCE.CryptUnprotectData(
            inBlob, null, null, null, null, 0, pOut
        )
        return pOut.data
    }
}
