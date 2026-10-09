package com.greninjaop.mailorganizer.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.repository.AccountRepository
import com.greninjaop.mailorganizer.data.repository.IntelligenceRepository
import com.greninjaop.mailorganizer.data.repository.MailRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Category list + detail state (Phase 11, phase §21–§24).
 */
sealed interface CategoriesContent {
    data object Loading : CategoriesContent
    data class Ready(
        val counts: Map<MailCategory, Int>,
        val account: AccountRecord,
    ) : CategoriesContent
    data class Empty(val hasAccount: Boolean) : CategoriesContent
    data class Error(val message: String) : CategoriesContent
}

/** One row in a category detail list. */
data class CategoryMessage(
    val messageId: String,
    val threadId: String,
    val subject: String,
    val senderName: String,
    val timestampEpochMs: Long,
    val unread: Boolean,
)

sealed interface CategoryDetailContent {
    data object Loading : CategoryDetailContent
    data class Loaded(
        val category: MailCategory,
        val messages: List<CategoryMessage>,
    ) : CategoryDetailContent
    data class Error(val message: String) : CategoryDetailContent
}

data class CategoriesUiState(
    val content: CategoriesContent = CategoriesContent.Loading,
)

/**
 * Backs the Categories destination (Phase 11).
 *
 * Counts come from one GROUP BY query (§23 — explicit about what a count
 * means: classified messages per category). The detail list resolves
 * messages for one category with a single bounded lookup. Empty
 * categories show an honest empty state (§24), never fake examples.
 */
class CategoriesViewModel(
    private val accounts: AccountRepository,
    private val mail: MailRepository,
    private val intelligence: IntelligenceRepository,
    private val dispatchers: AppDispatchers,
) : ViewModel() {

    companion object {
        private const val TAG = "CategoriesViewModel"
        private const val DETAIL_LIMIT = 50
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

    val state: StateFlow<CategoriesUiState> =
        activeAccount.map { account ->
            if (account == null) {
                CategoriesContent.Empty(hasAccount = false)
            } else {
                val counts = intelligence.categoryCounts(account.accountId)
                if (counts.isEmpty()) CategoriesContent.Empty(hasAccount = true)
                else CategoriesContent.Ready(counts, account)
            }
        }.map { CategoriesUiState(it) }
            .catch { e ->
                MoLogger.e(TAG, "Categories failed: ${e.javaClass.simpleName}")
                emit(CategoriesUiState(CategoriesContent.Error("Couldn't load categories.")))
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                CategoriesUiState(CategoriesContent.Loading),
            )

    private val selectedCategory = MutableStateFlow<MailCategory?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val detail: StateFlow<CategoryDetailContent> =
        combine(activeAccount, selectedCategory) { account, category ->
            account to category
        }.flatMapLatest { (account, category) ->
            if (account == null || category == null) {
                flowOf(CategoryDetailContent.Loading)
            } else {
                intelligence.observeByCategory(account.accountId, category, DETAIL_LIMIT)
                    .map { records ->
                        val messages = mail.getMessagesByIds(records.map { it.messageId })
                            .associateBy { it.messageId }
                        // Preserve classification order (newest classification first).
                        val items = records.mapNotNull { messages[it.messageId]?.toCategoryMessage() }
                        CategoryDetailContent.Loaded(category, items) as CategoryDetailContent
                    }
                    .catch { e ->
                        MoLogger.e(TAG, "Category detail failed: ${e.javaClass.simpleName}")
                        emit(CategoryDetailContent.Error("Couldn't load this category."))
                    }
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            CategoryDetailContent.Loading,
        )

    fun selectCategory(category: MailCategory?) {
        selectedCategory.value = category
    }

    /** Exposed for the shared top bar (§36). */
    val currentAccount: StateFlow<AccountRecord?> = activeAccount

    private fun MessageRecord.toCategoryMessage() = CategoryMessage(
        messageId = messageId,
        threadId = threadId,
        subject = subject.ifBlank { "(no subject)" },
        senderName = fromName?.ifBlank { fromAddress } ?: fromAddress,
        timestampEpochMs = timestampEpochMs,
        unread = unread,
    )
}
