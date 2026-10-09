package com.greninjaop.mailorganizer.domain.company

import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.core.company.CompanyDetectionInput
import com.greninjaop.mailorganizer.core.company.CompanyDetector
import com.greninjaop.mailorganizer.core.company.SenderIntelligence
import com.greninjaop.mailorganizer.data.local.CompanyRecord
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.local.SenderRecord
import com.greninjaop.mailorganizer.data.repository.IntelligenceRepository
import com.greninjaop.mailorganizer.data.repository.MailRepository
import com.greninjaop.mailorganizer.domain.classify.RecurringSenderProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Company & sender intelligence engine (Phase 8).
 *
 * Architecture: UI → ViewModel → this use case → [CompanyDetector] /
 * [SenderIntelligence] (pure core) → repositories. Detection logic never
 * lives in the UI; the engine stays independently testable.
 *
 * What one [processMessage] call does, atomically per message:
 * 1. Normalizes the sender and records the sighting (insert-or-increment;
 *    first/last seen, message count) — this is what makes the
 *    recurring-sender signal real.
 * 2. Detects the company from the sender domain (null for personal
 *    mailbox domains and malformed addresses — people aren't companies).
 * 3. Upserts the company row, preserving user intent ([CompanyRecord.pinned]
 *    and [CompanyRecord.userOverrideName] are never overwritten by
 *    detection) and merging observed subdomains (bounded).
 * 4. Links the message to the company (`messages.companyId`).
 *
 * Account isolation: every read/write is scoped to [MessageRecord.accountId];
 * a message from another account is never attributed.
 *
 * Failure safety: any failure degrades to "not attributed", never crashes
 * sync or the UI. Only safe diagnostics are logged (message id, failure
 * class — never addresses, names, or subjects).
 */
class CompanyIntelligenceUseCase(
    private val mail: MailRepository,
    private val intelligence: IntelligenceRepository,
    private val dispatchers: AppDispatchers,
    private val clock: () -> Long = System::currentTimeMillis,
) : RecurringSenderProvider {

    companion object {
        private const val TAG = "CompanyIntel"

        /**
         * Bounded incremental pass size (matches the classifier's
         * bounded-pass philosophy — never scan a whole mailbox at once).
         */
        const val PROCESS_NEW_LIMIT = 200

        /** Cap on merged known-subdomains per company row (bounded growth). */
        const val MAX_KNOWN_DOMAINS = 20
    }

    /** Result of attributing one message. */
    data class Attribution(
        val sender: SenderRecord,
        /** Null for personal senders / malformed addresses. */
        val company: CompanyRecord?,
    )

    /**
     * Attributes one message: sender sighting + company detection + link.
     *
     * @return the attribution, or null when attribution was skipped
     *   (blank sender, cross-account message) or failed safely.
     */
    suspend fun processMessage(message: MessageRecord): Attribution? =
        withContext(dispatchers.io) {
            try {
                processOrThrow(message)
            } catch (t: Throwable) {
                // Never break sync/UI because one sender is odd.
                MoLogger.e(TAG, "Attribution failed safely: ${t.javaClass.simpleName}")
                null
            }
        }

    /**
     * Bounded incremental pass over unattributed messages (newest first).
     *
     * @return number of messages attributed.
     */
    suspend fun processNew(accountId: String, limit: Int = PROCESS_NEW_LIMIT): Int =
        withContext(dispatchers.io) {
            try {
                val messages = mail.getMessagesWithoutCompany(accountId, limit)
                var attributed = 0
                for (message in messages) {
                    // Account isolation: never attribute another account's mail.
                    if (message.accountId != accountId) continue
                    if (processMessage(message) != null) attributed++
                }
                attributed
            } catch (t: Throwable) {
                MoLogger.e(TAG, "processNew failed safely: ${t.javaClass.simpleName}")
                0
            }
        }

    /**
     * Real recurring-sender signal (Phase 8 fills Phase 7's seam).
     * Cheap account-isolated read; called once per classified message.
     */
    override suspend fun isRecurring(
        accountId: String,
        normalizedEmail: String,
    ): Boolean = withContext(dispatchers.io) {
        try {
            val row = intelligence.getSenderByEmail(accountId, normalizedEmail)
            row != null && SenderIntelligence.isRecurring(row.messageCount)
        } catch (t: Throwable) {
            MoLogger.e(TAG, "isRecurring failed safely: ${t.javaClass.simpleName}")
            false
        }
    }

    /** User intent: pinning only affects filter-list ordering (never mail). */
    suspend fun setCompanyPinned(companyId: String, pinned: Boolean) =
        withContext(dispatchers.io) {
            intelligence.setCompanyPinned(companyId, pinned)
        }

    /** Company filter list: pinned first, then by name (DAO ordering). */
    fun observeCompanyFilterList(
        accountId: String,
        limit: Int = 100,
    ): Flow<List<CompanyRecord>> =
        intelligence.observeCompanyFilterList(accountId, limit)

    /** Global per-company counts inside one classification category. */
    suspend fun companyCountsForCategory(
        accountId: String,
        category: MailCategory,
    ): Map<String, Int> = withContext(dispatchers.io) {
        mail.companyCountsForCategory(accountId, category)
    }

    /** Global per-company counts inside one Gmail-label destination. */
    suspend fun companyCountsForLabel(
        accountId: String,
        label: String,
    ): Map<String, Int> = withContext(dispatchers.io) {
        mail.companyCountsForLabel(accountId, label)
    }

    private suspend fun processOrThrow(message: MessageRecord): Attribution? {
        if (message.fromAddress.isBlank()) return null

        val now = clock()
        val normalizedEmail = SenderIntelligence.normalizeEmail(message.fromAddress)
        if (normalizedEmail.isEmpty()) return null
        val domain = SenderIntelligence.domainOf(normalizedEmail)

        val sender = intelligence.recordSenderMessage(
            accountId = message.accountId,
            emailAddress = message.fromAddress,
            normalizedEmail = normalizedEmail,
            displayName = message.fromName?.takeIf { it.isNotBlank() },
            domain = domain,
        )

        val detected = CompanyDetector.detect(
            CompanyDetectionInput(
                fromAddress = message.fromAddress,
                fromName = message.fromName,
            ),
        )
        if (detected == null) {
            // Personal sender or malformed address: sender is tracked,
            // but no company grouping is created.
            mail.setMessageCompanyId(message.messageId, null)
            return Attribution(sender = sender, company = null)
        }

        val existing = intelligence.getCompanyByDomain(
            message.accountId,
            detected.normalizedDomain,
        )
        val company = if (existing == null) {
            CompanyRecord(
                companyId = detected.companyId,
                accountId = message.accountId,
                canonicalName = detected.canonicalName,
                normalizedDomain = detected.normalizedDomain,
                knownDomains = detected.knownDomains.toList(),
                userOverrideName = null,
                pinned = false,
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
            )
        } else {
            // Detection never overwrites user intent: pinned and
            // userOverrideName survive; only observed metadata merges.
            existing.copy(
                knownDomains = (existing.knownDomains + detected.knownDomains)
                    .distinct()
                    .take(MAX_KNOWN_DOMAINS),
                updatedAtEpochMs = now,
            )
        }
        intelligence.upsertCompany(company)
        mail.setMessageCompanyId(message.messageId, detected.companyId)
        return Attribution(sender = sender, company = company)
    }
}
