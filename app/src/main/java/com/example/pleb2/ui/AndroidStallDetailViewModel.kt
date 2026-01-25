package com.example.pleb2.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.nostr.NostrRepository
import com.example.data.repository.StallRepository
import com.example.data.settings.RelayRepository
import com.example.data.uiModels.StallUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import com.example.data.viewmodel.StallDetailScreenViewModel as SharedStallDetailScreenViewModel

@HiltViewModel
class AndroidStallDetailViewModel @Inject constructor(
    repository: NostrRepository,
    relayRepository: RelayRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    val shared = SharedStallDetailScreenViewModel(
        stallRepository = StallRepository(repository),
        relayRepository = relayRepository,
        coroutineScope = viewModelScope
    )
    val stallUiModel: StateFlow<StallUiModel?> = shared.stallUiModel
    val isLoading: StateFlow<Boolean> = shared.isLoading
    val error: StateFlow<String?> = shared.error

    fun fetchStallById(stallId: String) {
        shared.fetchStallById(stallId)
    }

    override fun onCleared() {
        shared.close()
        super.onCleared()
    }
}
