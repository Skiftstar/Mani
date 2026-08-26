package xyz.skifty.mani.security

interface SecureStorage {

    fun save(key: String, value: String)
    fun delete(key: String)
    fun get(key: String): String?

}