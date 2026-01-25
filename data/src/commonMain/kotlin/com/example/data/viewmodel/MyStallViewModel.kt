package com.example.data.viewmodel

import com.example.data.repository.BroadcastRepository
import com.example.data.repository.ProductRepository
import com.example.data.repository.StallRepository
import com.example.data.settings.AccountRepository
import com.example.data.settings.RelayRepository
import com.example.data.uiModels.ProductUiModel
import com.example.data.uiModels.StallUiModel
import com.example.nostr.EventFactory
import com.example.nostr.models.NostrEnvelope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.collect

class MyStallViewModel(
    private val broadcastRepository: BroadcastRepository,
    private val productRepository: ProductRepository,
    private val stallRepository: StallRepository,
    private val relayRepository: RelayRepository,
    private val accountRepository: AccountRepository,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val _productUiModels = MutableStateFlow<List<ProductUiModel>>(emptyList())
    val productUiModels: StateFlow<List<ProductUiModel>> = _productUiModels

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _pubKeyHex = MutableStateFlow<String?>(null)

    private val _stallCardUiModels = MutableStateFlow<List<StallUiModel>>(emptyList())
    val stallCardUiModels: StateFlow<List<StallUiModel>> = _stallCardUiModels

    private val _selectedStallStallId = MutableStateFlow<String?>(null)
    val selectedStallStallId: StateFlow<String?> = _selectedStallStallId

    private val _selectedStallEventId = MutableStateFlow<String?>(null)
    val selectedStallEventId: StateFlow<String?> = _selectedStallEventId

    private val _showEmptyProductState = MutableStateFlow(false)
    val showEmptyProductState: StateFlow<Boolean> = _showEmptyProductState

    // --- Empty state for create new stall (best practice, do not remove any code, just comment out if needed) ---
    // Add a state for empty stall UI, but do not remove any code, just comment if needed
    private val _showEmptyStallState = MutableStateFlow(false)
    val showEmptyStallState: StateFlow<Boolean> = _showEmptyStallState

    private var fetchJob: Job? = null
    private val reloadMutex = Mutex()

    init {
        coroutineScope.launch {
            accountRepository.publicAccountInfoFlow
                .onEach { publicAccountInfo ->
                    _pubKeyHex.value = publicAccountInfo?.pubKeyHex
                    reloadAll(publicAccountInfo?.pubKeyHex)
                }
                .collect()
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun selectStall(stallEventId: String?) {
        _selectedStallEventId.value = stallEventId
        // Find the stall by event.id and set selectedStallStallId to its stall.id
        val stall = _stallCardUiModels.value.find { it.event_id == stallEventId }
        _selectedStallStallId.value = stall?.stall_id
        // Debug log to help trace selection
        println("MyStallViewModel.kt - selectStall: eventId=$stallEventId, found stall? ${stall != null}")
        println("MyStallViewModel.kt - selectStall: available ids=${_stallCardUiModels.value.map { it.event_id }}")
    }

    // Use this function to update both the stall list and the empty state
    fun setStallsWithEmptyState(stalls: List<StallUiModel>) {
        _stallCardUiModels.value = stalls
        _showEmptyStallState.value = stalls.isEmpty()
    }
    fun reloadAll() {
        coroutineScope.launch {
            val pubKey = accountRepository.getPublicAccountInfo()?.pubKeyHex
            reloadAll(pubKey)
        }
    }

    private fun reloadAll(pubKeyHex: String?) {
        fetchJob?.cancel()
        fetchJob = coroutineScope.launch {
            // 1. Turn on the loading indicator for immediate user feedback.
            _isLoading.value = true
            _error.value = null

            reloadMutex.withLock {
                try {
                    if (pubKeyHex == null) {
                        setStallsWithEmptyState(emptyList())
                        _productUiModels.value = emptyList()
                        _isLoading.value = false
                        return@withLock
                    }
                    // Fetch relays once and pass them to the functions
                    val relays = relayRepository.relaysFlow.first()
                    // 3. Launch stall and product data fetching in parallel.
                    // The loading indicator will be managed by the stall fetching process.
                    launch { fetchProductsByPubKey(pubKeyHex, relays) }
                    launch { fetchStallsByPubKey(pubKeyHex, relays) }
                } catch (e: Exception) {
                    if (e is CancellationException) {
                        // This is expected when a new reload is triggered while a previous one is running.
                        // We can safely ignore it and let the new job continue.
                    } else {
                        _error.value = e.message
                        _isLoading.value = false // Turn off on error
                    }
                }
            }
        }
    }

    fun deleteEventsAndReload(
        eventIdsToDelete: List<String>,
        reason: String,
        signer: (ByteArray) -> ByteArray
    ) {
        coroutineScope.launch {
            reloadMutex.withLock {
                //_isLoading.value = true
                _error.value = null
                try {
                    // Step 1: Create the delete event
                    val publicAccountInfo = accountRepository.getPublicAccountInfo()
                    if (publicAccountInfo == null) {
                        _error.value = "Not logged in"
                        return@withLock
                    }
                    val event = EventFactory.createSignedEvent(
                        pubkeyHex = publicAccountInfo.pubKeyHex,
                        kind = 5,
                        content = reason,
                        tagsBuilder = {
                            eventIdsToDelete.forEach { e(it) }
                        },
                        signer = signer
                    )

                    // Step 2: Broadcast the delete event
                    val relays = relayRepository.relaysFlow.first()
                    val timeout = relayRepository.relayTimeoutMsFlow.first()
                    val results = broadcastRepository.broadcastEvent(event, relays, timeout)
                    if (results.values.any { !it }) {
                        val failedRelays = results.filter { !it.value }.keys.joinToString(", ")
                        _error.value = "Broadcast failed for: $failedRelays"
                    }

                    // Step 3: The repositories will handle the deletion event and update the UI automatically.
                    // No explicit reload is needed here.

                } catch (e: Exception) {
                    if (e is CancellationException) {
                        // Expected, do nothing
                    } else {
                        _error.value = "Error during deletion and reload: ${e.message}"
                    }
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }
    private suspend fun fetchStallsByPubKey(pubKeyHex: String, relays: List<String>) {
        println("MyStallViewModel.kt - fetchStallsByPubKey called")
        try {
            // Listen for stall data continuously to get real-time updates.
            stallRepository.getStallsByAuthor(relays, listOf(pubKeyHex))
                .collect { stallList ->
                    // Update the live list of stalls
                    withContext(Dispatchers.Main) {
                        val sortedStallList = stallList.sortedByDescending { it.created_at }
                        setStallsWithEmptyState(sortedStallList)
                        // If stalls exist, select the newest one by default.
                        if (sortedStallList.isNotEmpty()) {
                            _isLoading.value = false // Hide loading indicator as soon as data arrives
                            _selectedStallEventId.value = sortedStallList.first().event_id
                            _selectedStallStallId.value = sortedStallList.first().stall_id
                        }
                    }
                }
        } catch (e: Exception) {
            if (e is CancellationException) return
            _error.value = e.message
            _isLoading.value = false // Also turn off on error
        }
    }

    /**
     * Fetches products for the current user's public key.
     * This runs in the background and does not affect the main loading indicator,
     * allowing products to load seamlessly after the initial stall information is displayed.
     */
    private suspend fun fetchProductsByPubKey(pubKeyHex: String, relays: List<String>) {
        println("MyStallViewModel.kt - fetchProductsByPubKey called")
        try {
            productRepository.getProductsByAuthor(relays, listOf(pubKeyHex))
                .collect { productList ->
                    val models = productList.sortedByDescending { it.created_at }
                    _productUiModels.value = models
                    _showEmptyProductState.value = models.isEmpty()
                }
        } catch (e: Exception) {
            if (e is CancellationException) return
            _error.value = e.message
        }
    }

    fun reportVerificationError(eventId: String) {
        val event = _productUiModels.value.find { it.event_id == eventId }?.envelope
        _error.value = "Warning: Tampered event detected! , copy this message and report\n\n" +
                "id: ${event?.id}\n\n" +
                "pubkey: ${event?.pubkey}\n\n" +
                "created_at: ${event?.created_at}\n\n" +
                "relay: ${event?.relayUrl}"
    }

    fun createDeleteEvent(
        eventIdsToDelete: List<String>,
        reason: String,
        signer: (ByteArray) -> ByteArray,
        onResult: (NostrEnvelope) -> Unit
    ) {
        println("MyStallViewModel.kt - createDeleteEvent called with eventIds: $eventIdsToDelete, reason: $reason")
        coroutineScope.launch {
            //_isLoading.value = true
            try {
                val publicAccountInfo = accountRepository.getPublicAccountInfo()
                if (publicAccountInfo == null) {
                    _error.value = "Not logged in"
                    return@launch
                }

                val event = EventFactory.createSignedEvent(
                    pubkeyHex = publicAccountInfo.pubKeyHex,
                    kind = 5,
                    content = reason,
                    tagsBuilder = {
                        eventIdsToDelete.forEach { e(it) }
                    },
                    signer = signer
                )
                println("MyStallViewModel.kt - created delete event: $event")
                withContext(Dispatchers.Main) {
                    onResult(event)
                }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    // Expected, do nothing
                } else {
                    _error.value = "Error creating delete event: ${e.message}"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun broadcastEvent(
        event: NostrEnvelope,
        onResult: (Map<String, Boolean>) -> Unit
    ) {
        println("MyStallViewModel.kt - broadcastEvent called with event: $event")
        coroutineScope.launch {
            //_isLoading.value = true
            try {
                val relays = relayRepository.relaysFlow.first()
                val timeout = relayRepository.relayTimeoutMsFlow.first()
                val results = broadcastRepository.broadcastEvent(event, relays, timeout)
                println("MyStallViewModel.kt - broadcastEvent results: $results")
                if (results.values.any { !it }) {
                    val failedRelays = results.filter { !it.value }.keys.joinToString(", ")
                    _error.value = "Broadcast failed for: $failedRelays"
                } else {
                    _error.value = null
                }
                withContext(Dispatchers.Main) { onResult(results) }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    // Expected, do nothing
                } else {
                    _error.value = "Error broadcasting event: ${e.message}"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun close() {
        println("MyStallViewModel.kt - close() called")
        coroutineScope.cancel()
    }
}
