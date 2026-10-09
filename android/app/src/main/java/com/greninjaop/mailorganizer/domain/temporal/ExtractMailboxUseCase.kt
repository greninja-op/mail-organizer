package com.greninjaop.mailorganizer.domain.temporal

import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.data.repository.MailRepository
import kotlinx.coroutines.withContext

/**
 * Batch temporal extraction over an account's mailbox (Phase 13).
 *
 * - Incremental: only messages with no extracted-items row are processed —
 *   the mailbox is never re-extracted wholesale on every run.
 * - Bounded: at most [limit] messages per call; callers repeat for more.
 * - Background-safe: runs on the IO dispatcher; per-message failures are
 *   isolated by [ExtractTemporalUseCase] and never abort the batch.
 */
class ExtractMailboxUseCase(
    private val mail: MailRepository,
    private val extractMessage: ExtractTemporalUseCase,
    private val dispatchers: AppDispatchers,
) {

    companion object {
        private const val TAG = "ExtractMailbox"

        /** Default batch bound — keeps any single run cheap. */
        const val DEFAULT_LIMIT = 200
    }

    /**
     * Extracts temporal items for up to [limit] unextracted messages.
     *
     * @return how many messages produced items (skipped messages —
     *   current versions, missing rows — are not counted).
     */
    suspend fun extractNew(
        accountId: String,
        limit: Int = DEFAULT_LIMIT,
    ): Int = withContext(dispatchers.io) {
        val pending = try {
            mail.getUnextractedMessages(accountId, limit.coerceIn(1, 1_000))
        } catch (t: Throwable) {
            MoLogger.e(TAG, "Unextracted lookup failed: ${t.javaClass.simpleName}")
            return@withContext 0
        }
        var extracted = 0
        for (message in pending) {
            // Account isolation is enforced per message inside the use case,
            // but the query is already account-scoped — defense in depth.
            val result = extractMessage.extract(message.messageId, accountId)
            if (result != null && result.items.isNotEmpty()) {
                extracted++
            }
        }
        if (extracted > 0) {
            MoLogger.i(TAG, "Extracted temporal items for $extracted messages")
        }
        extracted
    }
}
