package com.greninjaop.mailorganizer.domain.temporal

import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.core.temporal.DeterministicTemporalExtractor
import com.greninjaop.mailorganizer.core.temporal.ExtractionInput
import com.greninjaop.mailorganizer.core.temporal.ExtractionResult
import com.greninjaop.mailorganizer.data.local.ExtractedItemRecord
import com.greninjaop.mailorganizer.data.repository.IntelligenceRepository
import com.greninjaop.mailorganizer.data.repository.MailRepository
import kotlinx.coroutines.withContext
import java.util.TimeZone

/**
 * Extracts meetings/deadlines from one message and persists them (Phase 13).
 *
 * Architecture: UI → ViewModel → this use case →
 * [DeterministicTemporalExtractor] → [IntelligenceRepository].
 * Extraction logic never lives in the UI; the engine stays independently
 * testable.
 *
 * Storage: Phase 2's `extracted_items` table. Queryable attributes (type,
 * account, due/start date) are typed columns; the temporal detail
 * (end, timezone, location, URL, confidence, explanation, version) lives in
 * the versioned payload JSON via [TemporalPayloadJson].
 *
 * Idempotency & corrections (phase §26):
 * - Items stamped with the current [DeterministicTemporalExtractor.VERSION]
 *   are skipped unless [force] — re-running is idempotent.
 * - Items with an older version are replaced (delete + insert) unless the
 *   user marked them completed — user intent outranks automation.
 * - The model is correction-ready: a future Phase 12-style override can
 *   target items by id; extraction never writes immutable rows.
 *
 * Failure safety: any failure degrades to "no items extracted", never
 * crashes sync or the UI. Only safe diagnostics are logged (failure class
 * and message id — never email content).
 */
class ExtractTemporalUseCase(
    private val mail: MailRepository,
    private val intelligence: IntelligenceRepository,
    private val dispatchers: AppDispatchers,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    companion object {
        private const val TAG = "ExtractTemporal"
    }

    /**
     * Extracts temporal items for [messageId] and stores them.
     *
     * @return the extraction result, or null when extraction was skipped
     *   (message missing, already current, or a safe failure occurred).
     */
    suspend fun extract(
        messageId: String,
        accountId: String,
        force: Boolean = false,
    ): ExtractionResult? = withContext(dispatchers.io) {
        try {
            extractOrThrow(messageId, accountId, force)
        } catch (t: Throwable) {
            // Never break sync/UI because one email is odd.
            MoLogger.e(TAG, "Extraction failed safely: ${t.javaClass.simpleName}")
            null
        }
    }

    private suspend fun extractOrThrow(
        messageId: String,
        accountId: String,
        force: Boolean,
    ): ExtractionResult? {
        if (!force) {
            val existing = intelligence.getExtractedItems(messageId)
            if (existing.isNotEmpty() &&
                existing.all {
                    (TemporalPayloadJson.decode(it.payload)?.version ?: 0) >=
                        DeterministicTemporalExtractor.VERSION
                }
            ) {
                // Idempotent: already extracted at the current version.
                return null
            }
        }

        val message = mail.getMessage(messageId) ?: return null
        // Account isolation: never extract from another account's message.
        if (message.accountId != accountId) return null

        val input = ExtractionInput(
            messageId = messageId,
            threadId = message.threadId,
            subject = message.subject,
            snippet = message.snippet,
            bodyText = message.bodyText,
            // Phase §12: relative dates resolve against the message's
            // received timestamp — never "now".
            referenceEpochMs = message.timestampEpochMs,
            fallbackZoneId = TimeZone.getDefault().id,
        )
        val result = DeterministicTemporalExtractor.extract(input, clock)

        // Replace stale rows (keep user-completed ones — intent outranks).
        val existing = intelligence.getExtractedItems(messageId)
        val completedKeys = existing
            .filter { it.completed }
            .map { it.itemType to it.dueDateEpochMs }
            .toSet()
        val staleIds = existing.filter { !it.completed }.map { it.id }
        if (staleIds.isNotEmpty()) {
            intelligence.deleteExtractedItems(staleIds)
        }
        result.items.forEach { item ->
            // Don't duplicate a user-completed row with fresh output.
            val key = item.type.toExtractedItemType() to item.startEpochMs
            if (key in completedKeys) return@forEach
            intelligence.addExtractedItem(
                ExtractedItemRecord(
                    messageId = messageId,
                    accountId = accountId,
                    itemType = item.type.toExtractedItemType(),
                    title = item.title,
                    payload = TemporalPayloadJson.encode(item),
                    dueDateEpochMs = item.startEpochMs,
                    detectedAtEpochMs = item.extractedAtEpochMs,
                    completed = false,
                ),
            )
        }
        return result
    }
}
