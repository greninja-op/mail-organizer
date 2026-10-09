package com.greninjaop.mailorganizer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Gmail account identity — the parent boundary for all account-owned tables.
 *
 * Phase 2 expands the Phase 0 seed with connection/sync bookkeeping. OAuth
 * tokens are NEVER stored here (or anywhere in this database); Phase 3 will
 * use Android's secure credential storage. See docs/data-model.md §token
 * boundary.
 *
 * New columns were appended after the Phase 0 columns with safe defaults so
 * the v1→v2 migration is purely additive (see [Migrations]).
 */
@Entity(tableName = "accounts")
data class AccountRecord(
    @PrimaryKey val accountId: String,
    val emailAddress: String,
    val displayName: String?,
    val createdAtEpochMs: Long,
    // ---- Phase 2 additions (all defaulted for the additive migration) ----
    /** Google account identifier, populated when OAuth lands (Phase 3). */
    val googleAccountId: String? = null,
    /** Provider id; "google" today, extensible for future providers. */
    val provider: String = PROVIDER_GOOGLE,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val lastSyncEpochMs: Long? = null,
    val updatedAtEpochMs: Long = createdAtEpochMs,
    val isEnabled: Boolean = true,
) {
    companion object {
        const val PROVIDER_GOOGLE = "google"
    }
}
