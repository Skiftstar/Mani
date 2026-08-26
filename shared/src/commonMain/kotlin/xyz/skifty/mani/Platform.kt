package xyz.skifty.mani

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform