package com.example.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RelayRepositoryImpl(
    private val context: Context,
    private val dataStore: DataStore<Preferences>
) : RelayRepository {
    companion object {
        private val RELAYS_KEY = stringSetPreferencesKey("relays")
        private val TIMEOUT_KEY = longPreferencesKey("relay_timeout_ms")
        private const val DEFAULT_TIMEOUT_MS = 10000L // 10 seconds default
    }

    override val relaysFlow: Flow<List<String>> = dataStore.data.map { prefs ->
        // ADDED LOGGING to debug re-subscriptions
        println("!!! RelayRepositoryImpl: dataStore emitted. Creating new relay list.")
        (prefs[RELAYS_KEY] ?: DEFAULT_RELAYS.toSet()).toList()
    }

    override val relayTimeoutMsFlow: Flow<Long> = dataStore.data.map { prefs ->
        prefs[TIMEOUT_KEY] ?: DEFAULT_TIMEOUT_MS
    }

    override suspend fun setRelays(relays: List<String>) {
        dataStore.edit { prefs ->
            prefs[RELAYS_KEY] = relays.toSet()
        }
    }

    override suspend fun setRelayTimeoutMs(timeoutMs: Long) {
        dataStore.edit { prefs ->
            prefs[TIMEOUT_KEY] = timeoutMs
        }
    }
}
