package com.example.data.viewmodel

import com.example.data.repository.ProductRepository
import com.example.data.repository.StallRepository
import com.example.data.settings.RelayRepository
import com.example.data.uiModels.ProductUiModel
import com.example.data.uiModels.StallUiModel
import com.example.nostr.NostrRepository
import com.example.nostr.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProductDetailViewModel(
    private val repository: NostrRepository,
    private val productRepository: ProductRepository,
    private val stallRepository: StallRepository,
    private val relayRepository: RelayRepository,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val _productUiModel = MutableStateFlow<ProductUiModel?>(null)
    val productUiModel: StateFlow<ProductUiModel?> = _productUiModel

    private val _loadedStall = MutableStateFlow<StallUiModel?>(null)
    val loadedStall: StateFlow<StallUiModel?> = _loadedStall

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadProductAndStall(eventId: String) {
        coroutineScope.launch {
            _isLoading.value = true
            _error.value = null
            _productUiModel.value = null // Reset product on new load

            val relays = relayRepository.relaysFlow.first()

            // Subscribe to the product flow
            productRepository.getProductByEventIdWithEose(relays, eventId)
                .collectLatest { productData ->
                    val product = productData.product
                    if (product != null) {
                        if (product.envelope?.verify() == true) {
                            _productUiModel.value = product

                            // Once we have a valid product, subscribe to its stall
                            subscribeToStall(product.stall_id, product.pubkey, relays)
                        } else {
                            _error.value = "Warning: Tampered product event detected!"
                        }
                    }

                    // Correctly handle loading state based on EOSE
                    if (productData.eoseHit) {
                        _isLoading.value = false
                    }
                }
        }
    }

    private fun subscribeToStall(stallId: String, authorPubkey: String, relays: List<String>) {
        coroutineScope.launch {
            stallRepository.getStallByStallId(relays, stallId, authorPubkey)
                .collectLatest { stall ->
                    if (stall != null) {
                        if (stall.envelope?.verify() == true) {
                            _loadedStall.value = stall
                        } else {
                            println("ProductDetailViewModel.kt: Stall event failed verification for eventId=${stall.event_id} pubkey=${stall.pubkey}")
                            // Optionally set a stall-specific error
                        }
                    }
                    // No need to handle the eoseHit case for the stall.
                    // The main loading indicator is controlled by the product fetch.
                    // If the stall is not found, the UI will simply show the stall ID.
                }
        }
    }

    fun close() {
        println("!!! ProductDetailViewModel: close() called. Cancelling coroutine scope.")
        coroutineScope.cancel()
    }

    fun clearError() {
        _error.value = null
    }
}
