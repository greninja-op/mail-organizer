package com.greninjaop.mailorganizer.core.conversation

import com.greninjaop.mailorganizer.core.classify.Confidence
import com.greninjaop.mailorganizer.data.local.ActionItemRecord
import com.greninjaop.mailorganizer.data.local.ClassificationRecord
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.local.PriorityRecord

/**
 * Input package for analyzing a conversation/thread.
 */
data class ConversationAnalysisInput(
    val accountId: String,
    val userEmailAddress: String,
    val threadId: String,
    val messages: List<MessageRecord>,
    val classificationsByMessageId: Map<String, ClassificationRecord?> = emptyMap(),
    val prioritiesByMessageId: Map<String, PriorityRecord?> = emptyMap(),
    val actionItemsByMessageId: Map<String, List<ActionItemRecord>> = emptyMap(),
    val nowEpochMs: Long = System.currentTimeMillis(),
)

/**
 * Deterministic Conversation Intelligence Analyzer (Phase 21 §7–§35).
 *
 * Guarantees:
 * - Local-first, pure function of inputs, zero network calls, zero AI hallucinations.
 * - Explains what appears to be happening in a thread with transparent human-readable reasons.
 * - Conservative: never equates "no reply" with "user forgot" or assumes an incoming email requires a reply.
 * - Negative signal protection: newsletters, notifications, automated receipts, spam, and no-reply
 *   senders are never labeled as awaiting user reply.
 * - Multi-account safe: account-scoped direction.
 */
class ConversationAnalyzer(
    private val clock: () -> Long = System::currentTimeMillis,
) {

    fun analyze(input: ConversationAnalysisInput): ConversationAnalysisResult {
        val now = if (input.nowEpochMs > 0) input.nowEpochMs else clock()
        val accountId = input.accountId
        val threadId = input.threadId

        if (input.messages.isEmpty()) {
            return ConversationAnalysisResult(
                accountId = accountId,
                threadId = threadId,
                state = ConversationState.NO_ACTION,
                confidence = Confidence.HIGH,
                explanation = ConversationExplanation(
                    summary = "No messages found in this conversation.",
                    reasons = listOf("Thread contains no recorded messages."),
                ),
                lastMessageId = null,
                lastMessageEpochMs = 0L,
                lastUserMessageEpochMs = null,
                lastOtherPartyMessageEpochMs = null,
                pendingSinceEpochMs = null,
                staleSinceEpochMs = null,
            )
        }

        // 1. Thread ordering: sort messages strictly by timestamp ascending (oldest first, newest last)
        val sortedMessages = input.messages.sortedWith(
            compareBy<MessageRecord> { it.timestampEpochMs }.thenBy { it.messageId }
        )

        val latestMessage = sortedMessages.last()

        // 2. Build timeline and classify participant roles
        val timeline = sortedMessages.map { msg ->
            val role = ParticipantClassifier.classifyRole(msg.fromAddress, input.userEmailAddress)
            ConversationTimelineEntry(
                messageId = msg.messageId,
                senderName = msg.fromName?.ifBlank { msg.fromAddress } ?: msg.fromAddress,
                senderAddress = msg.fromAddress,
                role = role,
                timestampEpochMs = msg.timestampEpochMs,
                isLatest = msg.messageId == latestMessage.messageId,
            )
        }

        val userMessages = timeline.filter { it.role == ParticipantRole.USER }
        val otherMessages = timeline.filter { it.role == ParticipantRole.OTHER_PARTY }

        val lastUserMessage = userMessages.maxByOrNull { it.timestampEpochMs }
        val lastOtherMessage = otherMessages.maxByOrNull { it.timestampEpochMs }

        val latestRole = ParticipantClassifier.classifyRole(latestMessage.fromAddress, input.userEmailAddress)
        val latestClassification = input.classificationsByMessageId[latestMessage.messageId]
        val latestCategory = latestClassification?.category
        val latestActionItems = input.actionItemsByMessageId[latestMessage.messageId].orEmpty()
        val hasActionRequired = latestCategory == MailCategory.ACTION_REQUIRED ||
            latestActionItems.any { !it.completed && !it.dismissed }

        // Check for resolution statements in the latest message
        val resolutionSignals = ConversationSignals.extractResolutionSignals(
            bodyText = latestMessage.bodyText,
            subject = latestMessage.subject,
        )

        // Negative category signals (newsletters, promotions, notifications, receipts, spam)
        val isNegativeCategory = latestCategory in listOf(
            MailCategory.PROMOTIONS,
            MailCategory.NEWSLETTERS,
            MailCategory.NOTIFICATIONS,
            MailCategory.RECEIPTS_ORDERS,
            MailCategory.LOW_VALUE,
        ) || latestRole == ParticipantRole.AUTOMATED_NO_REPLY

        // 3. Evaluate conversation state machine

        // Case A: Latest message contains clear closure / resolution language
        if (resolutionSignals.isNotEmpty()) {
            return ConversationAnalysisResult(
                accountId = accountId,
                threadId = threadId,
                state = ConversationState.RESOLVED,
                confidence = Confidence.HIGH,
                explanation = ConversationExplanation(
                    summary = "Conversation appears resolved",
                    reasons = listOf(
                        "Latest message contains closure phrase: ${resolutionSignals.first()}",
                        "No outstanding requests detected afterward.",
                    ),
                    keySignals = resolutionSignals,
                ),
                lastMessageId = latestMessage.messageId,
                lastMessageEpochMs = latestMessage.timestampEpochMs,
                lastUserMessageEpochMs = lastUserMessage?.timestampEpochMs,
                lastOtherPartyMessageEpochMs = lastOtherMessage?.timestampEpochMs,
                pendingSinceEpochMs = null,
                staleSinceEpochMs = null,
                timeline = timeline,
            )
        }

        // Case B: Latest message was sent by the USER
        if (latestRole == ParticipantRole.USER) {
            val timeSinceUserReply = now - latestMessage.timestampEpochMs
            val isRecentlyReplied = timeSinceUserReply < ConversationAnalysisResult.RECENT_REPLY_WINDOW_MS
            val isStale = timeSinceUserReply >= ConversationAnalysisResult.STALE_THRESHOLD_MS
            val isFollowUpCandidate = timeSinceUserReply >= ConversationAnalysisResult.FOLLOW_UP_THRESHOLD_MS && !isStale

            // Check if user asked a question or has expectation of response
            val userExpectationSignals = ConversationSignals.extractReplyExpectationSignals(
                latestMessage.bodyText,
                latestMessage.subject,
            )

            val state = when {
                isStale -> ConversationState.STALE_CONVERSATION
                isRecentlyReplied && userExpectationSignals.isEmpty() -> ConversationState.RECENTLY_REPLIED
                else -> ConversationState.AWAITING_OTHER_PARTY
            }

            val summary = when (state) {
                ConversationState.STALE_CONVERSATION -> "No response detected for over 7 days"
                ConversationState.RECENTLY_REPLIED -> "You replied recently"
                ConversationState.AWAITING_OTHER_PARTY -> "Likely waiting for response from other party"
                else -> "Awaiting response"
            }

            val reasons = mutableListOf<String>()
            reasons.add("You sent the latest message in this thread.")
            if (otherMessages.isNotEmpty()) {
                val counterparty = otherMessages.last().senderName
                reasons.add("Waiting for response from $counterparty.")
            }
            if (isFollowUpCandidate) {
                reasons.add("Sent over 3 days ago with no reply received yet.")
            }

            return ConversationAnalysisResult(
                accountId = accountId,
                threadId = threadId,
                state = state,
                confidence = if (userExpectationSignals.isNotEmpty() || hasActionRequired) Confidence.HIGH else Confidence.MEDIUM,
                explanation = ConversationExplanation(
                    summary = summary,
                    reasons = reasons,
                    keySignals = userExpectationSignals,
                ),
                lastMessageId = latestMessage.messageId,
                lastMessageEpochMs = latestMessage.timestampEpochMs,
                lastUserMessageEpochMs = latestMessage.timestampEpochMs,
                lastOtherPartyMessageEpochMs = lastOtherMessage?.timestampEpochMs,
                pendingSinceEpochMs = latestMessage.timestampEpochMs,
                staleSinceEpochMs = if (isStale) latestMessage.timestampEpochMs + ConversationAnalysisResult.STALE_THRESHOLD_MS else null,
                timeline = timeline,
                isFollowUpCandidate = isFollowUpCandidate,
                followUpReason = if (isFollowUpCandidate) "You replied more than 3 days ago; no response detected." else null,
            )
        }

        // Case C: Latest message was sent by an AUTOMATED / NO-REPLY address or is in a passive category
        if (isNegativeCategory) {
            return ConversationAnalysisResult(
                accountId = accountId,
                threadId = threadId,
                state = ConversationState.NO_ACTION,
                confidence = Confidence.HIGH,
                explanation = ConversationExplanation(
                    summary = "No response expected for automated or informational message",
                    reasons = listOf(
                        if (latestRole == ParticipantRole.AUTOMATED_NO_REPLY)
                            "Latest message is from an automated/no-reply sender (${latestMessage.fromAddress})."
                        else
                            "Latest message is classified as ${latestCategory?.name?.lowercase() ?: "passive"}."
                    ),
                ),
                lastMessageId = latestMessage.messageId,
                lastMessageEpochMs = latestMessage.timestampEpochMs,
                lastUserMessageEpochMs = lastUserMessage?.timestampEpochMs,
                lastOtherPartyMessageEpochMs = lastOtherMessage?.timestampEpochMs,
                pendingSinceEpochMs = null,
                staleSinceEpochMs = null,
                timeline = timeline,
            )
        }

        // Case D: Latest message was sent by OTHER PARTY
        val replyExpectationSignals = ConversationSignals.extractReplyExpectationSignals(
            latestMessage.bodyText,
            latestMessage.subject,
        )

        // Evaluate whether a reply is expected
        val expectsReply = replyExpectationSignals.isNotEmpty() || hasActionRequired
        val timeSinceOtherMessage = now - latestMessage.timestampEpochMs
        val isStale = timeSinceOtherMessage >= ConversationAnalysisResult.STALE_THRESHOLD_MS

        if (expectsReply) {
            val state = if (isStale) ConversationState.STALE_CONVERSATION else ConversationState.AWAITING_USER_REPLY
            val senderName = latestMessage.fromName?.ifBlank { latestMessage.fromAddress } ?: latestMessage.fromAddress

            val reasons = mutableListOf<String>()
            reasons.add("Latest message is from $senderName.")
            if (replyExpectationSignals.isNotEmpty()) {
                reasons.add(replyExpectationSignals.first())
            }
            if (hasActionRequired) {
                reasons.add("Action is required on this email.")
            }
            reasons.add("No reply from you was detected afterward.")

            return ConversationAnalysisResult(
                accountId = accountId,
                threadId = threadId,
                state = state,
                confidence = if (replyExpectationSignals.isNotEmpty() && hasActionRequired) Confidence.HIGH else Confidence.MEDIUM,
                explanation = ConversationExplanation(
                    summary = if (state == ConversationState.STALE_CONVERSATION)
                        "Pending message from $senderName has been inactive for over 7 days"
                    else
                        "Likely awaiting your reply",
                    reasons = reasons,
                    keySignals = replyExpectationSignals,
                ),
                lastMessageId = latestMessage.messageId,
                lastMessageEpochMs = latestMessage.timestampEpochMs,
                lastUserMessageEpochMs = lastUserMessage?.timestampEpochMs,
                lastOtherPartyMessageEpochMs = latestMessage.timestampEpochMs,
                pendingSinceEpochMs = latestMessage.timestampEpochMs,
                staleSinceEpochMs = if (isStale) latestMessage.timestampEpochMs + ConversationAnalysisResult.STALE_THRESHOLD_MS else null,
                timeline = timeline,
                isFollowUpCandidate = false,
            )
        }

        // If no explicit question or action required, standard counterparty message without reply expectation
        return ConversationAnalysisResult(
            accountId = accountId,
            threadId = threadId,
            state = ConversationState.NO_ACTION,
            confidence = Confidence.MEDIUM,
            explanation = ConversationExplanation(
                summary = "Informational update; no reply explicitly expected",
                reasons = listOf(
                    "Latest message is from ${latestMessage.fromName?.ifBlank { latestMessage.fromAddress } ?: latestMessage.fromAddress}.",
                    "No explicit request, question, or action required detected.",
                ),
            ),
            lastMessageId = latestMessage.messageId,
            lastMessageEpochMs = latestMessage.timestampEpochMs,
            lastUserMessageEpochMs = lastUserMessage?.timestampEpochMs,
            lastOtherPartyMessageEpochMs = latestMessage.timestampEpochMs,
            pendingSinceEpochMs = null,
            staleSinceEpochMs = null,
            timeline = timeline,
        )
    }
}
