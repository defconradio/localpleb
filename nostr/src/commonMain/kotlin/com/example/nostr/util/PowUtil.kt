package com.example.nostr.util

import com.example.crypto.util.SecureRNG
import com.example.crypto.util.sha256
import com.example.crypto.util.toHexKey
import com.example.nostr.models.NostrEnvelope
import kotlinx.coroutines.*

object PowUtil {
    /**
     * Mines a proof-of-work nonce for a NostrEnvelope in parallel, modifying it until the difficulty is met.
     * This function is suspendable and should be called from a coroutine.
     *
     * @param event The event to mine.
     * @param difficulty The target number of leading zero bits for the event ID.
     * @param numCores The number of parallel coroutines to use for mining. Defaults to 4.
     * @return The same NostrEnvelope instance, now updated with a valid "nonce" tag and a null signature.
     */
    suspend fun minePow(
        event: NostrEnvelope,
        difficulty: Int,
        numCores: Int = 4 // A reasonable default for multi-core devices.
    ): NostrEnvelope {
        // Use withContext to ensure this heavy computation runs on a background thread pool.
        return withContext(Dispatchers.Default) {
            val result = CompletableDeferred<NostrEnvelope>()
            val jobs = mutableListOf<Job>()

            // Launch multiple worker coroutines.
            // The first one to find a result will complete the deferred object and cancel the others.
            repeat(numCores) {
                jobs += launch {
                    // Each coroutine gets its own random starting point to minimize collision
                    var counter = SecureRNG.nextLong()
                    while (isActive) { // Loop will be broken by cancellation from outside
                        val newEvent = createEventWithNonce(event, counter, difficulty)
                        if (checkDifficulty(newEvent.id, difficulty)) {
                            // Try to complete the result. If another coroutine already has, this will return false.
                            if (result.complete(newEvent)) {
                                // If we were the first to complete it, cancel all other jobs.
                                jobs.forEach { it.cancel() }
                            }
                        }
                        counter++
                    }
                }
            }

            // Await and return the result from the first coroutine that finishes.
            result.await()
        }
    }

    /**
     * Helper function to create a new event with an updated nonce tag and a recalculated ID.
     * The signature is explicitly nulled out as it is no longer valid.
     */
    private fun createEventWithNonce(
        baseEvent: NostrEnvelope,
        nonce: Long,
        difficulty: Int
    ): NostrEnvelope {
        // Remove any existing nonce tag to avoid duplicates
        val originalTags = baseEvent.tags.filter { it.firstOrNull() != "nonce" }
        val nonceTag = listOf("nonce", nonce.toString(), difficulty.toString())
        val newTags = originalTags + listOf(nonceTag)

        // Create an EventTemplate to generate the canonical form
        val template = EventTemplate(
            kind = baseEvent.kind,
            tags = newTags,
            content = baseEvent.content,
            createdAt = baseEvent.created_at
        )

        // Canonicalize and hash to get the event ID
        val canonicalJson = template.toCanonicalJson(baseEvent.pubkey)
        val idBytes = sha256(canonicalJson.toByteArray())
        val idHex = idBytes.toHexKey()

        return baseEvent.copy(
            id = idHex,
            tags = newTags,
            sig = null // Signature must be recalculated after mining.
        )
    }

    private fun checkDifficulty(id: String, difficulty: Int): Boolean {
        var leadingZeros = 0
        for (c in id) {
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
        return leadingZeros >= difficulty
    }
}

