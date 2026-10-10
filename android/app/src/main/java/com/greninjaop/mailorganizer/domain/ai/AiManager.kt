package com.greninjaop.mailorganizer.domain.ai

import com.greninjaop.mailorganizer.MoError
import com.greninjaop.mailorganizer.MoResult
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.core.ai.AiAvailabilityState
import com.greninjaop.mailorganizer.core.ai.AiCache
import com.greninjaop.mailorganizer.core.ai.AiCapability
import com.greninjaop.mailorganizer.core.ai.AiClassificationOutput
import com.greninjaop.mailorganizer.core.ai.AiOutputValidator
import com.greninjaop.mailorganizer.core.ai.AiProviderId
import com.greninjaop.mailorganizer.core.ai.AiRequest
import com.greninjaop.mailorganizer.core.ai.AiResult
import com.greninjaop.mailorganizer.core.ai.AiStructuredOutput
import com.greninjaop.mailorganizer.core.ai.AiTemporalOutput
import com.greninjaop.mailorganizer.core.ai.AiThreadSummaryOutput
import com.greninjaop.mailorganizer.core.ai.DataMinimizer
import com.greninjaop.mailorganizer.data.ai.AiProvider
import com.greninjaop.mailorganizer.data.ai.AiProviderRegistry
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.prefs.AiPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Authoritative Coordinator for Optional AI Fallback operations (Phase 26 §6, §12, §84, §85).
 *
 * Enforces the central principle:
 * **AI is an optional fallback, never the foundation of Mail Organizer.**
 *
 * Pipeline:
 * Deterministic check -> Policy check -> Data Minimization -> Cache check
 * -> Provider execution -> Schema validation -> Domain validation -> Result.
 */
class AiManager(
    private val preferences: AiPreferences,
    private val registry: AiProviderRegistry,
    private val cache: AiCache = AiCache(),
    private val dispatchers: AppDispatchers,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    companion object {
        private const val TAG = "AiManager"
        const val SCHEMA_VERSION = 1
    }

    val aiCache: AiCache
        get() = cache

    /**
     * Executes an AI classification fallback for an email only if allowed and eligible.
     *
     * @param message the message to classify.
     * @param deterministicCategory the category produced by deterministic classification.
     * @return [AiResult] on success, or null if AI is disabled/ineligible/fails.
     */
    suspend fun fallbackClassify(
        message: MessageRecord,
        deterministicCategory: MailCategory,
    ): AiResult<AiClassificationOutput>? = withContext(dispatchers.io) {
        // Deterministic sufficiency check (§12, §53):
        // If deterministic engine has classified with confidence, do not call AI!
        if (deterministicCategory != MailCategory.UNCLASSIFIED) {
            return@withContext null
        }

        // Privacy and policy check (§85)
        if (!isCapabilityPermitted(AiCapability.CLASSIFY_EMAIL)) {
            return@withContext null
        }

        // Cache check (§43)
        cache.get<AiClassificationOutput>(
            accountId = message.accountId,
            sourceId = message.messageId,
            capability = AiCapability.CLASSIFY_EMAIL,
            version = SCHEMA_VERSION,
        )?.let { return@withContext it }

        // Data Minimization boundary (§24, §84)
        val minimalContext = DataMinimizer.minimize(message)

        val request = AiRequest(
            capability = AiCapability.CLASSIFY_EMAIL,
            accountId = message.accountId,
            sourceMessageIds = listOf(message.messageId),
            sourceThreadId = message.threadId,
            minimalContext = minimalContext,
            schemaVersion = SCHEMA_VERSION,
        )

        val provider = getActiveProvider() ?: return@withContext null

        val startTime = clock()
        val rawResponse = when (val result = provider.execute(request)) {
            is MoResult.Success -> result.value
            is MoResult.Failure -> {
                MoLogger.w(TAG, "Provider execution failed: ${result.error}")
                return@withContext null
            }
        }
        val duration = clock() - startTime

        // Schema and domain validation (§16, §19, §60)
        val validatedOutput = when (val res = AiOutputValidator.validateClassification(
            rawJson = rawResponse.rawPayload,
            deterministicCandidate = deterministicCategory,
        )) {
            is MoResult.Success -> res.value
            is MoResult.Failure -> {
                MoLogger.w(TAG, "Schema validation failed: ${res.error}")
                return@withContext null
            }
        }

        val aiResult = AiResult(
            capability = AiCapability.CLASSIFY_EMAIL,
            providerId = provider.providerId,
            modelId = rawResponse.modelId,
            sourceId = message.messageId,
            accountId = message.accountId,
            output = validatedOutput,
            confidence = validatedOutput.confidence,
            processingTimeMs = duration,
            createdAtEpochMs = clock(),
            version = SCHEMA_VERSION,
        )

        // Cache result (§43)
        cache.put(aiResult)
        aiResult
    }

    /**
     * Executes on-demand, user-initiated thread summarization (Phase 26 §49, §57).
     */
    suspend fun summarizeThread(
        messages: List<MessageRecord>,
        accountId: String,
        threadId: String,
    ): MoResult<AiResult<AiThreadSummaryOutput>> = withContext(dispatchers.io) {
        if (messages.isEmpty()) {
            return@withContext MoResult.Failure(MoError.InvalidConfiguration("No messages in thread"))
        }

        // Account isolation check (§90)
        if (messages.any { it.accountId != accountId }) {
            return@withContext MoResult.Failure(MoError.InvalidConfiguration("Cross-account data detected in thread"))
        }

        // Privacy and policy check (§85)
        if (!isCapabilityPermitted(AiCapability.SUMMARIZE_THREAD)) {
            return@withContext MoResult.Failure(
                MoError.InvalidConfiguration("AI thread summarization is not enabled in settings")
            )
        }

        // Cache check (§43)
        cache.get<AiThreadSummaryOutput>(
            accountId = accountId,
            sourceId = threadId,
            capability = AiCapability.SUMMARIZE_THREAD,
            version = SCHEMA_VERSION,
        )?.let { return@withContext MoResult.Success(it) }

        // Data Minimization (§24, §25)
        val minimalContext = DataMinimizer.minimizeThread(messages)

        val request = AiRequest(
            capability = AiCapability.SUMMARIZE_THREAD,
            accountId = accountId,
            sourceMessageIds = messages.map { it.messageId },
            sourceThreadId = threadId,
            minimalContext = minimalContext,
            schemaVersion = SCHEMA_VERSION,
        )

        val provider = getActiveProvider() ?: return@withContext MoResult.Failure(
            MoError.InvalidConfiguration("No active AI provider available")
        )

        val startTime = clock()
        val rawResponse = when (val res = provider.execute(request)) {
            is MoResult.Success -> res.value
            is MoResult.Failure -> return@withContext res
        }
        val duration = clock() - startTime

        val validated = when (val res = AiOutputValidator.validateThreadSummary(rawResponse.rawPayload)) {
            is MoResult.Success -> res.value
            is MoResult.Failure -> return@withContext res
        }

        val aiResult = AiResult(
            capability = AiCapability.SUMMARIZE_THREAD,
            providerId = provider.providerId,
            modelId = rawResponse.modelId,
            sourceId = threadId,
            accountId = accountId,
            output = validated,
            confidence = validated.confidence,
            processingTimeMs = duration,
            createdAtEpochMs = clock(),
            version = SCHEMA_VERSION,
        )

        cache.put(aiResult)
        MoResult.Success(aiResult)
    }

    /**
     * Checks whether AI is permitted for the given capability under current user settings.
     */
    suspend fun isCapabilityPermitted(capability: AiCapability): Boolean {
        val enabled = preferences.isAiEnabled.first()
        if (!enabled) return false

        val provider = getActiveProvider() ?: return false

        // If remote, verify explicit consent
        if (provider.info.isRemote) {
            val consented = preferences.hasUserConsented.first()
            if (!consented) return false
        }

        val allowedCapabilities = preferences.allowedCapabilities.first()
        return allowedCapabilities.contains(capability)
    }

    /**
     * Resolves the active provider based on preferences and registry.
     */
    suspend fun getActiveProvider(): AiProvider? {
        val selectedIdStr = preferences.selectedProviderId.first()
        val providerId = AiProviderId(selectedIdStr)
        val provider = registry.get(providerId)
            ?: registry.getAllProviders().firstOrNull()

        if (provider == null) return null

        val availability = provider.checkAvailability()
        return if (availability == AiAvailabilityState.AVAILABLE) {
            provider
        } else {
            null
        }
    }

    /**
     * Cleans up all AI cache and state when an account is removed (Phase 26 §92).
     */
    fun handleAccountRemoved(accountId: String) {
        cache.invalidateAccount(accountId)
        MoLogger.i(TAG, "Invalidated AI cache for removed account")
    }
}
