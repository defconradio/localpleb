package com.example.data

expect object KeyManager {
    fun hasPrivateKey(): Boolean
    fun savePrivateKey(hex: String)
    fun loadPrivateKey(): String?
    fun deletePrivateKey()
}

