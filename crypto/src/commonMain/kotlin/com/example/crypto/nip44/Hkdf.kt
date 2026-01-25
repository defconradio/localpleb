package com.example.crypto.nip44

import fr.acinq.bitcoin.Crypto

class Hkdf(
    val algorithm: String = "HmacSHA256",
    val hashLen: Int = 32,
) {
    // Implements HMAC-SHA256 using the Crypto.sha256 primitive from bitcoin-kmp.
    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val blockSize = 64
        val opad = 0x5c.toByte()
        val ipad = 0x36.toByte()

        val actualKey = if (key.size > blockSize) {
            Crypto.sha256(key)
        } else {
            key
        }

        val keyPadded = actualKey.copyOf(blockSize)

        val oKeyPad = ByteArray(blockSize)
        val iKeyPad = ByteArray(blockSize)

        for (i in 0 until blockSize) {
            oKeyPad[i] = (keyPadded[i].toInt() xor opad.toInt()).toByte()
            iKeyPad[i] = (keyPadded[i].toInt() xor ipad.toInt()).toByte()
        }

        val innerHash = Crypto.sha256(iKeyPad + data)
        return Crypto.sha256(oKeyPad + innerHash)
    }

    fun extract(
        key: ByteArray,
        salt: ByteArray,
    ): ByteArray {
        // In HKDF-Extract, the salt is the key for HMAC, and the IKM (key) is the data.
        return hmacSha256(salt, key)
    }

    fun expand(
        key: ByteArray,
        nonce: ByteArray,
        outputLength: Int,
    ): ByteArray {
        check(key.size == hashLen)
        check(nonce.size == hashLen)

        val n = (outputLength + hashLen - 1) / hashLen // Ceiling division
        var hashRound = ByteArray(0)
        val generatedBytes = ByteArray(n * hashLen)
        var offset = 0

        for (roundNum in 1..n) {
            // Replaced ByteBuffer with ByteArray concatenation
            val input = hashRound + nonce + roundNum.toByte()
            hashRound = hmacSha256(key, input)
            hashRound.copyInto(generatedBytes, offset)
            offset += hashLen
        }

        // Return a copy of the required length
        return generatedBytes.copyOfRange(0, outputLength)
    }
}
