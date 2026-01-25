package com.example.nostr.models

/**
 * Represents a Nostr public key in bech32 (npub) format.
 */
data class NostrPublicKey(val npub: String)

/**
 * Represents a Nostr private key in bech32 (nsec) format.
 */
data class NostrPrivateKey(val nsec: String)
