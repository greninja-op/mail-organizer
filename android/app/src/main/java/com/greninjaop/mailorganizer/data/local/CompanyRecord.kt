package com.greninjaop.mailorganizer.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Company grouping row (Phase 2 — storage foundation only).
 *
 * Company *detection* (domain → canonical company) lands in Phase 8; this
 * table stores its future output plus user intent. Pinning is independent
 * from email starring (requirements.md): [pinned] only affects ordering
 * inside the category's company filter list.
 */
@Entity(
    tableName = "companies",
    foreignKeys = [
        ForeignKey(
            entity = AccountRecord::class,
            parentColumns = ["accountId"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("accountId"),
        Index(value = ["accountId", "normalizedDomain"], unique = true),
    ],
)
data class CompanyRecord(
    @PrimaryKey val companyId: String,
    val accountId: String,
    val canonicalName: String,
    val normalizedDomain: String,
    val knownDomains: List<String> = emptyList(),
    /** User override for the display name; null means "use canonical". */
    val userOverrideName: String? = null,
    /** Pinned companies sort above unpinned ones in the filter list. */
    val pinned: Boolean = false,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)
