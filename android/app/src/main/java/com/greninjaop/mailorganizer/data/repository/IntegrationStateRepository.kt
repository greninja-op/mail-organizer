package com.greninjaop.mailorganizer.data.repository

import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.data.local.IntegrationStateDao
import com.greninjaop.mailorganizer.data.local.IntegrationStateRecord
import kotlinx.coroutines.withContext

/**
 * Integration metadata persistence (Phase 17, phase §34–§36).
 *
 * Stores last-known integration snapshots — status metadata only, never
 * credentials (phase §35). All operations are account-scoped; cleanup on
 * account removal deletes only that account's rows (phase §36).
 *
 * The public API uses nullable [String] for "no account in scope"; the
 * Room layer maps null to the "" sentinel (Room forbids nullable primary
 * keys — see [IntegrationStateRecord]).
 */
interface IntegrationStateRepository {
    suspend fun upsert(record: IntegrationStateRecord)
    suspend fun getForAccount(accountId: String?): List<IntegrationStateRecord>
    suspend fun clear(integrationId: String, accountId: String?)
    suspend fun clearForAccount(accountId: String?)
}

class RoomIntegrationStateRepository(
    private val dao: IntegrationStateDao,
    private val dispatchers: AppDispatchers,
) : IntegrationStateRepository {

    override suspend fun upsert(record: IntegrationStateRecord) =
        withContext(dispatchers.io) { dao.upsert(record) }

    override suspend fun getForAccount(accountId: String?): List<IntegrationStateRecord> =
        withContext(dispatchers.io) { dao.getForAccount(accountId.orEmpty()) }

    override suspend fun clear(integrationId: String, accountId: String?) =
        withContext(dispatchers.io) { dao.delete(integrationId, accountId.orEmpty()) }

    override suspend fun clearForAccount(accountId: String?) =
        withContext(dispatchers.io) { dao.deleteForAccount(accountId.orEmpty()) }
}
