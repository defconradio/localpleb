package com.example.data.viewmodel

import com.example.crypto.NostrKeyInfo
import com.example.crypto.PublicAccountInfo
import com.example.crypto.generateNostrKeyInfo
import com.example.crypto.importNostrKeyInfoFromHex
import com.example.crypto.importNostrPrivHexFromNsec
import com.example.data.settings.AccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class OnboardingViewModel(
    private val accountRepository: AccountRepository
) {
    private val _hasKey = MutableStateFlow(false)
    val hasKey: StateFlow<Boolean> = _hasKey

    private val _accountInfo = MutableStateFlow<NostrKeyInfo?>(null)
    val accountInfo: StateFlow<NostrKeyInfo?> = _accountInfo

    private val _publicAccountInfo = MutableStateFlow<PublicAccountInfo?>(null)
    val publicAccountInfo: StateFlow<PublicAccountInfo?> = _publicAccountInfo

    private val _isPrivateKeyVisible = MutableStateFlow(false)
    val isPrivateKeyVisible: StateFlow<Boolean> = _isPrivateKeyVisible

    private val _accounts = MutableStateFlow<List<PublicAccountInfo>>(emptyList())
    val accounts: StateFlow<List<PublicAccountInfo>> = _accounts

    init {
        checkKey()
        loadPublicAccountInfo()
        loadAccounts()
    }

    fun checkKey() {
        _hasKey.value = accountRepository.hasKeyPair()
    }

    fun loadPublicAccountInfo() {
        _publicAccountInfo.value = accountRepository.getPublicAccountInfo()
    }

    fun loadAccounts() {
        _accounts.value = accountRepository.listAccounts()
    }

    fun switchAccount(pubKeyHex: String) {
        accountRepository.switchAccount(pubKeyHex)
        loadPublicAccountInfo()
        hidePrivateKey()
        loadAccounts()
    }

    fun revealPrivateKey() {
        _accountInfo.value = accountRepository.getKeyInfo()
        _isPrivateKeyVisible.value = true
    }

    fun hidePrivateKey() {
        _isPrivateKeyVisible.value = false
        _accountInfo.value = null
    }

    fun saveKey(hex: String) {
        accountRepository.saveKey(hex)
        _hasKey.value = true
        loadAccounts()
        loadPublicAccountInfo()
    }

    fun deleteKey() {
        accountRepository.deleteKey()
        _hasKey.value = accountRepository.hasKeyPair()
        _accountInfo.value = null
        _publicAccountInfo.value = accountRepository.getPublicAccountInfo()
        loadAccounts()
    }

    fun getKeyInfo(): NostrKeyInfo? {
        return accountRepository.getKeyInfo()
    }

    fun generateKeyForUi(): NostrKeyInfo {
        return generateNostrKeyInfo()
    }

    fun importKeyForUi(hex: String): NostrKeyInfo? {
        return try {
            importNostrKeyInfoFromHex(hex)
        } catch (e: Exception) {
            null
        }
    }

    fun importKeyFromNsec(nsec: String): Boolean {
        return try {
            val privHex = importNostrPrivHexFromNsec(nsec)
            saveKey(privHex)
            true
        } catch (e: Exception) {
            false
        }
    }
}
