package com.greninjaop.mailorganizer.ui.home

import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.CompanyRecord
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.Priority

/**
 * Home dashboard state (Phase 11, phase §8–§19).
 *
 * One explicit state — no scattered booleans. Every section is driven by
 * real local data; nothing is fabricated (§10). [Empty] covers a new
 * account with no synchronized mail (§11); [Error] is retryable.
 */
sealed interface HomeContent {
    data object Loading : HomeContent

    data class Loaded(
        val account: AccountRecord,
        val attention: List<HomeMessage>,
        val highPriority: List<HomeMessage>,
        val recent: List<HomeMessage>,
        val categoryCounts: Map<MailCategory, Int>,
        val companies: List<HomeCompany>,
        val totalMessages: Int,
        val unreadCount: Int,
        val isSampleData: Boolean,
        val isOffline: Boolean,
    ) : HomeContent

    /** Connected account, but no mail yet — honest empty state (§11). */
    data class Empty(
        val account: AccountRecord?,
        val isOffline: Boolean,
    ) : HomeContent

    /** No account at all (fresh install before Phase 3). */
    data object NoAccount : HomeContent

    data class Error(val message: String) : HomeContent
}

/** One message row on the dashboard — display fields only, never bodies. */
data class HomeMessage(
    val messageId: String,
    val threadId: String,
    val subject: String,
    val senderName: String,
    val timestampEpochMs: Long,
    val unread: Boolean,
    val priority: Priority?,
    val category: MailCategory?,
)

/** One company entry on the dashboard. */
data class HomeCompany(
    val record: CompanyRecord,
    val messageCount: Int,
)

data class HomeUiState(
    val content: HomeContent = HomeContent.Loading,
)
