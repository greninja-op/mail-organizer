package com.greninjaop.mailorganizer.data.sync

/**
 * Opaque sync cursor codec (Phase 4).
 *
 * [com.greninjaop.mailorganizer.data.local.SyncStateRecord.cursor] is an
 * opaque string by design; this is where Phase 4 defines its semantics:
 *
 * - `v1:page:<token>`   — initial sync resume point (Gmail list page token).
 *                         Empty token = start of initial sync.
 * - `v1:history:<id>`   — incremental sync position (Gmail history id).
 *
 * Decoding is defensive: null, blank, or unrecognized values safely restart
 * an initial sync from the beginning (controlled recovery, phase §24) rather
 * than crashing or assuming a false synchronized state. Re-running initial
 * sync is idempotent (phase §18), so this is always safe.
 */
sealed interface SyncCursor {

    /** Resume an initial (paged) sync. Null token = start from the first page. */
    data class Page(val pageToken: String?) : SyncCursor {
        override fun encode(): String = "$PREFIX$PAGE_KIND${pageToken.orEmpty()}"
    }

    /** Incremental position from the Gmail history API. */
    data class History(val historyId: String) : SyncCursor {
        override fun encode(): String = "$PREFIX$HISTORY_KIND$historyId"
    }

    /** Encodes this cursor for [com.greninjaop.mailorganizer.data.local.SyncStateRecord.cursor]. */
    fun encode(): String

    companion object {
        private const val PREFIX = "v1:"
        private const val PAGE_KIND = "page:"
        private const val HISTORY_KIND = "history:"

        fun decode(raw: String?): SyncCursor {
            if (raw.isNullOrBlank() || !raw.startsWith(PREFIX)) return Page(null)
            val body = raw.removePrefix(PREFIX)
            return when {
                body.startsWith(PAGE_KIND) -> {
                    val token = body.removePrefix(PAGE_KIND)
                    Page(token.ifEmpty { null })
                }
                body.startsWith(HISTORY_KIND) -> {
                    val id = body.removePrefix(HISTORY_KIND)
                    if (id.isBlank()) Page(null) else History(id)
                }
                else -> Page(null)
            }
        }
    }
}
