package com.greninjaop.mailorganizer.domain.priority

import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.data.repository.MailRepository
import kotlinx.coroutines.withContext

/**
 * Batch prioritization over an account's mailbox (Phase 9).
 *
 * - Incremental: only messages with no priority row are processed — the
 *   mailbox is never reprioritized wholesale on every run.
 * - Bounded: at most [limit] messages per call; callers repeat for more.
 * - Background-safe: runs on the IO dispatcher; per-message failures are
 *   isolated by [PrioritizeMessageUseCase] and never abort the batch.
 * - Re-prioritization across engine versions is handled per message by
 *   [PrioritizeMessageUseCase] (older
 *   [com.greninjaop.mailorganizer.core.priority.DeterministicPriorityEngine.VERSION]
 *   rows are eligible).
 */
class PrioritizeMailboxUseCase(
    private val mail: MailRepository,
    private val prioritizeMessage: PrioritizeMessageUseCase,
    private val dispatchers: AppDispatchers,
) {

    companion object {
        private const val TAG = "PrioritizeMailbox"

        /** Default batch bound — keeps any single run cheap. */
        const val DEFAULT_LIMIT = 200
    }

    /**
     * Prioritizes up to [limit] unprioritized messages for [accountId].
     *
     * @return how many messages the engine prioritized (skipped messages —
     *   overrides, current versions, missing rows — are not counted).
     */
    suspend fun prioritizeNew(
        accountId: String,
        limit: Int = DEFAULT_LIMIT,
    ): Int = withContext(dispatchers.io) {
        val pending = try {
            mail.getUnprioritizedMessages(accountId, limit.coerceIn(1, 1_000))
        } catch (t: Throwable) {
            MoLogger.e(TAG, "Unprioritized lookup failed: ${t.javaClass.simpleName}")
            return@withContext 0
        }
        var prioritized = 0
        for (message in pending) {
            // Account isolation is enforced per message inside the use case,
            // but the query is already account-scoped — defense in depth.
            if (prioritizeMessage.prioritize(message.messageId, accountId) != null) {
                prioritized++
            }
        }
        if (prioritized > 0) {
            MoLogger.i(TAG, "Prioritized $prioritized new messages (v${version()})")
        }
        prioritized
    }

    private fun version(): Int =
        com.greninjaop.mailorganizer.core.priority.DeterministicPriorityEngine.VERSION
}
