package com.example.pleb2.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.BroadcastRepository
import com.example.data.repository.OrderRepository
import com.example.data.settings.AccountRepository
import com.example.data.settings.RelayRepository
import com.example.data.uiModels.OrderUiModel
import com.example.data.viewmodel.OrderViewModel
import com.example.nostr.NostrRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AndroidOrderViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val nostrRepository: NostrRepository,
    private val relayRepository: RelayRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val pubkeyHex = accountRepository.getPublicAccountInfo()?.pubKeyHex ?: error("No pubkey")
    private val signer = accountRepository.getSigningLambda() ?: error("No signing key")

    // Assuming 'conversationId' and 'partnerPubkeys' are passed as navigation arguments.
    // The keys must match what's defined in your navigation graph.
    private val conversationId: String = savedStateHandle["conversationId"] ?: error("conversationId not found in navigation arguments")
    private val partnerPubkeys: List<String> = (savedStateHandle.get<String>("partnerPubkeys")?.split(',') ?: emptyList()) + pubkeyHex

    val shared = OrderViewModel(
        //pubkeyHex = pubkeyHex,
        conversationPartnerPubkeys = partnerPubkeys,
        conversationId = conversationId,
        //signer = signer,
        //broadcastRepository = BroadcastRepository(nostrRepository),
        orderRepository = OrderRepository(nostrRepository, relayRepository, accountRepository),
        relayRepository = relayRepository,
        coroutineScope = viewModelScope
    )

    val messages: StateFlow<List<OrderUiModel>> = shared.messages
    val error: StateFlow<String?> = shared.error
    val isLoading: StateFlow<Boolean> = shared.isLoading
    val isInitialLoad: StateFlow<Boolean> = shared.isInitialLoad
    //val eoseHit: StateFlow<Boolean> = shared.eoseHit

    /*
    fun subscribeToMessages() {
        shared.subscribeToMessages()
    }
    */

//    fun sendMessage(text: String) {
//        shared.sendMessage(text)
//    }

    fun sendKind1059Message(text: String) {
        shared.sendKind1059Message(text)
    }

    fun clearError() {
        shared.clearError()
    }

    fun deleteMessage(messageId: String) {
        shared.deleteMessage(messageId)
    }

    fun resendMessage(messageId: String) {
        shared.resendMessage(messageId)
    }

    override fun onCleared() {
        super.onCleared()
        shared.close()
    }
}
