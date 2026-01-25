package com.example.crypto

/**
 * A secure interface for signing.
 *
 * This class provides a way to sign a hash without exposing the private key.
 * It holds the public key and a reference to a signing function (lambda) that can be
 * called with the event hash (ID) to produce a signature.
 *
 * The signing lambda is created and contained entirely within the `crypto` module.
 * This `SecureSigner` object can then be safely passed to other modules.
 */
class SecureSigner(
    val pubKeyHex: String,
    val signer: (hashToSign: ByteArray) -> ByteArray
)
