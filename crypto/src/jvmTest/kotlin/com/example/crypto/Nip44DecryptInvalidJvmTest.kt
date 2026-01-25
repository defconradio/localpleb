package com.example.crypto

import com.example.crypto.nip44.Nip44v2
import kotlin.test.Test
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

class Nip44DecryptInvalidJvmTest {
    @Test
    fun testDecryptInvalidVectors() {
        val filePath = "src/commonTest/resources/nip44.vectors.json"
        val errorFile = "/tmp/nip44_decrypt_invalid_test_errors.txt"
        val errorMessages = mutableListOf<String>()
        val jsonText = File(filePath).readText()
        val json = Json.parseToJsonElement(jsonText)
        val vectorsArray = json.jsonObject["v2"]?.jsonObject?.get("invalid")?.jsonObject?.get("decrypt")?.jsonArray
        if (vectorsArray == null) throw Exception("decrypt invalid vectors not found in JSON")
        var failed = false
        val nip44 = Nip44v2()
        fun hexToByteArray(hex: String): ByteArray = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        for ((i, obj) in vectorsArray.withIndex()) {
            val conversationKeyHex = obj.jsonObject["conversation_key"]?.jsonPrimitive?.content ?: ""
            val payload = obj.jsonObject["payload"]?.jsonPrimitive?.content ?: ""
            val conversationKey = hexToByteArray(conversationKeyHex)
            var threw = false
            try {
                val decrypted = nip44.decrypt(payload, conversationKey)
                nip44.unpad(decrypted.ciphertext)
            } catch (e: Exception) {
                threw = true
                val msg = "Vector $i EXCEPTION: ${e.message}"
                errorMessages.add(msg)
            }
            if (!threw) {
                val msg = "Vector $i FAILED: did not throw for invalid payload or padding"
                errorMessages.add(msg)
                failed = true
            }
        }
        if (failed) {
            File(errorFile).writeText(errorMessages.joinToString("\n"))
            kotlin.test.fail("Some vectors failed. See $errorFile for details.")
        } else {
            File(errorFile).writeText(errorMessages.joinToString("\n"))
        }
    }

/* this test dont pass the test review the decrypt method or padd or unpad here :
{   /home/blackbox/pocAndroid/crypto/src/commonMain/kotlin/com/example/crypto/Nip44v2.kt
   
          "conversation_key": "5254827d29177622d40a7b67cad014fe7137700c3c523903ebbe3e1b74d40214",
          "nonce": "7ab65dbb8bbc2b8e35cafb5745314e1f050325a864d11d0475ef75b3660d91c1",
          "plaintext": "elliptic-curve cryptography",
          "payload": "Anq2XbuLvCuONcr7V0UxTh8FAyWoZNEdBHXvdbNmDZHB573MI7R7rrTYftpqmvUpahmBC2sngmI14/L0HjOZ7lWGJlzdh6luiOnGPc46cGxf08MRC4CIuxx3i2Lm0KqgJ7vA",
          "note": "invalid padding"
        },
        {
          "conversation_key": "fea39aca9aa8340c3a78ae1f0902aa7e726946e4efcd7783379df8096029c496",
          "nonce": "7d4283e3b54c885d6afee881f48e62f0a3f5d7a9e1cb71ccab594a7882c39330",
          "plaintext": "noble",
          "payload": "An1Cg+O1TIhdav7ogfSOYvCj9dep4ctxzKtZSniCw5MwRrrPJFyAQYZh5VpjC2QYzny5LIQ9v9lhqmZR4WBYRNJ0ognHVNMwiFV1SHpvUFT8HHZN/m/QarflbvDHAtO6pY16",
          "note": "invalid padding"
        },
        {
          "conversation_key": "0c4cffb7a6f7e706ec94b2e879f1fc54ff8de38d8db87e11787694d5392d5b3f",
          "nonce": "6f9fd72667c273acd23ca6653711a708434474dd9eb15c3edb01ce9a95743e9b",
          "plaintext": "censorship-resistant and global social network",
          "payload": "Am+f1yZnwnOs0jymZTcRpwhDRHTdnrFcPtsBzpqVdD6b2NZDaNm/TPkZGr75kbB6tCSoq7YRcbPiNfJXNch3Tf+o9+zZTMxwjgX/nm3yDKR2kHQMBhVleCB9uPuljl40AJ8kXRD0gjw+aYRJFUMK9gCETZAjjmrsCM+nGRZ1FfNsHr6Z",
          "note": "invalid padding"
        },
*/
}
