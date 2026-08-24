package xyz.skifty.moonlight.security

expect object SecureStorageFactory {
    fun create(): SecureStorage
}