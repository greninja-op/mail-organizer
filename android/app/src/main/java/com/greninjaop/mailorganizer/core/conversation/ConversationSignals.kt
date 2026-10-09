package com.greninjaop.mailorganizer.core.conversation

import com.greninjaop.mailorganizer.core.classify.TextNormalizer
import java.util.Locale

/**
 * Deterministic detection of conversational expectations and negative signals (Phase 21 §18, §19, §25).
 *
 * Checks normalized email body and subject for:
 * - Direct requests, questions, confirmation demands
 * - Closure / resolution statements ("Thank you, resolved", "All set", etc.)
 * - System/automated broadcast negative signals
 */
object ConversationSignals {

    private val DIRECT_REQUEST_PATTERNS = listOf(
        "please let me know",
        "please let us know",
        "can you please",
        "could you please",
        "would you please",
        "can you confirm",
        "could you confirm",
        "please confirm",
        "let me know if",
        "let us know if",
        "what are your thoughts",
        "what do you think",
        "what time works",
        "are you available",
        "does that work for you",
        "please send me",
        "please send us",
        "please reply",
        "waiting for your",
        "looking forward to hearing from you",
        "any update on",
        "have you had a chance to",
        "kindly provide",
        "kindly confirm",
        "please provide",
    )

    private val RESOLUTION_PATTERNS = listOf(
        "thank you, this is all set",
        "this is all set",
        "all set now",
        "issue has been resolved",
        "has been resolved",
        "case closed",
        "ticket closed",
        "matter is closed",
        "[closed]",
        "closed",
        "resolved",
        "no further action needed",
        "no further response needed",
        "thanks for confirming",
        "thanks for letting me know",
        "thanks, that answers my question",
        "sounds good, see you then",
        "perfect, see you then",
        "got it, thanks",
        "received with thanks",
    )

    /**
     * Extracts reply-expectation signals from text.
     * Returns a list of human-readable matched signal descriptions.
     */
    fun extractReplyExpectationSignals(bodyText: String?, subject: String?): List<String> {
        val signals = mutableListOf<String>()
        val normBody = TextNormalizer.normalizeForMatch(bodyText.orEmpty())
        val normSubject = TextNormalizer.normalizeSubject(subject.orEmpty())

        for (pattern in DIRECT_REQUEST_PATTERNS) {
            if (normBody.contains(pattern) || normSubject.contains(pattern)) {
                signals.add("Contains direct request phrase: \"$pattern\"")
            }
        }

        // Direct question mark detection on substantial lines (not generic newsletter rhetorical questions)
        if (bodyText != null && bodyText.contains("?")) {
            val lines = bodyText.lines().map { it.trim() }.filter { it.isNotBlank() }
            for (line in lines) {
                if (line.endsWith("?") && line.length in 10..150) {
                    val lower = line.lowercase(Locale.ROOT)
                    if (lower.startsWith("can you") || lower.startsWith("could you") ||
                        lower.startsWith("would you") || lower.startsWith("are you") ||
                        lower.startsWith("what ") || lower.startsWith("when ") ||
                        lower.startsWith("how ") || lower.startsWith("where ") ||
                        lower.startsWith("do you") || lower.startsWith("will you")
                    ) {
                        signals.add("Contains direct question: \"${line.take(60)}...\"")
                        break
                    }
                }
            }
        }

        return signals
    }

    /**
     * Checks if the email contains explicit closure or resolution statements.
     */
    fun extractResolutionSignals(bodyText: String?, subject: String?): List<String> {
        val signals = mutableListOf<String>()
        val normBody = TextNormalizer.normalizeForMatch(bodyText.orEmpty())
        val normSubject = TextNormalizer.normalizeSubject(subject.orEmpty())

        for (pattern in RESOLUTION_PATTERNS) {
            if (normBody.contains(pattern) || normSubject.contains(pattern)) {
                signals.add("Contains closure statement: \"$pattern\"")
            }
        }

        return signals
    }
}
