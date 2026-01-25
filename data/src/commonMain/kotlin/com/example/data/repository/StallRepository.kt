// TODO: Dependency Injection in Repositories: The ViewModels are already using Hilt for dependency injection. As the app grows, you could also have Hilt provide the repositories themselves, rather than creating them manually inside the Android...ViewModel files (e.g., ProductRepository(repository)). This is a minor point but can make testing and dependency management even cleaner in larger projects.
package com.example.data.repository

import com.example.data.uiModels.ShippingZoneUiModel
import com.example.data.uiModels.StallUiModel
import com.example.nostr.NostrRepository
import com.example.nostr.models.EventMessage
import com.example.nostr.models.EoseMessage
import com.example.nostr.models.Filter
import com.example.nostr.models.NostrEnvelope
import com.example.nostr.models.NoticeMessage
import com.example.nostr.models.StallContent
import com.example.nostr.models.AuthMessage
import com.example.nostr.models.ClosedMessage
import com.example.nostr.models.OkMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StallRepository(
    private val nostrRepository: NostrRepository,
) {
    private val json = Json { ignoreUnknownKeys = true }

    data class StallData(
        val stalls: List<NostrEnvelope>,
        val eoseHit: Boolean = false
    )

    private fun subscribeToStallsInternal(relayUrls: List<String>, customStallFilters: List<Filter>): Flow<StallData> {
        return flow {
            // 1. Emit an initial empty state immediately. This ensures the UI has something to display
            // and prevents it from getting stuck if no events are received.
            // emit(StallData(stalls = emptyList()))

            val stalls = mutableMapOf<String, NostrEnvelope>()
            val eoseReceivedFrom = mutableSetOf<String>()
            val deletionFilter = Filter(kinds = listOf(5))
            val allFilters = customStallFilters + deletionFilter

            nostrRepository.subscribeToEvents(relayUrls, allFilters)
                .collect { message ->
                    when (message) {
                        is EoseMessage -> {
                            eoseReceivedFrom.add(message.relayUrl)
                            println("[StallRepository] EOSE received from ${message.relayUrl}. Total EOSEs: ${eoseReceivedFrom.size}/${relayUrls.size}")
                            // Only emit eoseHit = true when all subscribed relays have responded.
                            if (eoseReceivedFrom.size >= relayUrls.size) {
                                emit(StallData(stalls = stalls.values.toList().sortedByDescending { it.created_at }, eoseHit = true))
                            }
                        }
                        is EventMessage -> {
                            val event = message.event
                            var listChanged = false
                            if (event.kind == 5) {
                                val eventIdToDelete = event.tags.find { it.size > 1 && it[0] == "e" }?.getOrNull(1)
                                if (eventIdToDelete != null) {
                                    val stallIdToRemove = stalls.entries.find { it.value.id == eventIdToDelete }?.key
                                    if (stallIdToRemove != null) {
                                        stalls.remove(stallIdToRemove)
                                        listChanged = true
                                    }
                                }
                            } else if (event.kind == 30017) {
                                val stallId = event.tags.find { it.size > 1 && it[0] == "d" }?.getOrNull(1)
                                if (stallId != null && mapToStallUiModel(event) != null) {
                                    val existingStall = stalls[stallId]
                                    if (existingStall == null || event.created_at > existingStall.created_at) {
                                        stalls[stallId] = event
                                        listChanged = true
                                    }
                                }
                            }

                            if (listChanged) {
                                val currentStalls = stalls.values.toList().sortedByDescending { it.created_at }
                                emit(StallData(stalls = currentStalls))
                            }
                        }
                        is NoticeMessage -> {
                            println("[StallRepository] Received notice from ${message.relayUrl}: ${message.message}")
                        }
                        is AuthMessage -> {
                            println("[StallRepository] Received auth from ${message.relayUrl}: ${message.challenge}")
                        }
                        is ClosedMessage -> {
                            println("[StallRepository] Received closed from ${message.relayUrl}: ${message.message}")
                        }
                        is OkMessage -> {
                            println("[StallRepository] Received ok from ${message.relayUrl}: ${message.message}")
                        }
                        else -> {
                            // This branch should not be reached if all message types are handled
                            println("[StallRepository] Received an unknown message type: $message")
                        }

                    }
                }
        }.debounce(500)
    }

    fun getStallsByAuthor(relayUrls: List<String>, authors: List<String>): Flow<List<StallUiModel>> {
        val filters = listOf(Filter(kinds = listOf(30017), authors = authors))
        return subscribeToStallsInternal(relayUrls, filters).map { stallData ->
            stallData.stalls.mapNotNull { event ->
                mapToStallUiModel(event)
            }
        }
    }

    /*
    fun getStallsByAuthor(relayUrls: List<String>, authors: List<String>): Flow<StallData> {
        val filters = listOf(Filter(kinds = listOf(30017), authors = authors))
        return subscribeToStallsInternal(relayUrls, filters)
    }
    */

//    fun getStallsByAuthor(relayUrls: List<String>, authors: List<String>, timeout: Long): Flow<NostrEnvelope> {
//        val filters = listOf(Filter(kinds = listOf(30017), authors = authors))
//        return nostrRepository.fetchEventsTillEose(relayUrls, filters, timeout)
//    }

    fun getStallByEventId(relayUrls: List<String>, eventId: String): Flow<StallUiModel?> {
        val filters = listOf(Filter(kinds = listOf(30017), ids = listOf(eventId)))
        return subscribeToStallsInternal(relayUrls, filters).map { stallData ->
            stallData.stalls.firstOrNull()?.let { mapToStallUiModel(it) }
        }
    }

    /*
    fun getStallByEventId(relayUrls: List<String>, eventId: String): Flow<NostrEnvelope?> {
        val filters = listOf(Filter(kinds = listOf(30017), ids = listOf(eventId)))
        return subscribeToStallsInternal(relayUrls, filters).map { it.stalls.firstOrNull() }
    }
    */

    fun getStallsByDTag(relayUrls: List<String>, dTag: String): Flow<StallData> {
        val filters = listOf(Filter(kinds = listOf(30017), d = listOf(dTag)))
        return subscribeToStallsInternal(relayUrls, filters)
    }

//    fun getStallsByDTag(relayUrls: List<String>, dTag: String, timeout: Long): Flow<NostrEnvelope> {
//        val filters = listOf(Filter(kinds = listOf(30017), d = listOf(dTag)))
//        return nostrRepository.fetchEventsTillEose(relayUrls, filters, timeout)
//    }

    fun getStallByStallId(relayUrls: List<String>, stallId: String, authorPubkey: String): Flow<StallUiModel?> {
        val filters = listOf(Filter(kinds = listOf(30017), authors = listOf(authorPubkey), d = listOf(stallId)))
        return subscribeToStallsInternal(relayUrls, filters).map { stallData ->
            stallData.stalls.firstOrNull()?.let { mapToStallUiModel(it) }
        }
    }

    /*
    fun getStallByStallId(relayUrls: List<String>, stallId: String, authorPubkey: String): Flow<StallData> {
        val filters = listOf(Filter(kinds = listOf(30017), authors = listOf(authorPubkey), d = listOf(stallId)))
        return subscribeToStallsInternal(relayUrls, filters)
    }
    */

    fun mapToStallUiModel(event: NostrEnvelope): StallUiModel? {
        // --- JSON validation logic ---
        val isValidJson = try {
            json.parseToJsonElement(event.content)
            true
        } catch (_: Exception) {
            println("StallRepository.kt - StallContent -  Invalid JSON: ${event.id}")
            false
        }
        if (!isValidJson) return null

        // --- StallContent parsing logic ---
        val stallContent = try {
            json.decodeFromString(StallContent.serializer(), event.content)
        } catch (_: Exception) {
            // Fallback for non-standard StallContent
            val fallbackElement = json.parseToJsonElement(event.content)
            val fallbackJson = fallbackElement as? JsonObject
            if (fallbackJson == null) {
                println("StallRepository.kt - StallContent - Fallback: Not a JsonObject - ${event.id}")
                return null
            }

            // --- Stricter Validation: Ensure required fields exist ---
            val stallId = fallbackJson["id"]?.let { it as? JsonPrimitive }?.contentOrNull
            if (stallId == null) {
                println("StallRepository.kt - StallContent - Fallback: Missing 'id' field in content - ${event.id}")
                return null
            }

            val name = fallbackJson["name"]?.let { it as? JsonPrimitive }?.contentOrNull
            if (name == null) {
                println("StallRepository.kt - StallContent - Fallback: Missing 'name' field in content - ${event.id}")
                return null
            }

            val description = fallbackJson["description"]?.let { it as? JsonPrimitive }?.contentOrNull
            val currency = fallbackJson["currency"]?.let { it as? JsonPrimitive }?.contentOrNull ?: ""

            StallContent(
                id = stallId,
                name = name,
                description = description,
                currency = currency,
                shipping = emptyList() // Fallback does not parse shipping zones
            )
        }

        val formattedCreatedAt = try {
            val date = Date(event.created_at * 1000)
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(date)
        } catch (_: Exception) {
            null
        }

        return StallUiModel(
            envelope = event,
            event_id = event.id,
            pubkey = event.pubkey,
            created_at = event.created_at,
            kind = event.kind,
            tags = event.tags,
            content = event.content,
            sig = event.sig ?: "",
            stall_id = stallContent.id,
            name = stallContent.name,
            description = stallContent.description,
            currency = stallContent.currency,
            shipping = stallContent.shipping.map { sz ->
                ShippingZoneUiModel(
                    sz.id,
                    sz.name,
                    sz.cost,
                    sz.regions ?: emptyList(),
                    sz.countries ?: emptyList()
                )
            },
            formattedCreatedAt = formattedCreatedAt
        )
    }
}
