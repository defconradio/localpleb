package com.example.data.repository
import com.example.data.settings.AccountRepository
import com.example.data.settings.RelayRepository
import com.example.data.uiModels.OrderUiModel
import com.example.data.util.Nip17Helpers
import com.example.nostr.EventFactory
import com.example.nostr.NostrRepository
import com.example.nostr.models.EventMessage
import com.example.nostr.models.EoseMessage
import com.example.nostr.models.Filter
import com.example.nostr.models.NostrEnvelope
import com.example.nostr.models.NoticeMessage
import com.example.nostr.models.AuthMessage
import com.example.nostr.models.ClosedMessage
import com.example.nostr.models.OkMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderRepository(
    private val nostrRepository: NostrRepository,
    private val relayRepository: RelayRepository,
    private val accountRepository: AccountRepository
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val pubkeyHex = accountRepository.getPublicAccountInfo()?.pubKeyHex ?: error("No pubkey")
    private val signer = accountRepository.getSigningLambda() ?: error("No signing key")
    //private val (randomPrivateKey, randomPublicKey) = accountRepository.generateNewKeyPair()
    //example random key
    data class OrderData(
        val orders: List<NostrEnvelope>
    )
    fun getConversations(): Flow<List<OrderUiModel>> {
        //this agroup all chats by participants only
        // use subject and or conversation id
        val filters = listOf(Filter(kinds = listOf(1059), p = listOf(pubkeyHex)))
        return relayRepository.relaysFlow.flatMapLatest { relayUrls ->
            subscribeToMessagesInternal(relayUrls, filters)
                .map { orderData ->
                    orderData.orders
                        .groupBy { event ->
                            val participants = event.tags.filter { it.size > 1 && it[0] == "p" }.map { it[1] }
                            val subject = event.tags.find { it.size > 1 && it[0] == "subject" }?.getOrNull(1) ?: ""
                            ((participants + event.pubkey).distinct().sorted().joinToString(",")) + ":$subject"
                        }
                        .mapNotNull { (_, eventsInGroup) ->
                            eventsInGroup.maxByOrNull { it.created_at }?.let { mapToOrderUiModel(it) }
                        }
                        .sortedByDescending { it.created_at }
                }
        }.flowOn(Dispatchers.IO)
    }

    fun getConversation(participants: List<String>, conversationId: String): Flow<List<OrderUiModel>> {
        return relayRepository.relaysFlow.flatMapLatest { relayUrls ->
            println("[OrderRepository.kt] getConversation participants=$participants conversationId=$conversationId")
            val targetParticipants = participants.distinct()
            val filters = listOf(
                Filter(kinds = listOf(1059), p = listOf(pubkeyHex))
            )
            subscribeToMessagesInternal(relayUrls, filters)
                .map { orderData ->
                    val conversationMessages = orderData.orders
                        .filter { event ->
                            val eventParticipants = (event.tags.filter { it.size > 1 && it[0] == "p" }.map { it[1] } + event.pubkey).distinct()
                            val match = targetParticipants.all { it in eventParticipants } && eventParticipants.size == targetParticipants.size
                            val subjectTag = event.tags.find { it.size > 1 && it[0] == "subject" }?.get(1)
                            val subjectMatches = subjectTag == conversationId
                            if (!match) println("[OrderRepository.kt] discard event from participants ${eventParticipants.joinToString()}}")
                            if (!subjectMatches) println("[OrderRepository.kt] discard event from subject tag: $subjectTag != $conversationId")
                            match && subjectMatches
                        }
                        .map { event ->
                            println("[OrderRepository.kt] Event passed filter, mapping to UI model: $event")
                            mapToOrderUiModel(event)
                        }
                        .sortedBy { it.created_at }

                    conversationMessages
                }.flowOn(Dispatchers.IO)
        }
    }

    fun createMessageEvent(
        content: String,
        participants: List<String>,
        subject: String,
        replyToId: String?
    ): NostrEnvelope {
        //return EventFactory.createSignedEvent(
        return EventFactory.createUnsignedEvent(
            pubkeyHex = pubkeyHex,
            kind = 14,
            content = content,
            tagsBuilder = {
                (participants + pubkeyHex).distinct().forEach { p(it) }
                subject(subject)
                replyToId?.let { e(it) }
            }//,
            //    signer = signer
        )
    }

    suspend fun publishEvents(eventList: List<NostrEnvelope>): List<Map<String, com.example.nostr.models.PublicationResult>> = coroutineScope {
        val relays = relayRepository.relaysFlow.first()
        val timeout = relayRepository.relayTimeoutMsFlow.first()

        eventList.map { event ->
            async {
                try {
                    nostrRepository.publishEventToRelaysWithStatus(relays, event, timeout)
                } catch (e: Exception) {
                    println("[OrderRepository] Failed to publish event: $e")
                    emptyMap<String, com.example.nostr.models.PublicationResult>() // Return empty map for failed events
                }
            }
        }.awaitAll()
    }

    fun createMessageKind1059Event(
        participant: String,
        payloadEvent: NostrEnvelope
    ): NostrEnvelope {
        return accountRepository.createMessageKind1059Event(
            //add here the random key pair
            participant = participant,
            payloadEvent = payloadEvent
        )
    }

    fun createDeleteEvent(
        messageId: String,
        reason: String
    ): NostrEnvelope {
        return EventFactory.createSignedEvent(
            pubkeyHex = pubkeyHex,
            kind = 5,
            content = reason,
            tagsBuilder = {
                e(messageId)
            },
            signer = signer
        )
    }

    suspend fun publishEvent(event: NostrEnvelope): Map<String, com.example.nostr.models.PublicationResult> {
        val relays = relayRepository.relaysFlow.first()
        val timeout = relayRepository.relayTimeoutMsFlow.first()
        val results = nostrRepository.publishEventToRelaysWithStatus(relays, event, timeout)
        results.forEach { (relay, result) ->
            println("OrderRepositoryhhh Relay: $relay, Status: ${result.status}")
        }
        return results
    }

    private fun subscribeToMessagesInternal(relayUrls: List<String>, customFilters: List<Filter>): Flow<OrderData> {
        return flow {
            val orders = mutableMapOf<String, NostrEnvelope>()
            val eoseReceivedFrom = mutableSetOf<String>()
            val deletionFilter = Filter(kinds = listOf(5))
            val allFilters = customFilters + deletionFilter

            nostrRepository.subscribeToEvents(relayUrls, allFilters)
                .collect { message ->
                    var listChanged = false
                    when (message) {
                        is EoseMessage -> {
                            println("[OrderRepository.kt] EoseMessage received: $message")
                            eoseReceivedFrom.add(message.relayUrl)
                            val allEoseReceived = eoseReceivedFrom.size >= relayUrls.size
                            val currentOrders = orders.values.toList().sortedByDescending { it.created_at }
                            emit(OrderData(orders = currentOrders))
                        }
                        is EventMessage -> {
                            val event = message.event
                            if (event.kind == 5) {
                                val eventIdToDelete = event.tags.find { it.size > 1 && it[0] == "e" }?.getOrNull(1)
                                if (eventIdToDelete != null && orders.containsKey(eventIdToDelete)) {
                                    orders.remove(eventIdToDelete)
                                    listChanged = true
                                }
                                // } else if (event.kind == 14) {
                                //     //println("[OrderRepository.kt] RECEIVED kind=14 event: id=${event.id} pubkey=${event.pubkey} created_at=${event.created_at} tags=${event.tags} content=${event.content}")
                                //     val existingEvent = orders[event.id]
                                //     if (existingEvent == null || event.created_at > existingEvent.created_at) {
                                //         orders[event.id] = event
                                //         listChanged = true
                                //     }
                            } else if (event.kind == 1059) {
                                println("[OrderRepository.kt] Received kind=1059 event: $event")
                                val decrypted = accountRepository.decryptGiftWrap(event)
                                println("[OrderRepository.kt] decrypted=$decrypted for event id=${event}")
                                if (decrypted != null) {
                                    println("[OrderRepository.kt] RECEIVED kind=1059 decrypted to kind=14 event: id=${decrypted.id} pubkey=${decrypted.pubkey} created_at=${decrypted.created_at} tags=${decrypted.tags} content=${decrypted.content}")
                                    val existingEvent = orders[decrypted.id]
                                    if (existingEvent == null || decrypted.created_at > existingEvent.created_at) {
                                        orders[decrypted.id] = decrypted
                                        listChanged = true
                                    }
                                } else {
                                    println("[OrderRepository.kt] Failed to decrypt kind=1059 event: id=${event.id}")
                                }
                            }
                            if (listChanged) {
                                println("[OrderRepository.kt] Emitting updated orders: ${orders.keys}")
                                val currentOrders = orders.values.toList().sortedByDescending { it.created_at }
                                emit(OrderData(orders = currentOrders))
                            }
                        }
                        is NoticeMessage -> {
                            println("[OrderRepository] Received notice from ${message.relayUrl}: ${message.message}")
                            //listChanged = true
                        }
                        is AuthMessage -> {
                            println("[OrderRepository] Received auth from ${message.relayUrl}: ${message.challenge}")
                            //listChanged = true
                        }
                        is ClosedMessage -> {
                            println("[OrderRepository] Received closed from ${message.relayUrl}: ${message.message}")
                            //listChanged = true
                        }
                        is OkMessage -> {
                            println("[OrderRepository] Received ok from ${message.relayUrl}: ${message.message}")
                            //listChanged = true
                        }
                        else -> {
                            // This branch should not be reached if all message types are handled
                            println("[OrderRepository] Received an unknown message type: $message")
                            //listChanged = true
                        }
                    }
                }
        }//.debounce(1000)
    }

    fun mapToOrderUiModel(event: NostrEnvelope): OrderUiModel {
        println("[OrderRepository] Mapping event to OrderUiModel: ${event.id}, content: ${event.content}")
        try {
            val formattedCreatedAt = try {
                val instant = Instant.fromEpochSeconds(event.created_at)
                val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
                "${localDateTime.year}-${localDateTime.monthNumber.toString().padStart(2, '0')}-${localDateTime.dayOfMonth.toString().padStart(2, '0')} " +
                        "${localDateTime.hour.toString().padStart(2, '0')}:${localDateTime.minute.toString().padStart(2, '0')}"
            } catch (_: Exception) {
                null
            }

            return OrderUiModel(
                envelope = event,
                event_id = event.id,
                pubkey = event.pubkey,
                created_at = event.created_at,
                kind = event.kind,
                tags = event.tags,
                content = event.content, // Content is plain text
                sig = event.sig ?: "",
                formattedCreatedAt = formattedCreatedAt,
                relayUrl = event.relayUrl,
                isFromCurrentUser = event.pubkey == pubkeyHex
            )
        } catch (e: Exception) {
            println("[OrderRepository] Failed to map event to OrderUiModel: id=${event.id}, error=${e}")
            throw e
        }
    }
}
