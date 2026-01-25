package com.example.nostr.models

import kotlinx.serialization.Serializable

/**
 * Represents the result of publishing an event to a single relay.
 *
 * @property status The outcome of the publication attempt.
 * @property message An optional message from the relay, providing details about the outcome (e.g., reason for failure).
 */
data class PublicationResult(
    val status: PublicationStatus,
    val message: String? = null
)

/**
 * Defines the possible outcomes of an event publication attempt.
 */
@Serializable
enum class PublicationStatus {
    /** The event was successfully published and saved by the relay. */
    SUCCESS, // Relay has accepted and saved the event
    /** The relay rejected the event because it was a duplicate. */
    DUPLICATE,
    /** The relay rejected the event because it required more proof of work. */
    POW,
    /** The relay rejected the event because the client is rate-limited. */
    RATE_LIMITED,
    /** The relay rejected the event because it was invalid. */
    INVALID,
    /** The relay rejected the event or another failure occurred. */
    FAILED, // Relay has rejected the event for a generic reason
    /** The relay requires authentication to publish. */
    AUTH_REQUIRED,
    /** The relay sent a notice. This is informational and may not indicate failure. */
    NOTICE, // Relay sent a notice message
    /** The relay rejected the event with a generic error. */
    ERROR
}
