package com.example.data.uiModels

import com.example.nostr.models.NostrEnvelope

enum class MessageStatus {
    SENDING,
    SENT,
    FAILED
}

data class OrderUiModel(
    val envelope: NostrEnvelope,
    val event_id: String,
    val pubkey: String,
    val created_at: Long,
    val kind: Int,
    val tags: List<List<String>>,
    val content: String, // Plain text content
    val sig: String,
    // UI-specific fields
    val formattedCreatedAt: String?,
    val relayUrl: String?,
    val isFromCurrentUser: Boolean,
    val isDeleting: Boolean = false,
    val status: MessageStatus = MessageStatus.SENT
)


