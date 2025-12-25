package xyz.skifty.moonlight

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform