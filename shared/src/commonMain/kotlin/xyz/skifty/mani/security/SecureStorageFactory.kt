package xyz.skifty.mani.security

expect object SecureStorageFactory {
    fun create(): SecureStorage
}