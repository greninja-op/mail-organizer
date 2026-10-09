package com.greninjaop.mailorganizer.data.repository

import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.data.local.AppDatabase
import com.greninjaop.mailorganizer.data.local.SyncStateDao
import com.greninjaop.mailorganizer.data.local.SyncStateRecord
import com.greninjaop.mailorganizer.data.local.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Sync-state bookkeeping (Phase 2 — foundation; the engine is Phase 4).
 *
 * [ensureForAccount] lazily creates the per-account row so the sync engine
 * never has to handle "no row yet" as a special case.
 */
interface SyncStateRepository {
    suspend fun ensureForAccount(accountId: String): SyncStateRecord
    fun observe(accountId: String): Flow<SyncStateRecord?>
    suspend fun markAttempt(accountId: String, status: SyncStatus, errorCode: String? = null)
    suspend fun markSuccess(accountId: String, cursor: String?)

    /**
     * Advances the opaque cursor mid-sync (Phase 4, phase §22). The cursor
     * must ONLY be advanced after the corresponding data is committed —
     * callers (the sync engine) own that ordering guarantee.
     */
    suspend fun updateCursor(accountId: String, cursor: String)
}

class RoomSyncStateRepository(
    private val db: AppDatabase,
    private val dispatchers: AppDispatchers,
    private val clock: () -> Long = System::currentTimeMillis,
) : SyncStateRepository {

    private val dao: SyncStateDao get() = db.syncStateDao()

    override suspend fun ensureForAccount(accountId: String): SyncStateRecord =
        withContext(dispatchers.io) {
            dao.getByAccount(accountId) ?: SyncStateRecord(accountId = accountId).also {
                dao.upsert(it)
            }
        }

    override fun observe(accountId: String): Flow<SyncStateRecord?> =
        dao.observeByAccount(accountId)

    override suspend fun markAttempt(
        accountId: String,
        status: SyncStatus,
        errorCode: String?,
    ) = withContext(dispatchers.io) {
        ensureForAccount(accountId)
        dao.updateAttempt(accountId, status, clock(), errorCode)
    }

    override suspend fun markSuccess(accountId: String, cursor: String?) =
        withContext(dispatchers.io) {
            val current = ensureForAccount(accountId)
            dao.upsert(
                current.copy(
                    status = SyncStatus.IDLE,
                    lastSuccessfulSyncEpochMs = clock(),
                    lastAttemptEpochMs = clock(),
                    cursor = cursor,
                    errorCode = null,
                    retryCount = 0,
                ),
            )
        }

    override suspend fun updateCursor(accountId: String, cursor: String) =
        withContext(dispatchers.io) {
            ensureForAccount(accountId)
            dao.updateCursor(accountId, cursor)
        }
}
