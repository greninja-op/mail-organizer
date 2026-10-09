package com.greninjaop.mailorganizer.data.sync

import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.ConnectionState
import com.greninjaop.mailorganizer.data.local.SyncStateRecord
import com.greninjaop.mailorganizer.data.local.SyncStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Triggers that can request or initiate account synchronization.
 * Defined per Phase 19 (§12, §34, §35, §36).
 */
enum class SyncTrigger {
    MANUAL,
    PERIODIC,
    APP_START,
    APP_RESUME,
    CONNECTIVITY_RECOVERY,
    ACCOUNT_CONNECTED,
    RETRY,
}

/**
 * Battery-conscious and network-aware constraints for background sync work.
 * Defined per Phase 19 (§15, §31, §32).
 */
data class SyncConstraints(
    val requiresNetwork: Boolean = true,
    val requiresUnmeteredNetwork: Boolean = false,
    val requiresBatteryNotLow: Boolean = true,
    val requiresCharging: Boolean = false,
)

/**
 * Per-account rich sync status model (Phase 19 §20).
 *
 * Distinguishes offline state from auth expiration and rate limits (§16).
 * Computed truthfully from persisted [SyncStateRecord] + current network & in-flight state.
 */
sealed interface AccountSyncStatus {
    data object NeverSynced : AccountSyncStatus
    data class Syncing(val stage: SyncStage, val messagesProcessed: Int = 0) : AccountSyncStatus
    data class Synced(val lastSuccessEpochMs: Long) : AccountSyncStatus
    data class Partial(val messagesProcessed: Int, val reason: String? = null) : AccountSyncStatus
    data object Offline : AccountSyncStatus
    data object AuthRequired : AccountSyncStatus
    data object PermissionRequired : AccountSyncStatus
    data class RateLimited(val retryAfterSeconds: Long? = null) : AccountSyncStatus
    data class TransientError(val message: String) : AccountSyncStatus
    data class PermanentError(val message: String) : AccountSyncStatus
    data object Paused : AccountSyncStatus
}

/**
 * Unified sync status summarizing state across all enabled accounts (Phase 19 §21).
 */
data class UnifiedSyncStatus(
    val totalAccounts: Int,
    val upToDateCount: Int,
    val syncingCount: Int,
    val attentionCount: Int,
    val offlineCount: Int,
    val lastSyncEpochMs: Long?,
    val headline: String,
    val detail: String? = null,
)

/**
 * Deterministic helper to evaluate per-account sync status.
 * Never displays "Reconnect Gmail" when the device is simply offline (§16).
 */
object AccountSyncStatusEvaluator {

    fun evaluate(
        record: SyncStateRecord?,
        inProgress: SyncProgress? = null,
        isOnline: Boolean = true,
        account: AccountRecord? = null,
    ): AccountSyncStatus {
        // Active in-flight progress takes priority
        if (inProgress is SyncProgress.Running) {
            return AccountSyncStatus.Syncing(inProgress.stage, inProgress.messagesProcessed)
        }

        // Account disconnected state (deferred OAuth / not connected)
        if (account != null && account.connectionState == ConnectionState.DISCONNECTED) {
            // If offline, device is offline first; otherwise needs auth/connection
            if (!isOnline) return AccountSyncStatus.Offline
            return AccountSyncStatus.AuthRequired
        }

        if (record == null || record.status == SyncStatus.NEVER_SYNCED) {
            if (!isOnline) return AccountSyncStatus.Offline
            return AccountSyncStatus.NeverSynced
        }

        if (record.status == SyncStatus.RUNNING) {
            return AccountSyncStatus.Syncing(SyncStage.CONNECTING, 0)
        }

        if (record.status == SyncStatus.PAUSED) {
            return AccountSyncStatus.Paused
        }

        if (record.status == SyncStatus.FAILED) {
            return when (record.errorCode) {
                "network" -> if (!isOnline) AccountSyncStatus.Offline else AccountSyncStatus.TransientError("Network error")
                "auth", "not_configured" -> AccountSyncStatus.AuthRequired
                "permission" -> AccountSyncStatus.PermissionRequired
                "rate_limited" -> AccountSyncStatus.RateLimited()
                else -> {
                    if (!isOnline) AccountSyncStatus.Offline
                    else AccountSyncStatus.TransientError(record.errorCode ?: "Sync failed")
                }
            }
        }

        // Status is IDLE: check offline first
        if (!isOnline) {
            return AccountSyncStatus.Offline
        }

        val lastSuccess = record.lastSuccessfulSyncEpochMs
        return if (lastSuccess != null && lastSuccess > 0) {
            AccountSyncStatus.Synced(lastSuccess)
        } else {
            AccountSyncStatus.NeverSynced
        }
    }

    fun evaluateUnified(
        accounts: List<AccountRecord>,
        statusesById: Map<String, AccountSyncStatus>,
        isOnline: Boolean,
    ): UnifiedSyncStatus {
        val enabled = accounts.filter { it.isEnabled }
        val total = enabled.size

        if (total == 0) {
            return UnifiedSyncStatus(
                totalAccounts = 0,
                upToDateCount = 0,
                syncingCount = 0,
                attentionCount = 0,
                offlineCount = 0,
                lastSyncEpochMs = null,
                headline = "No accounts connected",
                detail = null,
            )
        }

        var upToDate = 0
        var syncing = 0
        var attention = 0
        var offline = 0
        var latestSync: Long? = null

        for (acc in enabled) {
            val status = statusesById[acc.accountId] ?: AccountSyncStatus.NeverSynced
            when (status) {
                is AccountSyncStatus.Synced -> {
                    upToDate++
                    val sTime = status.lastSuccessEpochMs
                    if (latestSync == null || sTime > latestSync) {
                        latestSync = sTime
                    }
                }
                is AccountSyncStatus.Syncing -> syncing++
                is AccountSyncStatus.Offline -> offline++
                is AccountSyncStatus.AuthRequired,
                is AccountSyncStatus.PermissionRequired,
                is AccountSyncStatus.RateLimited,
                is AccountSyncStatus.PermanentError,
                is AccountSyncStatus.TransientError -> attention++
                is AccountSyncStatus.NeverSynced,
                is AccountSyncStatus.Partial,
                is AccountSyncStatus.Paused -> {
                    // Not up-to-date, but not hard attention
                }
            }
        }

        val headline = when {
            !isOnline || offline == total -> "Offline — showing saved mail"
            syncing > 0 -> "Syncing $syncing of $total accounts…"
            attention > 0 -> "$attention account${if (attention > 1) "s" else ""} need${if (attention == 1) "s" else ""} attention"
            upToDate == total -> "All accounts up to date"
            upToDate > 0 -> "$upToDate of $total accounts up to date"
            else -> "$total accounts connected"
        }

        val detail = when {
            syncing > 0 -> "Checking for new messages in background"
            attention > 0 -> "Tap to review account connection"
            !isOnline -> "Cached messages remain available"
            else -> null
        }

        return UnifiedSyncStatus(
            totalAccounts = total,
            upToDateCount = upToDate,
            syncingCount = syncing,
            attentionCount = attention,
            offlineCount = offline,
            lastSyncEpochMs = latestSync,
            headline = headline,
            detail = detail,
        )
    }
}

/**
 * Honest, relative & absolute last sync timestamp formatter (Phase 19 §19).
 * Derived strictly from real epoch milliseconds; never fabricates timestamps.
 */
object SyncTimeFormatter {

    fun formatLastSynced(
        epochMs: Long?,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): String {
        if (epochMs == null || epochMs <= 0L) return "Never synced"
        val diff = nowEpochMs - epochMs
        if (diff < 0L) return "Just now"

        val seconds = diff / 1000L
        val minutes = seconds / 60L
        val hours = minutes / 60L
        val days = hours / 24L

        return when {
            minutes < 1L -> "Just now"
            minutes == 1L -> "1 minute ago"
            minutes < 60L -> "$minutes minutes ago"
            hours == 1L -> "1 hour ago"
            hours < 24L -> "$hours hours ago"
            days == 1L -> "Yesterday"
            days < 7L -> "$days days ago"
            else -> {
                val sdf = SimpleDateFormat("MMM d", Locale.US)
                sdf.format(Date(epochMs))
            }
        }
    }
}
