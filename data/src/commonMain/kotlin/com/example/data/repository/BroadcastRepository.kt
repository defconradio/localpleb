package com.example.data.repository

import com.example.nostr.NostrRepository
import com.example.nostr.models.NostrEnvelope

class BroadcastRepository(
    private val nostrRepository: NostrRepository
) {

    /**
     * Publishes a Nostr event to the specified relays.
     * This is a generic function to broadcast any kind of event.
     *
     * @param event The NostrEnvelope to be published.
     * @param relays The list of relay URLs to publish to.
     * @param timeout The timeout in milliseconds for each relay connection.
     * @return A map where keys are relay URLs and values are booleans indicating success.
     */
    suspend fun broadcastEvent(
        event: NostrEnvelope,
        relays: List<String>,
        timeout: Long
    ): Map<String, Boolean> {
        return nostrRepository.publishEventToRelays(relays, event, timeout)
    }
}

