package com.example.data.viewmodel

import com.example.data.settings.RelayRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URI
import java.net.URISyntaxException

class RelayViewModel(
    private val relayRepository: RelayRepository,
    private val coroutineScope: CoroutineScope
) {
    private val _relays = MutableStateFlow<List<String>>(emptyList())
    val relays: StateFlow<List<String>> = _relays.asStateFlow()
    private val _newRelay = MutableStateFlow("")
    val newRelay: StateFlow<String> = _newRelay.asStateFlow()
    private val _relayInputError = MutableStateFlow<String?>(null)
    val relayInputError: StateFlow<String?> = _relayInputError
    private val _relayTimeoutMs = MutableStateFlow(10000L)
    val relayTimeoutMs: StateFlow<Long> = _relayTimeoutMs.asStateFlow()

    init {
        coroutineScope.launch {
            relayRepository.relayTimeoutMsFlow.collect { timeout ->
                _relayTimeoutMs.value = timeout
            }
        }
        coroutineScope.launch {
            _relays.value = relayRepository.relaysFlow.first()
        }
    }

    fun onNewRelayChanged(value: String) {
        _newRelay.value = value
        _relayInputError.value = null
    }

    fun addRelay() {
        val relay = _newRelay.value.trim()
        if (relay.isEmpty()) {
            _relayInputError.value = "Relay URL cannot be empty."
            return
        }
        if (!isValidWssUrl(relay)) {
            _relayInputError.value = "Relay must start with wss:// and be a valid WebSocket URL."
            return
        }
        if (_relays.value.contains(relay)) {
            _relayInputError.value = "Relay already exists."
            return
        }
        _relays.update { it + relay }
        _newRelay.value = ""
        _relayInputError.value = null
        coroutineScope.launch { relayRepository.setRelays(_relays.value) }
    }

    private fun isValidWssUrl(url: String): Boolean {
        return try {
            val uri = URI(url)
            uri.scheme == "wss" && uri.host != null && uri.host.isNotBlank()
        } catch (e: URISyntaxException) {
            false
        }
    }

    fun removeRelay(relay: String) {
        _relays.update { it.filterNot { it == relay } }
        coroutineScope.launch { relayRepository.setRelays(_relays.value) }
    }

    fun saveRelays() {
        coroutineScope.launch {
            relayRepository.setRelays(_relays.value)
        }
    }

    fun setRelayTimeoutMs(timeoutMs: Long) {
        _relayTimeoutMs.value = timeoutMs
        coroutineScope.launch {
            relayRepository.setRelayTimeoutMs(timeoutMs)
        }
    }
}

