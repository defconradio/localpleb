package com.example.pleb2.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.settings.RelayRepository
import com.example.data.viewmodel.RelayViewModel as SharedRelayViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AndroidRelayViewModel @Inject constructor(
    private val relayRepository: RelayRepository
) : ViewModel() {
    // Use the shared RelayViewModel, passing Android's viewModelScope
    private val shared = SharedRelayViewModel(relayRepository, viewModelScope)

    val relays = shared.relays
    val newRelay = shared.newRelay
    val relayInputError = shared.relayInputError
    val relayTimeoutMs = shared.relayTimeoutMs

    fun onNewRelayChanged(value: String) = shared.onNewRelayChanged(value)
    fun addRelay() = shared.addRelay()
    fun removeRelay(relay: String) = shared.removeRelay(relay)
    fun saveRelays() = shared.saveRelays()
    fun setRelayTimeoutMs(timeoutMs: Long) = shared.setRelayTimeoutMs(timeoutMs)
}
