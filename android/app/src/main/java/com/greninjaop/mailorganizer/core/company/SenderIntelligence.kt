package com.greninjaop.mailorganizer.core.company

import com.greninjaop.mailorganizer.core.classify.TextNormalizer

/**
 * Sender-side intelligence primitives (Phase 8).
 *
 * Pure functions over already-normalized data. The persistent sender
 * profile (first/last seen, message counts) lives in the data layer
 * ([com.greninjaop.mailorganizer.data.local.SenderRecord]); this object
 * owns the documented heuristics so they stay independently testable.
 *
 * The recurring-sender signal feeds Phase 7's classifier: a sender seen
 * in enough of the user's mail is someone the user actually corresponds
 * with, which is a legitimate importance signal. The threshold is a
 * documented heuristic, not a learned model — it never claims more than
 * "this address has appeared N times".
 */
object SenderIntelligence {

    /**
     * Messages from one address needed before it counts as recurring.
     * Deliberately low: with no sync history yet, even a small local
     * mailbox should surface repeat correspondents. Documented as a
     * heuristic in the "why" explanation, never as a probability.
     */
    const val RECURRING_SENDER_THRESHOLD = 3

    /** Locale-independent address normalization for sender matching. */
    fun normalizeEmail(address: String): String =
        TextNormalizer.normalizeAddress(address)

    /** Domain part of a normalized address ("" when malformed). */
    fun domainOf(normalizedAddress: String): String =
        TextNormalizer.domainOf(normalizedAddress)

    /** True once [messageCount] reaches [RECURRING_SENDER_THRESHOLD]. */
    fun isRecurring(messageCount: Int): Boolean =
        messageCount >= RECURRING_SENDER_THRESHOLD

    /**
     * Computes the next message count after observing one more message.
     * Saturates at [Int.MAX_VALUE] instead of overflowing — a mailbox
     * with two billion messages from one sender is not a case we need
     * to distinguish precisely.
     */
    fun nextCount(current: Int): Int =
        if (current >= Int.MAX_VALUE) Int.MAX_VALUE else current + 1
}
