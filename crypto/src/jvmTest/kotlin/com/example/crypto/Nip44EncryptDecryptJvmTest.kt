package com.example.crypto

import com.example.crypto.nip44.Nip44v2
import kotlin.test.Test
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

private fun hexToByteArray(hex: String): ByteArray = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

data class EncryptDecryptVector(val conversation_key: String, val nonce: String, val plaintext: String, val payload: String)

class Nip44EncryptDecryptJvmTest {
    @Test
    fun testEncryptDecryptVectors() {
        val filePath = "src/commonTest/resources/nip44.vectors.json"
        val errorFile = "/tmp/nip44_encrypt_decrypt_test_errors.txt"
        val errorMessages = mutableListOf<String>()
        try {
            val jsonText = File(filePath).readText()
            val json = Json.parseToJsonElement(jsonText)
            val vectorsArray = json.jsonObject["v2"]?.jsonObject?.get("valid")?.jsonObject?.get("encrypt_decrypt")?.jsonArray
            if (vectorsArray == null) throw Exception("encrypt_decrypt not found in JSON")
            val nip44 = Nip44v2()
            var failed = false
            for ((i, obj) in vectorsArray.withIndex()) {
                val conversationKeyHex = obj.jsonObject["conversation_key"]?.jsonPrimitive?.content ?: ""
                val nonceHex = obj.jsonObject["nonce"]?.jsonPrimitive?.content ?: ""
                val plaintext = obj.jsonObject["plaintext"]?.jsonPrimitive?.content ?: ""
                val expectedPayload = obj.jsonObject["payload"]?.jsonPrimitive?.content ?: ""
                val conversationKey = hexToByteArray(conversationKeyHex)
                val nonce = hexToByteArray(nonceHex)
                try {
                    val encryptedInfo = nip44.encryptWithNonce(plaintext, conversationKey, nonce)
                    val actualPayload = encryptedInfo.encodePayload()
                    if (actualPayload != expectedPayload) {
                        val msg = "Vector $i FAILED: expected payload='$expectedPayload', got='$actualPayload'"
                        errorMessages.add(msg)
                        failed = true
                    }
                    val decryptedInfo = nip44.decrypt(expectedPayload, conversationKey)
                    val decryptedText = nip44.unpad(decryptedInfo.ciphertext)
                    if (decryptedText != plaintext) {
                        val msg = "Vector $i FAILED: expected plaintext='$plaintext', got='$decryptedText'"
                        errorMessages.add(msg)
                        failed = true
                    }
                } catch (e: Exception) {
                    val msg = "Vector $i EXCEPTION: ${e.message}"
                    errorMessages.add(msg)
                    failed = true
                }
            }
            if (failed) {
                File(errorFile).writeText(errorMessages.joinToString("\n"))
                kotlin.test.fail("Some vectors failed. See /tmp/nip44_encrypt_decrypt_test_errors.txt for details.")
            }
        } catch (e: Exception) {
            val msg = "Serialization or file error: ${e.message}"
            File(errorFile).writeText(msg)
            kotlin.test.fail("Fatal error. See /tmp/nip44_encrypt_decrypt_test_errors.txt for details.")
        }
    }
}
