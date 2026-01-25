package com.example.data

import java.io.File

actual object KeyManager {
    private val keyFile = File("test_privkey.txt")

    actual fun hasPrivateKey(): Boolean {
        return keyFile.exists() && keyFile.readText().isNotEmpty()
    }

    actual fun savePrivateKey(hex: String) {
        keyFile.writeText(hex)
    }

    actual fun loadPrivateKey(): String? {
        return if (hasPrivateKey()) {
            keyFile.readText()
        } else {
            null
        }
    }

    actual fun deletePrivateKey() {
        if (keyFile.exists()) {
            keyFile.delete()
        }
    }
}
