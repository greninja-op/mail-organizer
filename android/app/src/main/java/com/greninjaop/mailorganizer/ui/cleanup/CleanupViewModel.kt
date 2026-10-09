package com.greninjaop.mailorganizer.ui.cleanup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.core.cleanup.CleanupCandidate
import com.greninjaop.mailorganizer.core.cleanup.CleanupCandidateStatus
import com.greninjaop.mailorganizer.core.cleanup.CleanupCandidateType
import com.greninjaop.mailorganizer.core.cleanup.CleanupGroup
import com.greninjaop.mailorganizer.core.cleanup.NewsletterSenderProfile
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.prefs.AccountSelection
import com.greninjaop.mailorganizer.data.prefs.ActiveAccountPreferences
import com.greninjaop.mailorganizer.data.repository.AccountRepository
import com.greninjaop.mailorganizer.di.AppContainer
import com.greninjaop.mailorganizer.domain.cleanup.CleanupUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UI State for the Noise, Newsletter & Cleanup Screen (Phase 20).
 */
data class CleanupUiState(
    val currentAccount: AccountRecord? = null,
    val isUnified: Boolean = false,
    val isLoading: Boolean = true,
    val cleanupGroups: List<CleanupGroup> = emptyList(),
    val newsletterSenders: List<NewsletterSenderProfile> = emptyList(),
    val totalReviewableCandidates: Int = 0,
    val selectedGroup: CleanupGroup? = null,
    val actionMessage: String? = null,
)

/**
 * Factory for CleanupViewModel.
 */
class CleanupViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CleanupViewModel(
            cleanupUseCase = container.cleanupUseCase,
            accounts = container.accountRepository,
            activeAccountPreferences = container.activeAccountPreferences,
            dispatchers = container.dispatchers,
        ) as T
    }
}

/**
 * ViewModel for Cleanup Screen.
 */
class CleanupViewModel(
    private val cleanupUseCase: CleanupUseCase,
    private val accounts: AccountRepository,
    private val activeAccountPreferences: ActiveAccountPreferences?,
    private val dispatchers: AppDispatchers,
) : ViewModel() {

    private val allAccounts = accounts.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val activeSelection = if (activeAccountPreferences != null) {
        activeAccountPreferences.activeAccountSelection
    } else {
        allAccounts.map { list ->
            val primary = list.filter { it.isEnabled }.minWithOrNull(
                compareBy<AccountRecord> { it.createdAtEpochMs }.thenBy { it.accountId },
            )
            if (primary != null) AccountSelection.Single(primary.accountId) else AccountSelection.Unified
        }
    }

    private val selectedGroupFlow = MutableStateFlow<CleanupGroup?>(null)
    private val actionMessageFlow = MutableStateFlow<String?>(null)
    private val refreshTrigger = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<CleanupUiState> = combine(
        allAccounts,
        activeSelection,
        selectedGroupFlow,
        actionMessageFlow,
        refreshTrigger,
    ) { accountList, selection, selectedGroup, actionMessage, _ ->
        val enabled = accountList.filter { it.isEnabled }
        val (currentAccount, isUnified) = when (selection) {
            is AccountSelection.Single -> {
                val acc = enabled.firstOrNull { it.accountId == selection.accountId } ?: enabled.firstOrNull()
                Pair(acc, false)
            }
            is AccountSelection.Unified -> {
                val primary = enabled.minWithOrNull(
                    compareBy<AccountRecord> { it.createdAtEpochMs }.thenBy { it.accountId },
                )
                Pair(primary, true)
            }
        }
        Triple(currentAccount, isUnified, selectedGroup to actionMessage)
    }.flatMapLatest { (account, isUnified, groupAndMessage) ->
        val (selectedGroup, actionMessage) = groupAndMessage
        flow {
            if (account == null) {
                emit(
                    CleanupUiState(
                        currentAccount = null,
                        isUnified = isUnified,
                        isLoading = false,
                        cleanupGroups = emptyList(),
                        newsletterSenders = emptyList(),
                        totalReviewableCandidates = 0,
                        selectedGroup = selectedGroup,
                        actionMessage = actionMessage,
                    )
                )
                return@flow
            }

            emit(
                CleanupUiState(
                    currentAccount = account,
                    isUnified = isUnified,
                    isLoading = true,
                    cleanupGroups = emptyList(),
                    newsletterSenders = emptyList(),
                    totalReviewableCandidates = 0,
                    selectedGroup = selectedGroup,
                    actionMessage = actionMessage,
                )
            )

            try {
                val groups = cleanupUseCase.getCleanupGroups(account.accountId)
                val senders = cleanupUseCase.getNewsletterSenders(account.accountId)
                val totalCandidates = groups.sumOf { it.candidateCount }

                emit(
                    CleanupUiState(
                        currentAccount = account,
                        isUnified = isUnified,
                        isLoading = false,
                        cleanupGroups = groups,
                        newsletterSenders = senders,
                        totalReviewableCandidates = totalCandidates,
                        selectedGroup = selectedGroup,
                        actionMessage = actionMessage,
                    )
                )
            } catch (t: Throwable) {
                MoLogger.e(TAG, "Failed loading cleanup data: ${t.javaClass.simpleName}")
                emit(
                    CleanupUiState(
                        currentAccount = account,
                        isUnified = isUnified,
                        isLoading = false,
                        cleanupGroups = emptyList(),
                        newsletterSenders = emptyList(),
                        totalReviewableCandidates = 0,
                        selectedGroup = selectedGroup,
                        actionMessage = "Failed to load cleanup recommendations.",
                    )
                )
            }
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        CleanupUiState(isLoading = true),
    )

    fun selectGroup(group: CleanupGroup?) {
        selectedGroupFlow.value = group
    }

    fun dismissCandidate(candidateId: String) {
        cleanupUseCase.updateCandidateStatus(candidateId, CleanupCandidateStatus.DISMISSED)
        actionMessageFlow.value = "Candidate dismissed from cleanup"
        refreshTrigger.value += 1
    }

    fun markCandidateReviewed(candidateId: String) {
        cleanupUseCase.updateCandidateStatus(candidateId, CleanupCandidateStatus.REVIEWED)
        actionMessageFlow.value = "Marked as reviewed"
        refreshTrigger.value += 1
    }

    fun consumeActionMessage() {
        actionMessageFlow.value = null
    }

    fun refresh() {
        refreshTrigger.value += 1
    }

    companion object {
        private const val TAG = "CleanupViewModel"
    }
}
