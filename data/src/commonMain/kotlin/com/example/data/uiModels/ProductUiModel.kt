package com.example.data.uiModels

import com.example.nostr.models.NostrEnvelope
import kotlinx.serialization.Serializable

@Serializable
data class ProductUiModel(
    val envelope: NostrEnvelope? = null, // Optionally keep the full event
    val isVerified: Boolean = false,
    // ProductContent fields
    val event_id: String = "",
    val pubkey: String = "",
    val created_at: Long = 0L,
    val kind: Int = 30018,
    val tags: List<List<String>> = emptyList(),
    val content: String = "",
    val sig: String = "",
    val stall_id: String = "",
    val name: String = "",
    val description: String? = null,
    val images: List<String>? = null,
    val currency: String = "",
    val price: Float = 0f,
    val quantity: Int? = null,
    val specs: List<List<String>> = emptyList(),
    val shipping: List<ProductShippingZoneUiModel> = emptyList(),
    val product_id: String = "",
    val formattedCreatedAt: String? = null, // Formatted date for UI
    val relayUrl: String? = null
)

@Serializable
data class ProductShippingZoneUiModel(
    val id: String,
    val cost: Float
)
