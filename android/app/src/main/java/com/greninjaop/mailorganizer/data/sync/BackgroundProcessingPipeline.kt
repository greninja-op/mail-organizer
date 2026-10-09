package com.greninjaop.mailorganizer.data.sync

import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.domain.actions.GenerateActionsUseCase
import com.greninjaop.mailorganizer.domain.classify.ClassifyMailboxUseCase
import com.greninjaop.mailorganizer.domain.company.CompanyIntelligenceUseCase
import com.greninjaop.mailorganizer.domain.priority.PrioritizeMailboxUseCase
import com.greninjaop.mailorganizer.domain.search.SearchIndexUseCase
import com.greninjaop.mailorganizer.domain.temporal.ExtractMailboxUseCase
import kotlinx.coroutines.withContext

/**
 * Summary of a completed background processing run for an account.
 */
data class ProcessingSummary(
    val accountId: String,
    val attributedSenders: Int,
    val classifiedMessages: Int,
    val prioritizedMessages: Int,
    val extractedTemporal: Int,
    val generatedActions: Int,
    val durationMs: Long,
)

/**
 * Local intelligence background processing pipeline (Phase 19 §38–§42).
 *
 * Runs locally after remote Gmail messages are safely persisted to Room:
 * ```text
 * Gmail data → normalize → parse → sender/company → classification → priority → Action Required → temporal → actions → search
 * ```
 *
 * Guarantees:
 * - Strict account isolation (§43): processes ONLY the provided [accountId].
 * - Error isolation (§40): failure in one engine or email never aborts the whole pipeline.
 * - Processing idempotency (§39): safe to run repeatedly; no duplicate records created.
 * - Local-only processing (§41): zero network requests, zero AI calls, zero external data leakage.
 * - Privacy-safe diagnostics: logs only counts and error types; never email bodies or subjects.
 */
class BackgroundProcessingPipeline(
    private val companyIntelligence: CompanyIntelligenceUseCase,
    private val classifyMailbox: ClassifyMailboxUseCase,
    private val prioritizeMailbox: PrioritizeMailboxUseCase,
    private val extractMailbox: ExtractMailboxUseCase,
    private val generateActions: GenerateActionsUseCase,
    private val searchIndex: SearchIndexUseCase? = null,
    private val dispatchers: AppDispatchers,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    suspend fun processAccount(accountId: String): ProcessingSummary = withContext(dispatchers.io) {
        val startMs = clock()
        MoLogger.i(TAG, "Starting background intelligence pipeline account=$accountId")

        // 1. Sender & Company attribution (must precede classification for recurring sender signal)
        var attributed = 0
        try {
            attributed = companyIntelligence.processNew(accountId)
        } catch (t: Throwable) {
            MoLogger.w(TAG, "Company attribution warning account=$accountId: ${t.javaClass.simpleName}")
        }

        // 2. Deterministic classification
        var classified = 0
        try {
            classified = classifyMailbox.classifyNew(accountId)
        } catch (t: Throwable) {
            MoLogger.w(TAG, "Classification warning account=$accountId: ${t.javaClass.simpleName}")
        }

        // 3. Priority & Action-Required engine
        var prioritized = 0
        try {
            prioritized = prioritizeMailbox.prioritizeNew(accountId)
        } catch (t: Throwable) {
            MoLogger.w(TAG, "Prioritization warning account=$accountId: ${t.javaClass.simpleName}")
        }

        // 4. Meeting & deadline extraction
        var extracted = 0
        try {
            extracted = extractMailbox.extractNew(accountId)
        } catch (t: Throwable) {
            MoLogger.w(TAG, "Temporal extraction warning account=$accountId: ${t.javaClass.simpleName}")
        }

        // 5. Action card generation
        var actions = 0
        try {
            actions = generateActions.generateNew(accountId)
        } catch (t: Throwable) {
            MoLogger.w(TAG, "Action generation warning account=$accountId: ${t.javaClass.simpleName}")
        }

        // 6. Local search indexing maintenance (best-effort)
        try {
            searchIndex?.indexNew(accountId)
        } catch (t: Throwable) {
            MoLogger.w(TAG, "Search index warning account=$accountId: ${t.javaClass.simpleName}")
        }

        val duration = clock() - startMs
        MoLogger.i(
            TAG,
            "Finished background intelligence pipeline account=$accountId: " +
                "attributed=$attributed classified=$classified prioritized=$prioritized " +
                "extracted=$extracted actions=$actions durationMs=$duration",
        )

        ProcessingSummary(
            accountId = accountId,
            attributedSenders = attributed,
            classifiedMessages = classified,
            prioritizedMessages = prioritized,
            extractedTemporal = extracted,
            generatedActions = actions,
            durationMs = duration,
        )
    }

    companion object {
        private const val TAG = "BackgroundPipeline"
    }
}
