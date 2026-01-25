package com.example.pleb2.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.BroadcastRepository
import com.example.data.repository.OrderRepository
import com.example.data.settings.AccountRepository
import com.example.data.settings.RelayRepository
import com.example.data.uiModels.OrderUiModel
import com.example.data.viewmodel.OrderBookViewModel
import com.example.nostr.NostrRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class OrderBookNavigationEvent {
    data class NavigateToOrder(val conversationId: String, val partnerPubkeys: String) : OrderBookNavigationEvent()
}

@HiltViewModel
class AndroidOrderBookViewModel @Inject constructor(
    // The secure AccountRepository is provided by Hilt.
    accountRepository: AccountRepository,
    // Other repositories are also provided by Hilt.
    nostrRepository: NostrRepository,
    relayRepository: RelayRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    // The shared KMP ViewModel is initialized here.
    val shared = OrderBookViewModel(
        // The OrderRepository is created here and given the secure accountRepository.
        // This is where the dependency injection happens, allowing the common data module
        // to use a secure, platform-specific dependency without knowing about it directly.
        orderRepository = OrderRepository(nostrRepository, relayRepository, accountRepository),
        coroutineScope = viewModelScope
    )

    val filteredOrderList: StateFlow<List<OrderUiModel>> = shared.filteredOrderList
    val searchText: StateFlow<String> = shared.searchText
    val error: StateFlow<String?> = shared.error
    val isLoading: StateFlow<Boolean> = shared.isLoading

    private val _navigationEvent = MutableSharedFlow<OrderBookNavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    fun onOrderClicked(order: OrderUiModel) {
        viewModelScope.launch {
            val subjectTag = order.tags.find { it.size > 1 && it[0] == "subject" }?.getOrNull(1)
            val participants = (order.tags.filter { it.size > 1 && it[0] == "p" }.map { it[1] } + order.pubkey).distinct()

            val conversationId = subjectTag ?: participants.sorted().joinToString(",")
            val partnerPubkeys = participants.joinToString(",")

            _navigationEvent.emit(OrderBookNavigationEvent.NavigateToOrder(conversationId, partnerPubkeys))
        }
    }

    fun updateSearchText(newText: String) {
        shared.updateSearchText(newText)
    }

    override fun onCleared() {
        super.onCleared()
        shared.close()
    }
}
