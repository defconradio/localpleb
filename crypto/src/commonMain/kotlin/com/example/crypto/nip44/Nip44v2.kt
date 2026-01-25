package com.example.crypto.nip44

import com.example.crypto.importPublicKeyFromXOnlyHex
import com.example.crypto.util.Base64
import com.example.crypto.util.SecureRNG
import fr.acinq.bitcoin.PrivateKey
import fr.acinq.bitcoin.PublicKey
import kotlin.math.floor
import kotlin.math.log2

class Nip44v2 {
    private val hkdf = Hkdf()
    private val saltPrefix = "nip44-v2".toByteArray(Charsets.UTF_8)
    private val hashLength = 32
    private val minPlaintextSize: Int = 0x0001 // 1b msg => padded to 32b
    private val maxPlaintextSize: Int = 0xffff // 65535 (64kb-1) => padded to 64kb

    fun encrypt(
        msg: String,
        privateKey: ByteArray,
        pubKey: ByteArray,
    ): EncryptedInfo = encrypt(msg, computeConversationKey(
        PrivateKey(privateKey),
        PublicKey(pubKey)
    ))

    fun encrypt(
        plaintext: String,
        conversationKey: ByteArray,
    ): EncryptedInfo {
        val nonce = SecureRNG.nextBytes(hashLength)
        return encryptWithNonce(plaintext, conversationKey, nonce)
    }

    fun encryptWithNonce(
        plaintext: String,
        conversationKey: ByteArray,
        nonce: ByteArray,
    ): EncryptedInfo {
        val messageKeys = getMessageKeys(conversationKey, nonce)
        val padded = pad(plaintext)
        val ciphertext = ChaCha20(messageKeys.chachaKey, messageKeys.chachaNonce).encrypt(padded)
        val mac = hmacAad(messageKeys.hmacKey, ciphertext, nonce)
        return EncryptedInfo(
            nonce = nonce,
            ciphertext = ciphertext,
            mac = mac,
        )
    }

    fun decrypt(
        payload: String,
        privateKey: ByteArray,
        pubKey: ByteArray,
    ): EncryptedInfo = decrypt(payload, computeConversationKey(
        PrivateKey(privateKey),
        PublicKey(pubKey)
    ))

    fun decrypt(
        payload: String,
        conversationKey: ByteArray,
    ): EncryptedInfo {
        val decoded = EncryptedInfo.decodePayload(payload)
        return decrypt(decoded, conversationKey)
    }

    fun decrypt(
        decoded: EncryptedInfo,
        conversationKey: ByteArray,
    ): EncryptedInfo {
        val messageKey = getMessageKeys(conversationKey, decoded.nonce)
        val calculatedMac = hmacAad(messageKey.hmacKey, decoded.ciphertext, decoded.nonce)
        check(calculatedMac.contentEquals(decoded.mac)) {
            "Invalid Mac: Calculated ..."
        }
        // KMP compatible ChaCha20 decryption
        val padded = ChaCha20(messageKey.chachaKey, messageKey.chachaNonce).encrypt(decoded.ciphertext)
        return EncryptedInfo(
            nonce = decoded.nonce,
            ciphertext = padded,
            mac = decoded.mac,
        )
    }

    fun calcPaddedLen(len: Int): Int {
        check(len > 0) { "expected positive integer" }
        if (len <= 32) return 32
        val nextPower = 1 shl (floor(log2(len - 1f)) + 1).toInt()
        val chunk = if (nextPower <= 256) 32 else nextPower / 8
        return chunk * (floor((len - 1f) / chunk).toInt() + 1)
    }

    private fun pad(plaintext: String): ByteArray {
        val unpadded = plaintext.toByteArray(Charsets.UTF_8)
        val unpaddedLen = unpadded.size
        check(unpaddedLen > 0) { "Message is empty ($unpaddedLen): $plaintext" }
        check(unpaddedLen <= maxPlaintextSize) { "Message is too long ($unpaddedLen): $plaintext" }
        // Manual 2-byte big-endian length prefix
        val prefix = byteArrayOf(
            ((unpaddedLen shr 8) and 0xFF).toByte(),
            (unpaddedLen and 0xFF).toByte()
        )
        val paddedLen = calcPaddedLen(unpaddedLen)
        val suffix = ByteArray(paddedLen - unpaddedLen)
        return prefix + unpadded + suffix
    }

    fun unpad(padded: ByteArray): String {
        val unpaddedLen = ((padded[0].toInt() and 0xFF) shl 8) or (padded[1].toInt() and 0xFF)
        val unpadded = padded.sliceArray(2 until 2 + unpaddedLen)
        check(
            unpaddedLen in minPlaintextSize..maxPlaintextSize &&
                unpadded.size == unpaddedLen &&
                padded.size == 2 + calcPaddedLen(unpaddedLen)
        ) { "invalid padding ${unpadded.size} != $unpaddedLen" }
        return unpadded.decodeToString()
    }

    private fun bytesToInt(
        byte1: Byte,
        byte2: Byte,
        bigEndian: Boolean,
    ): Int =
        if (bigEndian) {
            (byte1.toInt() and 0xFF shl 8 or (byte2.toInt() and 0xFF))
        } else {
            (byte2.toInt() and 0xFF shl 8 or (byte1.toInt() and 0xFF))
        }

    fun hmacAad(
        key: ByteArray,
        message: ByteArray,
        aad: ByteArray,
    ): ByteArray {
        check(aad.size == hashLength) {
            "AAD associated data must be 32 bytes, but it was ${aad.size} bytes"
        }

        return hkdf.extract(aad + message, key)
    }

    fun getMessageKeys(
        conversationKey: ByteArray,
        nonce: ByteArray,
    ): MessageKey {
        val keys = hkdf.expand(conversationKey, nonce, 76)
        return MessageKey(
            chachaKey = keys.copyOfRange(0, 32),
            chachaNonce = keys.copyOfRange(32, 44),
            hmacKey = keys.copyOfRange(44, 76),
        )
    }

    class MessageKey(
        val chachaKey: ByteArray,
        val chachaNonce: ByteArray,
        val hmacKey: ByteArray,
    )

    fun computeConversationKey(privateKey: PrivateKey, publicKey: PublicKey): ByteArray {
        val sharedPoint = publicKey * privateKey
        val sharedX = sharedPoint.xOnly().value.toByteArray()
        return hkdf.extract(sharedX, saltPrefix)
    }
    fun computeConversationKey(privateKey: ByteArray, publicKeyHex: String): ByteArray {
        val pk = PrivateKey(privateKey)
        val pub = importPublicKeyFromXOnlyHex(publicKeyHex)
        return computeConversationKey(pk, pub)
    }

    class EncryptedInfo(
        val nonce: ByteArray,
        val ciphertext: ByteArray,
        val mac: ByteArray,
    ) {
        companion object {
            const val V: Int = 2

            fun decodePayload(payload: String): EncryptedInfo {
                check(payload.length >= 132 || payload.length <= 87472) {
                    "Invalid payload length [${payload.length}[ for $payload"
                }
                check(payload[0] != '#') { "Unknown encryption version ${payload.get(0)}" }

                return try {
                    val byteArray = Base64.decodeBytes(payload)
                    check(byteArray[0].toInt() == V)
                    EncryptedInfo(
                        nonce = byteArray.copyOfRange(1, 33),
                        ciphertext = byteArray.copyOfRange(33, byteArray.size - 32),
                        mac = byteArray.copyOfRange(byteArray.size - 32, byteArray.size),
                    )
                } catch (e: Exception) {
                    throw IllegalStateException("NIP-44v2 Unable to Parse encrypted payload: $payload", e)
                }
            }
        }

        fun encodePayload(): String =
            Base64.encode(byteArrayOf(V.toByte()) + nonce + ciphertext + mac)
    }

    /**
     * Helper to encrypt a string payload with a conversation key and return the encoded payload.
     */
    fun encryptPayload(payload: String, conversationKey: ByteArray): String {
        val encrypted = encrypt(payload, conversationKey)
        return encrypted.encodePayload()
    }
}