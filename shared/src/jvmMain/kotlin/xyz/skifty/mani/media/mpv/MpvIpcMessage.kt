package xyz.skifty.mani.media.mpv

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * A single incoming line of mpv's JSON IPC protocol (https://mpv.io/manual/stable/#json-ipc) -
 * mpv sends two distinct shapes down the same socket, a command reply ([requestId]/[error]/[data])
 * or an async event ([event]/[id]/[name]/[data]/[reason]) - modeled as one flat class with
 * nullable fields for both, matching the actual wire shape mpv documents, rather than forcing an
 * artificial split [MpvIpcClient] would just have to merge back together to tell which shape it
 * received.
 */
@Serializable
data class MpvIpcMessage(
    @SerialName("request_id")
    val requestId: Int? = null,

    @SerialName("error")
    val error: String? = null,

    @SerialName("event")
    val event: String? = null,

    @SerialName("id")
    val id: Int? = null,

    @SerialName("name")
    val name: String? = null,

    @SerialName("data")
    val data: JsonElement? = null,

    // Only present on "end-file" events - "eof" (natural finish), "stop", "quit", "error" or
    // "redirect".
    @SerialName("reason")
    val reason: String? = null,
)
