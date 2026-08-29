package xyz.skifty.mani.ext

import xyz.skifty.mani.media.SongInfo

/** A short human-readable label for this song's audio quality - bit rate when known ("320 kbps"),
 *  falling back to the file format ("FLAC"), or "--" when neither is known. */
fun SongInfo.qualityLabel(): String {
    songBitRateKbps?.let { bitRateKbps ->
        return "$bitRateKbps kbps"
    }
    return songFormat?.uppercase() ?: "--"
}
