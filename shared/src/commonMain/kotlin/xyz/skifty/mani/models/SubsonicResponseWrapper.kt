package xyz.skifty.mani.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubsonicResponseWrapper(
    @SerialName("subsonic-response")
    val response: SubsonicResponse
)