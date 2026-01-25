package com.example.crypto

import kotlin.test.Test
import kotlin.test.assertTrue
import com.example.crypto.util.sha256

class CryptoSampleTest {

    @Test
    fun testSha256() {
        val hash = sha256("hello".encodeToByteArray()).toHex()
        assertTrue(hash.isNotEmpty(), "Hash should not be empty")
    }
}

private fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
