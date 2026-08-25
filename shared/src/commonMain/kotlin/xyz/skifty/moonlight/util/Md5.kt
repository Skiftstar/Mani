package xyz.skifty.moonlight.util

import kotlin.random.Random

expect fun md5Hex(input: String): String

fun generateSalt(length: Int = 12): String {
    val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    return (1..length).map { chars[Random.nextInt(chars.length)] }
        .joinToString("")
}
