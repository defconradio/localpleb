package com.example.data.viewmodel

import com.example.data.repository.ProductRepository
import com.example.data.settings.RelayRepository
import com.example.data.uiModels.ProductUiModel
//import com.example.nostr.NostrRepository
//import com.example.nostr.models.NostrEnvelope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

data class FilterState(
    val searchText: String = "",
    val selectedTag: String = "All"
)

sealed class ProductListUiEffect {
    object ClearFocus : ProductListUiEffect()
    object ScrollToTop : ProductListUiEffect()
}

sealed class ProductListNavigationEvent {
    data class NavigateToProductDetail(val event_id: String) : ProductListNavigationEvent()
}

class ProductListViewModel(
    private val productRepository: ProductRepository,
    private val relayRepository: RelayRepository,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val _tags = MutableStateFlow<Set<String>>(emptySet())

    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState

    private val _tagSuggestions = MutableStateFlow<List<String>>(emptyList())
    val tagSuggestions: StateFlow<List<String>> = _tagSuggestions

    private val _showTagSuggestions = MutableStateFlow(false)
    val showTagSuggestions: StateFlow<Boolean> = _showTagSuggestions

    private val _renderedCount = MutableStateFlow(20)
    val renderedCount: StateFlow<Int> = _renderedCount

    private val pageSize = 20
    private var lastScrollIndex = 0
    private var lastScrollOffset = 0

    private val _navigationEvent = MutableSharedFlow<ProductListNavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    private val _uiEffect = MutableSharedFlow<ProductListUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    private val _productUiModels = MutableStateFlow<List<ProductUiModel>>(emptyList())
    val productUiModels: StateFlow<List<ProductUiModel>> = _productUiModels

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        coroutineScope.launch {
            _isLoading.value = true
            relayRepository.relaysFlow
                .flatMapLatest { relays ->
                    // When the relay list changes, flatMapLatest automatically
                    // cancels the old subscription and starts a new one.
                    productRepository.getProducts(relays)
                }
                .onEach { productData ->
                    // Update loading state and tags based on the latest data from the repo.
                    // This runs for every emission.
                    //if (productData.products.isNotEmpty()) {
                    //    _isLoading.value = false
                    //}
                    _tags.value = productData.tags
                }
                .combine(filterState) { productData, filter ->
                    // Combine the latest product data with the latest filter state.
                    val search = filter.searchText.trim().lowercase()
                    val tag = filter.selectedTag

                    if (search.isEmpty() && tag == "All") {
                        productData.products
                    } else {
                        productData.products.filter { product ->
                            val name = product.name.lowercase()
                            val description = product.description?.lowercase() ?: ""
                            val eventTags = product.tags.filter { it.size > 1 && it[0] == "t" }.map { it[1].lowercase() }

                            val tagMatch = if (tag != "All") eventTags.any { it.contains(tag, ignoreCase = true) } else true
                            val textMatch = name.contains(search) || description.contains(search) || eventTags.any { it.contains(search) }

                            if (tag != "All") {
                                tagMatch && textMatch
                            } else {
                                textMatch
                            }
                        }
                    }
                }
                // .debounce(300) // Wait for 300ms of silence before emitting the latest list
                .catch { e ->
                    println("ProductListViewModel.kt - Error in data flow: ${e.message}")
                    _error.value = e.message ?: "Unknown error in data flow."
                    _isLoading.value = false
                }
                .collectLatest { filteredProducts ->
                    // Update the UI with the final, filtered list.
                    _productUiModels.value = filteredProducts
                    _isLoading.value = false
                    _renderedCount.value = pageSize
                }
        }
    }

    fun onProductClicked(event_id: String) { // CRITICAL CHANGE: now uses event.id instead of product_id
        coroutineScope.launch {
            _navigationEvent.emit(ProductListNavigationEvent.NavigateToProductDetail(event_id))
            // _navigationEvent.emit(ProductListNavigationEvent.NavigateToProductDetail(productId)) // Old: used product_id
        }
    }

    fun onScroll(firstVisibleItemIndex: Int, firstVisibleItemScrollOffset: Int, visibleItemsCount: Int, totalItemsCount: Int) {
        lastScrollIndex = firstVisibleItemIndex
        lastScrollOffset = firstVisibleItemScrollOffset
        val lastVisibleIndex = firstVisibleItemIndex + visibleItemsCount
        if (lastVisibleIndex >= _renderedCount.value && _renderedCount.value < totalItemsCount) {
            _renderedCount.value += pageSize
        }
    }

    fun reloadProducts() {
        coroutineScope.launch {
            val relays = relayRepository.relaysFlow.first()
            //startProductSubscription(relays)
        }
    }

    fun updateSelectedTag(tag: String) {
        _filterState.value = FilterState(searchText = tag, selectedTag = tag)
        _showTagSuggestions.value = false
        // Scrolling is now handled in the UI.
    }

    fun onSearchTextChanged(value: String) {
        if (value.isEmpty()) {
            // When search text is cleared, reset both search and tag filters.
            _filterState.value = FilterState(searchText = "", selectedTag = "All")
            _showTagSuggestions.value = false
        } else {
            // Otherwise, just update the search text and fetch new suggestions.
            _filterState.value = _filterState.value.copy(searchText = value)
            updateTagSuggestions()
            _showTagSuggestions.value = _tagSuggestions.value.isNotEmpty()
        }
    }

    fun hideSuggestions() {
        _showTagSuggestions.value = false
    }

    fun onSearchSubmitted(value: String) {
        _filterState.value = _filterState.value.copy(selectedTag = value)
        _showTagSuggestions.value = false
    }

    private fun updateTagSuggestions() {
        val search = _filterState.value.searchText
        val tag = _filterState.value.selectedTag
        val suggestions = if (search.isNotEmpty()) {
            _tags.value.filter { it.contains(search, ignoreCase = true) && it != tag }.sorted()
        } else emptyList()
        _tagSuggestions.value = suggestions
    }

    fun resetFilters() {
        _filterState.value = FilterState()
    }

    fun clearError() {
        _error.value = null
    }

    fun clearFocus() {
        coroutineScope.launch {
            _uiEffect.emit(ProductListUiEffect.ClearFocus)
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

    fun setTagsForTesting(tags: Set<String>) {
        coroutineScope.launch {
            _tags.value = tags
        }
    }

    fun simulateNetworkError() {
        coroutineScope.launch {
            _error.value = "Simulated network error"
        }
    }

    fun clearProducts() {
        coroutineScope.launch {
            _productUiModels.value = emptyList()
        }
    }

    fun close() {
        println("ProductListViewModel.kt - close() called")
        coroutineScope.cancel()
    }
}

sealed class ProductListEvent {
    data class SearchTextChanged(val text: String) : ProductListEvent()
    data class TagSelected(val tag: String) : ProductListEvent()
    data class ProductClicked(val event_id: String) : ProductListEvent()
    object LoadMore : ProductListEvent()
    object ClearFocus : ProductListEvent()
}
