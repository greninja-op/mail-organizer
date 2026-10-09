package com.greninjaop.mailorganizer.domain.rules

import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.core.company.SenderIntelligence
import com.greninjaop.mailorganizer.data.local.CorrectionField
import com.greninjaop.mailorganizer.data.local.CorrectionScope
import com.greninjaop.mailorganizer.data.local.UserCorrectionRecord
import com.greninjaop.mailorganizer.data.repository.IntelligenceRepository
import com.greninjaop.mailorganizer.data.repository.MailRepository
import com.greninjaop.mailorganizer.data.repository.RuleRepository
import kotlinx.coroutines.withContext

/**
 * Records and undoes user corrections (Phase 12, phase §8, §14, §27).
 *
 * A correction is authoritative user intent: it is stored in
 * `user_corrections` (never deleted except by explicit undo) and applied
 * through [ApplyRulesUseCase], which computes the effective result. The
 * distinction the phase requires is preserved:
 * - **One-time correction** → [CorrectionScope.MESSAGE]: this message only.
 * - **Broad correction** → [CorrectionScope.SENDER]/[CorrectionScope.DOMAIN]:
 *   all current and future mail from that sender/domain (future mail is
 *   covered because [ApplyRulesUseCase] consults corrections on every run,
 *   including after sync — phase §25).
 * - **Persistent rule** → created separately via [RuleManagementUseCase];
 *   "also create a rule" in the correction UI is a convenience that calls
 *   both.
 *
 * Company display-name corrections ([CorrectionField.COMPANY_NAME]) apply
 * to [com.greninjaop.mailorganizer.data.local.CompanyRecord.userOverrideName]
 * — the existing user-intent column — and are reapplied by the company
 * use case's merge logic, which never overwrites user intent.
 */
class RecordCorrectionUseCase(
    private val mail: MailRepository,
    private val intelligence: IntelligenceRepository,
    private val rules: RuleRepository,
    private val apply: ApplyRulesUseCase,
    private val dispatchers: AppDispatchers,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    companion object {
        private const val TAG = "RecordCorrection"

        /** Bound for sender/domain-scoped correction application. */
        const val SCOPED_REFRESH_LIMIT = 500
    }

    /**
     * Corrects one message's category or priority.
     *
     * @param value enum name, e.g. `"CAREER"` or `"HIGH"`.
     */
    suspend fun correctMessage(
        accountId: String,
        messageId: String,
        field: CorrectionField,
        value: String,
    ): Boolean = withContext(dispatchers.io) {
        val message = mail.getMessage(messageId) ?: return@withContext false
        if (message.accountId != accountId) return@withContext false
        if (field != CorrectionField.CATEGORY && field != CorrectionField.PRIORITY) {
            return@withContext false
        }
        recordAndApply(
            accountId = accountId,
            scope = CorrectionScope.MESSAGE,
            scopeKey = messageId,
            field = field,
            value = value,
            affectedMessageIds = listOf(messageId),
        )
    }

    /**
     * Corrects a whole sender (all their mail, current and future).
     *
     * @param senderEmail raw address; normalized before storage.
     */
    suspend fun correctSender(
        accountId: String,
        senderEmail: String,
        field: CorrectionField,
        value: String,
    ): Int = withContext(dispatchers.io) {
        if (field != CorrectionField.CATEGORY && field != CorrectionField.PRIORITY) return@withContext 0
        val normalized = SenderIntelligence.normalizeEmail(senderEmail)
        if (normalized.isBlank()) return@withContext 0
        rules.recordCorrection(
            UserCorrectionRecord(
                accountId = accountId,
                scope = CorrectionScope.SENDER,
                scopeKey = normalized,
                field = field,
                value = value,
                createdAtEpochMs = clock(),
            ),
        )
        refreshAll(
            accountId,
            mail.getMessageIdsBySender(accountId, normalized, SCOPED_REFRESH_LIMIT),
        )
    }

    /**
     * Corrects a whole sender domain (all its mail, current and future).
     */
    suspend fun correctDomain(
        accountId: String,
        domain: String,
        field: CorrectionField,
        value: String,
    ): Int = withContext(dispatchers.io) {
        if (field != CorrectionField.CATEGORY && field != CorrectionField.PRIORITY) return@withContext 0
        val normalized = domain.trim().lowercase()
        if (normalized.isBlank()) return@withContext 0
        rules.recordCorrection(
            UserCorrectionRecord(
                accountId = accountId,
                scope = CorrectionScope.DOMAIN,
                scopeKey = normalized,
                field = field,
                value = value,
                createdAtEpochMs = clock(),
            ),
        )
        refreshAll(
            accountId,
            mail.getMessageIdsByDomain(accountId, normalized, SCOPED_REFRESH_LIMIT),
        )
    }

    /**
     * Corrects a company's display name (Phase 12 §8).
     *
     * Stored as a correction row AND applied to the company row's
     * `userOverrideName` (the existing user-intent column, which detection
     * never overwrites). Undo clears both.
     */
    suspend fun correctCompanyName(
        accountId: String,
        companyId: String,
        newName: String,
    ): Boolean = withContext(dispatchers.io) {
        val trimmed = newName.trim().take(100)
        if (trimmed.isEmpty()) return@withContext false
        val company = intelligence.getCompanyByDomain(accountId, companyId.removePrefix("co:"))
            ?: return@withContext false
        if (company.accountId != accountId) return@withContext false
        rules.recordCorrection(
            UserCorrectionRecord(
                accountId = accountId,
                scope = CorrectionScope.COMPANY,
                scopeKey = companyId,
                field = CorrectionField.COMPANY_NAME,
                value = trimmed,
                createdAtEpochMs = clock(),
            ),
        )
        try {
            intelligence.upsertCompany(company.copy(userOverrideName = trimmed))
            true
        } catch (t: Throwable) {
            MoLogger.e(TAG, "Company name correction failed: ${t.javaClass.simpleName}")
            false
        }
    }

    /**
     * Undoes a correction: deletes the row and restores the underlying
     * result by re-running the effective pipeline (phase §27).
     *
     * @return how many messages were refreshed.
     */
    suspend fun undoCorrection(
        accountId: String,
        scope: CorrectionScope,
        scopeKey: String,
        field: CorrectionField,
    ): Int = withContext(dispatchers.io) {
        rules.deleteCorrection(accountId, scope, scopeKey, field)
        // Company-name undo also clears the applied override.
        if (scope == CorrectionScope.COMPANY && field == CorrectionField.COMPANY_NAME) {
            val company = intelligence.getCompanyByDomain(
                accountId,
                scopeKey.removePrefix("co:"),
            )
            if (company != null && company.accountId == accountId) {
                try {
                    intelligence.upsertCompany(company.copy(userOverrideName = null))
                } catch (t: Throwable) {
                    MoLogger.e(TAG, "Company name undo failed: ${t.javaClass.simpleName}")
                }
            }
            return@withContext 0
        }
        val ids = when (scope) {
            CorrectionScope.MESSAGE -> listOf(scopeKey)
            CorrectionScope.SENDER -> mail.getMessageIdsBySender(
                accountId, scopeKey, SCOPED_REFRESH_LIMIT,
            )
            CorrectionScope.DOMAIN -> mail.getMessageIdsByDomain(
                accountId, scopeKey, SCOPED_REFRESH_LIMIT,
            )
            CorrectionScope.COMPANY -> emptyList()
        }
        refreshAll(accountId, ids)
    }

    /** Existing correction for UI state (sheet shows current overrides). */
    suspend fun getCorrection(
        accountId: String,
        scope: CorrectionScope,
        scopeKey: String,
        field: CorrectionField,
    ): UserCorrectionRecord? = withContext(dispatchers.io) {
        rules.getCorrection(accountId, scope, scopeKey, field)
    }

    private suspend fun recordAndApply(
        accountId: String,
        scope: CorrectionScope,
        scopeKey: String,
        field: CorrectionField,
        value: String,
        affectedMessageIds: List<String>,
    ): Boolean {
        return try {
            rules.recordCorrection(
                UserCorrectionRecord(
                    accountId = accountId,
                    scope = scope,
                    scopeKey = scopeKey,
                    field = field,
                    value = value,
                    createdAtEpochMs = clock(),
                ),
            )
            refreshAll(accountId, affectedMessageIds) >= 0
        } catch (t: Throwable) {
            MoLogger.e(TAG, "Correction failed safely: ${t.javaClass.simpleName}")
            false
        }
    }

    private suspend fun refreshAll(accountId: String, ids: List<String>): Int {
        var refreshed = 0
        for (id in ids) {
            // Per-message failures are isolated inside refreshMessage.
            apply.refreshMessage(id, accountId)
            refreshed++
        }
        return refreshed
    }
}
