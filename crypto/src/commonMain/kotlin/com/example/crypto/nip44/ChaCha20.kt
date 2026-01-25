package com.example.crypto.nip44

class ChaCha20(
    private val key: ByteArray,
    private val nonce: ByteArray
) {
    private val state = IntArray(16)
    private var blockCounter = 0
    private lateinit var stateBytes: ByteArray

    init {
        require(key.size == 32)
        require(nonce.size == 12)
        state[0] = 0x61707865
        state[1] = 0x3320646e
        state[2] = 0x79622d32
        state[3] = 0x6b206574
        // Little-endian key
        for (i in 0..7) {
            state[4 + i] = toIntLE(key, i * 4)
        }
        state[12] = blockCounter
        // Little-endian nonce
        for (i in 0..2) {
            state[13 + i] = toIntLE(nonce, i * 4)
        }
    }

    fun encrypt(data: ByteArray): ByteArray = process(data)
    fun decrypt(data: ByteArray): ByteArray = process(data)

    private fun process(data: ByteArray): ByteArray {
        val result = ByteArray(data.size)
        var offset = 0
        while (offset < data.size) {
            block()
            val blockSize = minOf(64, data.size - offset)
            for (i in 0 until blockSize) {
                result[offset + i] = (data[offset + i].toInt() xor stateBytes[i].toInt()).toByte()
            }
            offset += blockSize
        }
        return result
    }

    private fun block() {
        val x = state.clone()
        for (i in 0..9) {
            quarterRound(x, 0, 4, 8, 12)
            quarterRound(x, 1, 5, 9, 13)
            quarterRound(x, 2, 6, 10, 14)
            quarterRound(x, 3, 7, 11, 15)
            quarterRound(x, 0, 5, 10, 15)
            quarterRound(x, 1, 6, 11, 12)
            quarterRound(x, 2, 7, 8, 13)
            quarterRound(x, 3, 4, 9, 14)
        }
        for (i in 0..15) {
            x[i] = x[i] + state[i]
        }
        stateBytes = ByteArray(64)
        for (i in 0..15) {
            val le = fromIntLE(x[i])
            for (j in 0..3) {
                stateBytes[i * 4 + j] = le[j]
            }
        }
        state[12]++
    }

    private fun quarterRound(x: IntArray, a: Int, b: Int, c: Int, d: Int) {
        x[a] += x[b]
        x[d] = rotl(x[d] xor x[a], 16)
        x[c] += x[d]
        x[b] = rotl(x[b] xor x[c], 12)
        x[a] += x[b]
        x[d] = rotl(x[d] xor x[a], 8)
        x[c] += x[d]
        x[b] = rotl(x[b] xor x[c], 7)
    }

    private fun rotl(x: Int, n: Int): Int {
        return (x shl n) or (x ushr (32 - n))
    }

    private fun toIntLE(bytes: ByteArray, offset: Int): Int {
        return (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 3].toInt() and 0xFF) shl 24)
    }

    private fun fromIntLE(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte()
        )
    }
}
