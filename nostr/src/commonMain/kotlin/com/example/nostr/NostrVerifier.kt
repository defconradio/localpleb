package com.example.nostr

import com.example.crypto.util.sha256
import com.example.nostr.models.NostrEnvelope
import com.example.nostr.util.EventTemplate
import com.example.crypto.verifyHashSignatureSchnorr

/**
 * Verifies the signature of a Nostr event.
 *
 * This extension function provides an easy and efficient way to validate an event,
 * mirroring the best practices seen in clients like Amethyst. It performs two critical checks:
 * 1.  It recalculates the event ID from its content to ensure the ID itself is valid (anti-tampering).
 * 2.  It verifies the Schnorr signature against the public key.
 *
 * @return `true` if the event ID is correct and the signature is valid, `false` otherwise.
 */
fun NostrEnvelope.verify(): Boolean {
    try {
        // 0. If the signature is null or empty, it cannot be verified.
        if (this.sig.isNullOrEmpty()) {
            return false
        }

        // 1. Re-create the event template from the envelope's data.
        val template = EventTemplate(
            kind = this.kind,
            tags = this.tags,
            content = this.content,
            createdAt = this.created_at
        )

        // 2. Re-calculate the canonical JSON and hash it to get the expected event ID.
        val canonicalJson = template.toCanonicalJson(this.pubkey)
        val calculatedIdBytes = sha256(canonicalJson.toByteArray())

        // 3. Get the ID from the event and compare. This prevents tampering.
        val receivedIdBytes = this.id.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        if (!calculatedIdBytes.contentEquals(receivedIdBytes)) {
            // If the provided ID doesn't match the hash of the content, it's an invalid event.
            return false
        }

        // 4. Convert the hex-encoded signature and public key into raw byte arrays.
        val sigBytes = this.sig.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val pubkeyBytes = this.pubkey.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

        // 5. Perform the Schnorr signature verification using the new helper.
        return verifyHashSignatureSchnorr(calculatedIdBytes, sigBytes, pubkeyBytes)
    } catch (e: Exception) {
        // If any error occurs during parsing or verification (e.g., invalid hex),
        // the event is considered invalid.
        return false
    }
}
