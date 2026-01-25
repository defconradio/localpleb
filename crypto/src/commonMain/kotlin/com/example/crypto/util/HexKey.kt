package com.example.crypto.util

typealias HexKey = String

fun ByteArray.toHexKey(): String = joinToString("") { "%02x".format(it) }
