package xyz.skifty.moonlight.models

import kotlinx.serialization.Serializable

@Serializable
data class SubsonicError(
    val code: Int,
    val message: String? = null
)
