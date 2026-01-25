package com.example.nostr.util

/**
 * A builder class for creating Nostr event tags in a type-safe way.
 * This follows the builder pattern seen in Amethyst.
 */
class Tags {
    private val tags = mutableListOf<List<String>>()

    fun t(value: String) {
        tags.add(listOf("t", value))
    }

    fun d(value: String) {
        tags.add(listOf("d", value))
    }

    fun name(value: String) {
        tags.add(listOf("name", value))
    }

    fun a(value: String) {
        tags.add(listOf("a", value))
    }

    fun e(eventId: String, relay: String? = null, marker: String? = null) {
        tags.add(listOfNotNull("e", eventId, relay, marker))
    }

    fun p(pubkey: String, relay: String? = null, petname: String? = null) {
        tags.add(listOfNotNull("p", pubkey, relay, petname))
    }

    fun r(url: String) {
        tags.add(listOf("r", url))
    }

    fun subject(subject: String) {
        tags.add(listOf("subject", subject))
    }

    fun raw(key: String, value: String) {
        tags.add(listOf(key, value))
    }

    fun k(kind: String) {
        tags.add(listOf("k", kind))
    }

    /**
     * Adds a NIP-13 proof-of-work nonce tag.
     *
     * @param counter The counter (nonce) found during mining.
     * @param difficulty The target difficulty.
     */
    fun nonce(counter: Long, difficulty: Int) {
        tags.add(listOf("nonce", counter.toString(), difficulty.toString()))
    }

    /**
     * Returns the final, immutable list of tags.
     */
    fun build(): List<List<String>> = tags
}