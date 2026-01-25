package com.example.crypto

import com.example.crypto.nip44.Nip44v2
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import fr.acinq.bitcoin.PrivateKey
import fr.acinq.bitcoin.PublicKey

class Nip44ConversationKeyInvalidJvmTest {
    @Test
    fun testConversationKeyInvalidVectors() {
        val filePath = "src/commonTest/resources/nip44.vectors.json"
        val errorFile = "/tmp/nip44_conversation_key_invalid_test_errors.txt"
        val errorMessages = mutableListOf<String>()
        val jsonText = File(filePath).readText()
        val json = Json.parseToJsonElement(jsonText)
        val vectorsArray = json.jsonObject["v2"]?.jsonObject?.get("invalid")?.jsonObject?.get("get_conversation_key")?.jsonArray
        if (vectorsArray == null) throw Exception("get_conversation_key invalid vectors not found in JSON")
        var failed = false
        val nip44 = Nip44v2()
        fun hexToByteArray(hex: String): ByteArray = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        for ((i, obj) in vectorsArray.withIndex()) {
            val sec1 = obj.jsonObject["sec1"]?.jsonPrimitive?.content ?: ""
            val pub2 = obj.jsonObject["pub2"]?.jsonPrimitive?.content ?: ""
            val pub2Hex = if (pub2.length == 64) "02$pub2" else pub2
            try {
                assertFailsWith<Exception>("Should fail for invalid get_conversation_key vector $i") {
                    val sec1Bytes = hexToByteArray(sec1)
                    val pub2Bytes = hexToByteArray(pub2Hex)
                    val priv = PrivateKey(sec1Bytes)
                    val pub = PublicKey(pub2Bytes)
                    nip44.computeConversationKey(priv, pub)
                }
            } catch (e: Exception) {
                val msg = "Vector $i EXCEPTION: ${e.message}"
                errorMessages.add(msg)
                failed = true
            }
        }
        if (failed) {
            File(errorFile).writeText(errorMessages.joinToString("\n"))
            kotlin.test.fail("Some vectors failed. See $errorFile for details.")
        }
    }
}
