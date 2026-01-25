package com.example.crypto.util

// XOnlyHexKey is a hex string representing a 32-byte x-only public key (64 hex chars)
typealias XOnlyHexKey = String

fun ByteArray.toXOnlyHexKey(): String = joinToString("") { "%02x".format(it) }
