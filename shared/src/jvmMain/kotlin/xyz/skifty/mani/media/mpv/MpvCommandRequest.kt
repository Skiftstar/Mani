package xyz.skifty.mani.media.mpv

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** One outgoing line of mpv's JSON IPC protocol (https://mpv.io/manual/stable/#json-ipc) -
 *  [requestId] is echoed back on the matching reply so [MpvIpcClient] can correlate it. */
@Serializable
data class MpvCommandRequest(
    @SerialName("command")
    val command: List<JsonElement>,

    @SerialName("request_id")
    val requestId: Int,
)
