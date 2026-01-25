package com.example.crypto.util

import fr.acinq.bitcoin.Crypto

fun sha256(data: ByteArray): ByteArray = Crypto.sha256(data)
