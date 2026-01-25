package com.example.data.settings

import com.example.crypto.NostrKeyInfo
import com.example.crypto.PublicAccountInfo
import com.example.crypto.SecureSigner
import com.example.nostr.models.NostrEnvelope
import kotlinx.coroutines.flow.StateFlow

interface AccountRepository {
    fun hasKeyPair(): Boolean
    fun getKeyInfo(): NostrKeyInfo? // This will be used for backup/export only
    fun getPublicAccountInfo(): PublicAccountInfo?
    fun getSecureSigner(): SecureSigner?
    fun saveKey(hex: String)
    fun deleteKey()
    fun switchAccount(pubKeyHex: String)
    fun listAccounts(): List<PublicAccountInfo>
    fun getSigningLambda(): ((ByteArray) -> ByteArray)?
    //suspend fun decryptGiftWrap(giftWrap: NostrEnvelope): NostrEnvelope?
    suspend fun decryptGiftWrap(giftWrap: NostrEnvelope): NostrEnvelope?

    fun createMessageKind1059Event(
        participant: String,
        payloadEvent: NostrEnvelope
    ): NostrEnvelope

    val publicAccountInfoFlow: StateFlow<PublicAccountInfo?>

    fun generateRandomKeyPair(): Pair<ByteArray, ByteArray>
}
