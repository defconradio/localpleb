package com.example.data.uiModels

import com.example.nostr.models.NostrEnvelope
import kotlinx.serialization.Serializable

@Serializable
data class StallUiModel(
    val envelope: NostrEnvelope? = null, // Keep the full event for verification
    val isVerified: Boolean = false,
    // Envelope fields
    val event_id: String = "",
    val pubkey: String = "",
    val created_at: Long = 0L,
    val kind: Int = 30017,
    val tags: List<List<String>> = emptyList(),
    val content: String = "",
    val sig: String = "",
    // StallContent fields
    val stall_id: String = "",
    val name: String = "",
    val description: String? = null,
    val currency: String = "",
    val shipping: List<ShippingZoneUiModel> = emptyList(),
    val formattedCreatedAt: String? = null, // Formatted date for UI
    val relayUrl: String? = null
)

@Serializable
data class ShippingZoneUiModel(
    val id: String,
    val name: String? = null,
    val cost: Float,
    val regions: List<String> = emptyList(),
    val countries: List<String> = emptyList()
)
