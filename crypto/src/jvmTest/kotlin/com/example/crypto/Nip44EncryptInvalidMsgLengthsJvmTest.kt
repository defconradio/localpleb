package com.example.crypto

import com.example.crypto.nip44.Nip44v2
import kotlin.test.Test
import kotlin.test.assertFailsWith

class Nip44EncryptInvalidMsgLengthsJvmTest {
    @Test
    fun testEncryptInvalidMsgLengths() {
        val invalidLengths = listOf(0, 65536, 100000, 10000000)
        val conversationKey = ByteArray(32) { 0x01 }
        val nonce = ByteArray(24) { 0x02 }
        val nip44 = Nip44v2()
        for (len in invalidLengths) {
            val msg = "A".repeat(len)
            assertFailsWith<Exception>("Should fail for length $len") {
                nip44.encryptWithNonce(msg, conversationKey, nonce)
            }
        }
    }
}
