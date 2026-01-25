package com.example.crypto.util

import java.nio.ByteBuffer
import kotlin.random.Random
//TODO IMPLEMENT ACTUAL SECURE RNG FOR EACH PLATFORM
object SecureRNG {
    fun nextBytes(size: Int): ByteArray {
        return Random.Default.nextBytes(size)
    }

    fun nextLong(): Long {
        val bytes = nextBytes(8)
        return ByteBuffer.wrap(bytes).long
    }
}
