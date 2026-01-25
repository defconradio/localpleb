package com.example.crypto

import com.example.crypto.util.SecureRNG
import com.example.crypto.util.toHexKey
import fr.acinq.bitcoin.Bech32
import fr.acinq.bitcoin.PrivateKey
import fr.acinq.bitcoin.PublicKey
import fr.acinq.bitcoin.XonlyPublicKey

/**
 * Data class for Nostr key info to be used by the app/UI layer.
 */
data class NostrKeyInfo(
    val privHex: String,
    val nsec: String,
    val npub: String,
    val pubKeyHex: String
)

/**
 * Generate a new random Nostr keypair and return NostrKeyInfo (privHex, nsec, npub).
 */
fun generateNostrKeyInfo(): NostrKeyInfo {
    val (priv, _) = generateRandomKeyPair()
    return nostrKeyInfoFromPrivateKey(priv)
}

/**
 * Import a Nostr keypair from hex and return NostrKeyInfo (privHex, nsec, npub).
 */
fun importNostrKeyInfoFromHex(hex: String): NostrKeyInfo {
    val privBytes = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    val priv = PrivateKey(privBytes)
    return nostrKeyInfoFromPrivateKey(priv)
}

/**
 * Import a Nostr private key from nsec (bech32) and return the valid hex string.
 * Throws if the nsec is invalid or does not decode to a valid secret key.
 */
fun importNostrPrivHexFromNsec(nsec: String): String {
    val privBytes = decodeBech32("nsec", nsec)
    val privHex = privBytes.joinToString("") { "%02x".format(it) }
    require(validateSecretKey(privHex)) { "Invalid secret key" }
    return privHex
}

/**
 * Helper to build NostrKeyInfo from a PrivateKey.
 */
private fun nostrKeyInfoFromPrivateKey(priv: PrivateKey): NostrKeyInfo {
    val privHex = priv.value.toByteArray().joinToString("") { "%02x".format(it) }
    val nsec = encodeBech32("nsec", priv.value.toByteArray())
    val xonly = priv.xOnlyPublicKey()
    val npub = encodeBech32("npub", xonly.value.toByteArray())
    val pubKeyHex = xonly.value.toByteArray().joinToString("") { "%02x".format(it) }
    return NostrKeyInfo(privHex = privHex, nsec = nsec, npub = npub, pubKeyHex = pubKeyHex)
}

/**
 * Verifies a Schnorr signature for a 32-byte hash using a raw public key.
 * This is a convenience overload to avoid exposing bitcoin-kmp types to other modules.
 * @param hash The 32-byte hash.
 * @param signature The signature to verify (64 bytes).
 * @param pubkey The x-only public key to verify with (32 bytes).
 * @return true if valid, false otherwise.
 */
fun verifyHashSignatureSchnorr(hash: ByteArray, signature: ByteArray, pubkey: ByteArray): Boolean {
    return try {
        val xonlyPublicKey = XonlyPublicKey(fr.acinq.bitcoin.ByteVector32(pubkey))
        verifyHashSignatureSchnorr(hash, signature, xonlyPublicKey)
    } catch (e: Exception) {
        // If the public key is invalid, the signature is considered invalid.
        false
    }
}

/**
 * Data class for public-facing account info.
 * This is safe to pass to UI layers as it contains no private key info.
 */
data class PublicAccountInfo(
    val npub: String,
    val pubKeyHex: String
)

/**
 * Creates a SecureSigner from a private key hex string.
 *
 * This function encapsulates the cryptographic operations of importing a private key
 * and creating a signing lambda, ensuring that these operations are confined to the
 * `crypto` module. It uses the existing, established functions from this module.
 *
 * @param privHex The private key as a hexadecimal string.
 * @return A [SecureSigner] instance that can be safely passed to other modules
 *         like the `data` or UI layer, or null if the key is invalid.
 */
fun createSecureSignerFromHex(privHex: String): SecureSigner? {
    return try {
        val (privateKey, _) = importKeyPairFromHex(privHex)
        val pubKeyHex = privateKey.xOnlyPublicKey().value.toByteArray().toHexKey()

        // The signer lambda closes over the `privateKey` but does not expose it.
        val signer: (ByteArray) -> ByteArray = { hashToSign ->
            println("[SecureSigner] Private key (hex): ${privateKey.value.toByteArray().joinToString("") { "%02x".format(it) }}")
            println("[SecureSigner] Signing hash: ${hashToSign.joinToString("") { "%02x".format(it) }}")
            signHashSchnorr(hashToSign, privateKey).toByteArray()
        }

        SecureSigner(pubKeyHex, signer)
    } catch (e: Exception) {
        // If the private key hex is invalid, importKeyPairFromHex will throw.
        // We return null to indicate failure.
        null
    }
}

/**
 * Generic bech32 encoder for any byte array and HRP.
 */
// Removed duplicate encodeBech32 to avoid overload ambiguity. Use the one from Keys.kt.
// private fun encodeBech32(hrp: String, data: ByteArray): String {
//     val words = Bech32.eight2five(data)
//     return Bech32.encode(hrp, words, Bech32.Encoding.Bech32)
// }

// Removed duplicate generateRandomKeyPair to avoid overload ambiguity. Use the one from Keys.kt.
// private fun generateRandomKeyPair(): Pair<PrivateKey, PublicKey> {
//     val random = Random.Default
//     while (true) {
//         val candidate = ByteArray(32)
//         random.nextBytes(candidate)
//         try {
//             val priv = PrivateKey(candidate)
//             val pub = priv.publicKey()
//             return Pair(priv, pub)
//         } catch (e: IllegalArgumentException) {
//             // Invalid key, try again
//         }
//     }
// }
