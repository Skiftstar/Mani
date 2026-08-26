package xyz.skifty.mani.models

import kotlinx.serialization.Serializable

@Serializable
data class SubsonicError(
    val code: Int,
    val message: String? = null
)
