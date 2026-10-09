package com.greninjaop.mailorganizer.ui.mail

import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.ConnectionState
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.local.SyncStateRecord
import com.greninjaop.mailorganizer.data.local.SyncStatus
import com.greninjaop.mailorganizer.data.local.ThreadRecord
import com.greninjaop.mailorganizer.data.repository.AccountRepository
import com.greninjaop.mailorganizer.data.repository.MailRepository
import com.greninjaop.mailorganizer.data.repository.SyncStateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory fakes for Phase 6 ViewModel/seeder tests (no Robolectric). */
class FakeAccountRepository : AccountRepository {
    private val accounts = MutableStateFlow<List<AccountRecord>>(emptyList())

    override suspend fun upsert(account: AccountRecord) {
        accounts.value = accounts.value.filterNot { it.accountId == account.accountId } + account
    }

    override suspend fun getById(accountId: String): AccountRecord? =
        accounts.value.firstOrNull { it.accountId == accountId }

    override fun observeAll(): Flow<List<AccountRecord>> = accounts

    override fun observeEnabled(): Flow<List<AccountRecord>> =
        accounts.map { list -> list.filter { it.isEnabled } }

    override suspend fun updateConnectionState(accountId: String, state: ConnectionState) {
        accounts.value = accounts.value.map {
            if (it.accountId == accountId) it.copy(connectionState = state) else it
        }
    }

    override suspend fun recordSync(accountId: String, syncEpochMs: Long) {
        accounts.value = accounts.value.map {
            if (it.accountId == accountId) it.copy(lastSyncEpochMs = syncEpochMs) else it
        }
    }

    override suspend fun setEnabled(accountId: String, enabled: Boolean) {
        accounts.value = accounts.value.map {
            if (it.accountId == accountId) it.copy(isEnabled = enabled) else it
        }
    }

    override suspend fun deleteById(accountId: String) {
        accounts.value = accounts.value.filterNot { it.accountId == accountId }
    }

    fun seed(vararg records: AccountRecord) {
        accounts.value = records.toList()
    }
}

class FakeMailRepository : MailRepository {
    private val threads = MutableStateFlow<List<ThreadRecord>>(emptyList())
    private val messages = MutableStateFlow<List<MessageRecord>>(emptyList())
    var failWith: Throwable? = null

    private fun check() {
        failWith?.let { throw it }
    }

    override suspend fun saveThreadWithMessages(
        thread: ThreadRecord,
        messages: List<MessageRecord>,
    ) {
        check()
        threads.value = threads.value.filterNot { it.threadId == thread.threadId } + thread
        val ids = messages.map { it.messageId }.toSet()
        this.messages.value = this.messages.value.filterNot { it.messageId in ids } + messages
    }

    override fun observeThreads(accountId: String, limit: Int): Flow<List<ThreadRecord>> =
        threads.map { list ->
            check()
            list.filter { it.accountId == accountId }
                .sortedByDescending { it.latestMessageEpochMs }
                .take(limit)
        }

    override fun observeMessages(threadId: String, limit: Int): Flow<List<MessageRecord>> =
        messages.map { list ->
            check()
            list.filter { it.threadId == threadId }
                .sortedBy { it.timestampEpochMs }
                .take(limit)
        }

    override fun observeUnread(accountId: String, limit: Int): Flow<List<MessageRecord>> =
        messages.map { list ->
            list.filter { it.accountId == accountId && it.unread }.take(limit)
        }

    override fun observeStarred(accountId: String, limit: Int): Flow<List<MessageRecord>> =
        messages.map { list ->
            check()
            list.filter { it.accountId == accountId && it.starred }
                .sortedByDescending { it.timestampEpochMs }
                .take(limit)
        }

    override suspend fun searchByText(
        accountId: String,
        query: String,
        limit: Int,
    ): List<MessageRecord> {
        check()
        return messages.value.filter {
            it.accountId == accountId &&
                (it.subject.contains(query, ignoreCase = true) ||
                    (it.snippet?.contains(query, ignoreCase = true) == true))
        }.take(limit)
    }

    override suspend fun setRead(messageId: String, read: Boolean) {
        check()
        messages.value = messages.value.map {
            if (it.messageId == messageId) it.copy(unread = !read) else it
        }
    }

    override suspend fun setStarred(messageId: String, starred: Boolean) {
        check()
        messages.value = messages.value.map {
            if (it.messageId == messageId) it.copy(starred = starred) else it
        }
    }

    override suspend fun countByAccount(accountId: String): Int {
        check()
        return messages.value.count { it.accountId == accountId }
    }

    override suspend fun getThreadByGmailId(
        accountId: String,
        gmailThreadId: String,
    ): ThreadRecord? {
        check()
        return threads.value.firstOrNull {
            it.accountId == accountId && it.gmailThreadId == gmailThreadId
        }
    }

    override suspend fun updateThreadAggregates(threadId: String) {
        check()
        val msgs = messages.value.filter { it.threadId == threadId }
        val latest = msgs.maxByOrNull { it.timestampEpochMs }
        threads.value = threads.value.map {
            if (it.threadId == threadId) {
                it.copy(
                    messageCount = msgs.size,
                    unreadCount = msgs.count { m -> m.unread },
                    latestMessageId = latest?.messageId,
                    latestMessageEpochMs = latest?.timestampEpochMs ?: 0L,
                )
            } else it
        }
    }

    override suspend fun existingGmailIds(
        accountId: String,
        gmailIds: List<String>,
    ): Set<String> {
        check()
        val have = messages.value
            .filter { it.accountId == accountId }
            .mapNotNull { it.gmailMessageId }
            .toSet()
        return gmailIds.filter { it in have }.toSet()
    }

    override suspend fun deleteMessagesByGmailIds(
        accountId: String,
        gmailIds: List<String>,
    ): List<String> {
        check()
        val doomed = messages.value.filter {
            it.accountId == accountId && it.gmailMessageId in gmailIds
        }
        messages.value = messages.value - doomed.toSet()
        return doomed.map { it.threadId }.distinct()
    }

    override suspend fun getMessagesByIds(messageIds: List<String>): List<MessageRecord> {
        check()
        if (messageIds.isEmpty()) return emptyList()
        val byId = messages.value.associateBy { it.messageId }
        return messageIds.mapNotNull { byId[it] }
    }

    fun threadCount(): Int = threads.value.size
    fun messageCount(): Int = messages.value.size
    fun allMessages(): List<MessageRecord> = messages.value
}

class FakeSyncStateRepository : SyncStateRepository {
    private val states = mutableMapOf<String, SyncStateRecord>()

    override suspend fun ensureForAccount(accountId: String): SyncStateRecord =
        states.getOrPut(accountId) {
            SyncStateRecord(accountId, SyncStatus.NEVER_SYNCED)
        }

    override fun observe(accountId: String): Flow<SyncStateRecord?> =
        MutableStateFlow(states[accountId])

    override suspend fun markAttempt(
        accountId: String,
        status: SyncStatus,
        errorCode: String?,
    ) {
    }

    override suspend fun markSuccess(accountId: String, cursor: String?) {}
    override suspend fun updateCursor(accountId: String, cursor: String) {}
}

class FakeConnectivityObserver(initial: Boolean = true) : ConnectivityObserver {
    private val state = MutableStateFlow(initial)
    override val isOnline: Flow<Boolean> = state
    fun setOnline(online: Boolean) {
        state.value = online
    }
}

class FakeSampleDataPolicy(private val seed: Boolean) : SampleDataPolicy {
    override fun shouldSeed(): Boolean = seed
}
