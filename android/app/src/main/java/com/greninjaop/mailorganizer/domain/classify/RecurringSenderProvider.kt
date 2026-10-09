package com.greninjaop.mailorganizer.domain.classify

/**
 * Real recurring-sender signal for the classifier (Phase 8).
 *
 * Phase 7 threaded `isRecurringSender` through the classifier with a
 * hard-coded `false`; this interface is the seam Phase 8 fills with real
 * data. The classifier package owns the interface (dependency inversion):
 * the company intelligence use case implements it, the classifier
 * consumes it, and neither knows the other's internals.
 *
 * Implementations must be cheap, account-isolated reads — this is called
 * once per classified message.
 */
interface RecurringSenderProvider {
    /**
     * True when [normalizedEmail] (already normalized via
     * [com.greninjaop.mailorganizer.core.company.SenderIntelligence.normalizeEmail])
     * has crossed the recurring threshold in [accountId]'s local mail.
     */
    suspend fun isRecurring(accountId: String, normalizedEmail: String): Boolean
}
