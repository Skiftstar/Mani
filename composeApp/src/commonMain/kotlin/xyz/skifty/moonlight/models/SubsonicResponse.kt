package xyz.skifty.moonlight.models

import kotlinx.serialization.Serializable

@Serializable
data class SubsonicResponse(
    val status: String,
    val starred2: Starred2? = null,
    val error: SubsonicError? = null
)
