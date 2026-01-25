package com.example.crypto.util

import io.ktor.util.decodeBase64Bytes
import io.ktor.util.encodeBase64

object Base64 {
    fun encode(input: ByteArray): String = input.encodeBase64()
    fun decodeBytes(input: String): ByteArray = input.decodeBase64Bytes()
}
