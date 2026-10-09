package com.greninjaop.mailorganizer.ui.mail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.core.temporal.ExtractedTemporal
import com.greninjaop.mailorganizer.data.local.ClassificationRecord
import com.greninjaop.mailorganizer.data.local.PriorityRecord
import com.greninjaop.mailorganizer.data.repository.IntelligenceRepository
import com.greninjaop.mailorganizer.data.repository.MailRepository
import com.greninjaop.mailorganizer.domain.temporal.toExtractedTemporal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

/**
 * Backs the Phase 6 thread/conversation screen.
 *
 * Thread ordering (phase §51): messages are shown oldest-first, newest last
 * — the Gmail conversation convention. The newest message starts expanded;
 * older messages are collapsed and expand inline (§19: no separate screens
 * just to read a thread). Account scope is inherited from the thread row
 * itself (threads are account-scoped by schema, §33).
 */
class ThreadViewModel(
    private val threadId: String,
    private val mail: MailRepository,
    private val intelligence: IntelligenceRepository,
    private val dispatchers: AppDispatchers,
) : ViewModel() {

    private val expandedIds = MutableStateFlow<Set<String>>(emptySet())
    private var defaultExpanded = false

    private val messages = mail.observeMessages(threadId, MAX_THREAD_MESSAGES)
        .map { records -> records.map { it.toMessageItem() }.inThreadOrder() }
        .onEach { items ->
            // Default: the newest message starts expanded (§19).
            if (!defaultExpanded && items.isNotEmpty()) {
                defaultExpanded = true
                expandedIds.value = setOf(items.last().messageId)
            }
        }
        .catch { t ->
            MoLogger.e(TAG, "Thread load failed: ${t.javaClass.simpleName}")
            emit(emptyList())
        }

    /**
     * Phase 7: classification per message (message-level evidence preserved,
     * phase §35). Threads are bounded (200), so one lookup per visible
     * message is fine; failures degrade to "no classification shown".
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val classifications =
        messages.mapLatest { items ->
            items.associate { item ->
                item.messageId to try {
                    intelligence.getClassification(item.messageId)
                } catch (t: Throwable) {
                    MoLogger.e(TAG, "Classification lookup failed: ${t.javaClass.simpleName}")
                    null
                }
            }
        }

    /**
     * Phase 9: priority per message (message-level evidence preserved).
     * Threads are bounded (200), so one batch lookup per visible thread
     * is fine; failures degrade to "no priority shown".
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val priorities =
        messages.mapLatest { items ->
            try {
                intelligence.getPriorities(items.map { it.messageId })
            } catch (t: Throwable) {
                MoLogger.e(TAG, "Priority lookup failed: ${t.javaClass.simpleName}")
                emptyMap()
            }
        }

    /**
     * Phase 13: temporal items per message (meetings/deadlines extracted
     * on-device). Threads are bounded (200); one lookup per visible message
     * is fine; failures degrade to "no temporal section shown".
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val temporalItems =
        messages.mapLatest { items ->
            items.associate { item ->
                item.messageId to try {
                    intelligence.getExtractedItems(item.messageId)
                        .mapNotNull { it.toExtractedTemporal() }
                } catch (t: Throwable) {
                    MoLogger.e(TAG, "Temporal lookup failed: ${t.javaClass.simpleName}")
                    emptyList<ExtractedTemporal>()
                }
            }
        }

    val state: StateFlow<ThreadDetailState> = combine(
        messages,
        expandedIds,
        classifications,
        priorities,
        temporalItems,
    ) { items, expanded, classById, prioById, temporalById ->
        if (items.isEmpty()) {
            ThreadDetailState.Empty
        } else {
            ThreadDetailState.Content(
                messages = items,
                expandedIds = expanded,
                classifications = classById,
                priorities = prioById,
                temporalItems = temporalById,
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ThreadDetailState.Loading,
    )

    /** Subject for the top bar: threads share one subject; derived from messages. */
    val subject: StateFlow<String> = messages
        .map { items -> items.firstOrNull()?.subject.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    fun toggleExpanded(messageId: String) {
        val current = expandedIds.value
        expandedIds.value =
            if (current.contains(messageId)) current - messageId else current + messageId
    }

    /** Deep-link support (§37): ensure a specific message is expanded. */
    fun ensureExpanded(messageId: String) {
        if (!expandedIds.value.contains(messageId)) {
            expandedIds.value = expandedIds.value + messageId
        }
    }

    private companion object {
        const val TAG = "ThreadViewModel"
        /** Threads are bounded; Gmail threads rarely exceed this (§17). */
        const val MAX_THREAD_MESSAGES = 200
    }
}

/** Explicit thread-screen state (phase §35). */
sealed interface ThreadDetailState {
    data object Loading : ThreadDetailState
    data class Content(
        val messages: List<MessageItem>,
        val expandedIds: Set<String>,
        /** Message-level classifications by message id (Phase 7). */
        val classifications: Map<String, ClassificationRecord?> = emptyMap(),
        /** Message-level priorities by message id (Phase 9). */
        val priorities: Map<String, PriorityRecord?> = emptyMap(),
        /** Message-level temporal items by message id (Phase 13). */
        val temporalItems: Map<String, List<ExtractedTemporal>> = emptyMap(),
    ) : ThreadDetailState
    data object Empty : ThreadDetailState
}
