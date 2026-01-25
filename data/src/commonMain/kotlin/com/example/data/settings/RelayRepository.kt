package com.example.data.settings

import kotlinx.coroutines.flow.Flow

/**
 * Shared interface for relay management, to be implemented per platform.
 */
interface RelayRepository {
    /**
     * Flow of relay URLs.
     */
    val relaysFlow: Flow<List<String>>

    /**
     * Set the list of relay URLs.
     */
    suspend fun setRelays(relays: List<String>)

    /**
     * Flow of relay connection timeout in milliseconds.
     */
    val relayTimeoutMsFlow: Flow<Long>

    /**
     * Set the relay connection timeout in milliseconds.
     */
    suspend fun setRelayTimeoutMs(timeoutMs: Long)
}

/**
 * Default relays for all platforms.
 */
val DEFAULT_RELAYS = listOf("wss://relay.damus.io",
    "wss://nos.lol",
    "wss://relay.snort.social",
    "wss://relay.nostr.info",
    "wss://brb.io",
    "wss://nostr.wine",
    "wss://eden.nostr.land",
    "wss://nostr.onsats.org")
