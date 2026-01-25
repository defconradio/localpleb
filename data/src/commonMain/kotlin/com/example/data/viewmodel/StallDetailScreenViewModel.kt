package com.example.data.viewmodel

import com.example.data.repository.StallRepository
import com.example.data.settings.RelayRepository
import com.example.data.uiModels.StallUiModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Job

class StallDetailScreenViewModel(
    private val stallRepository: StallRepository,
    private val relayRepository: RelayRepository,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val _stallUiModel = MutableStateFlow<StallUiModel?>(null)
    val stallUiModel: StateFlow<StallUiModel?> = _stallUiModel

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var fetchJob: Job? = null
    private var fetchedStallId: String? = null

    fun fetchStallById(stallId: String) {
        // Guard against re-fetching the same stall
        if (fetchedStallId == stallId) return

        fetchJob?.cancel()
        fetchedStallId = stallId

        fetchJob = coroutineScope.launch {
            _isLoading.value = true
            _error.value = null
            _stallUiModel.value = null // Reset on new fetch
            try {
                val relays = relayRepository.relaysFlow.first()
                stallRepository.getStallsByDTag(relays, stallId)
                    .collect { stallData ->
                        stallData.stalls.firstOrNull()?.let { event ->
                            stallRepository.mapToStallUiModel(event)?.let { stall ->
                                _stallUiModel.value = stall
                            }
                        }
                        // Correctly handle loading state based on EOSE
                        if (stallData.eoseHit) {
                            _isLoading.value = false
                        }
                    }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun close() {
        coroutineScope.cancel()
    }
}
