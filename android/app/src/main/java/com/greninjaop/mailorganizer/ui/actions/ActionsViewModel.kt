package com.greninjaop.mailorganizer.ui.actions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.core.actions.ActionStatus
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.ActionItemRecord
import com.greninjaop.mailorganizer.data.repository.AccountRepository
import com.greninjaop.mailorganizer.data.repository.IntelligenceRepository
import com.greninjaop.mailorganizer.data.repository.MailRepository
import com.greninjaop.mailorganizer.domain.actions.ConfirmationOutcome
import com.greninjaop.mailorganizer.domain.actions.ReviewActionUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Actions destination state (Phase 14).
 *
 * Backed by the action engine's cards — not the raw ACTION_REQUIRED
 * classification view (Phase 11's view-only seam, now filled). Cards are
 * ordered by urgency, then by due date. The ACTION_REQUIRED email list
 * remains available through the mail screen's action-required filter chip
 * (Phase 9).
 */
sealed interface ActionsContent {
    data object Loading : ActionsContent
    data class Loaded(
        val cards: List<ActionCardUi>,
        val account: AccountRecord,
    ) : ActionsContent
    data class Empty(val hasAccount: Boolean) : ActionsContent
    data class Error(val message: String) : ActionsContent
}

data class ActionsUiState(
    val content: ActionsContent = ActionsContent.Loading,
)

/**
 * One-shot UI events: confirmation dialog requests and outcome messages.
 * Copy is honest — "created" is never claimed without a real executor.
 */
sealed interface ActionsEvent {
    /** Show the confirmation dialog for this card (phase §31). */
    data class ConfirmAction(val card: ActionCardUi) : ActionsEvent

    /** Open the source email thread. */
    data class OpenThread(val threadId: String) : ActionsEvent

    /** Transient message (snackbar). */
    data class Message(val text: String) : ActionsEvent
}

class ActionsViewModel(
    private val accounts: AccountRepository,
    private val mail: MailRepository,
    private val intelligence: IntelligenceRepository,
    private val review: ReviewActionUseCase,
    private val dispatchers: AppDispatchers,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    companion object {
        private const val TAG = "ActionsViewModel"
        private const val LIMIT = 50
    }

    private val _events = MutableSharedFlow<ActionsEvent>()
    val events: SharedFlow<ActionsEvent> = _events.asSharedFlow()

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
                intelligence.observeOpenActionItems(account.accountId, LIMIT)
                    .map { rows -> toContent(rows, account) }
                    .catch { e ->
                        MoLogger.e(TAG, "Actions failed: ${e.javaClass.simpleName}")
                        emit(ActionsUiState(ActionsContent.Error("Couldn't load actions.")))
                    }
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ActionsUiState(ActionsContent.Loading),
        )

    /** Exposed for the shared top bar. */
    val currentAccount: StateFlow<AccountRecord?> = activeAccount

    private suspend fun toContent(
        rows: List<ActionItemRecord>,
        account: AccountRecord,
    ): ActionsUiState {
        // Only suggest-stage cards are shown; reviewed/confirmed cards stay
        // visible until dismissed/completed so the user sees their state.
        val visible = rows.filter {
            it.status == ActionStatus.SUGGESTED ||
                it.status == ActionStatus.REVIEWED ||
                it.status == ActionStatus.CONFIRMED
        }
        if (visible.isEmpty()) {
            return ActionsUiState(ActionsContent.Empty(hasAccount = true))
        }
        val messages = mail.getMessagesByIds(visible.map { it.messageId })
            .associateBy { it.messageId }
        val cards = visible.mapNotNull { row ->
            val m = messages[row.messageId] ?: return@mapNotNull null
            row.toCardUi(
                senderName = m.fromName?.ifBlank { m.fromAddress } ?: m.fromAddress,
                subject = m.subject,
            )
        }.sortedWith(
            compareBy<ActionCardUi> { it.urgency.ordinal }
                .thenBy { it.dueDateEpochMs ?: Long.MAX_VALUE },
        )
        return ActionsUiState(ActionsContent.Loaded(cards, account))
    }

    /** "Review" — records the user looked; the card expands its "why". */
    fun onReview(card: ActionCardUi) {
        viewModelScope.launch(dispatchers.io) {
            review.markReviewed(card.id)
        }
    }

    fun onDismiss(card: ActionCardUi) {
        viewModelScope.launch(dispatchers.io) {
            if (review.dismiss(card.id)) {
                _events.emit(ActionsEvent.Message("Suggestion dismissed."))
            }
        }
    }

    fun onComplete(card: ActionCardUi) {
        viewModelScope.launch(dispatchers.io) {
            if (review.complete(card.id)) {
                _events.emit(ActionsEvent.Message("Marked done."))
            }
        }
    }

    fun onOpenSource(card: ActionCardUi) {
        viewModelScope.launch {
            _events.emit(ActionsEvent.OpenThread(card.threadId))
        }
    }

    /**
     * "Act" — for internal suggestions this confirms and opens the source
     * email; for external proposals it requests the confirmation dialog
     * (phase §31). Nothing external ever happens without that dialog.
     */
    fun onAct(card: ActionCardUi) {
        viewModelScope.launch {
            if (card.requiresConfirmation) {
                _events.emit(ActionsEvent.ConfirmAction(card))
            } else {
                when (review.confirm(card.id)) {
                    is ConfirmationOutcome.RecordedInternal ->
                        _events.emit(ActionsEvent.OpenThread(card.threadId))
                    is ConfirmationOutcome.Failed ->
                        _events.emit(ActionsEvent.Message("Couldn't confirm that action."))
                    else ->
                        _events.emit(ActionsEvent.OpenThread(card.threadId))
                }
            }
        }
    }

    /**
     * Confirmation dialog result (phase §31). Confirming an external
     * proposal with no connected integration records the intent and
     * honestly reports "not connected" — never a fake success (§32).
     */
    fun onConfirmDialogResult(card: ActionCardUi, confirmed: Boolean) {
        if (!confirmed) return
        viewModelScope.launch(dispatchers.io) {
            when (val outcome = review.confirm(card.id)) {
                is ConfirmationOutcome.RecordedInternal ->
                    _events.emit(ActionsEvent.Message("Saved."))
                is ConfirmationOutcome.ExternalNotConnected ->
                    _events.emit(
                        ActionsEvent.Message(
                            outcome.detail?.let { detail ->
                                "$detail Your confirmation was saved — " +
                                    "nothing was created."
                            } ?: "Calendar isn't connected yet — nothing was " +
                                "created. Your confirmation was saved.",
                        ),
                    )
                is ConfirmationOutcome.Executed ->
                    _events.emit(ActionsEvent.Message("Done."))
                is ConfirmationOutcome.Failed ->
                    _events.emit(ActionsEvent.Message(outcome.message))
            }
        }
    }

}
