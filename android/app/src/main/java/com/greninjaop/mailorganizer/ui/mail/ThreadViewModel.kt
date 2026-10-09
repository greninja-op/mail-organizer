package com.greninjaop.mailorganizer.ui.mail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.data.repository.MailRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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

    val state: StateFlow<ThreadDetailState> = combine(
        messages,
        expandedIds,
    ) { items, expanded ->
        if (items.isEmpty()) {
            ThreadDetailState.Empty
        } else {
            ThreadDetailState.Content(
                messages = items,
                expandedIds = expanded,
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
    ) : ThreadDetailState
    data object Empty : ThreadDetailState
}
