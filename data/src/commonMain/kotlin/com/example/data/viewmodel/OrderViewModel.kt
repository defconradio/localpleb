package com.example.data.viewmodel

// import com.example.data.repository.BroadcastRepository
import com.example.data.repository.OrderRepository
//import com.example.data.settings.AccountRepository
import com.example.data.uiModels.MessageStatus
import com.example.data.settings.RelayRepository
import com.example.data.uiModels.OrderUiModel
import com.example.nostr.models.PublicationResult
import com.example.nostr.models.PublicationStatus
// import com.example.nostr.EventFactory
// import com.example.nostr.models.NostrEnvelope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
// import kotlinx.coroutines.Dispatchers
// import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
// import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.debounce

class OrderViewModel(
    // private val pubkeyHex: String,
    private val conversationPartnerPubkeys: List<String>,
    private val conversationId: String, // Used as the 'subject' tag
    // private val signer: (ByteArray) -> ByteArray,
    // private val broadcastRepository: BroadcastRepository,
    private val orderRepository: OrderRepository,
    private val relayRepository: RelayRepository,
    private val coroutineScope: CoroutineScope
) {
    private val _messages = MutableStateFlow<List<OrderUiModel>>(emptyList())
    val messages: StateFlow<List<OrderUiModel>> = _messages.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isInitialLoad = MutableStateFlow(true)
    val isInitialLoad: StateFlow<Boolean> = _isInitialLoad.asStateFlow()

    init {
        // println("[OrderViewModel] created pubkey=$pubkeyHex partners=$conversationPartnerPubkeys conversationId=$conversationId")
        // subscribeToMessages()
        
        _isLoading.value = true
        _isInitialLoad.value = true
        _error.value = null

        val allParticipants = (conversationPartnerPubkeys).distinct()
        orderRepository.getConversation(allParticipants, conversationId)
            .debounce(400L)
            .onEach { conversationOrders ->
                println("[OrderViewModel] getConversation returned: orders=${conversationOrders.map { it.event_id }}")
                _messages.value = conversationOrders
                println("[OrderViewModel] _messages updated: ${_messages.value.map { it.event_id }}")
                if (_isLoading.value) {
                    _isLoading.value = false
                    _isInitialLoad.value = false
                }
            }
            .catch { e ->
                _error.value = "Subscription failed: ${e.message}"
                _isLoading.value = false
                _isInitialLoad.value = false
            }
            .launchIn(coroutineScope)

    }

    /**
     * Sends a NIP-17 compliant group message (Kind 1059).
     *
     * This function orchestrates the creation and sending of a secure group message.
     * Since a group message requires sending a separate encrypted event (Kind 1059) to each
     * participant, the optimistic UI update is handled carefully to avoid message duplication.
     *
     * How the optimistic update works for group messages:
     *
     * 1.  **Create a Payload Event (Kind 14):**
     *     - A single, standard Nostr event (Kind 14, a DM) is created. This event contains the
     *       actual message content, tags, and a unique ID.
     *     - This event is NOT sent directly. It serves as the "payload."
     *
     * 2.  **Immediate UI Update:**
     *     - This `payloadEvent` is immediately used to create a UI model (`OrderUiModel`) with a
     *       `status` of `SENDING`.
     *     - The UI displays this message, providing instant feedback to the user. The key is that
     *       the UI is showing the content of the `payloadEvent`.
     *
     * 3.  **Create Wrapper Events (Kind 1059):**
     *     - The `payloadEvent` is then encrypted separately for each participant in the conversation.
     *     - Each encrypted payload is placed inside a "gift wrap" event (Kind 1059). This results
     *       in a list of Kind 1059 events—one for each recipient.
     *
     * 4.  **Broadcast:**
     *     - The list of Kind 1059 events is published to the relays.
     *
     * 5.  **Reconciliation:**
     *     - When the Nostr subscription receives and decrypts these Kind 1059 events, it reveals
     *       the original `payloadEvent` inside.
     *     - Because the `id` of this revealed event is **identical** to the `id` of the event used
     *       for the optimistic UI update, the system recognizes it as the same message.
     *     - Instead of adding a duplicate message, it updates the status of the existing optimistic
     *       message from `SENDING` to `SENT` (or its default state).
     *
     * This ensures a smooth, responsive user experience without duplicating messages, even though
     * multiple events are being sent behind the scenes.
     */
    fun sendKind1059Message(text: String) {
        if (text.isBlank()) return

        val replyToId = _messages.value.lastOrNull()?.event_id
        println("[OrderViewModel] sendKind1059Message called with text: \"$text\"" + (replyToId?.let { " in reply to $it" } ?: ""))

        // Step 1: Create the kind 14 event that will be the payload for the kind 1059 wrappers.
        // This event is used for the optimistic UI update and is the actual content to be sent.
        val payloadEvent = orderRepository.createMessageEvent(
            content = text,
            participants = conversationPartnerPubkeys,
            subject = conversationId,
            replyToId = replyToId
        )

        // Step 2: Optimistic UI update using the payload event.
        val optimisticMessage = OrderUiModel(
            envelope = payloadEvent,
            event_id = payloadEvent.id,
            pubkey = payloadEvent.pubkey,
            created_at = payloadEvent.created_at,
            kind = payloadEvent.kind, // This will be kind 14 for the UI model
            tags = payloadEvent.tags,
            content = payloadEvent.content,
            sig = payloadEvent.sig ?: "",
            formattedCreatedAt = null,
            relayUrl = null,
            isFromCurrentUser = true,
            status = MessageStatus.SENDING
        )

        _messages.update { currentMessages ->
            (currentMessages + optimisticMessage)
                .distinctBy { it.event_id }
                .sortedBy { it.created_at }
        }

        // **How the Optimistic Update Works for Group Messages (Kind 1059):**
        //
        // 1. **Single Payload, Multiple Wrappers:** We create a single `payloadEvent` (Kind 14)
        //    which contains the actual message content. This event is what we use for our
        //    optimistic UI update. Its ID is unique to the message content and sender.
        //
        // 2. **One Optimistic Message:** The UI immediately displays one message with a "sending"
        //    status, based on this `payloadEvent`.
        //
        // 3. **Broadcasting Wrappers:** We then create multiple `kind1059Events`. Each one is a
        //    "wrapper" that contains the *encrypted content* of the `payloadEvent`, addressed
        //    to a specific group member. These wrappers are what we actually broadcast.
        //
        // 4. **Receiving and Unwrapping:** When our client's subscription receives a kind 1059
        //    event meant for us, the repository layer decrypts its content. The decrypted
        //    content is the JSON of the original `payloadEvent`.
        //
        // 5. **Consistent ID:** When this JSON is parsed back into a `NostrEnvelope`, it produces
        //    an event with the *exact same ID* as the `payloadEvent` we created for the
        //    optimistic update.
        //
        // 6. **Seamless UI Transition:** The new, confirmed message (from the subscription)
        //    replaces the optimistic one in the UI list because they share the same ID
        //    (thanks to `.distinctBy { it.event_id }`). This provides a seamless transition
        //    from "sending" to "sent" without creating duplicate messages.

        // Step 3: Launch a coroutine to create and broadcast the actual kind 1059 events.
        coroutineScope.launch {
            try {
                var allSendsSuccessful = true

                // Iterate over each participant and send them a unique Kind 1059 event.
                for (participant in conversationPartnerPubkeys) {
                    // Create the actual kind 1059 event to be sent, using the payload event.
                    val kind1059Event = orderRepository.createMessageKind1059Event(
                        participant = participant,
                        payloadEvent = payloadEvent
                    )

                    println("[OrderViewModel] Payload event for kind 1059: $payloadEvent")
                    println("[OrderViewModel] Created kind 1059 event for $participant: $kind1059Event")

                    // Publish the kind 1059 event.
                    val results = orderRepository.publishEvent(kind1059Event)
                    println("[OrderViewModel] Kind 1059 publish results for $participant:")
                    results.forEach { (relay, result) ->
                        println("[OrderViewModel]  - Relay: $relay, Status: ${result.status}, Message: ${result.message}")
                    }

                    // A message is considered successful for a participant if it's sent to at least one relay.
                    val publishedToAtLeastOneRelay = results.values.any { it.status == PublicationStatus.SUCCESS }

                    if (!publishedToAtLeastOneRelay) {
                        allSendsSuccessful = false
                        println("[OrderViewModel] Group message failed to send to any relay for participant: $participant.")
                        // Optionally, you could break here if sending to all is critical,
                        // or collect results for each participant.
                    }
                }

                if (!allSendsSuccessful) {
                    _error.value = "Group message failed to send to at least one relay for every participant."
                    println("[OrderViewModel] Group message failed to send to at least one relay for every participant.")

                    // On failure, update the optimistic message status to FAILED.
                    _messages.update { currentMessages ->
                        currentMessages.map {
                            if (it.event_id == optimisticMessage.event_id) {
                                it.copy(status = MessageStatus.FAILED)
                            } else {
                                it
                            }
                        }
                    }
                }
                // On success, the subscription will eventually receive the new message,
                // which will replace the optimistic one.
            } catch (e: Exception) {
                _error.value = "Failed to send group message: ${e.message}"
                println("[OrderViewModel] Failed to send group message: ${e.message}")
                // On exception, update the message status to FAILED.
                _messages.update { currentMessages ->
                    currentMessages.map {
                        if (it.event_id == optimisticMessage.event_id) {
                            it.copy(status = MessageStatus.FAILED)
                        } else {
                            it
                        }
                    }
                }
            }
        }
    }

    fun resendMessage(messageId: String) {
        coroutineScope.launch {
            val failedMessage = _messages.value.find { it.event_id == messageId && it.status == MessageStatus.FAILED }
            if (failedMessage != null) {
                // 1. Optimistically update the UI: set status to SENDING
                _messages.update { currentMessages ->
                    currentMessages.map {
                        if (it.event_id == messageId) it.copy(status = MessageStatus.SENDING) else it
                    }
                }

                // 2. Try to publish the original event again
                try {
                    val results = orderRepository.publishEvent(failedMessage.envelope)
                    println("[OrderViewModel] Resend publish results for event ${failedMessage.event_id}:")
                    results.forEach { (relay, result) ->
                        println("  - Relay: $relay, Status: ${result.status}, Message: ${result.message}")
                    }

                    val failedRelays = results.filter { it.value.status != PublicationStatus.SUCCESS }.keys
                    if (failedRelays.isNotEmpty()) {
                        _error.value = "Message failed to send to: ${failedRelays.joinToString(", ")}"
                        println("[OrderViewModel] Message failed to send to: ${failedRelays.joinToString(", ")}")
                        // On failure, update the message status back to FAILED
                        _messages.update { currentMessages ->
                            currentMessages.map {
                                if (it.event_id == messageId) {
                                    it.copy(status = MessageStatus.FAILED)
                                } else {
                                    it
                                }
                            }
                        }
                    }
                    // If successful, the subscription will eventually confirm it.
                } catch (e: Exception) {
                    _error.value = "Failed to resend message: ${e.message}"
                    println("[OrderViewModel] Failed to resend message: ${e.message}")
                    // On exception, update the message status back to FAILED
                    _messages.update { currentMessages ->
                        currentMessages.map {
                            if (it.event_id == messageId) {
                                it.copy(status = MessageStatus.FAILED)
                            } else {
                                it
                            }
                        }
                    }
                }
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
    //this will actually dont delete a shit if kind1059 event but the ui will reflect the change
    // as we follow the standard documentation of kind 5 client "MUST" reflect changes but you know ai just amke ppl lazy af
    fun deleteMessage(
        messageId: String,
        reason: String = "",
        onResult: ((Map<String, PublicationResult>) -> Unit)? = null
    ) {
        println("OrderViewModel.kt - deleteMessage called for messageId: $messageId, reason: $reason")
        coroutineScope.launch {
            _error.value = null

            // Optimistically update the UI to a "deleting" state
            _messages.update { currentMessages ->
                currentMessages.map {
                    if (it.event_id == messageId) it.copy(isDeleting = true) else it
                }
            }

            try {
                // Step 1: Create the delete event (Kind 5)
                val event = orderRepository.createDeleteEvent(messageId, reason)
                println("OrderViewModel.kt - deleteMessage created delete event: $event")

                // Step 2: Broadcast the delete event
                val results = orderRepository.publishEvent(event)
                println("OrderViewModel.kt - deleteMessage broadcast results: $results")

                // If deletion failed on all relays, show an error and revert the UI change
                if (results.values.none { it.status == PublicationStatus.SUCCESS }) {
                    _error.value = "Failed to delete message on all relays."
                    _messages.update { currentMessages ->
                        currentMessages.map {
                            if (it.event_id == messageId) it.copy(isDeleting = false) else it
                        }
                    }
                }

                val failedRelays = results.filter { it.value.status != PublicationStatus.SUCCESS }.keys
                if (failedRelays.isNotEmpty()) {
                    _error.value = "Delete failed on: ${failedRelays.joinToString(", ")}"
                } else {
                    _error.value = null
                }
                onResult?.let { withContext(Dispatchers.Main) { it(results) } }
            } catch (e: Exception) {
                _error.value = "Failed to delete message: ${e.message}"
                println("OrderViewModel.kt - deleteMessage error: ${e.message}")
                // Also revert on exception
                _messages.update { currentMessages ->
                    currentMessages.map {
                        if (it.event_id == messageId) it.copy(isDeleting = false) else it
                    }
                }
            }
        }
    }

    fun close() {
        coroutineScope.cancel()
    }
}
