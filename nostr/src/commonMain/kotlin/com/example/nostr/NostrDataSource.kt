package com.example.nostr

import com.example.nostr.models.Filter
import com.example.nostr.models.NostrEnvelope
import com.example.nostr.models.RelayMessage
import com.example.nostr.models.EventMessage
import com.example.nostr.models.EoseMessage
import com.example.nostr.models.NoticeMessage
import com.example.nostr.models.OkMessage
import com.example.nostr.models.AuthMessage
import com.example.nostr.models.ClosedMessage
import com.example.nostr.models.NostrEnvelope.Companion.serializer
import com.example.nostr.models.PublicationResult
import com.example.nostr.models.PublicationStatus
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.serializer

class NostrDataSource(private val client: HttpClient) {
    suspend fun publishEventToRelaysWithStatus(
        relayUrls: List<String>,
        event: NostrEnvelope,
        timeoutMs: Long = 8000,
        useTestSimulation: Boolean = false // Added flag to enable/disable test simulation
    ): Map<String, PublicationResult> {
        println("[NostrDataSource.kt] publishEventToRelays called with relays: $relayUrls and event id: ${event.id}")
        val json = Json { ignoreUnknownKeys = true }
        val eventJson = json.encodeToString(NostrEnvelope.serializer(), event)
        val results = mutableMapOf<String, PublicationResult>()

        for (relayUrl in relayUrls) {
            var result: PublicationResult? = null
            try {
                println("[NostrDataSource.kt] Connecting to relay: $relayUrl for event publish with timeout: $timeoutMs ms")
                withTimeout(timeoutMs) {
                    if (useTestSimulation) {
                        // Simulate relay response for testing:
                        val simulateOk = true // set to false to simulate error
                        if (simulateOk) {
                            println("[NostrDataSource.kt] Simulated: Received OK from $relayUrl, event accepted.")
                            result = PublicationResult(PublicationStatus.SUCCESS, "Simulated OK")
                        } else {
                            throw Exception("Simulated relay error for $relayUrl: TEST ERROR")
                        }
                    } else {
                        client.webSocket(urlString = relayUrl) {
                            println("[NostrDataSource.kt] WebSocket opened: $relayUrl")
                            val msg = """["EVENT",$eventJson]"""
                            send(msg)
                            println("[NostrDataSource.kt] Sent EVENT message to $relayUrl: $msg")

                            for (frame in incoming) {
                                if (frame is Frame.Text) {
                                    val text = frame.readText()
                                    println("[NostrDataSource.kt] Received frame from $relayUrl: $text")
                                    val arr = try {
                                        json.parseToJsonElement(text) as? kotlinx.serialization.json.JsonArray
                                    } catch (e: Exception) {
                                        println("[NostrDataSource.kt] Malformed JSON from $relayUrl: $text. Error: ${e.message}")
                                        continue
                                    }

                                    if (arr != null && arr.isNotEmpty()) {
                                        when (val type = arr[0].jsonPrimitive.content) {
                                            "OK" -> {
                                                val eventId = arr.getOrNull(1)?.jsonPrimitive?.content ?: ""
                                                val wasSaved = arr.getOrNull(2)?.jsonPrimitive?.booleanOrNull ?: false
                                                val message = arr.getOrNull(3)?.jsonPrimitive?.content ?: ""
                                                println("[NostrDataSource.kt] $arr")

                                                println("[NostrDataSource.kt] Received OK from $relayUrl for event $eventId. Saved: $wasSaved. Message: $message")
                                                if (eventId == event.id) {
                                                    if (wasSaved) {
                                                        result = PublicationResult(PublicationStatus.SUCCESS, message.ifEmpty { "Event saved successfully" })
                                                    } else {
                                                        // NIP-20 command results
                                                        when {
                                                            message.startsWith("duplicate:") -> result = PublicationResult(PublicationStatus.DUPLICATE, message)
                                                            message.startsWith("pow:") -> result = PublicationResult(PublicationStatus.POW, message)
                                                            message.startsWith("rate-limited:") -> result = PublicationResult(PublicationStatus.RATE_LIMITED, message)
                                                            message.startsWith("invalid:") -> result = PublicationResult(PublicationStatus.INVALID, message)
                                                            message.startsWith("error:") -> result = PublicationResult(PublicationStatus.ERROR, message)
                                                            else -> result = PublicationResult(PublicationStatus.FAILED, message.ifEmpty { "Event not saved by relay." })
                                                        }
                                                    }
                                                    break
                                                }
                                            }
                                            "NOTICE" -> {
                                                val message = arr.getOrNull(1)?.jsonPrimitive?.content ?: ""
                                                println("[NostrDataSource.kt] Received NOTICE from $relayUrl: $message")
                                                // NOTICE is informational. We'll store it, but keep listening for a final OK/AUTH.
                                                // If we time out, this will be the result we use.
                                                result = PublicationResult(PublicationStatus.NOTICE, message)
                                            }
                                            "AUTH" -> {
                                                val challenge = arr.getOrNull(1)?.jsonPrimitive?.content ?: ""
                                                println("[NostrDataSource.kt] Received AUTH request from $relayUrl with challenge: $challenge. Publication failed.")
                                                result = PublicationResult(PublicationStatus.AUTH_REQUIRED, challenge)
                                                break // AUTH required, cannot publish
                                            }
                                            else -> {
                                                println("[NostrDataSource.kt] Received unhandled message type '$type' from $relayUrl: $text")
                                            }
                                        }
                                    }
                                } else {
                                    println("[NostrDataSource.kt] Received non-text frame from $relayUrl.")
                                }
                            }
                            println("[NostrDataSource.kt] Closing WebSocket after processing messages or timeout: $relayUrl")
                        }
                    }
                }
            } catch (e: Exception) {
                println("[NostrDataSource.kt] Error publishing to $relayUrl: ${e.message}")
                result = PublicationResult(PublicationStatus.FAILED, e.message)
            }
            results[relayUrl] = result ?: PublicationResult(PublicationStatus.FAILED, "No response from relay")
        }
        println("[NostrDataSource.kt] publishEventToRelays results: $results")
        return results
    }

    suspend fun publishEventToRelays(
        relayUrls: List<String>,
        event: NostrEnvelope,
        timeoutMs: Long = 8000,
        useTestSimulation: Boolean = false // Added flag to enable/disable test simulation
    ): Map<String, Boolean> {
        println("[NostrDataSource.kt] publishEventToRelays called with relays: $relayUrls and event id: ${event.id}")
        val json = Json { ignoreUnknownKeys = true }
        val results = mutableMapOf<String, Boolean>()

        for (relayUrl in relayUrls) {
            var published = false
            try {
                println("[NostrDataSource.kt] Connecting to relay: $relayUrl for event publish with timeout: $timeoutMs ms")
                withTimeout(timeoutMs) {
                    if (useTestSimulation) {
                        // Simulate relay response for testing:
                        val simulateOk = true // set to false to simulate error
                        if (simulateOk) {
                            println("[NostrDataSource.kt] Simulated: Received OK from $relayUrl, event accepted.")
                            published = true
                        } else {
                            throw Exception("Simulated relay error for $relayUrl: TEST ERROR")
                        }
                    } else {
                        client.webSocket(urlString = relayUrl) {
                            println("[NostrDataSource.kt] WebSocket opened: $relayUrl")
                            
                            val eventJson = json.encodeToString(NostrEnvelope.serializer(), event)
                            val msg = """["EVENT",$eventJson]"""
                            send(msg)
                            println("[NostrDataSource.kt] Sent EVENT message to $relayUrl: $msg")

                            for (frame in incoming) {
                                if (frame is Frame.Text) {
                                    val text = frame.readText()
                                    println("[NostrDataSource.kt] Received frame from $relayUrl: $text")
                                    val arr = try {
                                        json.parseToJsonElement(text) as? kotlinx.serialization.json.JsonArray
                                    } catch (e: Exception) {
                                        println("[NostrDataSource.kt] Malformed JSON from $relayUrl: $text. Error: ${e.message}")
                                        continue
                                    }

                                    if (arr != null && arr.isNotEmpty()) {
                                        when (val type = arr[0].jsonPrimitive.content) {
                                            "OK" -> {
                                                val eventId = arr.getOrNull(1)?.jsonPrimitive?.content ?: ""
                                                val wasSaved = arr.getOrNull(2)?.jsonPrimitive?.booleanOrNull ?: false
                                                val message = arr.getOrNull(3)?.jsonPrimitive?.content ?: ""
                                                println("[NostrDataSource.kt] Received OK from $relayUrl for event $eventId. Saved: $wasSaved. Message: $message")
                                                
                                                if (eventId == event.id && wasSaved) {
                                                    published = true
                                                }
                                                // In any OK case, we consider the interaction for this event done.
                                                break 
                                            }
                                            "NOTICE" -> {
                                                val message = arr.getOrNull(1)?.jsonPrimitive?.content ?: ""
                                                println("[NostrDataSource.kt] Received NOTICE from $relayUrl: $message")
                                                // NOTICE can be informational, continue listening for OK
                                            }
                                            "AUTH" -> {
                                                val challenge = arr.getOrNull(1)?.jsonPrimitive?.content ?: ""
                                                println("[NostrDataSource.kt] Received AUTH request from $relayUrl with challenge: $challenge. Publication failed.")
                                                published = false
                                                break // AUTH required, cannot publish
                                            }
                                            else -> {
                                                println("[NostrDataSource.kt] Received unhandled message type '$type' from $relayUrl: $text")
                                            }
                                        }
                                    }
                                } else {
                                    println("[NostrDataSource.kt] Received non-text frame from $relayUrl.")
                                }
                            }
                            println("[NostrDataSource.kt] Closing WebSocket after processing messages or timeout: $relayUrl")
                        }
                    }
                }
            } catch (e: Exception) {
                println("[NostrDataSource.kt] Error publishing to $relayUrl: ${e.message}")
                published = false
            }
            results[relayUrl] = published
        }
        println("[NostrDataSource.kt] publishEventToRelays results: $results")
        return results
    }

    /**
     * Subscribes to a set of relays with given filters and returns a Flow of events.
     * This connection is persistent and will attempt to reconnect on failure.
     * It does not close after EOSE, making it suitable for real-time updates like a chat feed.
     */
    //TODO diggest handle auth notice and close ? 
    // 23:23:09.362  I  [NostrDataSource.kt][subscribeToEvents] Sent subscription request to wss://relay.damus.io: ["REQ","sub-1756092189361-1606608262",{"kinds":[14,1059],"#p":["fbc4d9d8e28c7fdb93eb6dab1a0b10e83576f262883c6d476c7e388a459780a4"]},{"kinds":[5]}]
    // 23:23:09.406  I  [NostrDataSource.kt][subscribeToEvents] Sent subscription request to wss://nostr.wine: ["REQ","sub-1756092189404-558314954",{"kinds":[14,1059],"#p":["fbc4d9d8e28c7fdb93eb6dab1a0b10e83576f262883c6d476c7e388a459780a4"]},{"kinds":[5]}]
    // 23:23:09.539  I  [NostrDataSource.kt][subscribeToEvents] Received unhandled message type 'AUTH' from wss://nostr.wine: ["AUTH", "39704d45-7d60-4e2f-9c90-36615a4aac4a"]
    // 23:23:09.543  I  [NostrDataSource.kt][subscribeToEvents] Received unhandled message type 'CLOSED' from wss://nostr.wine: ["CLOSED", "sub-1756092189404-558314954", "auth-required: this relay only serves private notes to authenticated authors or recipients"]
    // 23:23:09.557  I  [NostrDataSource.kt][subscribeToEvents] Sent subscription request to wss://nos.lol: ["REQ","sub-1756092189556-1488935338",{"kinds":[14,1059],"#p":["fbc4d9d8e28c7fdb93eb6dab1a0b10e83576f262883c6d476c7e388a459780a4"]},{"kinds":[5]}]
    // 23:23:09.937  I  [NostrDataSource.kt][subscribeToEvents] Received EOSE from wss://relay.damus.io for subId sub-1756092189361-1606608262.
    // 23:23:10.665  I  [NostrDataSource.kt][subscribeToEvents] Received EOSE from wss://nos.lol for subId sub-1756092189556-1488935338.

    suspend fun subscribeToEvents(
         relayUrls: List<String>,
        filters: List<Filter>,
        useTestSimulation: Boolean = false
    ): Flow<RelayMessage> = callbackFlow {
        if (useTestSimulation) {
            //println("[NostrDataSource.kt][subscribeToEvents] Test simulation is enabled. No real relays will be contacted.")
            // Keep the flow open but do nothing, effectively disabling the subscription.
            awaitClose { }
            return@callbackFlow
        }

        //println("[NostrDataSource.kt][subscribeToEvents] Initializing persistent subscription for ${relayUrls.size} relays.")
        val json = Json { ignoreUnknownKeys = true }
        val activeWebSockets = mutableMapOf<String, WebSocketSession>()
        val subscriptions = mutableMapOf<String, String>()
        val mutex = Mutex()
        val seenEventIds = mutableSetOf<String>() // Set to track seen event IDs for deduplication
        //val eventCounter = atomic(0)

        if (relayUrls.isEmpty()) {
            //println("[NostrDataSource.kt][subscribeToEvents] No relay URLs provided. Closing flow immediately.")
            close()
            return@callbackFlow
        }

        relayUrls.forEach { relayUrl ->
            launch(Dispatchers.IO) {
                var attempt = 0
                while (true) { // Loop for reconnection
                    try {
                        //println("[NostrDataSource.kt][subscribeToEvents] Connecting to $relayUrl (Attempt ${attempt + 1})")
                        client.webSocket(urlString = relayUrl) {
                            //println("[NostrDataSource.kt][subscribeToEvents] WebSocket connection established with $relayUrl.")
                            mutex.withLock {
                                activeWebSockets[relayUrl] = this
                            }
                            val subId = "sub-${System.currentTimeMillis()}-${relayUrl.hashCode()}"
                            subscriptions[relayUrl] = subId
                            val reqJson = """["REQ","$subId",${filters.joinToString(",") { json.encodeToString(Filter.serializer(), it) }}]"""
                            send(reqJson)
                            println("[NostrDataSource.kt][subscribeToEvents] Sent subscription request to $relayUrl: $reqJson")

                            // Reset attempt count on successful connection
                            attempt = 0

                            for (frame in incoming) {
                                if (frame is Frame.Text) {
                                    val text = frame.readText()
                                    //println("[NostrDataSource.kt][subscribeToEvents] RAW JSON from $relayUrl: $text")
                                    val arr = try {
                                        json.parseToJsonElement(text) as? kotlinx.serialization.json.JsonArray
                                    } catch (e: Exception) {
                                        println("[NostrDataSource.kt][subscribeToEvents] Malformed JSON from $relayUrl: $text. Error: ${e.message}")
                                        continue
                                    }

                                    if (arr != null && arr.isNotEmpty()) {
                                        when (val type = arr[0].jsonPrimitive.content) {
                                            "EVENT" -> {
                                                //val currentCounter = eventCounter.incrementAndGet()
                                                //println("[NostrDataSource.kt][subscribeToEvents] Event counter: $currentCounter")
                                                if (arr.size > 2) {
                                                    //println("[NostrDataSource.kt][subscribeToEvents] Parsing EVENT from $relayUrl.")
                                                    val event = json.decodeFromJsonElement(NostrEnvelope.serializer(), arr[2].jsonObject).copy(relayUrl = relayUrl)
                                                    var isNewEvent = false
                                                    mutex.withLock {
                                                        isNewEvent = seenEventIds.add(event.id)
                                                    }
                                                    // Deduplicate events based on their ID
                                                    if (isNewEvent) {
                                                        //println("[NostrDataSource.kt][subscribeToEvents] Sending event ${event.id} from $relayUrl to flow.")
                                                        trySend(EventMessage(event, relayUrl))
                                                    } else {
                                                        //println("[NostrDataSource.kt][subscribeToEvents] Ignoring duplicate event ${event.id} from $relayUrl.")
                                                    }
                                                } else {
                                                    println("[NostrDataSource.kt][subscribeToEvents] Received invalid EVENT format from $relayUrl: $text")
                                                }
                                            }
                                            "EOSE" -> {
                                                val subIdFromServer = if (arr.size > 1) arr[1].jsonPrimitive.content else ""
                                                println("[NostrDataSource.kt][subscribeToEvents] Received EOSE from $relayUrl for subId $subIdFromServer.")
                                                trySend(EoseMessage(relayUrl, subIdFromServer))
                                            }
                                            "NOTICE" -> {
                                                if (arr.size > 1) {
                                                    val message = arr[1].jsonPrimitive.content
                                                    println("[NostrDataSource.kt][subscribeToEvents] Received NOTICE from $relayUrl: $message")
                                                    //trySend(NoticeMessage(relayUrl, message))
                                                    trySend(EoseMessage(relayUrl, message))

                                                }
                                            }
                                            "OK" -> {
                                                if (arr.size >= 4) {
                                                    val eventId = arr[1].jsonPrimitive.content
                                                    val saved = arr[2].jsonPrimitive.booleanOrNull ?: false
                                                    val message = arr[3].jsonPrimitive.content
                                                    val okMessage = OkMessage(relayUrl, eventId, saved, message)
                                                    println("[NostrDataSource.kt][subscribeToEvents] Received OK from $relayUrl: $okMessage")
                                                    trySend(okMessage)
                                                    //trySend(EoseMessage(relayUrl, message))

                                                } else {
                                                    println("[NostrDataSource.kt][subscribeToEvents] Received malformed OK message from $relayUrl: $text")
                                                }
                                            }
                                            "AUTH" -> {
                                                val challenge = if (arr.size > 1) arr[1].jsonPrimitive.content else ""
                                                println("[NostrDataSource.kt][subscribeToEvents] Received AUTH request from $relayUrl with challenge: $challenge.")
                                                trySend(AuthMessage(relayUrl, challenge) )
                                                //trySend(EoseMessage(relayUrl, challenge))

                                            }
                                            "CLOSED" -> {
                                                val subIdFromServer = if (arr.size > 1) arr[1].jsonPrimitive.content else ""
                                                val reason = if (arr.size > 2) arr[2].jsonPrimitive.content else ""
                                                println("[NostrDataSource.kt][subscribeToEvents] Received CLOSED from $relayUrl for subId $subIdFromServer. Reason: $reason.")
                                                trySend(ClosedMessage(relayUrl, subIdFromServer, reason))
                                                //trySend(EoseMessage(relayUrl, subIdFromServer))

                                            }
                                            else -> {
                                                println("[NostrDataSource.kt][subscribeToEvents] Received unhandled message type '$type' from $relayUrl: $text")
                                            }
                                        }
                                    }
                                } else if (frame is Frame.Close) {
                                    println("[NostrDataSource.kt][subscribeToEvents] Received Close frame from $relayUrl: ${frame.readReason()}")
                                }
                            }
                            //println("[NostrDataSource.kt][subscribeToEvents] Frame loop for $relayUrl exited.")
                        }
                    } catch (e: Exception) {
                        println("[NostrDataSource.kt][subscribeToEvents] Error with relay $relayUrl: ${e.message}")
                    } finally {
                        mutex.withLock {
                            activeWebSockets.remove(relayUrl)
                        }
                        // Reconnection logic
                        val delayMs = (5000 * (1 shl attempt.coerceAtMost(4))).toLong() // Exponential backoff
                        println("[NostrDataSource.kt][subscribeToEvents] Disconnected from $relayUrl. Reconnecting in ${delayMs}ms...")
                        kotlinx.coroutines.delay(delayMs)
                        attempt++
                    }
                }

            }
        }

        awaitClose {
            //println("[NostrDataSource.kt][subscribeToEvents] Flow closing. Cleaning up all active subscriptions...")
            launch(Dispatchers.IO) {
                mutex.withLock {
                    activeWebSockets.forEach { (relayUrl, session) ->
                        subscriptions[relayUrl]?.let { subId ->
                            launch {
                                try {
                                    //println("[NostrDataSource.kt][subscribeToEvents] Sending CLOSE to $relayUrl for subId $subId")
                                    session.send("""["CLOSE","$subId"]""")
                                    session.close(CloseReason(CloseReason.Codes.NORMAL, "Client closing flow"))
                                    //println("[NostrDataSource.kt][subscribeToEvents] Successfully closed connection to $relayUrl")
                                } catch (e: Exception) {
                                    println("[NostrDataSource.kt][subscribeToEvents] Error closing connection to $relayUrl: ${e.message}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    //DONT REMOVE USED BY TEST SUITE, AN ADULT DRAGON WILL BURN YOU TO ASHES
    fun fetchEventsTillEose(
        relayUrls: List<String>,
        filters: List<Filter>,
        timeoutMs: Long = 5000,
        frameTimeoutMs: Long? = 5000
    ): Flow<NostrEnvelope> = callbackFlow {
        val json = Json { ignoreUnknownKeys = true }
        val activeWebSockets = mutableListOf<WebSocketSession>()
        val seenIds = mutableSetOf<String>()
        val activeJobs = atomic(relayUrls.size)

        if (relayUrls.isEmpty()) {
            close()
            return@callbackFlow
        }

        relayUrls.forEach { relayUrl ->
            launch(Dispatchers.IO) {
                try {
                    val cappedTimeoutMs = minOf(timeoutMs, 15000L)
                    println("[NostrDataSource.kt] Subscribing to relay: $relayUrl with timeout: $cappedTimeoutMs ms")

                    withTimeoutOrNull(cappedTimeoutMs) {
                        try {
                            client.webSocket(urlString = relayUrl) {
                                activeWebSockets.add(this)
                                val subId = "sub-${System.currentTimeMillis()}"
                                val reqJson = """["REQ","$subId",${filters.joinToString(",") { json.encodeToString(Filter.serializer(), it) }}]"""
                                send(reqJson)
                                println("[NostrDataSource.kt] Sent subscription request to $relayUrl: $reqJson")

                                frameLoop@ while (true) {
                                    val frame = if (frameTimeoutMs != null) {
                                        withTimeoutOrNull(frameTimeoutMs) { incoming.receive() }
                                    } else {
                                        incoming.receive()
                                    }

                                    if (frame == null) {
                                        println("[NostrDataSource.kt] Frame timeout reached, closing connection to $relayUrl")
                                        break@frameLoop
                                    }

                                    if (frame is Frame.Text) {
                                        val text = frame.readText()
                                        // LOG RAW JSON DATA (add file name for clarity)
                                        println("[NostrDataSource.kt] fetchEventsTillEose RAW JSON from $relayUrl: $text")
                                        val arr = try {
                                            json.parseToJsonElement(text) as? kotlinx.serialization.json.JsonArray
                                        } catch (e: Exception) {
                                            println("[NostrDataSource.kt] Malformed JSON frame from $relayUrl: $text")
                                            continue@frameLoop
                                        }

                                        if (arr != null && arr.isNotEmpty()) {
                                            val type = arr[0].jsonPrimitive.content
                                            if (type == "EVENT") {
                                                val event = json.decodeFromJsonElement(NostrEnvelope.serializer(), arr[2].jsonObject).copy(relayUrl = relayUrl)
                                                if (seenIds.add(event.id)) {
                                                    trySend(event)
                                                }
                                            } else if (type == "EOSE") {
                                                println("[NostrDataSource.kt] Received EOSE from $relayUrl, closing subscription.")
                                                break@frameLoop
                                            }
                                        }
                                    }
                                }
                                // After loop, send CLOSE and close the socket
                                try {
                                    val closeMsg = """["CLOSE","$subId"]"""
                                    send(closeMsg)
                                    println("[NostrDataSource.kt] Sent CLOSE message to $relayUrl: $closeMsg")
                                } catch (e: Exception) {
                                    println("[NostrDataSource.kt] Error sending CLOSE message to $relayUrl: ${e.message}")
                                }
                            }
                        } catch (e: Exception) {
                            println("[NostrDataSource.kt] Error with relay $relayUrl: ${e.message}")
                            // close(e) // Optionally close the whole flow on error
                        }
                    } ?: println("[NostrDataSource.kt] Timeout connecting to $relayUrl")
                } finally {
                    if (activeJobs.decrementAndGet() == 0) {
                        println("[NostrDataSource.kt] All relays finished, closing flow.")
                        close()
                    }
                }
            }
        }

        awaitClose {
            println("[NostrDataSource.kt] Flow closing. Cleaning up active WebSockets.")
            activeWebSockets.forEach {
                launch {
                    try {
                        it.close()
                    } catch (e: Exception) {
                        // Ignore exceptions on close
                    }
                }
            }
        }
    }
    fun close() {
        client.close()
    }
}