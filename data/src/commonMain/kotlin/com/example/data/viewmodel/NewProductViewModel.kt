package com.example.data.viewmodel

import com.example.data.repository.ProductRepository
import com.example.data.repository.StallRepository
import com.example.data.settings.RelayRepository
import com.example.data.uiModels.ProductShippingZoneUiModel
import com.example.data.uiModels.ProductUiModel
import com.example.data.uiModels.StallUiModel
import com.example.data.uiModels.TagUiModel
import com.example.nostr.models.ProductContent
import com.example.nostr.models.NostrEnvelope
import com.example.nostr.EventFactory
import com.example.data.repository.BroadcastRepository
import com.example.nostr.models.ProductShippingZone
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
//import com.example.pleb2.uiModels.NostrEnvelopeUiModel

// TODO: Code Duplication - This ViewModel shares a significant amount of boilerplate code
//  with NewStallViewModel. Functions like createAndBroadcast...Event, broadcast...Event,
//  fetch...ByEventId, and the state flow declarations (_eventState, _error, _isLoading)
//  are nearly identical in structure. Consider creating a generic base view model to
//  abstract the shared logic.
class NewProductViewModel(
    private val pubkeyHex: String,
    @JvmSuppressWildcards
    private val signer: (ByteArray) -> ByteArray,
    private val broadcastRepository: BroadcastRepository,
    private val productRepository: ProductRepository,
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
    private val _loadedProduct = MutableStateFlow<ProductUiModel?>(null)
    val loadedProduct: StateFlow<ProductUiModel?> = _loadedProduct.asStateFlow()
    private val _loadedStall = MutableStateFlow<StallUiModel?>(null)
    val loadedStall: StateFlow<StallUiModel?> = _loadedStall.asStateFlow()
    private val _productConflict = MutableStateFlow(false)
    val productConflict: StateFlow<Boolean> = _productConflict.asStateFlow()

    fun createAndBroadcastProductEvent(
        id: String?,
        stallId: String?,
        name: String?,
        description: String?,
        images: List<String>?,
        currency: String?,
        price: Float?,
        quantity: Int?,
        specs: List<List<String>>,
        shipping: List<ProductShippingZoneUiModel>,
        tags: List<TagUiModel>,
        onResult: (Map<String, Boolean>, String) -> Unit
    ) {
        coroutineScope.launch {
            _isLoading.value = true
            var eventId = ""
            try {
                if (id.isNullOrBlank() || stallId.isNullOrBlank() || name.isNullOrBlank() || currency.isNullOrBlank() || price == null) {
                    _error.value = "Product ID, stall ID, name, price, and currency are required."
                    withContext(Dispatchers.Main) { onResult(emptyMap(), "") }
                    return@launch
                }

                val productContent = ProductContent(
                    id = id,
                    stall_id = stallId,
                    name = name,
                    description = description,
                    images = images?.distinct(),
                    currency = currency,
                    price = price,
                    quantity = quantity,
                    specs = specs,
                    shipping = shipping.map { ProductShippingZone(it.id, it.cost) }
                )
                val contentJson = Json.encodeToString(productContent)
                val event = EventFactory.createSignedEvent(
                    pubkeyHex = pubkeyHex,
                    kind = 30018,
                    content = contentJson,
                    tagsBuilder = {
                        d(productContent.id)
                        a("30018:$pubkeyHex:${productContent.id}")
                        name(productContent.name)
                        tags.distinctBy { it.type to it.value }
                            .filter { it.type != "d" && it.type != "a" && it.type != "name" }
                            .forEach { tag -> raw(tag.type, tag.value) }
                    },
                    signer = signer
                )
                _eventState.value = event
                eventId = event.id

                val relays = relayRepository.relaysFlow.first()
                val timeout = relayRepository.relayTimeoutMsFlow.first()
                val results = broadcastRepository.broadcastEvent(event, relays, timeout)

                if (results.values.any { !it }) {
                    val failedRelays = results.filter { !it.value }.keys.joinToString(", ")
                    _error.value = "Broadcast failed for: $failedRelays"
                } else {
                    _error.value = null
                }
                withContext(Dispatchers.Main) { onResult(results, eventId) }
            } catch (e: Exception) {
                _error.value = e.message
                withContext(Dispatchers.Main) { onResult(emptyMap(), eventId) }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /*
     fun broadcastProductEvent(
        event: NostrEnvelope,
        onResult: (Map<String, Boolean>) -> Unit = {}
    ) {
        println("[NewProductViewModel] broadcastProductEvent called with event id: ${event.id}")
        coroutineScope.launch {
            _isLoading.value = true
            try {
                val relays = relayRepository.relaysFlow.first()
                val timeout = relayRepository.relayTimeoutMsFlow.first()
                println("[NewProductViewModel] relays for publish: $relays with timeout: $timeout")
                val results = broadcastRepository.broadcastEvent(event, relays, timeout)
                println("[NewProductViewModel] publishEventToRelays returned: $results")
                if (results.values.any { !it }) {
                    val failedRelays = results.filter { !it.value }.keys.joinToString(", ")
                    _error.value = "Broadcast failed for: $failedRelays"
                } else {
                    _error.value = null
                }
                withContext(Dispatchers.Main) { onResult(results) }
            } catch (e: Exception) {
                _error.value = e.message
                println("[NewProductViewModel] Exception in broadcastProductEvent: ${e.message}")
                withContext(Dispatchers.Main) { onResult(emptyMap()) }
            } finally {
                _isLoading.value = false
            }
        }
    }
    */

    fun clearError() {
        _error.value = null
    }

    fun resetProductConflict() {
        _productConflict.value = false
    }

    fun fetchProductByEventId(productEventId: String) {
        coroutineScope.launch {
            var isInitialLoad = true
            _isLoading.value = true
            try {
                val relays = relayRepository.relaysFlow.first()
                println("[NewProductViewModel] fetchProductByEventId productEventId: $productEventId")

                productRepository.getProductByEventIdWithEose(relays, productEventId)
                    .collect { productData ->
                        val product = productData.product
                        if (product != null) {
                            if (!isInitialLoad && product != _loadedProduct.value) {
                                _productConflict.value = true
                            }
                            _loadedProduct.value = product
                        }
                        // Data has been loaded, so stop showing the loading indicator
                        if (isInitialLoad && productData.eoseHit) {
                            _isLoading.value = false
                            isInitialLoad = false
                        }
                    }
            } catch (e: Exception) {
                _error.value = "Failed to fetch product: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun fetchStallByEventId(stallEventId: String) {
        coroutineScope.launch {
            // This fetch should not trigger the main loading indicator for the product screen
            // as it's a background detail. If a specific indicator is needed, it should be separate.
            try {
                val relays = relayRepository.relaysFlow.first()
                println("[NewProductViewModel] fetchStallByEventId stallEventId: $stallEventId")

                stallRepository.getStallByEventId(relays, stallEventId)
                    .collect { stall ->
                        if (stall != null) {
                            _loadedStall.value = stall
                        }
                    }
            } catch (e: Exception) {
                // Silently fail or log, to not disrupt the main UI
                println("[NewProductViewModel] Failed to fetch stall details: ${e.message}")
            }
        }
    }

    fun clearState() {
        _eventState.value = null
        _error.value = null
        _isLoading.value = false
        _loadedProduct.value = null
        _loadedStall.value = null
        _productConflict.value = false
    }

    fun close() {
        coroutineScope.cancel()
    }
}
