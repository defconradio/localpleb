package com.example.nostr.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.encodeToJsonElement
import com.example.nostr.util.Tags
import java.time.Instant

/**
 * A template for a Nostr event, containing all the fields that need to be signed.
 * This aligns with Amethyst's `EventTemplate` and NIP-01.
 */
data class EventTemplate(
    val kind: Int,
    val tags: List<List<String>>,
    val content: String,
    val createdAt: Long // Removed default value to make it mandatory
) {
    /**
     * Serializes the event template into the canonical JSON format required for signing,
     * according to NIP-01.
     * [0, pubkey, created_at, kind, tags, content]
     */
    fun toCanonicalJson(pubkey: String): String {
        val eventData = buildJsonArray {
            add(JsonPrimitive(0))
            add(JsonPrimitive(pubkey))
            add(JsonPrimitive(createdAt))
            add(JsonPrimitive(kind))
            add(Json.Default.encodeToJsonElement(tags))
            add(JsonPrimitive(content))
        }
        val canonical = eventData.toString()
        //println("[EventTemplate.kt] Canonical event array for signing: $canonical")
        return canonical
    }

    companion object {
        /**
         * The generic event builder, inspired by Amethyst.
         * It uses a builder lambda (`tagsBuilder`) for type-safe tag construction.
         */
        fun create(
            kind: Int,
            content: String = "",
            createdAt: Long, // Now mandatory
            tagsBuilder: Tags.() -> Unit = {}
        ): EventTemplate {
            val tags = Tags().apply(tagsBuilder).build()
            return EventTemplate(
                kind = kind,
                tags = tags,
                content = content,
                createdAt = createdAt
            )
        }
    }
}