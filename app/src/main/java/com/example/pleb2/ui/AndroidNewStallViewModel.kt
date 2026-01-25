package com.example.pleb2.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.BroadcastRepository
import com.example.data.uiModels.ShippingZoneUiModel
import com.example.data.uiModels.StallUiModel
import com.example.data.viewmodel.NewStallViewModel
import com.example.nostr.NostrRepository
import com.example.data.settings.RelayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.data.settings.AccountRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.data.uiModels.TagUiModel
import com.example.data.repository.StallRepository

@HiltViewModel
class AndroidNewStallViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val repository: NostrRepository,
    private val relayRepository: RelayRepository
) : ViewModel() {
    private val pubkeyHex = accountRepository.getPublicAccountInfo()?.pubKeyHex ?: error("No pubkey")
    private val signer = accountRepository.getSigningLambda() ?: error("No signing key")
    private val delegate = NewStallViewModel(pubkeyHex, signer, BroadcastRepository(repository), StallRepository(repository), relayRepository, viewModelScope)
    val eventState: StateFlow<com.example.nostr.models.NostrEnvelope?> = delegate.eventState
    val error: StateFlow<String?> = delegate.error
    val isLoading: StateFlow<Boolean> = delegate.isLoading
    val loadedStall: StateFlow<StallUiModel?> = delegate.loadedStall

    fun createAndBroadcastStallEvent(
        id: String?,
        name: String?,
        description: String?,
        currency: String?,
        shippingZonesUi: List<ShippingZoneUiModel>,
        tags: List<TagUiModel>,
        onResult: (Map<String, Boolean>, String?) -> Unit
    ) {
        viewModelScope.launch {
            delegate.createAndBroadcastStallEvent(
                id = id,
                name = name,
                description = description,
                currency = currency,
                shippingZonesUi = shippingZonesUi,
                tags = tags,
                onResult = onResult
            )
        }
    }

    /*
    fun createStallEvent(
        id: String?,
        name: String?,
        description: String?,
        currency: String?,
        shippingZonesUi: List<ShippingZoneUiModel>,
        tags: List<TagUiModel> // Changed to List<TagUiModel>
    ) {
        viewModelScope.launch {
            delegate.createStallEvent(
                id = id,
                name = name,
                description = description,
                currency = currency,
                shippingZonesUi = shippingZonesUi,
                tags = tags // Forwarding tags to delegate
            )
        }
    }


    fun broadcastStallEvent(
        event: com.example.nostr.models.NostrEnvelope,
        onResult: (Map<String, Boolean>) -> Unit = {}
    ) {
        println("[AndroidNewStallViewModel] broadcastStallEvent called with event id: ${event.id}")
        viewModelScope.launch {
            delegate.broadcastStallEvent(event, onResult)
        }
    }
    */


    fun clearError() {
        delegate.clearError()
    }

    fun fetchStallByEventId(stallEventId: String) = delegate.fetchStallByEventId(stallEventId)

    override fun onCleared() {
        delegate.close()
        super.onCleared()
    }
}
