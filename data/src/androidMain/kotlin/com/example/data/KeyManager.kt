package com.example.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.crypto.importNostrKeyInfoFromHex
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

actual object KeyManager {
    private const val PREFS_NAME = "secure_prefs"
    private const val ACCOUNTS_KEY = "nostr_accounts"
    private const val ACTIVE_ACCOUNT_KEY = "nostr_active_account"

    internal fun getSharedPreferences(context: Context): EncryptedSharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ) as EncryptedSharedPreferences
    }

    actual fun hasPrivateKey(): Boolean {
        throw NotImplementedError("Use hasAccounts(context) on Android")
    }

    actual fun savePrivateKey(hex: String) {
        throw NotImplementedError("Use addAccount(context, hex) on Android")
    }

    actual fun loadPrivateKey(): String? {
        throw NotImplementedError("Use loadActivePrivateKey(context) on Android")
    }

    actual fun deletePrivateKey() {
        throw NotImplementedError("Use deleteAccount(context, pubKeyHex) on Android")
    }

    fun hasAccounts(context: Context): Boolean {
        val prefs = getSharedPreferences(context)
        return prefs.getString(ACCOUNTS_KEY, null)?.let {
            Json.decodeFromString<Map<String, String>>(it).isNotEmpty()
        } ?: false
    }

    fun addAccount(context: Context, hex: String) {
        val keyInfo = importNostrKeyInfoFromHex(hex)
        val prefs = getSharedPreferences(context)
        val accountsJson = prefs.getString(ACCOUNTS_KEY, null)
        val accounts = accountsJson?.let {
            Json.decodeFromString<MutableMap<String, String>>(it)
        } ?: mutableMapOf()

        accounts[keyInfo.pubKeyHex] = hex
        prefs.edit().putString(ACCOUNTS_KEY, Json.encodeToString(accounts)).apply()

        // If this is the first account, make it active
        if (accounts.size == 1) {
            switchAccount(context, keyInfo.pubKeyHex)
        }
    }

    fun loadActivePrivateKey(context: Context): String? {
        val prefs = getSharedPreferences(context)
        val activePubKey = prefs.getString(ACTIVE_ACCOUNT_KEY, null) ?: return null
        val accountsJson = prefs.getString(ACCOUNTS_KEY, null)
        val accounts = accountsJson?.let {
            Json.decodeFromString<Map<String, String>>(it)
        }
        return accounts?.get(activePubKey)
    }

    fun deleteAccount(context: Context, pubKeyHex: String) {
        val prefs = getSharedPreferences(context)
        val accountsJson = prefs.getString(ACCOUNTS_KEY, null)
        val accounts = accountsJson?.let {
            Json.decodeFromString<MutableMap<String, String>>(it)
        } ?: return

        if (accounts.remove(pubKeyHex) != null) {
            prefs.edit().putString(ACCOUNTS_KEY, Json.encodeToString(accounts)).apply()
        }

        val activePubKey = prefs.getString(ACTIVE_ACCOUNT_KEY, null)
        if (activePubKey == pubKeyHex) {
            prefs.edit().remove(ACTIVE_ACCOUNT_KEY).apply()
            // Optionally, set another account as active if available
            accounts.keys.firstOrNull()?.let { newActivePubKey ->
                switchAccount(context, newActivePubKey)
            }
        }
    }

    fun switchAccount(context: Context, pubKeyHex: String) {
        val prefs = getSharedPreferences(context)
        prefs.edit().putString(ACTIVE_ACCOUNT_KEY, pubKeyHex).apply()
    }

    fun listAccountPubKeys(context: Context): List<String> {
        val prefs = getSharedPreferences(context)
        val accountsJson = prefs.getString(ACCOUNTS_KEY, null)
        return accountsJson?.let {
            Json.decodeFromString<Map<String, String>>(it).keys.toList()
        } ?: emptyList()
    }
}
