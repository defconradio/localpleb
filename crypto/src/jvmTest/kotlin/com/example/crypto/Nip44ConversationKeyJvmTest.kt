package com.example.crypto

import com.example.crypto.nip44.Nip44v2
import kotlin.test.Test
import fr.acinq.bitcoin.PrivateKey
import fr.acinq.bitcoin.PublicKey
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

data class ConversationKeyVector(val sec1: String, val pub2: String, val conversation_key: String)

private fun hexToByteArray(hex: String): ByteArray = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

class Nip44ConversationKeyJvmTest {
    @Test
    fun testGetConversationKeyVectors() {
        val filePath = "src/commonTest/resources/nip44.vectors.json"
        val errorFile = "/tmp/nip44_test_errors.txt"
        val errorMessages = mutableListOf<String>()
        try {
            val jsonText = File(filePath).readText()
            val json = Json.parseToJsonElement(jsonText)
            val vectorsJson = json.jsonObject["v2"]?.jsonObject?.get("valid")?.jsonObject?.get("get_conversation_key")?.jsonArray
            if (vectorsJson == null) throw Exception("get_conversation_key not found in JSON")
            val vectors = vectorsJson.map {
                val sec1 = it.jsonObject["sec1"]?.jsonPrimitive?.content ?: ""
                val pub2 = it.jsonObject["pub2"]?.jsonPrimitive?.content ?: ""
                val conversation_key = it.jsonObject["conversation_key"]?.jsonPrimitive?.content ?: ""
                ConversationKeyVector(sec1, pub2, conversation_key)
            }
            val nip44 = Nip44v2()
            var failed = false
            for ((i, vector) in vectors.withIndex()) {
                val sec1Bytes = hexToByteArray(vector.sec1)
                val pub2Hex = if (vector.pub2.length == 64) "02${vector.pub2}" else vector.pub2
                val pub2Bytes = hexToByteArray(pub2Hex)
                val expectedKey = vector.conversation_key
                try {
                    val sec1 = PrivateKey(sec1Bytes)
                    val pub2 = PublicKey(pub2Bytes)
                    val computedKey = nip44.computeConversationKey(sec1, pub2)
                    val computedHex = computedKey.joinToString("") { "%02x".format(it) }
                    if (computedHex != expectedKey) {
                        val msg = "Vector $i FAILED: sec1=${vector.sec1}, pub2=$pub2Hex, expected=$expectedKey, got=$computedHex"
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
                kotlin.test.fail("Some vectors failed. See /tmp/nip44_test_errors.txt for details.")
            }
        } catch (e: Exception) {
            val msg = "Serialization or file error: ${e.message}"
            File(errorFile).writeText(msg)
            kotlin.test.fail("Fatal error. See /tmp/nip44_test_errors.txt for details.")
        }
    }
}
