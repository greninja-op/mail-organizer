package com.greninjaop.mailorganizer.domain.ai

import com.greninjaop.mailorganizer.MoResult
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.core.ai.AiClassificationOutput
import com.greninjaop.mailorganizer.core.ai.AiResult
import com.greninjaop.mailorganizer.core.ai.AiThreadSummaryOutput
import com.greninjaop.mailorganizer.data.local.ClassificationRecord
import com.greninjaop.mailorganizer.data.local.ClassificationSource
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.prefs.AiPreferences
import com.greninjaop.mailorganizer.data.repository.MailRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * High-level domain use case for Optional AI Fallback operations (Phase 26 §6, §12, §50, §51).
 *
 * Implements deterministic precedence:
 * User correction > User rule > Deterministic intelligence > AI fallback > Unknown.
 */
class AiFallbackUseCase(
    private val aiManager: AiManager,
    private val preferences: AiPreferences,
    private val mailRepository: MailRepository? = null,
    private val dispatchers: AppDispatchers,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    companion object {
        private const val TAG = "AiFallbackUseCase"
    }

    /**
     * Attempts an optional AI classification fallback for an email when deterministic classification is insufficient.
     *
     * @param message the message to classify.
     * @param deterministicCategory the result from the deterministic engine.
     * @return an AI-assisted [ClassificationRecord] if AI succeeded, or null if AI was skipped or failed.
     */
    suspend fun classifyWithFallback(
        message: MessageRecord,
        deterministicCategory: MailCategory,
    ): ClassificationRecord? = withContext(dispatchers.io) {
        try {
            val aiResult: AiResult<AiClassificationOutput>? = aiManager.fallbackClassify(
                message = message,
                deterministicCategory = deterministicCategory,
            )

            if (aiResult == null) return@withContext null

            ClassificationRecord(
                messageId = message.messageId,
                accountId = message.accountId,
                category = aiResult.output.category,
                confidence = aiResult.confidence,
                source = ClassificationSource.OPTIONAL_AI,
                version = aiResult.version,
                explanation = aiResult.output.explanation,
                overridden = false,
                classifiedAtEpochMs = aiResult.createdAtEpochMs,
            )
        } catch (t: Throwable) {
            MoLogger.w(TAG, "Safe AI classification fallback degradation: ${t.javaClass.simpleName}")
            null
        }
    }

    /**
     * User-initiated on-demand thread summarization (Phase 26 §49, §57).
     */
    suspend fun summarizeThread(
        accountId: String,
        threadId: String,
    ): MoResult<AiResult<AiThreadSummaryOutput>> = withContext(dispatchers.io) {
        val repo = mailRepository
            ?: return@withContext MoResult.Failure(com.greninjaop.mailorganizer.MoError.InvalidConfiguration("MailRepository not provided"))

        val messages = repo.observeMessages(threadId, 200).first()
        aiManager.summarizeThread(
            messages = messages,
            accountId = accountId,
            threadId = threadId,
        )
    }

    /**
     * Account removal boundary (Phase 26 §92).
     * Clears all AI cache entries for the removed account.
     */
    fun handleAccountRemoved(accountId: String) {
        aiManager.handleAccountRemoved(accountId)
    }

    /**
     * Provider disconnect boundary (Phase 26 §71, §93, §124).
     * Clears credentials, resets AI settings to disabled, and clears AI cache.
     */
    suspend fun disconnectProvider() {
        preferences.clearConfiguration()
        aiManager.aiCache.clear()
        MoLogger.i(TAG, "AI provider disconnected and configuration reset to default OFF")
    }
}
