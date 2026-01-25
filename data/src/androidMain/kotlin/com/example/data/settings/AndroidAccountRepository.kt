package com.example.data.settings

import android.content.Context
import com.example.crypto.NostrKeyInfo
import com.example.crypto.PublicAccountInfo
import com.example.crypto.SecureSigner
import com.example.crypto.createSecureSignerFromHex
import com.example.crypto.importNostrKeyInfoFromHex
import com.example.data.KeyManager
import com.example.data.util.Nip17Helpers
import com.example.data.util.sayHello
import com.example.nostr.models.NostrEnvelope
import com.example.crypto.generateRandomKeyPairBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class AndroidAccountRepository(private val context: Context) : AccountRepository {
    private val _publicAccountInfoFlow = MutableStateFlow(getPublicAccountInfo())
    override val publicAccountInfoFlow: StateFlow<PublicAccountInfo?> = _publicAccountInfoFlow.asStateFlow()
    
    override fun hasKeyPair(): Boolean {
        return KeyManager.hasAccounts(context)
    }

    override fun getKeyInfo(): NostrKeyInfo? {
        val privHex = KeyManager.loadActivePrivateKey(context) ?: return null
        return try {
            importNostrKeyInfoFromHex(privHex)
        } catch (e: Exception) {
            null
        }
    }

    override fun getPublicAccountInfo(): PublicAccountInfo? {
        val privHex = KeyManager.loadActivePrivateKey(context) ?: return null
        return try {
            val keyInfo = importNostrKeyInfoFromHex(privHex)
            PublicAccountInfo(
                npub = keyInfo.npub,
                pubKeyHex = keyInfo.pubKeyHex
            )
        } catch (e: Exception) {
            null
        }
    }

    override fun getSecureSigner(): SecureSigner? {
        val privHex = KeyManager.loadActivePrivateKey(context) ?: return null
        return try {
            createSecureSignerFromHex(privHex)
        } catch (e: Exception) {
            // Handle potential errors during key import
            null
        }
    }

    override fun saveKey(hex: String) {
        KeyManager.addAccount(context, hex)
        val newKeyInfo = importNostrKeyInfoFromHex(hex)
        KeyManager.switchAccount(context, newKeyInfo.pubKeyHex)
        _publicAccountInfoFlow.value = getPublicAccountInfo()
    }

    override fun deleteKey() {
        getKeyInfo()?.pubKeyHex?.let {
            KeyManager.deleteAccount(context, it)
            _publicAccountInfoFlow.value = getPublicAccountInfo()
        }
    }

    override fun switchAccount(pubKeyHex: String) {
        KeyManager.switchAccount(context, pubKeyHex)
        _publicAccountInfoFlow.value = getPublicAccountInfo()
    }

    override fun listAccounts(): List<PublicAccountInfo> {
        return KeyManager.listAccountPubKeys(context).mapNotNull { pubKeyHex ->
            // This is inefficient, but we don't have a direct way to get public info from pubkey
            // A better implementation would store public info alongside the private key
            val accountsJson = KeyManager.getSharedPreferences(context).getString("nostr_accounts", null)
            val accounts = accountsJson?.let { Json.decodeFromString<Map<String, String>>(it) }
            accounts?.get(pubKeyHex)?.let { privHex ->
                importNostrKeyInfoFromHex(privHex).let { keyInfo ->
                    PublicAccountInfo(npub = keyInfo.npub, pubKeyHex = keyInfo.pubKeyHex)
                }
            }
        }
    }

    /**
     * Returns a lambda suitable for passing to the shared ViewModel for secure event signing.
     * The lambda loads the private key at signing time and signs the provided hash using SecureSigner.
     * The private key is never exposed to shared code.
     */
    override fun getSigningLambda(): ((ByteArray) -> ByteArray)? {
        val repo = this
        return if (hasKeyPair()) {
            { hash ->
                val signer = repo.getSecureSigner()
                    ?: throw IllegalStateException("No private key available for signing")
                signer.signer(hash)
            }
        } else {
            null
        }
    }

    override fun generateRandomKeyPair(): Pair<ByteArray, ByteArray> {
        return generateRandomKeyPairBytes()
    }
    
    override fun createMessageKind1059Event(
        participant: String,
        payloadEvent: NostrEnvelope
    ): NostrEnvelope {
        val privHex = KeyManager.loadActivePrivateKey(context) ?: error("No private key available")
        val privateKeyBytes = privHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        return Nip17Helpers.createNip17Events(
            senderPriv = privateKeyBytes,
            participant = participant,
            payloadEvent = payloadEvent
        )
    }

    override suspend fun decryptGiftWrap(giftWrap: NostrEnvelope): NostrEnvelope? {
        val privHex = KeyManager.loadActivePrivateKey(context) ?: return null
        val privateKeyBytes = privHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        return Nip17Helpers.handleKind1059EventFull(
            kind1059Envelope = giftWrap,
            recipientPrivateKey = privateKeyBytes
        )
    }


//    override suspend fun decryptGiftWrap(giftWrap: NostrEnvelope): NostrEnvelope? {
//        return withContext(Dispatchers.IO) {
//            val privHex = KeyManager.loadPrivateKey(context) ?: return@withContext null
//            val privateKeyBytes = privHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
//            Nip17Helpers.handleKind1059EventFull(
//                kind1059Envelope = giftWrap,
//                recipientPrivateKey = privateKeyBytes
//            )
//        }
//    }
}
