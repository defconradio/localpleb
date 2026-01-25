package com.example.data.viewmodel

import com.example.data.repository.BroadcastRepository
import com.example.data.repository.StallRepository
import com.example.data.settings.RelayRepository
import com.example.data.uiModels.ShippingZoneUiModel
import com.example.data.uiModels.StallUiModel
import com.example.data.uiModels.TagUiModel
import com.example.nostr.models.StallContent
import com.example.nostr.models.NostrEnvelope
import com.example.nostr.EventFactory
import com.example.nostr.models.ShippingZone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

// TODO: Code Duplication - This ViewModel shares a significant amount of boilerplate code
//  with NewProductViewModel. Functions like createAndBroadcast...Event, broadcast...Event,
//  fetch...ByEventId, and the state flow declarations (_eventState, _error, _isLoading)
//  are nearly identical in structure. Consider creating a generic base view model to
//  abstract the shared logic.
class NewStallViewModel(
    private val pubkeyHex: String,
    @JvmSuppressWildcards // Ensure no wildcard is introduced when passing signer
    private val signer: (ByteArray) -> ByteArray, // Not suspend!
    private val broadcastRepository: BroadcastRepository,
    private val stallRepository: StallRepository,
    private val relayRepository: RelayRepository,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val _eventState = MutableStateFlow<NostrEnvelope?>(null)
    val eventState: StateFlow<NostrEnvelope?> = _eventState.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _loadedStall = MutableStateFlow<StallUiModel?>(null)
    val loadedStall: StateFlow<StallUiModel?> = _loadedStall.asStateFlow()
    private val _stallConflict = MutableStateFlow(false)
    val stallConflict: StateFlow<Boolean> = _stallConflict.asStateFlow()

    fun createAndBroadcastStallEvent(
        id: String?,
        name: String?,
        description: String?,
        currency: String?,
        shippingZonesUi: List<ShippingZoneUiModel>,
        tags: List<TagUiModel>,
        onResult: (Map<String, Boolean>, String?) -> Unit
    ) {
        coroutineScope.launch {
            _isLoading.value = true
            _error.value = null
            var event: NostrEnvelope? = null
            try {
                // Validation
                if (id.isNullOrBlank() || name.isNullOrBlank() || currency.isNullOrBlank()) {
                    _error.value = "Stall ID, name, and currency are required."
                    onResult(emptyMap(), null)
                    return@launch
                }

                // Create Event
                val shippingZones = shippingZonesUi.map {
                    ShippingZone(
                        id = it.id, name = it.name, cost = it.cost, regions = it.regions, countries = null
                    )
                }
                val stallContent = StallContent(
                    id = id, name = name, description = description, currency = currency, shipping = shippingZones
                )
                val contentJson = Json.encodeToString(stallContent)
                event = EventFactory.createSignedEvent(
                    pubkeyHex = pubkeyHex,
                    kind = 30017,
                    content = contentJson,
                    tagsBuilder = {
                        d(stallContent.id)
                        a("30017:$pubkeyHex:${stallContent.id}")
                        name(stallContent.name)
                        tags.distinctBy { it.type to it.value }
                            .filter { it.type != "d" && it.type != "a" && it.type != "name" }
                            .forEach { tag -> raw(tag.type, tag.value) }
                    },
                    signer = signer
                )
                _eventState.value = event

                // Broadcast Event
                val relays = relayRepository.relaysFlow.first()
                val timeout = relayRepository.relayTimeoutMsFlow.first()
                val results = broadcastRepository.broadcastEvent(event, relays, timeout)

                if (results.values.any { !it }) {
                    val failedRelays = results.filter { !it.value }.keys.joinToString(", ")
                    _error.value = "Broadcast failed for: $failedRelays"
                } else {
                    _error.value = null
                }
                withContext(Dispatchers.Main) { onResult(results, event.id) }

            } catch (e: Exception) {
                _error.value = e.message
                withContext(Dispatchers.Main) { onResult(emptyMap(), event?.id) }
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Add this function to allow clearing the error from the UI
    fun clearError() {
        _error.value = null
    }

    fun resetStallConflict() {
        _stallConflict.value = false
    }

    fun fetchStallByEventId(stallEventId: String) {
        coroutineScope.launch {
            _isLoading.value = true
            var isInitialLoad = true
            try {
                val relays = relayRepository.relaysFlow.first()
                stallRepository.getStallByEventId(relays, stallEventId)
                    .collect { stall ->
                        if (stall != null) {
                            if (!isInitialLoad && stall != _loadedStall.value) {
                                _stallConflict.value = true
                            }
                            _loadedStall.value = stall
                        }
                        if (_isLoading.value) {
                            _isLoading.value = false
                            isInitialLoad = false
                        }
                    }
            } catch (e: Exception) {
                _error.value = "Failed to fetch stall: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun close() {
        coroutineScope.cancel()
    }
}
