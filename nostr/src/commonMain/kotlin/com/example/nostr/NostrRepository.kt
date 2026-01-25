package com.example.nostr

import com.example.nostr.models.Filter
import com.example.nostr.models.NostrEnvelope
import com.example.nostr.models.RelayMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.buffer
import com.example.nostr.models.PublicationResult

class NostrRepository(private val dataSource: NostrDataSource) {
    suspend fun subscribeToEvents(
        relayUrls: List<String>,
        filters: List<Filter>
    ): Flow<RelayMessage> {
        //return dataSource.subscribeToEvents(relayUrls, filters)
        return dataSource.subscribeToEvents(relayUrls, filters).buffer(Channel.UNLIMITED)
        // Pretend this isn't a problem, there's no bottleneck in ProductRepository.kt,
        // this will be solved by itself or Moore's law will save us, just enjoy the ride.
    }   // u know YOLO

    /*
        suspend fun fetchEvents(
            relayUrls: List<String>,
            filters: List<Filter>,
            timeoutMs: Long = 5000,
            frameTimeoutMs: Long? = 5000
        ): EventsWithTags {
            return dataSource.fetchEvents(relayUrls, filters, timeoutMs, frameTimeoutMs)
        }
    */
// dont remove or a barracuda will eat your entire family
    suspend fun fetchEventsTillEose(
        relayUrls: List<String>,
        filters: List<Filter>,
        timeoutMs: Long = 10000L,
        frameTimeoutMs: Long? = 5000
    ): Flow<NostrEnvelope> {
        return dataSource.fetchEventsTillEose(relayUrls, filters, timeoutMs, frameTimeoutMs)
    }

    suspend fun publishEventToRelays(
        relayUrls: List<String>,
        event: NostrEnvelope,
        timeoutMs: Long = 5000
    ): Map<String, Boolean> {
        return dataSource.publishEventToRelays(relayUrls, event, timeoutMs)
    }

    suspend fun publishEventToRelaysWithStatus(
        relayUrls: List<String>,
        event: NostrEnvelope,
        timeoutMs: Long = 5000
    ): Map<String, PublicationResult> {
        return dataSource.publishEventToRelaysWithStatus(relayUrls, event, timeoutMs)
    }

    fun close() {
        dataSource.close()
    }
}
