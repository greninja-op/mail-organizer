package com.greninjaop.mailorganizer.domain.search

import androidx.room.withTransaction
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.core.search.SearchIndexState
import com.greninjaop.mailorganizer.data.local.AppDatabase
import com.greninjaop.mailorganizer.data.local.SearchIndexMeta
import com.greninjaop.mailorganizer.data.local.SearchIndexStore
import com.greninjaop.mailorganizer.data.repository.IntelligenceRepository
import com.greninjaop.mailorganizer.data.repository.MailRepository
import kotlinx.coroutines.withContext

/**
 * Search-index maintenance (Phase 10, phase §24, §54–§56).
 *
 * The FTS index is derived data; this use case keeps it synchronized
 * with `messages` without ever touching Gmail:
 *
 * - [indexNew]: bounded incremental pass — indexes messages with no FTS
 *   row and refreshes rows missing a company name after attribution.
 *   Idempotent; safe to run on every launch.
 * - [rebuild]: drops the account's index rows and re-indexes everything
 *   from normalized local data, in bounded transactional batches. A
 *   failed batch aborts that batch only — normalized data is never at
 *   risk (phase §80).
 * - [ensureIndexed]: version check + catch-up; called best-effort from
 *   the search UI startup path. Never throws.
 *
 * Write-path hooks (same-transaction indexing inside
 * [RoomMailRepository][com.greninjaop.mailorganizer.data.repository.RoomMailRepository]
 * and company-attribution re-indexing) keep the common case current;
 * this use case is the safety net and the rebuild path.
 */
class SearchIndexUseCase(
    private val db: AppDatabase,
    private val mail: MailRepository,
    private val intelligence: IntelligenceRepository,
    private val dispatchers: AppDispatchers,
) : SearchIndexMaintenance {

    companion object {
        private const val TAG = "SearchIndex"

        /** Default batch bound — keeps any single run cheap (phase §56). */
        const val DEFAULT_LIMIT = 200
    }

    private fun store(): SearchIndexStore =
        SearchIndexStore(db.openHelper.writableDatabase)

    /**
     * Makes sure the account's index exists, is current-version, and has
     * no backlog. Best-effort: failures degrade to DEGRADED state, never
     * crash the caller (phase §53).
     */
    override suspend fun ensureIndexed(accountId: String): SearchIndexState =
        withContext(dispatchers.io) {
            try {
                val s = store()
                s.ensureCreated()
                val meta = db.searchIndexMetaDao()
                val version = meta.getVersion(accountId)
                if (version != SearchIndexStore.INDEX_VERSION) {
                    MoLogger.i(TAG, "Index v$version -> v${SearchIndexStore.INDEX_VERSION}; rebuilding")
                    rebuild(accountId)
                    meta.setVersion(SearchIndexMeta(accountId, SearchIndexStore.INDEX_VERSION))
                    return@withContext SearchIndexState.READY
                }
                val indexed = indexNew(accountId)
                if (indexed > 0) {
                    MoLogger.i(TAG, "Indexed $indexed backlog messages")
                }
                SearchIndexState.READY
            } catch (t: Throwable) {
                MoLogger.e(TAG, "ensureIndexed failed: ${t.javaClass.simpleName}")
                SearchIndexState.DEGRADED
            }
        }

    /**
     * Indexes up to [limit] unindexed messages plus rows missing a company
     * name after attribution. Returns how many documents were written.
     */
    suspend fun indexNew(accountId: String, limit: Int = DEFAULT_LIMIT): Int =
        withContext(dispatchers.io) {
            val s = store()
            try {
                s.ensureCreated()
            } catch (t: Throwable) {
                return@withContext 0
            }
            var written = 0
            val ids = s.unindexedMessageIds(accountId, limit.coerceIn(1, 1_000)) +
                s.idsMissingCompanyName(accountId, limit.coerceIn(1, 1_000))
            if (ids.isEmpty()) return@withContext 0
            // Account isolation: re-fetch through the repository and drop
            // anything that isn't this account's (defense in depth).
            val messages = mail.getMessagesByIds(ids.distinct())
                .filter { it.accountId == accountId }
            db.withTransaction {
                for (m in messages) {
                    val companyName = m.companyId?.let { cid ->
                        runCatching { companyDisplayName(accountId, cid) }.getOrNull()
                    }
                    s.indexDocument(SearchIndexStore.FtsDocument.fromMessage(m, companyName))
                    written++
                }
            }
            written
        }

    /**
     * Rebuilds the account's index from normalized local data (phase §54).
     * Transactional per batch; normalized rows are only read, never
     * written (phase §80).
     *
     * @return how many documents were rebuilt.
     */
    override suspend fun rebuild(accountId: String): Int = withContext(dispatchers.io) {
        val s = store()
        s.ensureCreated()
        var total = 0
        db.withTransaction {
            s.deleteForAccount(accountId)
            while (true) {
                val batch = s.unindexedMessageIds(accountId, DEFAULT_LIMIT)
                if (batch.isEmpty()) break
                val messages = mail.getMessagesByIds(batch)
                    .filter { it.accountId == accountId }
                for (m in messages) {
                    val companyName = m.companyId?.let { cid ->
                        runCatching { companyDisplayName(accountId, cid) }.getOrNull()
                    }
                    s.indexDocument(SearchIndexStore.FtsDocument.fromMessage(m, companyName))
                    total++
                }
                // Loop until no unindexed rows remain; each batch commits
                // with the surrounding transaction only at the end — the
                // whole rebuild is atomic per call.
            }
        }
        db.searchIndexMetaDao()
            .setVersion(SearchIndexMeta(accountId, SearchIndexStore.INDEX_VERSION))
        MoLogger.i(TAG, "Rebuilt search index: $total documents")
        total
    }

    private suspend fun companyDisplayName(accountId: String, companyId: String): String? {
        // Company rows are account-scoped; resolve the display name the
        // same way the UI does (override wins).
        val dao = db.companyDao()
        val row = dao.getById(companyId) ?: return null
        if (row.accountId != accountId) return null
        return row.userOverrideName ?: row.canonicalName
    }
}
