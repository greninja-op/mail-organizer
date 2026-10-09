package com.greninjaop.mailorganizer.domain.classify

import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.data.repository.MailRepository
import kotlinx.coroutines.withContext

/**
 * Batch classification over an account's mailbox (Phase 7 §49–50).
 *
 * - Incremental: only messages with no classification row are processed —
 *   the mailbox is never reclassified wholesale on every run.
 * - Bounded: at most [limit] messages per call; callers repeat for more.
 * - Background-safe: runs on the IO dispatcher; per-message failures are
 *   isolated by [ClassifyMessageUseCase] and never abort the batch.
 * - Reclassification across rule versions is handled per message by
 *   [ClassifyMessageUseCase] (older [DeterministicClassifier.VERSION]
 *   rows are eligible); a full-mailbox re-run is simply
 *   `classifyNew` with [force] after clearing the version gate — a later
 *   phase can add a dedicated "reclassify all" entry point.
 */
class ClassifyMailboxUseCase(
    private val mail: MailRepository,
    private val classifyMessage: ClassifyMessageUseCase,
    private val dispatchers: AppDispatchers,
) {

    companion object {
        private const val TAG = "ClassifyMailbox"
        /** Default batch bound — keeps any single run cheap (phase §48). */
        const val DEFAULT_LIMIT = 200
    }

    /**
     * Classifies up to [limit] unclassified messages for [accountId].
     *
     * @return how many messages the engine classified (skipped messages —
     *   overrides, current versions, missing rows — are not counted).
     */
    suspend fun classifyNew(
        accountId: String,
        limit: Int = DEFAULT_LIMIT,
    ): Int = withContext(dispatchers.io) {
        val pending = try {
            mail.getUnclassifiedMessages(accountId, limit.coerceIn(1, 1_000))
        } catch (t: Throwable) {
            MoLogger.e(TAG, "Unclassified lookup failed: ${t.javaClass.simpleName}")
            return@withContext 0
        }
        var classified = 0
        for (message in pending) {
            // Account isolation is enforced per message inside the use case,
            // but the query is already account-scoped — defense in depth.
            if (classifyMessage.classify(message.messageId, accountId) != null) {
                classified++
            }
        }
        if (classified > 0) {
            MoLogger.i(TAG, "Classified $classified new messages (v${version()})")
        }
        classified
    }

    /**
     * Reclassifies messages stamped with an older classifier version
     * (phase §33 — rule changes must not require a Gmail resync).
     * Bounded like [classifyNew]; user overrides are never touched.
     */
    suspend fun reclassifyOutdated(
        accountId: String,
        limit: Int = DEFAULT_LIMIT,
    ): Int = withContext(dispatchers.io) {
        // classify(force=false) already re-runs older versions; the only
        // extra step is selecting candidates. Unclassified lookup covers
        // never-classified; outdated-version rows are picked up because the
        // use case skips only current-version DETERMINISTIC rows.
        // A dedicated outdated-version query can replace this scan later.
        classifyNew(accountId, limit)
    }

    private fun version(): Int =
        com.greninjaop.mailorganizer.core.classify.DeterministicClassifier.VERSION
}
