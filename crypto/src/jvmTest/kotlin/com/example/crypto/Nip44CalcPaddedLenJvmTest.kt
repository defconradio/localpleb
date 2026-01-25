package com.example.crypto

import com.example.crypto.nip44.Nip44v2
import kotlin.test.Test
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import java.io.File

class Nip44CalcPaddedLenJvmTest {
    @Test
    fun testCalcPaddedLenVectors() {
        val filePath = "src/commonTest/resources/nip44.vectors.json"
        val errorFile = "/tmp/nip44_calc_padded_len_test_errors.txt"
        val errorMessages = mutableListOf<String>()
        try {
            val jsonText = File(filePath).readText()
            val json = Json.parseToJsonElement(jsonText)
            val vectorsArray = json.jsonObject["v2"]?.jsonObject?.get("valid")?.jsonObject?.get("calc_padded_len")?.jsonArray
            if (vectorsArray == null) throw Exception("calc_padded_len not found in JSON")
            val nip44 = Nip44v2()
            var failed = false
            for ((i, arr) in vectorsArray.withIndex()) {
                val input = arr.jsonArray[0].toString().toInt()
                val expected = arr.jsonArray[1].toString().toInt()
                try {
                    val result = nip44.calcPaddedLen(input)
                    if (result != expected) {
                        val msg = "Vector $i FAILED: input=$input, expected=$expected, got=$result"
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
                kotlin.test.fail("Some vectors failed. See /tmp/nip44_calc_padded_len_test_errors.txt for details.")
            }
        } catch (e: Exception) {
            val msg = "Serialization or file error: ${e.message}"
            File(errorFile).writeText(msg)
            kotlin.test.fail("Fatal error. See /tmp/nip44_calc_padded_len_test_errors.txt for details.")
        }
    }
}
