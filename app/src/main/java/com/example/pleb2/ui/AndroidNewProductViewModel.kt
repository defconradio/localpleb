package com.example.pleb2.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.BroadcastRepository
import com.example.data.uiModels.StallUiModel
import com.example.data.viewmodel.NewProductViewModel
import com.example.nostr.NostrRepository
import com.example.data.settings.RelayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.data.settings.AccountRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.data.uiModels.TagUiModel
import com.example.data.uiModels.ProductUiModel
import com.example.data.uiModels.ProductShippingZoneUiModel
import com.example.data.repository.ProductRepository
import com.example.data.repository.StallRepository

@HiltViewModel
class AndroidNewProductViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val repository: NostrRepository,
    private val relayRepository: RelayRepository
) : ViewModel() {
    private val pubkeyHex = accountRepository.getPublicAccountInfo()?.pubKeyHex ?: error("No pubkey")
    private val signer = accountRepository.getSigningLambda() ?: error("No signing key")
    private val delegate = NewProductViewModel(pubkeyHex, signer, BroadcastRepository(repository), ProductRepository(repository), StallRepository(repository), relayRepository, viewModelScope)
    val eventState: StateFlow<com.example.nostr.models.NostrEnvelope?> = delegate.eventState
    val error: StateFlow<String?> = delegate.error
    val isLoading: StateFlow<Boolean> = delegate.isLoading
    val loadedProduct: StateFlow<ProductUiModel?> = delegate.loadedProduct
    val loadedStall: StateFlow<StallUiModel?> = delegate.loadedStall
    val productConflict: StateFlow<Boolean> = delegate.productConflict

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
        viewModelScope.launch {
            delegate.createAndBroadcastProductEvent(
                id, stallId, name, description, images, currency, price, quantity, specs, shipping, tags, onResult
            )
        }
    }

    /*fun createProductEvent(
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
        tags: List<TagUiModel>
    ) {
        viewModelScope.launch {
            delegate.createProductEvent(
                id = id,
                stallId = stallId,
                name = name,
                description = description,
                images = images,
                currency = currency,
                price = price,
                quantity = quantity,
                specs = specs,
                shipping = shipping,
                tags = tags
            )
        }
    }*/

    /*
    fun broadcastProductEvent(
        event: com.example.nostr.models.NostrEnvelope,
        onResult: (Map<String, Boolean>) -> Unit = {}
    ) {
        println("[AndroidNewProductViewModel] broadcastProductEvent called with event id: ${event.id}")
        viewModelScope.launch {
            delegate.broadcastProductEvent(event, onResult)
        }
    }
    */

    fun clearError() {
        delegate.clearError()
    }

    fun clearState() {
        delegate.clearState()
    }

    fun fetchProductByEventId(productEventId: String) = delegate.fetchProductByEventId(productEventId)
    fun fetchStallByEventId(stallEventId: String) = delegate.fetchStallByEventId(stallEventId)

    fun resetProductConflict() {
        delegate.resetProductConflict()
    }

    override fun onCleared() {
        delegate.close()
        super.onCleared()
    }
}
