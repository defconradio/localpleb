package com.example.nostr.util
import kotlinx.datetime.Clock
import kotlin.random.Random

/**
 * Returns a random Unix timestamp up to 2 days in the past from now.
 * This version ensures the offset is always positive and subtracted correctly.
 */
fun randomTimeUpTo2DaysInThePast(): Long {
    val now = Clock.System.now().epochSeconds
    val twoDaysInSeconds = 2L * 24 * 60 * 60 // Using Long for clarity

    // Ensure we generate a random positive offset within the 2-day range
    val offset = Random.nextLong(twoDaysInSeconds)

    // Subtract the positive offset from the current time to get a time in the past
    return now - offset
}