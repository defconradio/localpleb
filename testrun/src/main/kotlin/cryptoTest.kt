import com.example.crypto.encodeNostrBech32
import com.example.crypto.generateRandomKeyPair
import com.example.crypto.importKeyPairFromHex
import com.example.crypto.signStringSchnorr
import com.example.crypto.verifyStringSignatureSchnorr
import com.example.crypto.signHashSchnorr
import com.example.crypto.verifyHashSignatureSchnorr
import com.example.crypto.util.sha256
import com.example.nostr.models.NostrEnvelope
import com.example.nostr.models.StallContent
import com.example.nostr.util.EventTemplate
import com.example.crypto.util.toHexKey
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import fr.acinq.bitcoin.ByteVector32
import fr.acinq.bitcoin.XonlyPublicKey
import com.example.nostr.models.ShippingZone
import com.example.nostr.EventFactory
import com.example.data.KeyManager
import com.example.nostr.verify
import com.example.crypto.importNostrPrivHexFromNsec

fun main() {
    //TEST1
    val (priv, pub) = generateRandomKeyPair()
    val xonlyPub = priv.xOnlyPublicKey()
    println("Generate a Random key pair for new user onboard")
    println("Private key (hex): \u001B[36m${priv.value.toByteArray().joinToString("") { "%02x".format(it) }}\u001B[0m")
    println("Compressed Public key (hex): ${pub.value.toByteArray().joinToString("") { "%02x".format(it) }}")
    println("X-Only Public key (hex): ${xonlyPub.value.toByteArray().joinToString("") { "%02x".format(it) }}")
    println("\n")

    //TEST2
    val importedPrivKeyHex = "c3975b71347b41a066516e66ae15596fe336c3e290d2dba1dbc9a98e249d3c38"
    val (importedPriv, importedPub) = importKeyPairFromHex(importedPrivKeyHex)
    val importedXOnlyPub = importedPriv.xOnlyPublicKey()
    println("Import an existing private key (simulate old user)")
    println("Imported Private key (hex): ${importedPriv.value.toByteArray().joinToString("") { "%02x".format(it) }}")
    println("Imported Compressed Public key (hex): ${importedPub.value.toByteArray().joinToString("") { "%02x".format(it) }}")
    println("Imported X-Only Public key (hex): ${importedXOnlyPub.value.toByteArray().joinToString("") { "%02x".format(it) }}")
    println("\n")

    //TEST3
    println("Encode imported user key pair to NIP-19 bech32:")
    val npub = encodeNostrBech32("npub", importedXOnlyPub.value.toByteArray())
    val nsec = encodeNostrBech32("nsec", importedPriv.value.toByteArray())
    println("npub: $npub")
    println("nsec: $nsec")
    println("\n")

    // TEST4: Sign a message with imported private key (Schnorr signature)
    val message = "this is a nostr evet message placeholder to be signed"
    val signature = signStringSchnorr(message, importedPriv)
    println("Signed message: '$message'")
    println("Signature (hex): ${signature.toByteArray().joinToString("") { "%02x".format(it) }}")
    println()

    // TEST5: Verify the signature with imported x-only public key
    val isValid = verifyStringSignatureSchnorr(message, signature.toByteArray(), importedXOnlyPub)
    println("Signature valid? $isValid")

    // TEST6: Create a simple Nostr note event (kind=1, no tags) using the new builder
    println("\nTEST6: Create a simple Nostr note event (kind=1, no tags) using the new builder")
    val noteTemplate = EventTemplate.create(
        kind = 1,
        content = "Hello from the new EventTemplate builder!",
        createdAt = System.currentTimeMillis() / 1000
    )
    println(noteTemplate)


    // TEST7: Canonicalize, hash, sign, and verify a Nostr event (NIP-01 compliant)
    val pubkeyHex = importedXOnlyPub.value.toByteArray().toHexKey()
    val canonicalJson = noteTemplate.toCanonicalJson(pubkeyHex)
    val eventId = sha256(canonicalJson.toByteArray()).toHexKey()
    println("\nTEST7: Canonicalize, hash, sign, and verify a Nostr event (NIP-01 compliant)")
    println("Event ID (hash, hex): $eventId")

    // Sign the event hash (eventId is hex, convert to bytes)
    val eventIdBytes = eventId.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    val eventSignature = signHashSchnorr(eventIdBytes, importedPriv)
    println("Event signature (hex): ${eventSignature.toByteArray().toHexKey()}")

    // Verify the event signature
    val isEventSigValid = verifyHashSignatureSchnorr(eventIdBytes, eventSignature.toByteArray(), importedXOnlyPub)
    println("Event signature valid? $isEventSigValid")

    // Assemble the final signed event (add id and sig fields)
    val signedEvent = NostrEnvelope(
        id = eventId,
        pubkey = pubkeyHex,
        created_at = noteTemplate.createdAt,
        kind = noteTemplate.kind,
        tags = noteTemplate.tags,
        content = noteTemplate.content,
        sig = eventSignature.toByteArray().toHexKey()
    )
    println("\nFinal signed Nostr event:")
    println(signedEvent)

    // TEST8: Validate the signature of the final signed Nostr event
    // 1. Convert the event id (hex) to bytes
    val signedEventIdBytes = signedEvent.id.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    // 2. Convert the signature (hex) to bytes
    val signedEventSigBytes = signedEvent.sig!!.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    // 3. Convert the pubkey (hex) to x-only public key object
    val signedEventPubkeyBytes = signedEvent.pubkey.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    val signedEventXOnlyPub = XonlyPublicKey(ByteVector32(signedEventPubkeyBytes))
    // 4. Verify the signature
    val isFinalSigValid = verifyHashSignatureSchnorr(signedEventIdBytes, signedEventSigBytes, signedEventXOnlyPub)
    println("\nTEST8: Final Nostr event signature valid? $isFinalSigValid")

    // TEST9: Create and sign a 30017 (stall) event with the new EventTemplate builder
    println("\nTEST9: Create and sign a 30017 (stall) event with the new EventTemplate builder")
    // 1. Prepare StallContent and serialize it
    val stallContent = StallContent(
        id = "stall-123",
        name = "Satoshi's Coffee Stand",
        description = "Best coffee on Nostr!",
        currency = "USD",
        shipping = listOf(
            ShippingZone(
                id = "zone-1",
                name = "USA",
                cost = 5.0f,
                regions = listOf("CA", "NY"),
                countries = listOf("US")
            )
        )
    )
    val contentJson = Json.encodeToString(stallContent)

    // 2. Create the event template using the generic builder and tag helper
    val stallTemplate = EventTemplate.create(
        kind = 30017,
        content = contentJson,
        createdAt = System.currentTimeMillis() / 1000

    ) {
        d("stall-123")
        name("Satoshi's Coffee Stand")
        t("coffee")
        t("beverage")
    }

    // 3. Canonicalize and hash event
    val stallCanonicalJson = stallTemplate.toCanonicalJson(pubkeyHex)
    val eventId9 = sha256(stallCanonicalJson.toByteArray()).toHexKey()

    // 4. Sign event id
    val eventId9Bytes = eventId9.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    val eventSig9 = signHashSchnorr(eventId9Bytes, importedPriv)

    // 5. Assemble NostrEnvelope
    val stallEvent = NostrEnvelope(
        id = eventId9,
        pubkey = pubkeyHex,
        created_at = stallTemplate.createdAt,
        kind = stallTemplate.kind,
        tags = stallTemplate.tags,
        content = stallTemplate.content,
        sig = eventSig9.toByteArray().toHexKey()
    )

    println("\nTEST9: 30017 Stall Event with tags (using new EventTemplate builder)")
    println(stallEvent)

    // 6. Verify signature
    val isStallSigValid = verifyHashSignatureSchnorr(
        eventId9Bytes,
        eventSig9.toByteArray(),
        importedXOnlyPub
    )
    println("TEST9: Stall event signature valid? $isStallSigValid\n")


    // TEST10: Get Taproot address exactly as Bitcoin Core does
    val taprootCoreAddress = importedXOnlyPub.p2trAddress(fr.acinq.bitcoin.Block.Testnet3GenesisBlock.hash)
    println("Bitcoin Core-style Taproot address (tb1p...): $taprootCoreAddress")

    // TEST11: Create a signed event using the EventFactory (ViewModel simulation)
    println("\nTEST11: Create a signed event using the EventFactory (ViewModel simulation)")

    // --- The Secure Context (e.g., a ViewModel or a secure key manager) ---
    // The user's private key is loaded and held securely within this limited scope.
    // It is NEVER passed directly to the EventFactory or any other generic library.
    val userPrivateKey = importedPriv
    val userPublicKey = userPrivateKey.xOnlyPublicKey()
    val userPublicKeyHex = userPublicKey.value.toByteArray().toHexKey()

    // --- Calling the Generic, Untrusted Factory ---
    // The ViewModel calls the factory to create an event.
    // Instead of passing the private key, it passes a SIGNING FUNCTION (a lambda).
    val factoryStallEvent = EventFactory.createSignedEvent(
        pubkeyHex = userPublicKeyHex,
        kind = 30017,
        content = contentJson, // Re-using content from TEST9
        tagsBuilder = {
            d("stall-456-factory")
            name("Factory Coffee Stand")
            t("coffee")
            t("factory")
        },
        // This is the core of the secure pattern.
        // The `signer` lambda is defined here, in the secure context.
        signer = { eventIdToSign ->
            // The lambda "closes over" `userPrivateKey`, meaning it can use it
            // from its parent scope without it being passed as an argument.
            // The EventFactory calls this function with the data it needs signed,
            // but it has NO ACCESS to the key itself.
            val signatureBytes = signHashSchnorr(eventIdToSign, userPrivateKey)

            // The only thing returned to the factory is the final signature.
            signatureBytes.toByteArray()
        }
    )

    println("\nTEST11: 30017 Stall Event with tags (using EventFactory)")
    println(factoryStallEvent)

    // --- Verification ---
    // We can still verify the event was signed correctly with the public key.
    val factoryEventIdBytes = factoryStallEvent.id.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    val factoryEventSigBytes = factoryStallEvent.sig!!.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    val isFactorySigValid = verifyHashSignatureSchnorr(
        factoryEventIdBytes,
        factoryEventSigBytes,
        userPublicKey
    )
    println("TEST11: Factory event signature valid? $isFactorySigValid\n")

    // TEST12: Simulate real app flow using KeyManager for key persistence
    println("\nTEST12: Simulate real app flow using KeyManager for key persistence")

    // --- Onboarding Simulation ---
    // 1. A new key is generated for the user.
    val (newUserPriv, _) = generateRandomKeyPair()
    val newUserPrivHex = newUserPriv.value.toByteArray().toHexKey()
    println("Generated a new private key for the user: $newUserPrivHex")

    // 2. The app saves this key using the platform-specific KeyManager.
    // We are using the JVM implementation for this test.
    KeyManager.savePrivateKey(newUserPrivHex)
    println("Saved the key using KeyManager.")

    // --- App Usage Simulation (e.g., in a ViewModel) ---
    // 3. The app needs to sign an event. It loads the key from KeyManager.
    val loadedPrivHex = KeyManager.loadPrivateKey()
    println("Loaded the key from KeyManager: $loadedPrivHex")

    if (loadedPrivHex == null) {
        println("TEST12 FAILED: Could not load private key from KeyManager.")
        return
    }

    // 4. The loaded hex key is converted back into a PrivateKey object.
    val (loadedPrivateKey, _) = importKeyPairFromHex(loadedPrivHex)
    val loadedPublicKey = loadedPrivateKey.xOnlyPublicKey()
    val loadedPublicKeyHex = loadedPublicKey.value.toByteArray().toHexKey()

    // 5. The app uses the loaded key to create a signer and sign an event via EventFactory.
    println("Using loaded key to sign a new event via EventFactory...")
    val eventFromLoadedKey = EventFactory.createSignedEvent(
        pubkeyHex = loadedPublicKeyHex,
        kind = 1,
        content = "This event was signed with a key loaded from KeyManager.",
        tagsBuilder = { t("key-manager-test") },
        signer = { eventIdToSign ->
            // The signer lambda uses the securely loaded private key.
            val signatureBytes = signHashSchnorr(eventIdToSign, loadedPrivateKey)
            signatureBytes.toByteArray()
        }
    )

    println("\nTEST12: Event signed with loaded key:")
    println(eventFromLoadedKey)

    // 6. Verification
    val eventFromLoadedKeyIdBytes = eventFromLoadedKey.id.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    val eventFromLoadedKeySigBytes = eventFromLoadedKey.sig!!.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    val isLoadedKeySigValid = verifyHashSignatureSchnorr(
        eventFromLoadedKeyIdBytes,
        eventFromLoadedKeySigBytes,
        loadedPublicKey
    )
    println("TEST12: Event signature valid? $isLoadedKeySigValid")

    // 7. Cleanup
    KeyManager.deletePrivateKey()
    println("TEST12: Cleaned up by deleting the test key file.\n")

    // TEST13: Verify a complete event using the new NostrEnvelope.verify() extension function
    println("\nTEST13: Verify a complete event using the new NostrEnvelope.verify() extension function")
    // We'll use the event created by the factory in TEST11.
    val isEventValidWithExtension = factoryStallEvent.verify()
    println("TEST13: Verification using extension function successful? $isEventValidWithExtension")

    // Let's also test a tampered event. We'll change the content without re-signing.
    val tamperedEvent = factoryStallEvent.copy(content = "This content has been tampered with!")
    val isTamperedEventValid = tamperedEvent.verify()
    println("TEST13: Verification of tampered event successful? $isTamperedEventValid")

    // TEST14: Sign a known event id and compare signature with Python using the nsec from Python
    println("\nTEST14: Sign known event id for cross-language signature check (using nsec from Python)")
    val nsecPython = "nsec1uahc686dwqwrh0n3gdghj4y58xh9j5jhrdl7appgzetgnkh4fqxq4x9fan"
    val privHexPython = importNostrPrivHexFromNsec(nsecPython)
    val (privPython, _) = importKeyPairFromHex(privHexPython)
    val eventIdHex14 = "20c325316262c64b0d188904fd4ea69f1e3a478e474167ba0f5da61ff75e8b49"
    val eventIdBytes14 = eventIdHex14.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    val eventSig14 = signHashSchnorr(eventIdBytes14, privPython)
    println("Event id (hex): $eventIdHex14")
    println("Signature (hex): ${eventSig14.toByteArray().joinToString("") { "%02x".format(it) }}")
    // For reference, Python expected signature: 4497f4bbf8ae61bef88cd87dab7e6307a74bac9d838dda8f4f3bdc69c01c8b645efa84f3837b5e9cbea7eb76601a331e997a19d8b3475539dc097163135d671f

    // TEST15: Sign a known event id and compare signature with Python/Android using the nsec and event id from latest event
    println("\nTEST15: Sign known event id for cross-language signature check (latest Android/Python event)")
    val nsecLatest = "nsec1uahc686dwqwrh0n3gdghj4y58xh9j5jhrdl7appgzetgnkh4fqxq4x9fan"
    val privHexLatest = importNostrPrivHexFromNsec(nsecLatest)
    val (privLatest, _) = importKeyPairFromHex(privHexLatest)
    val eventIdHex15 = "f0e71bf0f0af09fd79af9394fabdb9489b5bc6720750b48b02c0e940e5e6cfb9"
    val eventIdBytes15 = eventIdHex15.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    val eventSig15 = signHashSchnorr(eventIdBytes15, privLatest)
    println("Private key (hex): $privHexLatest")
    println("Event id (hex): $eventIdHex15")
    println("Signature (hex): ${eventSig15.toByteArray().joinToString("") { "%02x".format(it) }}")
    // For reference, Android expected signature: a3eca4d906de573f20ed0190f25781b61e487ee308df17b2e2c7e59aca237a8592e76087e83bf068d3983cbbb0f410ffb412b914c025f424d0e457e1e2341e1d


    println("\nTEST16: Create a kind 14 direct message event")

    // 1. Define participants and relay
    val (senderPriv, _) = generateRandomKeyPair()
    val senderPub = senderPriv.xOnlyPublicKey()
    val senderPubHex = senderPub.value.toByteArray().toHexKey()

    val (receiver1Priv, _) = generateRandomKeyPair()
    val receiver1PubHex = receiver1Priv.xOnlyPublicKey().value.toByteArray().toHexKey()

    val (receiver2Priv, _) = generateRandomKeyPair()
    val receiver2PubHex = receiver2Priv.xOnlyPublicKey().value.toByteArray().toHexKey()

    val relayUrl = "wss://relay.damus.io"
    val conversationSubject = "Public Nostr Chat" // Changed from "Secret" to avoid confusion
    val messageContent = "Hello, this is a public message in a kind 14 event!"

    println("Sender pubkey: $senderPubHex")
    println("Receiver 1 pubkey: $receiver1PubHex")
    println("Receiver 2 pubkey: $receiver2PubHex")

    // 2. Create the event using EventFactory
    val kind14Event = EventFactory.createSignedEvent(
        pubkeyHex = senderPubHex,
        kind = 14,
        content = messageContent,
        tagsBuilder = {
            p(receiver1PubHex, relayUrl)
            p(receiver2PubHex, relayUrl)
            subject(conversationSubject)
        },
        signer = { eventIdToSign ->
            signHashSchnorr(eventIdToSign, senderPriv).toByteArray()
        }
    )

    println("\nFinal Kind 14 Event (Public Group Message):")
    println(Json.encodeToString(kind14Event))

    // 3. Verify the signature
    // Note: This only verifies that the sender is authentic. It does not imply privacy.
    // The content of a kind 14 event is public and not encrypted.
    // For encrypted DMs, use kind 4 (NIP-04).
    val isKind14EventValid = kind14Event.verify()
    println("\nTEST16: Kind 14 event signature valid? $isKind14EventValid")

    // TEST17: Decode nsec to hex public key
    println("\nTEST17: Decode nsec to hex public key")
    val nsecToDecode = "nsec1w750aeup238ajx7hqd50qd23dzfw3y79assssq4mn2slt5s6q8qq86uj05"
    println("Decoding nsec: $nsecToDecode")
    val decodedPrivHex = importNostrPrivHexFromNsec(nsecToDecode)
    val (decodedPrivKey, _) = importKeyPairFromHex(decodedPrivHex)
    val decodedXOnlyPubKey = decodedPrivKey.xOnlyPublicKey()
    val decodedPubKeyHex = decodedXOnlyPubKey.value.toByteArray().toHexKey()
    println("Corresponding public key (hex): $decodedPubKeyHex")
}
