package com.example.pleb2.ui
import com.example.nostr.NostrRepository
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.ProductRepository
import com.example.data.uiModels.ProductUiModel
import com.example.data.repository.StallRepository
import com.example.data.settings.RelayRepository
import com.example.data.viewmodel.ProductDetailViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class AndroidProductDetailViewModel @Inject constructor(
    private val repository: NostrRepository,
    private val relayRepository: RelayRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    val shared = ProductDetailViewModel(
        repository = repository,
        productRepository = ProductRepository(repository),
        stallRepository = StallRepository(repository),
        relayRepository = relayRepository,
        coroutineScope = viewModelScope
    )
    val productUiModel: StateFlow<ProductUiModel?> = shared.productUiModel
    val isLoading: StateFlow<Boolean> = shared.isLoading

    override fun onCleared() {
        shared.close()
        super.onCleared()
    }
}
