package nip17

import com.example.nostr.EventFactory
import com.example.crypto.nip44.Nip44v2
import com.example.crypto.util.toHexKey
import kotlinx.serialization.json.Json
import com.example.nostr.models.NostrEnvelope
import fr.acinq.bitcoin.PrivateKey
import com.example.crypto.importPublicKeyFromXOnlyHex
import com.example.nostr.util.randomTimeUpTo2DaysInThePast
import com.example.crypto.signHashSchnorr
import com.example.crypto.generateRandomKeyPair
import com.example.crypto.util.XOnlyHexKey

fun createEncryptedRumorKind14(
    senderPriv: PrivateKey,
    participants: List<XOnlyHexKey>, //this must be a list of x-only hex keys
    content: String,
    subject: String,
    //no timestamp needed here as we will reuse the same payload for the chacha, no more work only fun chacha
): List<String> {
     val rumorKind14 = EventFactory.createUnsignedEvent(
        pubkeyHex = senderPriv.xOnlyPublicKey().value.toByteArray().toHexKey(),
        kind = 14,
        content = content,
        tagsBuilder = {
            participants.forEach { p(it) }
            subject(subject)
        }
    )
    //val rumorKind14jsonString = Json.encodeToString(rumorKind14)
    val rumorKind14jsonString = Json.encodeToString(NostrEnvelope.serializer(), rumorKind14)

    val nip44v2 = Nip44v2()
    val encryptedPayloadList = mutableListOf<String>()
    for (participantHex in participants) {
        val receiverPub = importPublicKeyFromXOnlyHex(participantHex)
        val nip44Key = nip44v2.computeConversationKey(senderPriv, receiverPub)
        val encryptedPayload = nip44v2.encryptPayload(rumorKind14jsonString, nip44Key)
        encryptedPayloadList.add(encryptedPayload)
    }
    return encryptedPayloadList
}

//keep for ref but we using fun createASingleEncryptedSealKind13andGiftWrapKind1059(
fun createASingleEncryptedSealKind13(
    senderPriv: PrivateKey, //this only to nip44 encrypt we sign this event with a random one 
    encryptedRumorPayload: String,
    randomPriv: PrivateKey,
    participant: XOnlyHexKey, //x only pub key hex
): String {
    val sealKind13 = EventFactory.createSignedEvent(
        pubkeyHex = senderPriv.xOnlyPublicKey().value.toByteArray().toHexKey(),
        kind = 13,
        content = encryptedRumorPayload,
        createdAt = randomTimeUpTo2DaysInThePast(),
        tagsBuilder = { /* No tags needed for sealing, just the content */ },
        signer = { eventIdToSign ->
            signHashSchnorr(eventIdToSign, randomPriv).toByteArray()
        }
    )
    val sealKind13jsonString = Json.encodeToString(NostrEnvelope.serializer(), sealKind13)
    val nip44v2              = Nip44v2()
    val receiverPub          = importPublicKeyFromXOnlyHex(participant)
    //this is wrong it must be encrypted with the random priv key not the sender one
    val nip44Key             = nip44v2.computeConversationKey(senderPriv, receiverPub)
    val encryptedPayload     = nip44v2.encryptPayload(sealKind13jsonString, nip44Key)
    return encryptedPayload

}


fun createASingleEncryptedSealKind13andGiftWrapKind1059(
    senderPriv: PrivateKey, //this only to nip44 encrypt we sign this event with a random one 
    encryptedRumorPayload: String,
    participant: XOnlyHexKey, //x only pub key hex
): String {
    val (randomPriv, _) = generateRandomKeyPair() //how inject this properly in same way as senderPriv
    val sealKind13 = EventFactory.createSignedEvent(
        pubkeyHex = senderPriv.xOnlyPublicKey().value.toByteArray().toHexKey(),
        kind = 13,
        content = encryptedRumorPayload,
        createdAt = randomTimeUpTo2DaysInThePast(),
        tagsBuilder = { /* No tags needed for sealing, just the content */ },
        signer = { eventIdToSign ->
            signHashSchnorr(eventIdToSign, randomPriv).toByteArray()
        }
    )
    val sealKind13jsonString  = Json.encodeToString(NostrEnvelope.serializer(), sealKind13)
    val nip44v2               = Nip44v2()
    val receiverPub           = importPublicKeyFromXOnlyHex(participant)
    val nip44Key              = nip44v2.computeConversationKey(randomPriv, receiverPub)
    val encryptedSealPayload  = nip44v2.encryptPayload(sealKind13jsonString, nip44Key)

    val randomPubHex = randomPriv.xOnlyPublicKey().value.toByteArray().toHexKey()
    val kind1059Event = EventFactory.createSignedEvent(
        pubkeyHex = randomPubHex,
        kind = 1059,
        content = encryptedSealPayload,
        createdAt = randomTimeUpTo2DaysInThePast(),
        tagsBuilder = {
            p(participant) // Single recipient only
        },
        signer = { eventIdToSign ->
            signHashSchnorr(eventIdToSign, randomPriv).toByteArray()
        }
    )
    //val kind1059EventJsonString = Json.encodeToString(kind1059Event)
    val kind1059EventJsonString = Json.encodeToString(NostrEnvelope.serializer(), kind1059Event)

    return kind1059EventJsonString
}

fun createNip17Events(
    senderPriv: PrivateKey,
    participants: List<XOnlyHexKey>,
    content: String,
    subject: String
): List<String> {
    val rumorEncryptedPayload = createEncryptedRumorKind14(
        senderPriv = senderPriv,
        participants = participants,
        content = content,
        subject = subject
    )
    return participants.mapIndexed { idx, participant ->
        createASingleEncryptedSealKind13andGiftWrapKind1059(
            senderPriv = senderPriv,
            encryptedRumorPayload = rumorEncryptedPayload[idx],
            participant = participant
        )
    }
}

fun handleKind1059Event(
    envelope: NostrEnvelope,
    recipientPriv: PrivateKey,
    nip44v2: Nip44v2 = Nip44v2() //never use this as input here
): NostrEnvelope {
    // Import the sender's pubkey from the envelope
    val senderPub = importPublicKeyFromXOnlyHex(envelope.pubkey)
    // Compute the conversation key
    val nip44Key = nip44v2.computeConversationKey(recipientPriv, senderPub)
    // Decrypt the envelope's content
    val decryptedInfo = nip44v2.decrypt(envelope.content, nip44Key)
    val decryptedContent = nip44v2.unpad(decryptedInfo.ciphertext)
    // Parse and return the inner kind13 NostrEnvelope
    return Json.decodeFromString(NostrEnvelope.serializer(), decryptedContent)
}

fun handleKind13Event(
    kind13Envelope: NostrEnvelope,
    recipientPriv: PrivateKey,
    nip44v2: Nip44v2 = Nip44v2()
): NostrEnvelope? {
    val kind13SenderPub = importPublicKeyFromXOnlyHex(kind13Envelope.pubkey)
    val nip44Key = nip44v2.computeConversationKey(recipientPriv, kind13SenderPub)
    val decryptedInfo = nip44v2.decrypt(kind13Envelope.content, nip44Key)
    val decryptedKind14Content = nip44v2.unpad(decryptedInfo.ciphertext)
    val kind14Envelope = Json.decodeFromString(NostrEnvelope.serializer(), decryptedKind14Content)
    return if (kind13Envelope.pubkey == kind14Envelope.pubkey) {
        kind14Envelope
    } else {
        null
    }


}

fun handleKind1059EventFull(
    kind1059Envelope: NostrEnvelope,
    recipientPriv: PrivateKey
): NostrEnvelope? {
    val kind13Envelope = handleKind1059Event(kind1059Envelope, recipientPriv)
    return handleKind13Event(kind13Envelope, recipientPriv)
}
