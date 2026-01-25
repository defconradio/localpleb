package com.example.nostr.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive

/**
 * Base Event class with a polymorphic serializer to handle the mixed-type array
 * required for canonicalization.
 */
@Serializable(with = Event.PolymorphicEventSerializer::class)
sealed class Event {
    object PolymorphicEventSerializer : KSerializer<Event> {
        override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

        override fun serialize(encoder: Encoder, value: Event) {
            // This serializer is a placeholder for the canonical form and should not be called directly.
            // The actual serialization logic is in EventTemplate.toCanonicalJson().
            throw NotImplementedError("This serializer is for polymorphic dispatch only.")
        }

        override fun deserialize(decoder: Decoder): Event {
            // This is primarily for serialization, deserialization is not the main focus here.
            throw NotImplementedError("Deserialization is not implemented for the polymorphic event serializer.")
        }
    }
}


/**
 * NostrEnvelope: The protocol-level, generic Nostr event envelope.
 * Contains all common fields for any Nostr event.
 * Use this as the base for all event parsing; parse the content field into a specific data class as needed.
 * NostrEnvelope: The new protocol-level, generic Nostr event data class (replaces NostrEvent for new code).
 * ProductContent, StallContent, ShippingZone, ProductShippingZone: Strongly-typed data classes for event.content, as examples.
 * parseNostrEnvelope: Helper function to parse a NostrEnvelope from a JsonObject.
 */

@Serializable
data class NostrEnvelope(
    val id: String,
    val pubkey: String,
    val created_at: Long,
    val kind: Int,
    val tags: List<List<String>>,
    val content: String,
    val sig: String? = null,
    // Add relayUrl for tracking which relay sent the event
    val relayUrl: String? = null
)

/**
 * Example: Stall event content (for kind = stall, kind = 30017)
 * https://github.com/nostr-protocol/nips/blob/master/15.md#event-30017-create-or-update-a-stall
 */
@Serializable
data class StallContent(
    val id: String,
    val name: String,
    val description: String? = null,
    val currency: String,
    val shipping: List<ShippingZone> = emptyList()
)

@Serializable
data class ShippingZone(
    val id: String,
    val name: String? = null,
    val cost: Float,
    val regions: List<String>? = null,
    val countries: List<String>? = null
)

/**
 * Example: Product event content (for kind = product, kind = 30018)
 * https://github.com/nostr-protocol/nips/blob/master/15.md#event-30018-create-or-update-a-product
 */
@Serializable
data class ProductContent(
    val id: String,
    val stall_id: String,
    val name: String,
    val description: String? = null,
    val images: List<String>? = null,
    val currency: String,
    val price: Float,
    val quantity: Int? = null,
    val specs: List<List<String>> = emptyList(),
    val shipping: List<ProductShippingZone> = emptyList()
)

@Serializable
data class ProductShippingZone(
    val id: String,
    val cost: Float
)

//kind 14
@Serializable
data class DirectMessageContent(
    val content: String
)

//kind 13
@Serializable
data class SealContent(
    val content: String
)

//kind 1059
@Serializable
data class GiftWrapContent(
    val content: String
)


/**
 * Helper to parse a NostrEnvelope from a JsonObject.
 */
fun parseNostrEnvelope(obj: kotlinx.serialization.json.JsonObject, json: kotlinx.serialization.json.Json): NostrEnvelope {
    return json.decodeFromJsonElement(NostrEnvelope.serializer(), obj)
}
