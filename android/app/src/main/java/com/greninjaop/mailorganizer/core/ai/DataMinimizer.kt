package com.greninjaop.mailorganizer.core.ai

import com.greninjaop.mailorganizer.core.company.SenderIntelligence
import com.greninjaop.mailorganizer.core.privacy.SecuritySanitizer
import com.greninjaop.mailorganizer.data.local.MessageRecord

/**
 * Data Minimization Engine for AI Fallback requests (Phase 26 §24, §25, §26, §84, §86).
 *
 * Enforces the strict privacy boundary before any email text can leave the local environment:
 * - Omits all OAuth credentials, bearer tokens, passwords, and cookies.
 * - Extracts ONLY the sender's registrable domain (omits full personal email mailbox names).
 * - Caps subject to a bounded character length (120 chars).
 * - Truncates body/snippet to a minimal preview length (500 chars).
 * - Strips all attachment metadata, raw MIME boundaries, and raw HTML.
 * - Scrubs any inline authentication token patterns or email strings via [SecuritySanitizer].
 */
object DataMinimizer {

    private const val MAX_SUBJECT_LENGTH = 120
    private const val MAX_SNIPPET_LENGTH = 500

    /**
     * Minimizes a [MessageRecord] into an [AiMinimalContext].
     *
     * Total function — handles null/blank fields gracefully without exceptions.
     */
    fun minimize(message: MessageRecord): AiMinimalContext {
        val domain = SenderIntelligence.domainOf(message.fromAddress)

        val boundedSubject = (message.subject)
            .take(MAX_SUBJECT_LENGTH)
            .trim()

        val rawText = message.snippet?.takeIf { it.isNotBlank() } ?: message.bodyText.orEmpty()
        val boundedText = rawText
            .take(MAX_SNIPPET_LENGTH)
            .trim()

        // Sanitize any potential tokens, auth headers, or raw personal emails from text
        val sanitizedSubject = SecuritySanitizer.sanitizeForLog(boundedSubject)
        val sanitizedSnippet = SecuritySanitizer.sanitizeForLog(boundedText)

        return AiMinimalContext(
            subject = sanitizedSubject,
            senderDomain = domain,
            snippet = sanitizedSnippet,
            timestampEpochMs = message.timestampEpochMs,
        )
    }

    /**
     * Minimizes a thread's messages into an aggregated minimal context for summarization.
     * Takes only newest and oldest messages, capping total context length to 1000 chars.
     */
    fun minimizeThread(messages: List<MessageRecord>): AiMinimalContext {
        if (messages.isEmpty()) {
            return AiMinimalContext(
                subject = "",
                senderDomain = "",
                snippet = "",
                timestampEpochMs = 0L,
            )
        }

        val first = messages.first()
        val latest = messages.last()
        val domain = SenderIntelligence.domainOf(latest.fromAddress)

        val subject = (first.subject.ifBlank { latest.subject })
            .take(MAX_SUBJECT_LENGTH)
            .trim()

        val combinedSnippets = messages
            .takeLast(3) // Only the 3 most recent messages in conversation
            .mapNotNull { m ->
                val text = m.snippet?.takeIf { it.isNotBlank() } ?: m.bodyText.orEmpty()
                text.take(300).trim().takeIf { it.isNotEmpty() }
            }
            .joinToString(separator = "\n---\n")
            .take(1000)

        return AiMinimalContext(
            subject = SecuritySanitizer.sanitizeForLog(subject),
            senderDomain = domain,
            snippet = SecuritySanitizer.sanitizeForLog(combinedSnippets),
            timestampEpochMs = latest.timestampEpochMs,
        )
    }
}
