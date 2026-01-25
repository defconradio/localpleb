package com.example.crypto

import fr.acinq.bitcoin.Bech32
import fr.acinq.bitcoin.PrivateKey
import fr.acinq.bitcoin.PublicKey
import fr.acinq.bitcoin.ByteVector32
import fr.acinq.bitcoin.ByteVector64
import fr.acinq.bitcoin.Crypto
import fr.acinq.bitcoin.XonlyPublicKey
import com.example.crypto.util.SecureRNG

/**
 * Generates a new random valid secp256k1 key pair using bitcoin-kmp high-level API.
 * @return Pair of PrivateKey and PublicKey
 */
fun generateRandomKeyPair(): Pair<PrivateKey, PublicKey> {
    while (true) {
        val candidate = SecureRNG.nextBytes(32)
        try {
            val priv = PrivateKey(candidate)
            val pub = priv.publicKey()
            return Pair(priv, pub)
        } catch (e: IllegalArgumentException) {
            // Invalid key, try again
        }
    }
}
fun generateRandomKeyPairBytes(): Pair<ByteArray, ByteArray> {
    while (true) {
        val candidate = SecureRNG.nextBytes(32)
        try {
            val priv = PrivateKey(candidate)
            val pub = priv.publicKey()
            return Pair(priv.value.toByteArray(), pub.value.toByteArray())
        } catch (e: IllegalArgumentException) {
            // Invalid key, try again
        }
    }
}
/**
 * Imports a private key from a hex string and returns the PrivateKey and PublicKey.
 * Throws if the key is invalid.
 */
fun importKeyPairFromHex(hex: String): Pair<PrivateKey, PublicKey> {
    val privBytes = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    val priv = PrivateKey(privBytes)
    val pub = priv.publicKey()
    return Pair(priv, pub)
}

/**
 * Encodes a 32-byte Nostr key (private or public) to bech32 with the given HRP (e.g., "nsec" or "npub").
 * Uses Bech32.eight2five from bitcoin-kmp for 8-to-5 bit conversion.
 */
fun encodeNostrBech32(hrp: String, key: ByteArray): String {
    val words = Bech32.eight2five(key)
    return Bech32.encode(hrp, words, Bech32.Encoding.Bech32)
}

/**
 * Generic bech32 encoder for any byte array and HRP.
 */
fun encodeBech32(hrp: String, data: ByteArray): String {
    val words = Bech32.eight2five(data)
    return Bech32.encode(hrp, words, Bech32.Encoding.Bech32)
}

/**
 * Signs an arbitrary string message using Schnorr signature (BIP-340).
 * This function first hashes the message and then signs the hash.
 * @param message The message to sign (as String).
 * @param privateKey The PrivateKey to sign with.
 * @return The signature as ByteVector64 (64 bytes).
 */
fun signStringSchnorr(message: String, privateKey: PrivateKey): ByteVector64 {
    val messageHash = ByteVector32(Crypto.sha256(message.encodeToByteArray()))
    return Crypto.signSchnorr(messageHash, privateKey, Crypto.SchnorrTweak.NoTweak)
}

/**
 * Signs a 32-byte hash using Schnorr signature (BIP-340).
 * This is the function to use for signing Nostr events, where the event ID is the hash.
 * @param hash The 32-byte hash to sign.
 * @param privateKey The PrivateKey to sign with.
 * @return The signature as ByteVector64 (64 bytes).
 */
fun signHashSchnorr(hash: ByteArray, privateKey: PrivateKey): ByteVector64 {
    val messageHash = ByteVector32(hash)
    return Crypto.signSchnorr(messageHash, privateKey, Crypto.SchnorrTweak.NoTweak)
}
fun signHashSchnorr(hash: ByteArray, privateKey: ByteArray): ByteVector64 {
    val privateKey = PrivateKey(privateKey)
    val messageHash = ByteVector32(hash)
    return Crypto.signSchnorr(messageHash, privateKey, Crypto.SchnorrTweak.NoTweak)
}
/**
 * Verifies a Schnorr signature for an arbitrary string message.
 * @param message The original message (as String).
 * @param signature The signature to verify (64 bytes).
 * @param xonlyPublicKey The x-only public key to verify with.
 * @return true if valid, false otherwise.
 */
fun verifyStringSignatureSchnorr(message: String, signature: ByteArray, xonlyPublicKey: XonlyPublicKey): Boolean {
    val messageHash = ByteVector32(Crypto.sha256(message.encodeToByteArray()))
    return Crypto.verifySignatureSchnorr(messageHash, ByteVector64(signature), xonlyPublicKey)
}

/**
 * Verifies a Schnorr signature for a 32-byte hash.
 * This is the function to use for verifying Nostr events.
 * @param hash The 32-byte hash.
 * @param signature The signature to verify (64 bytes).
 * @param xonlyPublicKey The x-only public key to verify with.
 * @return true if valid, false otherwise.
 */
fun verifyHashSignatureSchnorr(hash: ByteArray, signature: ByteArray, xonlyPublicKey: XonlyPublicKey): Boolean {
    val messageHash = ByteVector32(hash)
    return Crypto.verifySignatureSchnorr(messageHash, ByteVector64(signature), xonlyPublicKey)
}

// Data class to safely expose key data to the app/UI layer
data class NostrKeyPair(
    val privHex: String,
    val pubHex: String,
    val xOnlyPubHex: String
)

fun generateNostrKeyPairForUi(): NostrKeyPair {
    val (priv, pub) = generateRandomKeyPair()
    val xonly = priv.xOnlyPublicKey()
    return NostrKeyPair(
        privHex = priv.value.toByteArray().joinToString("") { "%02x".format(it) },
        pubHex = pub.value.toByteArray().joinToString("") { "%02x".format(it) },
        xOnlyPubHex = xonly.value.toByteArray().joinToString("") { "%02x".format(it) }
    )
}

fun importNostrKeyPairForUi(hex: String): NostrKeyPair {
    val (priv, pub) = importKeyPairFromHex(hex)
    val xonly = priv.xOnlyPublicKey()
    return NostrKeyPair(
        privHex = priv.value.toByteArray().joinToString("") { "%02x".format(it) },
        pubHex = pub.value.toByteArray().joinToString("") { "%02x".format(it) },
        xOnlyPubHex = xonly.value.toByteArray().joinToString("") { "%02x".format(it) }
    )
}

/**
 * Returns the x-only public key from a PrivateKey.
 */
fun getXOnlyKeyFromPrivateKey(priv: fr.acinq.bitcoin.PrivateKey): fr.acinq.bitcoin.XonlyPublicKey {
    return priv.xOnlyPublicKey()
}

/**
 * Validates if a hex string is a valid secp256k1 private key.
 */
fun validateSecretKey(hex: String): Boolean {
    return try {
        val privBytes = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        fr.acinq.bitcoin.PrivateKey(privBytes)
        true
    } catch (e: Exception) {
        false
    }
}

/**
 * Decodes a bech32-encoded Nostr key (e.g., nsec, npub) and returns the raw bytes.
 * Throws if the bech32 string is invalid or the HRP does not match.
 */
fun decodeBech32(hrp: String, bech32: String): ByteArray {
    val (decodedHrp, words, _) = Bech32.decode(bech32)
    require(decodedHrp == hrp) { "Expected HRP '$hrp' but got '$decodedHrp'" }
    return Bech32.five2eight(words, 0)
}

/**
 * Imports a compressed public key from a x-only public key hex string (32 bytes).
 * Prepends '02' as prefix to create a valid compressed public key (33 bytes),
 * then returns the PublicKey object.
 * Throws if the hex is invalid.
 */
//TODO why this sorcery why not actually find the real public key from the x-only hex?
// i mean is not nuclear science to do it, but we need to be consistent with the NIP-44v2 spec
// who desined this? remeber kick their ass 
fun importPublicKeyFromXOnlyHex(xOnlyHex: String): PublicKey {
    require(xOnlyHex.length == 64) { "X-only public key hex must be 64 characters (32 bytes)" }
    val compressedHex = "02" + xOnlyHex
    val compressedBytes = compressedHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    return PublicKey(compressedBytes)
}

// UI-specific helpers like UiKeyInfo and getUiKeyInfoFromHex should be implemented in the data/viewmodel layer, not in /crypto.
// Leaving here for reference, but should not be used in production:
//
// data class UiKeyInfo(val privHex: String, val nsec: String, val npub: String)
//
// fun getUiKeyInfoFromHex(hex: String): UiKeyInfo {
//     val (priv, _) = importKeyPairFromHex(hex)
//     val xonly = priv.xOnlyPublicKey()
//     val nsec = encodeBech32("nsec", priv.value.toByteArray())
//     val npub = encodeBech32("npub", xonly.value.toByteArray())
//     return UiKeyInfo(privHex = hex, nsec = nsec, npub = npub)
// }
