import com.example.crypto.generateRandomKeyPair
import com.example.crypto.signHashSchnorr
import com.example.crypto.util.toHexKey
import com.example.nostr.EventFactory
import com.example.nostr.models.NostrEnvelope
import com.example.nostr.util.PowUtil
import com.example.nostr.verify
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    println("TEST NIP-13: Proof of Work")

    // 1. Set up keys and difficulty
    val (privateKey, _) = generateRandomKeyPair()
    val publicKey = privateKey.xOnlyPublicKey()
    val pubkeyHex = publicKey.value.toByteArray().toHexKey()
    val difficulty = 28 // Using a more reasonable difficulty for a test

    println("Using pubkey: $pubkeyHex")
    println("Target difficulty: $difficulty leading zero bits")

    // 2. Create a base event template.
    val unsignedEvent = EventFactory.createUnsignedEvent(
        pubkeyHex = pubkeyHex,
        kind = 1,
        content = "This is a message with proof-of-work!",
        tagsBuilder = {
            t("nip-13")
            t("test")
        }
    )

    println("\nOriginal (unsigned) event template:")
    println(unsignedEvent)

    // 3. Mine the event to meet the difficulty requirement.
    println("\nMining for a valid nonce (in parallel)...")
    val startTime = System.currentTimeMillis()
    val minedEvent = PowUtil.minePow(unsignedEvent, difficulty)
    val endTime = System.currentTimeMillis()
    println("Mining complete in ${endTime - startTime} ms!")
    println("Mined Event ID: ${minedEvent.id}")
    println("Nonce Tag: ${minedEvent.tags.find { it.firstOrNull() == "nonce" }}")

    // 4. Sign the MINED event.
    val eventIdBytes = minedEvent.id.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    val signature = signHashSchnorr(eventIdBytes, privateKey)

    // 5. Create the final, signed event by adding the signature to the mined event.
    val finalSignedPowEvent = minedEvent.copy(sig = signature.toByteArray().toHexKey())

    println("\nFinal Signed Proof-of-Work Event:")
    println(finalSignedPowEvent)

    // 6. Verification
    println("\nVerifying Proof-of-Work...")
    var leadingZeros = 0
    for (c in finalSignedPowEvent.id) {
        if (c == '0') {
            leadingZeros += 4
        } else {
            leadingZeros += when (c) {
                '1' -> 3
                '2', '3' -> 2
                '4', '5', '6', '7' -> 1
                else -> 0
            }
            break
        }
    }
    println("Calculated leading zero bits: $leadingZeros")
    println("Difficulty requirement met? ${leadingZeros >= difficulty}")

    println("\nVerifying signature...")
    val isSignatureValid = finalSignedPowEvent.verify()
    println("Signature valid? $isSignatureValid")

    if (isSignatureValid && leadingZeros >= difficulty) {
        println("\nSUCCESS: NIP-13 Proof-of-Work test passed!")
    } else {
        println("\nFAILURE: NIP-13 Proof-of-Work test failed.")
    }
}
