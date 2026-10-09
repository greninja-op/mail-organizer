package com.greninjaop.mailorganizer.data.repository

import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.data.local.AccountDao
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.ConnectionState
import com.greninjaop.mailorganizer.data.local.SearchIndexStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Account boundary operations (Phase 2).
 *
 * The UI and (later) the sync engine talk to accounts through this
 * interface — never directly to [AccountDao]. Deleting an account cascades
 * to all account-owned rows via foreign keys.
 */
interface AccountRepository {
    suspend fun upsert(account: AccountRecord)
    suspend fun getById(accountId: String): AccountRecord?
    fun observeAll(): Flow<List<AccountRecord>>
    fun observeEnabled(): Flow<List<AccountRecord>>
    suspend fun updateConnectionState(accountId: String, state: ConnectionState)
    suspend fun recordSync(accountId: String, syncEpochMs: Long)
    suspend fun setEnabled(accountId: String, enabled: Boolean)
    suspend fun deleteById(accountId: String)
}

class RoomAccountRepository(
    private val accountDao: AccountDao,
    private val dispatchers: AppDispatchers,
    private val clock: () -> Long = System::currentTimeMillis,
    /**
     * FTS index writer (Phase 10). Account deletion cascades to messages
     * via foreign keys, which would orphan FTS rows — the account's index
     * rows are dropped first. If the account delete then fails, the
     * background indexer re-adds them (self-healing).
     */
    private val searchIndex: SearchIndexStore? = null,
) : AccountRepository {

    override suspend fun upsert(account: AccountRecord) =
        withContext(dispatchers.io) { accountDao.upsert(account) }

    override suspend fun getById(accountId: String): AccountRecord? =
        withContext(dispatchers.io) { accountDao.getById(accountId) }

    override fun observeAll(): Flow<List<AccountRecord>> = accountDao.observeAll()

    override fun observeEnabled(): Flow<List<AccountRecord>> = accountDao.observeEnabled()

    override suspend fun updateConnectionState(accountId: String, state: ConnectionState) =
        withContext(dispatchers.io) {
            accountDao.updateConnectionState(accountId, state, clock())
        }

    override suspend fun recordSync(accountId: String, syncEpochMs: Long) =
        withContext(dispatchers.io) {
            accountDao.updateLastSync(accountId, syncEpochMs, clock())
        }

    override suspend fun setEnabled(accountId: String, enabled: Boolean) =
        withContext(dispatchers.io) { accountDao.setEnabled(accountId, enabled) }

    override suspend fun deleteById(accountId: String) =
        withContext(dispatchers.io) {
            searchIndex?.deleteForAccount(accountId)
            accountDao.deleteById(accountId)
        }
}
