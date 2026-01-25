package com.example.pleb2.ui

import androidx.lifecycle.ViewModel
import com.example.nostr.NostrRepository
import com.example.data.repository.ProductRepository
import com.example.data.settings.RelayRepository // KMP MIGRATION: Use shared interface
import com.example.data.uiModels.ProductUiModel
import com.example.data.viewmodel.FilterState
import com.example.data.viewmodel.ProductListNavigationEvent
import com.example.data.viewmodel.ProductListUiEffect
import com.example.data.viewmodel.ProductListViewModel as SharedProductListViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AndroidProductListViewModel @Inject constructor(
    nostrRepository: NostrRepository,
    relayRepository: RelayRepository
) : ViewModel() {
    // Shared KMP ViewModel instance
    private val shared: SharedProductListViewModel = SharedProductListViewModel(
        //nostrRepository = nostrRepository,
        productRepository = ProductRepository(nostrRepository),
        relayRepository = relayRepository
    )

    val filterState: StateFlow<FilterState> = shared.filterState
    val showTagSuggestions: StateFlow<Boolean> = shared.showTagSuggestions
    val tagSuggestions: StateFlow<List<String>> = shared.tagSuggestions
    val renderedCount: StateFlow<Int> = shared.renderedCount
    val isLoading: StateFlow<Boolean> = shared.isLoading
    val productUiModels: StateFlow<List<ProductUiModel>> = shared.productUiModels
    val error: StateFlow<String?> = shared.error
    val uiEffect: SharedFlow<ProductListUiEffect> = shared.uiEffect
    val navigationEvent: SharedFlow<ProductListNavigationEvent> = shared.navigationEvent

    fun onSearchTextChanged(text: String) = shared.onSearchTextChanged(text)
    fun onSearchSubmitted(text: String) = shared.onSearchSubmitted(text)
    fun updateSelectedTag(tag: String) = shared.updateSelectedTag(tag)
    fun resetFilters() = shared.resetFilters()
    fun hideSuggestions() = shared.hideSuggestions()
    fun onScroll(firstVisibleItemIndex: Int, firstVisibleItemScrollOffset: Int, visibleItemsCount: Int, totalItemsCount: Int) =
        shared.onScroll(firstVisibleItemIndex, firstVisibleItemScrollOffset, visibleItemsCount, totalItemsCount)
    fun onProductClicked(eventId: String) = shared.onProductClicked(eventId)
    fun reportVerificationError(eventId: String) = shared.reportVerificationError(eventId)
    fun reloadProducts() = shared.reloadProducts()
    fun clearError() = shared.clearError()


    override fun onCleared() {
        shared.close()
        super.onCleared()
    }
}
