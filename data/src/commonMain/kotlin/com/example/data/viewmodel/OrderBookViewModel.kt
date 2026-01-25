package com.example.data.viewmodel

import com.example.data.repository.OrderRepository
import com.example.data.uiModels.OrderUiModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

class OrderBookViewModel(
    private val orderRepository: OrderRepository,
    coroutineScope: CoroutineScope
) {
    private val viewModelScope = coroutineScope

    // Raw list of conversations from the repository
    private val _orderList = MutableStateFlow<List<OrderUiModel>>(emptyList())

    // Search-related state
    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    // The final list of orders to be displayed, filtered by the search text
    val filteredOrderList: StateFlow<List<OrderUiModel>> = searchText
        .combine(_orderList) { text, orders ->
            if (text.isBlank()) {
                orders
            } else {
                orders.filter { order ->
                    order.content.contains(text, ignoreCase = true) ||
                            order.pubkey.contains(text, ignoreCase = true)
                }
            }
        }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // UI state for loading indicators
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // UI state for error messages
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        _isLoading.value = true
        _error.value = null

        orderRepository.getConversations()
            .debounce(400L)
            .onEach { conversations ->
                println("[OrderBookViewModel] RReceived ${conversations.size} conversations from repository.")
                _orderList.value = conversations
                // Always set loading to false after the first emission.
                if (_isLoading.value) {
                    _isLoading.value = false
                }
            }
            .catch { e ->
                _error.value = "Subscription failed: ${e.message}"
                _isLoading.value = false
            }
            .launchIn(viewModelScope)
    }

    fun updateSearchText(newText: String) {
        _searchText.value = newText
    }

    fun clearError() {
        _error.value = null
    }

    fun close() {
        println("OrderBookViewModel.kt - close() called")
        viewModelScope.cancel()
    }
}
