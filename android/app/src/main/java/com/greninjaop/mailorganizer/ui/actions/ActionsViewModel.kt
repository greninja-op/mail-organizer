package com.greninjaop.mailorganizer.ui.actions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.Priority
import com.greninjaop.mailorganizer.data.repository.AccountRepository
import com.greninjaop.mailorganizer.data.repository.IntelligenceRepository
import com.greninjaop.mailorganizer.data.repository.MailRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Actions destination state (Phase 11, phase §29–§32).
 */
sealed interface ActionsContent {
    data object Loading : ActionsContent
    data class Loaded(
        val items: List<ActionItem>,
        val account: AccountRecord,
    ) : ActionsContent
    data class Empty(val hasAccount: Boolean) : ActionsContent
    data class Error(val message: String) : ActionsContent
}

/**
 * One item needing attention. Display fields + the persisted explanation
 * only — the UI never needs raw email content to explain why an item is
 * here (same discipline as Phase 7/9).
 */
data class ActionItem(
    val messageId: String,
    val threadId: String,
    val subject: String,
    val senderName: String,
    val timestampEpochMs: Long,
    val unread: Boolean,
    val priority: Priority?,
    val explanation: String?,
)

data class ActionsUiState(
    val content: ActionsContent = ActionsContent.Loading,
)

/**
 * Backs the Actions destination (Phase 11).
 *
 * A view over Phase 9's action-required detection — **view only**
 * (phase §29–§30). Nothing here executes external actions; the action
 * engine itself is Phase 14's scope. No fake deadlines (§31).
 */
class ActionsViewModel(
    private val accounts: AccountRepository,
    private val mail: MailRepository,
    private val intelligence: IntelligenceRepository,
    private val dispatchers: AppDispatchers,
) : ViewModel() {

    companion object {
        private const val TAG = "ActionsViewModel"
        private const val LIMIT = 50
    }

    private val activeAccount: StateFlow<AccountRecord?> =
        accounts.observeAll()
            .map { list ->
                list.filter { it.isEnabled }.minWithOrNull(
                    compareBy<AccountRecord> { it.createdAtEpochMs }
                        .thenBy { it.accountId },
                )
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<ActionsUiState> =
        activeAccount.flatMapLatest { account ->
            if (account == null) {
                flowOf(ActionsUiState(ActionsContent.Empty(hasAccount = false)))
            } else {
                intelligence.observeByCategory(
                    account.accountId,
                    MailCategory.ACTION_REQUIRED,
                    LIMIT,
                ).map { records ->
                    val messages = mail.getMessagesByIds(records.map { it.messageId })
                        .associateBy { it.messageId }
                    val priorities = intelligence.getPriorities(records.map { it.messageId })
                    val items = records.mapNotNull { record ->
                        messages[record.messageId]?.let { m ->
                            ActionItem(
                                messageId = m.messageId,
                                threadId = m.threadId,
                                subject = m.subject.ifBlank { "(no subject)" },
                                senderName = m.fromName?.ifBlank { m.fromAddress } ?: m.fromAddress,
                                timestampEpochMs = m.timestampEpochMs,
                                unread = m.unread,
                                priority = priorities[m.messageId]?.priority,
                                explanation = record.explanation,
                            )
                        }
                    }
                    val content = if (items.isEmpty()) {
                        ActionsContent.Empty(hasAccount = true)
                    } else {
                        ActionsContent.Loaded(items, account)
                    }
                    ActionsUiState(content)
                }.catch { e ->
                    MoLogger.e(TAG, "Actions failed: ${e.javaClass.simpleName}")
                    emit(ActionsUiState(ActionsContent.Error("Couldn't load actions.")))
                }
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ActionsUiState(ActionsContent.Loading),
        )

    /** Exposed for the shared top bar (§36). */
    val currentAccount: StateFlow<AccountRecord?> = activeAccount
}
