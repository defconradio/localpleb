package com.example.data.util

import com.example.crypto.generateRandomKeyPair
import com.example.crypto.nip44.Nip44v2
import com.example.crypto.signHashSchnorr
import com.example.crypto.util.toHexKey
import com.example.nostr.EventFactory
import com.example.nostr.models.NostrEnvelope
import com.example.nostr.util.randomTimeUpTo2DaysInThePast
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.example.crypto.importPublicKeyFromXOnlyHex
import com.example.crypto.util.XOnlyHexKey
import com.example.crypto.verifyHashSignatureSchnorr
import fr.acinq.bitcoin.XonlyPublicKey

import fr.acinq.bitcoin.PrivateKey
//import com.example.crypto.importPrivateKeyFromBytes

object Nip17Helpers {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Decrypts the outer layer of a kind 1059 event to reveal the inner kind 13 event.
     */
    fun handleKind1059Event(
        kind1059Envelope: NostrEnvelope,
        recipientPrivateKey: ByteArray
    ): NostrEnvelope? {
        if (kind1059Envelope.kind != 1059) return null
        return try {
            val nip44v2 = Nip44v2()
            val conversationKey = nip44v2.computeConversationKey(
                recipientPrivateKey,
                kind1059Envelope.pubkey
            )
            val decryptedInfo = nip44v2.decrypt(kind1059Envelope.content, conversationKey)
            val decryptedContent = nip44v2.unpad(decryptedInfo.ciphertext)
            val result = json.decodeFromString<NostrEnvelope>(decryptedContent)
            println("[Nip17Helpers.kt] handleKind1059Event successful decryption: $result")
            result
        } catch (e: Exception) {
            println("[Nip17Helpers.kt] Failed to decrypt kind 1059 event: ${e.message}")
            null
        }
    }

    /**
     * Decrypts a kind 13 event to reveal the final rumor event and validates the sender's pubkey.
     */
    fun handleKind13Event(
        kind13Envelope: NostrEnvelope,
        recipientPrivateKey: ByteArray,
        //recipientPrivateKey: PrivateKey,

    ): NostrEnvelope? {
        if (kind13Envelope.kind != 13) return null
        return try {
            val nip44v2 = Nip44v2()
            val conversationKey = nip44v2.computeConversationKey(
                recipientPrivateKey,
                kind13Envelope.pubkey
            )
            val decryptedInfo = nip44v2.decrypt(kind13Envelope.content, conversationKey)
            val decryptedContent = nip44v2.unpad(decryptedInfo.ciphertext)
            val kind14Envelope = json.decodeFromString<NostrEnvelope>(decryptedContent)

            // Security Check: Ensure the pubkey of the seal matches the pubkey of the inner message
            if (kind13Envelope.pubkey == kind14Envelope.pubkey) {
                kind14Envelope
            } else {
                println("[Nip17Helpers.kt] Pubkey mismatch! Seal pubkey ${kind13Envelope.pubkey} does not match rumor pubkey ${kind14Envelope.pubkey}")
                null
            }
        } catch (e: Exception) {
            println("[Nip17Helpers.kt] Failed to decrypt kind 13 event: ${e.message}")
            null
        }
    }

    /**
     * Fully decrypts a kind 1059 event, handling both layers and performing security validation.
     */
    fun handleKind1059EventFull(
        kind1059Envelope: NostrEnvelope,
        recipientPrivateKey: ByteArray
    ): NostrEnvelope? {
        // DANGER REMOVE PRIV KEYS FROM LOGS
        //println("[Nip17Helpers.kt] kind1059Envelope: $kind1059Envelope")
        //println("[Nip17Helpers.kt] recipientPrivateKey: ${recipientPrivateKey.joinToString("") { "%02x".format(it) }}")

        val kind13Envelope = handleKind1059Event(kind1059Envelope, recipientPrivateKey)
            ?: return null

        return handleKind13Event(kind13Envelope, recipientPrivateKey)
    }

    fun createNip17Events(
        senderPriv: ByteArray,
        participant: XOnlyHexKey,
        payloadEvent: NostrEnvelope
    ): NostrEnvelope {
        val rumorEncryptedPayload = createEncryptedRumorKind14(
            senderPriv = senderPriv,
            participant = participant,
            payloadEvent = payloadEvent
        )
        return createASingleEncryptedSealKind13andGiftWrapKind1059(
            senderPriv = senderPriv,
            encryptedRumorPayload = rumorEncryptedPayload,
            participant = participant
        )
    }

    private fun createEncryptedRumorKind14(
        senderPriv: ByteArray,
        participant: XOnlyHexKey,
        payloadEvent: NostrEnvelope
    ): String {
        val senderPrivKey = PrivateKey(senderPriv)
        val rumorKind14jsonString = Json.encodeToString(NostrEnvelope.serializer(), payloadEvent)

        val nip44v2 = Nip44v2()
        val receiverPub = importPublicKeyFromXOnlyHex(participant)
        val nip44Key = nip44v2.computeConversationKey(senderPrivKey, receiverPub)
        return nip44v2.encryptPayload(rumorKind14jsonString, nip44Key)
    }

    private fun createASingleEncryptedSealKind13andGiftWrapKind1059(
        senderPriv: ByteArray, //this only to nip44 encrypt we sign this event with a random one
        encryptedRumorPayload: String,
        participant: XOnlyHexKey, //x only pub key hex
    ): NostrEnvelope {
        val senderPrivKey = PrivateKey(senderPriv)
        val (randomPriv, randomPub) = generateRandomKeyPair() //how inject this properly in same way as senderPriv
        val sealKind13 = EventFactory.createSignedEvent(
            pubkeyHex = senderPrivKey.xOnlyPublicKey().value.toByteArray().toHexKey(),
            kind = 13,
            content = encryptedRumorPayload,
            createdAt = randomTimeUpTo2DaysInThePast(),
            tagsBuilder = { /* No tags needed for sealing, just the content */ },
            signer = { eventIdToSign ->
                signHashSchnorr(eventIdToSign, senderPrivKey).toByteArray()
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
        // val kind1059EventJsonString = Json.encodeToString(NostrEnvelope.serializer(), kind1059Event)

        println("[Nip17Helpers.kt] $kind1059Event")

        // --- Verification ---
        // We can still verify the event was signed correctly with the public key.
        val factoryEventIdBytes = kind1059Event.id.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val factoryEventSigBytes = kind1059Event.sig!!.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val isFactorySigValid = verifyHashSignatureSchnorr(
            factoryEventIdBytes,
            factoryEventSigBytes,
            randomPriv.xOnlyPublicKey()
        )
        println("[Nip17Helpers.kt] Is kind 1059 event signature valid? $isFactorySigValid")

        return kind1059Event
    }
}


