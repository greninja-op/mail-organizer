package com.greninjaop.mailorganizer.core.conversation

import com.greninjaop.mailorganizer.core.classify.Confidence

/**
 * State of an email conversation/thread (Phase 21).
 *
 * Explains what appears to be happening in a thread without inventing intent.
 */
enum class ConversationState(val displayLabel: String) {
    /**
     * The conversation is passive or completed; no pending response or attention required.
     */
    NO_ACTION("No action needed"),

    /**
     * Another party sent the latest message which appears to expect or require a user response.
     */
    AWAITING_USER_REPLY("Awaiting your reply"),

    /**
     * The user sent the latest message asking or awaiting a response from another participant.
     */
    AWAITING_OTHER_PARTY("Waiting for response"),

    /**
     * The user recently replied (within the recent window), acknowledging or following up.
     */
    RECENTLY_REPLIED("Recently replied"),

    /**
     * A conversation was awaiting a reply, but a meaningful amount of time has elapsed
     * without new activity. Documented stale threshold applies (e.g. 7 days).
     */
    STALE_CONVERSATION("Stale conversation"),

    /**
     * Explicit resolution language, completion confirmation, or closure detected.
     */
    RESOLVED("Resolved"),

    /**
     * Insufficient evidence or conflicting signals to infer a confident state.
     */
    UNKNOWN("Unknown"),
}

/**
 * Role/direction of a participant or message sender relative to the account.
 */
enum class ParticipantRole {
    /**
     * The message was sent by the authenticated user / account owner.
     */
    USER,

    /**
     * The message was sent by an external person or counterparty.
     */
    OTHER_PARTY,

    /**
     * The message was sent by an automated/system/no-reply address.
     */
    AUTOMATED_NO_REPLY,
}

/**
 * Timeline entry for explainability in conversation details.
 */
data class ConversationTimelineEntry(
    val messageId: String,
    val senderName: String,
    val senderAddress: String,
    val role: ParticipantRole,
    val timestampEpochMs: Long,
    val isLatest: Boolean,
)

/**
 * Structured evidence and explanation supporting the conversation state.
 */
data class ConversationExplanation(
    val summary: String,
    val reasons: List<String> = emptyList(),
    val keySignals: List<String> = emptyList(),
)

/**
 * Result of analyzing a conversation/thread.
 */
data class ConversationAnalysisResult(
    val accountId: String,
    val threadId: String,
    val state: ConversationState,
    val confidence: Confidence,
    val explanation: ConversationExplanation,
    val lastMessageId: String?,
    val lastMessageEpochMs: Long,
    val lastUserMessageEpochMs: Long?,
    val lastOtherPartyMessageEpochMs: Long?,
    val pendingSinceEpochMs: Long?,
    val staleSinceEpochMs: Long?,
    val timeline: List<ConversationTimelineEntry> = emptyList(),
    val isFollowUpCandidate: Boolean = false,
    val followUpReason: String? = null,
    val analysisVersion: Int = CURRENT_VERSION,
) {
    companion object {
        const val CURRENT_VERSION = 1
        /** 7 days in milliseconds for stale threshold */
        const val STALE_THRESHOLD_MS = 7L * 24 * 60 * 60 * 1000
        /** 2 days in milliseconds for recently replied window */
        const val RECENT_REPLY_WINDOW_MS = 2L * 24 * 60 * 60 * 1000
        /** 3 days in milliseconds before an awaiting-other-party thread is a follow-up candidate */
        const val FOLLOW_UP_THRESHOLD_MS = 3L * 24 * 60 * 60 * 1000
    }
}
