import com.example.crypto.generateRandomKeyPair
import com.example.crypto.util.toHexKey
import kotlinx.serialization.json.Json
import com.example.nostr.EventFactory
import com.example.crypto.signHashSchnorr
import com.example.crypto.nip44.Nip44v2
import kotlinx.serialization.encodeToString
import com.example.nostr.util.randomTimeUpTo2DaysInThePast
import com.example.crypto.importKeyPairFromHex
import com.example.crypto.importPublicKeyFromXOnlyHex

fun main() {
    // TEST1 simulate a NIP-17 build step by step 
    // step1 Create an unsigned kind 14 event
    val (senderPriv, _) = generateRandomKeyPair()
    val senderPub = senderPriv.xOnlyPublicKey()
    val senderPubHex = senderPub.value.toByteArray().toHexKey()
    
    val (receiver1Priv, receiver1PubObj) = generateRandomKeyPair()
    val receiver1Pub = receiver1Priv.xOnlyPublicKey()
    val receiver1PubHex = receiver1Pub.value.toByteArray().toHexKey()

    val (receiver2Priv, _) = generateRandomKeyPair()
    val receiver2Pub = receiver2Priv.xOnlyPublicKey()
    val receiver2PubHex = receiver2Pub.value.toByteArray().toHexKey()
 
    val relayUrl = "wss://relay.damus.io"

    val unsignedKind14Event = EventFactory.createUnsignedEvent(
        pubkeyHex = senderPubHex,
        kind = 14,
        content = "this is a rumor example, and unsigned message kin14",
        tagsBuilder = {
            p(receiver1PubHex, relayUrl)
            p(receiver2PubHex, relayUrl)
            subject("group chat example")
        }
    )
    val unsignedKind14EventjsonString = Json.encodeToString(unsignedKind14Event)
    //val unsignedKind14EventjsonString = Json.encodeToString(NostrEnvelope.serializer(), unsignedKind14Event)
    println(unsignedKind14EventjsonString)

    // step2 encrypt the rumor
    val nip44v2 = Nip44v2()
    val conversationKey = nip44v2.computeConversationKey(senderPriv, receiver1PubObj)
    val encryptedKind14Payload = nip44v2.encryptPayload(unsignedKind14EventjsonString, conversationKey)
    // this is wrong this must use a random key pair so the test must define a random key for this
    // step3 The Seal Event Kind 13, seal the unsigned event "the rumor"
    //  "created_at": randomTimeUpTo2DaysInThePast(),
    val kind13Event = EventFactory.createSignedEvent(
        pubkeyHex = senderPubHex,
        kind = 13,
        content = encryptedKind14Payload,
        createdAt = randomTimeUpTo2DaysInThePast(),
        tagsBuilder = { }, // No tags needed for sealing, just the content
        signer = { eventIdToSign ->
            signHashSchnorr(eventIdToSign, senderPriv).toByteArray()
        }
    )
    val kind13EventJsonString = Json.encodeToString(kind13Event)
    //val kind13EventJsonString = Json.encodeToString(NostrEnvelope.serializer(), kind13Event)
    println("\nFinal Kind 13 Event (Seal Event):")
    println(kind13EventJsonString)
    
    // step4 encrypt the seal
    val encryptedKind13Payload = nip44v2.encryptPayload(kind13EventJsonString, conversationKey)

    // step5 wrap the seal in a gift wrap kin 1059
    // single p tag with the receiver's public key
    // random key   "pubkey": "<random, one-time-use pubkey>",
    val (randomPriv, _) = generateRandomKeyPair()
    val randomPub = randomPriv.xOnlyPublicKey()
    val randomPubHex = randomPub.value.toByteArray().toHexKey()
    //  "created_at": randomTimeUpTo2DaysInThePast(),
    val kind1059Event = EventFactory.createSignedEvent(
        pubkeyHex = randomPubHex,
        kind = 1059,
        content = encryptedKind13Payload,
        createdAt = randomTimeUpTo2DaysInThePast(),
        tagsBuilder = {
            p(receiver1PubHex, relayUrl) // Single recipient only 
        },
        signer = { eventIdToSign ->
            signHashSchnorr(eventIdToSign, randomPriv).toByteArray()
        }
    )
    val kind1059EventJsonString = Json.encodeToString(kind1059Event)
    //val kind1059EventJsonString = Json.encodeToString(NostrEnvelope.serializer(), kind1059Event)
    println("\nFinal Kind 1059 Event (Gift Wrap Event):")
    println(kind1059EventJsonString)
    //Clients MUST verify if pubkey of the kind:13 is the same pubkey on the kind:14, otherwise any sender can impersonate others by simply changing the pubkey on kind:14.

    //TEST2 simulate a NIP-17 step by step version 2
    println("\nTEST2\n")

    // 1 sender 2 receivers alice will send a message to bob and charlie
    // from alice the initiator we have the private key and pub key objects already imported from the device 
    // alice generate a random key pair to use all the way for kind1059 and kind13
    val alicePrivKeyHex       = "c3975b71347b41a066516e66ae15596fe336c3e290d2dba1dbc9a98e249d3c38"
    val (alicePriv, alicePub) = importKeyPairFromHex(alicePrivKeyHex)
    val aliceXOnlyPub         = alicePriv.xOnlyPublicKey()
    val aliceXonlyPubHex      = aliceXOnlyPub.value.toByteArray().toHexKey()

    println("Alice compressed pubkey (hex):   " + alicePub.value.toByteArray().joinToString("") { "%02x".format(it) })
    //TODO define a secure rng for the android app we using a dummy one as kmp common
    val (aliceRandomPriv, aliceRandomPub) = generateRandomKeyPair()
    val aliceRandomXOnlyPub   = aliceRandomPriv.xOnlyPublicKey()
    val aliceRandomOnlyPubHex = aliceRandomXOnlyPub.value.toByteArray().toHexKey()
    println("Alice random pubkey (hex):       " + aliceRandomPub.value.toByteArray().joinToString("") { "%02x".format(it) })
    // from bob and charlie we only know the X only public keys
    // we need the pub key object to perform the point multimplication as we using bitcoin kmp li   
    // TODO perform a benchmark for this if not use the secpt256k1 library directly
    val bobXOnlyPubHex       = "4618e3558693c8fd4f830fcc7ab2f3a1e6eb2998442aef8ee3479a6a4f9344eb"
    val bobPub               = importPublicKeyFromXOnlyHex(bobXOnlyPubHex)
    println("Bob compressed pubkey (hex):     " + bobPub.value.toByteArray().joinToString("") { "%02x".format(it) })
    val charlieXOnlyPubHex   = "840268019fd97011e50f61921f0101403faebb3075cf989755e6a236dc1c0a32"
    val charliePub           = importPublicKeyFromXOnlyHex(charlieXOnlyPubHex)
    println("Charlie compressed pubkey (hex): " + charliePub.value.toByteArray().joinToString("") { "%02x".format(it) })

    // Create the rumor list
    //the rumor must be done for all paritcipants we asume we have all partipcipants x only pub key
    // val participants = listOf(aliceXonlyPubHex, bobXOnlyPubHex, charlieXOnlyPubHex)
    // val rumorEncryptedPayload = nip44.createEncryptedRumorKind14(
    //     senderPriv = alicePriv,
    //     participants = participants,
    //     content = "this is a rumor example, and unsigned message kin14",
    //     subject = "group chat example"
    // )
    // println("Rumor kind14 encrypted payloads:")
    // rumorEncryptedPayload.forEachIndexed { idx, payload ->
    //     println("Participant ${participants[idx]}: $payload")
    // }
    // we use the seal and gift wrap one at once keep this as ref
    // Create the seal for each element in that list using aliceRandomPriv as randomPriv for all
    // val sealEvents = rumorEncryptedPayload.mapIndexed { idx, payload ->
    //     nip44.createASingleEncryptedSealKind13(
    //         senderPriv = alicePriv,
    //         encryptedRumorPayload = payload,
    //         randomPriv = aliceRandomPriv,
    //         participant = participants[idx]
    //     )
    // }
    // println("Seal kind13 events:")
    // sealEvents.forEachIndexed { idx, eventJson ->
    //     println("Participant ${participants[idx]}: $eventJson")
    // }

    // Create the seal and the gift wrap for each element in that list using aliceRandomPriv as randomPriv for all
    // val giftWrapEvents = rumorEncryptedPayload.mapIndexed { idx, payload ->
    //     nip44.createASingleEncryptedSealKind13andGiftWrapKind1059(
    //         senderPriv = alicePriv,
    //         encryptedRumorPayload = payload,
    //         //randomPriv = aliceRandomPriv, //this done by the funtion directly
    //         participant = participants[idx]
    //     )
    // }
    // println("Gift wrap kind1059 events:")
    // giftWrapEvents.forEachIndexed { idx, eventJson ->
    //     println("Participant ${participants[idx]}: $eventJson")
    // }

    //TEST 3 create a nip 17 event for all the participants in a group chat
    val participantsXOnlyHexKeyList = listOf(aliceXonlyPubHex, bobXOnlyPubHex, charlieXOnlyPubHex)
    val senderPrivKeyObject         = alicePriv
    val messageContent              = "this is a rumor example, and unsigned message kin14"
    val messageSubject              = "group chat example"

    // Example usage of the new helper with XOnlyHex type
    val allGiftWrapEvents = nip17.createNip17Events(
        senderPriv = senderPrivKeyObject,
        participants = participantsXOnlyHexKeyList, // List<XOnlyHexKey>
        content = messageContent,
        subject = messageSubject
    )
    println("All gift wrap kind1059 events (via helper):")
    allGiftWrapEvents.forEachIndexed { idx, eventJson ->
        println("Participant ${participantsXOnlyHexKeyList[idx]}: $eventJson")
    }


    
    //TEST 3 decrypt alice inbound messages 

    // 1st step read kind1059 and decrypt .content payload
    val aliceKind1059 = allGiftWrapEvents.getOrNull(0) ?: "no events"
    val kind1059Envelope = Json.decodeFromString(com.example.nostr.models.NostrEnvelope.serializer(), aliceKind1059)
    println("content: ${kind1059Envelope.content}")
    val kind13Envelope = nip17.handleKind1059Event(kind1059Envelope, alicePriv)
    println("Decrypted kind13: ${kind13Envelope.content}")
    // 2nd  step read kin13 from decryptedText and decrypt .content again
    val kind14Envelope = nip17.handleKind13Event(kind13Envelope, alicePriv)
    if (kind14Envelope != null) {
        println("Decrypted kind14: ${kind14Envelope.content}")
    } else {
        println("break something trying impersonate the kind14 author")
    }

    //TEST 4 decrypt bob and charlie inbound messages using a complete funtion 
    val bobPrivKeyHex = "d1cc1ae94947a97a57a7c38f6179f5f3cc84ee850b463d45a9819a112f0a8265"
    val (bobPriv, _) = importKeyPairFromHex(bobPrivKeyHex)
    val bobKind1059 = allGiftWrapEvents.getOrNull(1) ?: "no events"
    if (bobKind1059 != "no events") {
        val bobEnvelope = Json.decodeFromString(com.example.nostr.models.NostrEnvelope.serializer(), bobKind1059)
        val bobKind14 = nip17.handleKind1059EventFull(bobEnvelope, bobPriv)
        if (bobKind14 != null) {
            println("Bob decrypted kind14: ${bobKind14.content}")
        } else {
            println("Bob: break something trying impersonate the kind14 author")
        }
    }
    val charliePrivKeyHex = "1edc6a786a4bc2e868b4a24f60032735dbf8420a920c43411244c37e2a104c90"
    val (charliePriv, _) = importKeyPairFromHex(charliePrivKeyHex)
    val charlieKind1059 = allGiftWrapEvents.getOrNull(2) ?: "no events"

    if (charlieKind1059 != "no events") {
        val charlieEnvelope = Json.decodeFromString(com.example.nostr.models.NostrEnvelope.serializer(), charlieKind1059)
        val charlieKind14 = nip17.handleKind1059EventFull(charlieEnvelope, charliePriv)
        if (charlieKind14 != null) {
            println("Charlie decrypted kind14: ${charlieKind14.content}")
        } else {
            println("Charlie: break something trying impersonate the kind14 author")
        }
    }
}