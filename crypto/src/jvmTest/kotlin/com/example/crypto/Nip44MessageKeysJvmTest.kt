package com.example.crypto

import com.example.crypto.nip44.Nip44v2
import kotlin.test.Test
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

private fun hexToByteArray(hex: String): ByteArray = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

data class MessageKeysVector(val conversation_key: String, val nonce: String, val chacha_key: String, val chacha_nonce: String, val hmac_key: String)

class Nip44MessageKeysJvmTest {
    @Test
    fun testGetMessageKeysVectors() {
        val filePath = "src/commonTest/resources/nip44.vectors.json"
        val errorFile = "/tmp/nip44_messagekeys_test_errors.txt"
        val errorMessages = mutableListOf<String>()
        try {
            val jsonText = File(filePath).readText()
            val json = Json.parseToJsonElement(jsonText)
            val getMessageKeysObj = json.jsonObject["v2"]?.jsonObject?.get("valid")?.jsonObject?.get("get_message_keys")?.jsonObject
            if (getMessageKeysObj == null) throw Exception("get_message_keys not found in JSON")
            val conversationKeyHex = getMessageKeysObj["conversation_key"]?.jsonPrimitive?.content ?: ""
            val keysArray = getMessageKeysObj["keys"]?.jsonArray ?: throw Exception("keys array not found in get_message_keys")
            val nip44 = Nip44v2()
            var failed = false
            for ((i, keyObj) in keysArray.withIndex()) {
                val nonceHex = keyObj.jsonObject["nonce"]?.jsonPrimitive?.content ?: ""
                val chachaKeyExpected = keyObj.jsonObject["chacha_key"]?.jsonPrimitive?.content ?: ""
                val chachaNonceExpected = keyObj.jsonObject["chacha_nonce"]?.jsonPrimitive?.content ?: ""
                val hmacKeyExpected = keyObj.jsonObject["hmac_key"]?.jsonPrimitive?.content ?: ""
                val conversationKeyBytes = hexToByteArray(conversationKeyHex)
                val nonceBytes = hexToByteArray(nonceHex)
                try {
                    val keys = nip44.getMessageKeys(conversationKeyBytes, nonceBytes)
                    val chachaKeyHex = keys.chachaKey.joinToString("") { "%02x".format(it) }
                    val chachaNonceHex = keys.chachaNonce.joinToString("") { "%02x".format(it) }
                    val hmacKeyHex = keys.hmacKey.joinToString("") { "%02x".format(it) }
                    if (chachaKeyHex != chachaKeyExpected || chachaNonceHex != chachaNonceExpected || hmacKeyHex != hmacKeyExpected) {
                        val msg = "Vector $i FAILED: nonce=$nonceHex, expected chacha_key=$chachaKeyExpected, got $chachaKeyHex, expected chacha_nonce=$chachaNonceExpected, got $chachaNonceHex, expected hmac_key=$hmacKeyExpected, got $hmacKeyHex"
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
                kotlin.test.fail("Some vectors failed. See /tmp/nip44_messagekeys_test_errors.txt for details.")
            }
        } catch (e: Exception) {
            val msg = "Serialization or file error: ${e.message}"
            File(errorFile).writeText(msg)
            kotlin.test.fail("Fatal error. See /tmp/nip44_messagekeys_test_errors.txt for details.")
        }
    }
}
