package com.example.crypto

import com.example.crypto.nip44.Nip44v2
import kotlin.test.Test
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import com.example.crypto.util.sha256

private fun hexToByteArray(hex: String): ByteArray = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

class Nip44EncryptDecryptLongMsgJvmTest {
    @Test
    fun testEncryptDecryptLongMsgVectors() {
        val filePath = "src/commonTest/resources/nip44.vectors.json"
        val errorFile = "/tmp/nip44_encrypt_decrypt_long_msg_test_errors.txt"
        val errorMessages = mutableListOf<String>()
        try {
            val jsonText = File(filePath).readText()
            val json = Json.parseToJsonElement(jsonText)
            val vectorsArray = json.jsonObject["v2"]?.jsonObject?.get("valid")?.jsonObject?.get("encrypt_decrypt_long_msg")?.jsonArray
            if (vectorsArray == null) throw Exception("encrypt_decrypt_long_msg not found in JSON")
            val nip44 = Nip44v2()
            var failed = false
            for ((i, obj) in vectorsArray.withIndex()) {
                val conversationKeyHex = obj.jsonObject["conversation_key"]?.jsonPrimitive?.content ?: ""
                val nonceHex = obj.jsonObject["nonce"]?.jsonPrimitive?.content ?: ""
                val pattern = obj.jsonObject["pattern"]?.jsonPrimitive?.content ?: ""
                val repeat = obj.jsonObject["repeat"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                val expectedPlaintextSha256 = obj.jsonObject["plaintext_sha256"]?.jsonPrimitive?.content ?: ""
                val expectedPayloadSha256 = obj.jsonObject["payload_sha256"]?.jsonPrimitive?.content ?: ""
                val conversationKey = hexToByteArray(conversationKeyHex)
                val nonce = hexToByteArray(nonceHex)
                try {
                    val plaintext = pattern.repeat(repeat)
                    val plaintextSha256 = sha256(plaintext.toByteArray(Charsets.UTF_8)).toHex()
                    if (plaintextSha256 != expectedPlaintextSha256) {
                        val msg = "Vector $i FAILED: expected plaintext_sha256='$expectedPlaintextSha256', got='$plaintextSha256'"
                        errorMessages.add(msg)
                        failed = true
                    }
                    val encryptedInfo = nip44.encryptWithNonce(plaintext, conversationKey, nonce)
                    val payload = encryptedInfo.encodePayload()
                    val payloadSha256 = sha256(payload.toByteArray(Charsets.UTF_8)).toHex()
                    if (payloadSha256 != expectedPayloadSha256) {
                        val msg = "Vector $i FAILED: expected payload_sha256='$expectedPayloadSha256', got='$payloadSha256'"
                        errorMessages.add(msg)
                        failed = true
                    }
                    val decryptedInfo = nip44.decrypt(payload, conversationKey)
                    val decryptedText = nip44.unpad(decryptedInfo.ciphertext)
                    val decryptedSha256 = sha256(decryptedText.toByteArray(Charsets.UTF_8)).toHex()
                    if (decryptedSha256 != expectedPlaintextSha256) {
                        val msg = "Vector $i FAILED: decrypted plaintext_sha256='$decryptedSha256', expected='$expectedPlaintextSha256'"
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
                kotlin.test.fail("Some vectors failed. See /tmp/nip44_encrypt_decrypt_long_msg_test_errors.txt for details.")
            }
        } catch (e: Exception) {
            val msg = "Serialization or file error: ${e.message}"
            File(errorFile).writeText(msg)
            kotlin.test.fail("Fatal error. See /tmp/nip44_encrypt_decrypt_long_msg_test_errors.txt for details.")
        }
    }
}
