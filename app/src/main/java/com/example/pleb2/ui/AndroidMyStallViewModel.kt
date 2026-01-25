package com.example.pleb2.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.BroadcastRepository
import com.example.nostr.NostrRepository
import com.example.nostr.models.NostrEnvelope
import com.example.data.repository.ProductRepository
import com.example.data.repository.StallRepository
import com.example.data.settings.AccountRepository
import com.example.data.settings.RelayRepository
import com.example.data.uiModels.ProductUiModel
import com.example.data.uiModels.StallUiModel
import com.example.data.viewmodel.MyStallViewModel as SharedMyStallViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

// ISSUE: Passing data between MyStallScreen and NewStallScreen via navigation arguments requires JSON serialization and URL encoding, which can lead to decode bugs and data corruption due to automatic decoding by the navigation system.
// TODO: Use a shared ViewModel for both MyStallScreen and NewStallScreen to avoid encoding/decoding issues and pass data directly as Kotlin objects.
// This will make data transfer robust and eliminate the need for JSON/URL encoding workarounds.
// TODO: Consider joining MyStallViewModel and NewStallViewModel into a shared StallViewModel for better data sharing and navigation between MyStallScreen and NewStallScreen.

@HiltViewModel
class AndroidMyStallViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val repository: NostrRepository,
    private val relayRepository: RelayRepository
) : ViewModel() {
    private val shared = SharedMyStallViewModel(
        broadcastRepository = BroadcastRepository(repository),
        productRepository = ProductRepository(repository),
        stallRepository = StallRepository(repository),
        relayRepository = relayRepository,
        accountRepository = accountRepository,
        coroutineScope = viewModelScope
    )

    val productUiModels: StateFlow<List<ProductUiModel>> = shared.productUiModels
    val isLoading: StateFlow<Boolean> = shared.isLoading
    val error: StateFlow<String?> = shared.error
    val stallCardUiModels: StateFlow<List<StallUiModel>> = shared.stallCardUiModels
    val selectedStallStallId: StateFlow<String?> = shared.selectedStallStallId
    val selectedStallEventId: StateFlow<String?> = shared.selectedStallEventId
    //val deletedEventIds: StateFlow<Set<String>> = shared.deletedEventIds
    val showEmptyStallState: StateFlow<Boolean> = shared.showEmptyStallState
    val showEmptyProductState: StateFlow<Boolean> = shared.showEmptyProductState

    fun reloadAll() {
        shared.reloadAll()
    }

    fun selectStall(stallEventId: String?) {
        shared.selectStall(stallEventId)
    }

    fun reportVerificationError(eventId: String) {
        shared.reportVerificationError(eventId)
    }

    fun deleteEventsAndReload(eventIdsToDelete: List<String>, reason: String) {
        val signer = accountRepository.getSigningLambda() ?: error("No signing key")
        shared.deleteEventsAndReload(eventIdsToDelete, reason, signer)
    }

    fun createDeleteEvent(
        eventIdsToDelete: List<String>,
        reason: String,
        onResult: (NostrEnvelope) -> Unit
    ) {
        val signer = accountRepository.getSigningLambda() ?: error("No signing key")
        shared.createDeleteEvent(eventIdsToDelete, reason, signer, onResult)
    }

    fun broadcastEvent(
        event: NostrEnvelope,
        onResult: (Map<String, Boolean>) -> Unit
    ) {
        shared.broadcastEvent(event, onResult)
    }

    fun clearError() {
        shared.clearError()
    }

    override fun onCleared() {
        shared.close()
        super.onCleared()
    }
}
