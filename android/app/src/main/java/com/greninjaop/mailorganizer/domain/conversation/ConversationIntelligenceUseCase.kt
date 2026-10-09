package com.greninjaop.mailorganizer.domain.conversation

import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.core.conversation.ConversationAnalysisInput
import com.greninjaop.mailorganizer.core.conversation.ConversationAnalysisResult
import com.greninjaop.mailorganizer.core.conversation.ConversationAnalyzer
import com.greninjaop.mailorganizer.core.conversation.ConversationState
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.ActionItemRecord
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.repository.AccountRepository
import com.greninjaop.mailorganizer.data.repository.IntelligenceRepository
import com.greninjaop.mailorganizer.data.repository.MailRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Domain Use Case for Conversation Intelligence (Phase 21).
 *
 * Responsibilities:
 * - Gathers thread messages and message-level intelligence (classification, priority, actions)
 * - Evaluates thread state using [ConversationAnalyzer]
 * - Provides account-scoped and thread-scoped observation methods
 * - Safe for unified inbox presentation (account-isolated analysis)
 */
class ConversationIntelligenceUseCase(
    private val mail: MailRepository,
    private val intelligence: IntelligenceRepository,
    private val accounts: AccountRepository,
    private val dispatchers: AppDispatchers,
    private val analyzer: ConversationAnalyzer = ConversationAnalyzer(),
    private val clock: () -> Long = System::currentTimeMillis,
) {

    /**
     * Analyzes a single thread by its local [threadId] and returns its conversation intelligence result.
     */
    suspend fun analyzeThread(
        threadId: String,
        nowEpochMs: Long = clock(),
    ): ConversationAnalysisResult? = withContext(dispatchers.io) {
        val messageList = getMessagesForThread(threadId)
        if (messageList.isEmpty()) {
            return@withContext null
        }

        val accountId = messageList.first().accountId
        val account = accounts.getById(accountId) ?: return@withContext null

        val messageIds = messageList.map { it.messageId }
        val classifications = intelligence.getClassifications(messageIds)
        val priorities = intelligence.getPriorities(messageIds)
        val actionItems = messageIds.associateWith { mid ->
            intelligence.getActionItemsByMessage(mid)
        }

        val input = ConversationAnalysisInput(
            accountId = accountId,
            userEmailAddress = account.emailAddress,
            threadId = threadId,
            messages = messageList,
            classificationsByMessageId = classifications,
            prioritiesByMessageId = priorities,
            actionItemsByMessageId = actionItems,
            nowEpochMs = nowEpochMs,
        )

        analyzer.analyze(input)
    }

    /**
     * Evaluates a known list of messages belonging to a thread.
     */
    suspend fun analyzeMessages(
        threadId: String,
        messages: List<MessageRecord>,
        userEmailAddress: String,
        accountId: String,
        nowEpochMs: Long = clock(),
    ): ConversationAnalysisResult = withContext(dispatchers.io) {
        if (messages.isEmpty()) {
            return@withContext ConversationAnalysisResult(
                accountId = accountId,
                threadId = threadId,
                state = ConversationState.NO_ACTION,
                confidence = com.greninjaop.mailorganizer.core.classify.Confidence.HIGH,
                explanation = com.greninjaop.mailorganizer.core.conversation.ConversationExplanation(
                    summary = "No messages found in this conversation.",
                ),
                lastMessageId = null,
                lastMessageEpochMs = 0L,
                lastUserMessageEpochMs = null,
                lastOtherPartyMessageEpochMs = null,
                pendingSinceEpochMs = null,
                staleSinceEpochMs = null,
            )
        }

        val messageIds = messages.map { it.messageId }
        val classifications = intelligence.getClassifications(messageIds)
        val priorities = intelligence.getPriorities(messageIds)
        val actionItems = messageIds.associateWith { mid ->
            intelligence.getActionItemsByMessage(mid)
        }

        val input = ConversationAnalysisInput(
            accountId = accountId,
            userEmailAddress = userEmailAddress,
            threadId = threadId,
            messages = messages,
            classificationsByMessageId = classifications,
            prioritiesByMessageId = priorities,
            actionItemsByMessageId = actionItems,
            nowEpochMs = nowEpochMs,
        )

        analyzer.analyze(input)
    }

    /**
     * Observes conversation state for a specific thread.
     */
    fun observeThreadConversation(
        threadId: String,
        accountEmail: String,
        accountId: String,
    ): Flow<ConversationAnalysisResult> {
        return mail.observeMessages(threadId, 200).map { messages ->
            if (messages.isEmpty()) {
                ConversationAnalysisResult(
                    accountId = accountId,
                    threadId = threadId,
                    state = ConversationState.NO_ACTION,
                    confidence = com.greninjaop.mailorganizer.core.classify.Confidence.HIGH,
                    explanation = com.greninjaop.mailorganizer.core.conversation.ConversationExplanation(
                        summary = "No messages",
                    ),
                    lastMessageId = null,
                    lastMessageEpochMs = 0L,
                    lastUserMessageEpochMs = null,
                    lastOtherPartyMessageEpochMs = null,
                    pendingSinceEpochMs = null,
                    staleSinceEpochMs = null,
                )
            } else {
                val messageIds = messages.map { it.messageId }
                val classifications = intelligence.getClassifications(messageIds)
                val priorities = intelligence.getPriorities(messageIds)
                val actionItems = messageIds.associateWith { mid ->
                    intelligence.getActionItemsByMessage(mid)
                }
                val input = ConversationAnalysisInput(
                    accountId = accountId,
                    userEmailAddress = accountEmail,
                    threadId = threadId,
                    messages = messages,
                    classificationsByMessageId = classifications,
                    prioritiesByMessageId = priorities,
                    actionItemsByMessageId = actionItems,
                    nowEpochMs = clock(),
                )
                analyzer.analyze(input)
            }
        }
    }

    private suspend fun getMessagesForThread(threadId: String): List<MessageRecord> {
        var result = emptyList<MessageRecord>()
        try {
            mail.observeMessages(threadId, 200).firstOrNull()?.let {
                result = it
            }
        } catch (t: Throwable) {
            MoLogger.w(TAG, "Error fetching messages for thread $threadId: ${t.message}")
        }
        return result
    }

    companion object {
        private const val TAG = "ConversationUseCase"
    }
}
