package com.example.nostr.models

/**
 * Represents a message received from a Nostr relay.
 * This can be an event, a notice, or an end-of-stored-events (EOSE) marker.
 */
sealed class RelayMessage

/**
 * Represents a standard Nostr event.
 *
 * @property event The Nostr event envelope.
 * @property relayUrl The URL of the relay that sent this event.
 */
data class EventMessage(val event: NostrEnvelope, val relayUrl: String) : RelayMessage()

/**
 * Represents a notice message from a relay.
 *
 * @property relayUrl The URL of the relay that sent the notice.
 * @property message The content of the notice.
 */
data class NoticeMessage(val relayUrl: String, val message: String) : RelayMessage()

/**
 * Represents an 'End of Stored Events' (EOSE) message from a relay.
 * This indicates that the relay has finished sending all stored events that match a subscription.
 *
 * @property relayUrl The URL of the relay that sent the EOSE message.
 * @property subscriptionId The subscription ID for which the EOSE is relevant.
 */
data class EoseMessage(val relayUrl: String, val subscriptionId: String) : RelayMessage()

/**
 * Represents an 'OK' message from a relay, confirming the result of an EVENT submission.
 *
 * @property relayUrl The URL of the relay that sent the OK message.
 * @property eventId The ID of the event that was processed.
 * @property saved Whether the event was successfully saved by the relay.
 * @property message An informational message from the relay, which may include reasons for failure (e.g., "pow:", "duplicate:").
 */
data class OkMessage(
    val relayUrl: String,
    val eventId: String,
    val saved: Boolean,
    val message: String
) : RelayMessage()

/**
 * Represents an 'AUTH' challenge from a relay.
 * This is sent when a client needs to authenticate to perform certain actions.
 *
 * @property relayUrl The URL of the relay that sent the AUTH challenge.
 * @property challenge The challenge string to be signed by the client.
 */
data class AuthMessage(val relayUrl: String, val challenge: String) : RelayMessage()

/**
 * Represents a 'CLOSED' message from a relay.
 * This indicates that a subscription has been closed by the relay.
 *
 * @property relayUrl The URL of the relay that sent the CLOSED message.
 * @property subscriptionId The ID of the subscription that was closed.
 * @property message An informational message from the relay.
 */
data class ClosedMessage(
    val relayUrl: String,
    val subscriptionId: String,
    val message: String
) : RelayMessage()

