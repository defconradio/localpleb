package com.example.nostr

import com.example.crypto.util.sha256
import com.example.crypto.util.toHexKey
import com.example.nostr.models.NostrEnvelope
import com.example.nostr.util.EventTemplate
import com.example.nostr.util.Tags
import java.time.Instant

/**
 * A factory for creating Nostr events.
 * It decouples the event creation logic from the signing process.
 */
object EventFactory {

    /**
     * Creates a complete, signed Nostr event.
     *
     * @param pubkeyHex The public key of the event creator in hex format.
     * @param kind The kind of event.
     * @param content The content of the event.
     * @param tagsBuilder A lambda for building the event's tags.
     * @param signer A function that takes the event ID hash (ByteArray) and returns a signature (ByteArray).
     *               This allows the caller to handle signing without exposing private keys to the factory.
     * @param createdAt An optional timestamp for the event creation time. If not provided, defaults to the template's createdAt.
     * @return A signed NostrEnvelope.
     */
    fun createSignedEvent(
        pubkeyHex: String,
        kind: Int,
        content: String,
        tagsBuilder: (Tags.() -> Unit)? = null,
        signer: (ByteArray) -> ByteArray,
        createdAt: Long? = null
    ): NostrEnvelope {
        try {
            println("[EventFactory] ENTERED createSignedEvent")
            // 1. Create the event template
            val finalCreatedAt = createdAt ?: Instant.now().epochSecond
            val template = EventTemplate.create(
                kind = kind,
                content = content,
                createdAt = finalCreatedAt,
                tagsBuilder = tagsBuilder ?: {}
            )
            println("[EventFactory] pubkeyHex: $pubkeyHex kind: $kind content: $content tags: ${template.tags}")

            // 2. Canonicalize and hash event to get the event ID
            val canonicalJson = template.toCanonicalJson(pubkeyHex)
            println("[EventFactory] Canonical event array for signing: $canonicalJson")
            val eventId = sha256(canonicalJson.toByteArray())
            println("[EventFactory] Event ID (SHA-256, hex): ${eventId.toHexKey()}")

            // 3. Sign the event ID using the provided signer function
            println("[EventFactory] Calling signer lambda with eventId: ${eventId.toHexKey()}")
            val signature = signer(eventId)
            println("[EventFactory] Signature (hex): ${signature.toHexKey()}")

            // 4. Assemble and return the final NostrEnvelope
            return NostrEnvelope(
                id = eventId.toHexKey(),
                pubkey = pubkeyHex,
                created_at = template.createdAt,
                kind = template.kind,
                tags = template.tags,
                content = template.content,
                sig = signature.toHexKey()
            )
        } catch (e: Exception) {
            println("[EventFactory] ERROR: ${e.message}")
            throw e
        }
    }

    /**
     * Creates a Nostr event without a signature.
     *
     * @param pubkeyHex The public key of the event creator in hex format.
     * @param kind The kind of event.
     * @param content The content of the event.
     * @param tagsBuilder A lambda for building the event's tags.
     * @param createdAt An optional timestamp for the event creation time. If not provided, defaults to the template's createdAt.
     * @return An unsigned NostrEnvelope.
     */
    fun createUnsignedEvent(
        pubkeyHex: String,
        kind: Int,
        content: String,
        tagsBuilder: (Tags.() -> Unit)? = null,
        createdAt: Long? = null
    ): NostrEnvelope {
        try {
            println("[EventFactory] ENTERED createUnsignedEvent")
            // 1. Create the event template
            val finalCreatedAt = createdAt ?: Instant.now().epochSecond
            val template = EventTemplate.create(
                kind = kind,
                content = content,
                createdAt = finalCreatedAt,
                tagsBuilder = tagsBuilder ?: {}
            )
            println("[EventFactory] pubkeyHex: $pubkeyHex kind: $kind content: $content tags: ${template.tags}")

            // 2. Canonicalize and hash event to get the event ID
            val canonicalJson = template.toCanonicalJson(pubkeyHex)
            println("[EventFactory] Canonical event array for signing: $canonicalJson")
            val eventId = sha256(canonicalJson.toByteArray())
            println("[EventFactory] Event ID (SHA-256, hex): ${eventId.toHexKey()}")

            // 3. Assemble and return the final NostrEnvelope without a signature
            return NostrEnvelope(
                id = eventId.toHexKey(),
                pubkey = pubkeyHex,
                created_at = template.createdAt,
                kind = template.kind,
                tags = template.tags,
                content = template.content,
                sig = "" // Signature is empty for an unsigned event
            )
        } catch (e: Exception) {
            println("[EventFactory] ERROR: ${e.message}")
            throw e
        }
    }
}
