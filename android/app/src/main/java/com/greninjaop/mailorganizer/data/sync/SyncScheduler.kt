package com.greninjaop.mailorganizer.data.sync

import com.greninjaop.mailorganizer.core.AccountId

/**
 * Background-sync scheduling seam (Phase 4, phase §28).
 *
 * Establishes the *contract* for battery-conscious background sync:
 * account-scoped work, network constraints, cancellation. The concrete
 * mechanism is WorkManager (selected by the architecture); wiring it is
 * deferred to Phase 19 (Background Sync & Offline Behavior), which owns
 * the full background strategy, because WorkManager behavior cannot be
 * meaningfully verified without a device in this environment.
 *
 * Deliberately NOT here: aggressive periodic polling, permanent foreground
 * services, wake locks (phase §59).
 */
interface SyncScheduler {
    /** Request a background sync for [accountId]; coalesces duplicates. */
    fun requestSync(accountId: AccountId, trigger: SyncTrigger)

    /** Cancel pending background work for [accountId]. */
    fun cancelPending(accountId: AccountId)
}

/**
 * Declarative description of one account's background sync work — the
 * input Phase 19's WorkManager wiring will consume.
 */
data class SyncWorkSpec(
    val accountId: AccountId,
    val trigger: SyncTrigger,
    /** Never sync without connectivity (phase §33). */
    val requiresNetwork: Boolean = true,
    /** Battery-conscious: defer when the battery is low (phase §28, §59). */
    val requiresBatteryNotLow: Boolean = true,
    /** Backoff on failure is handled by [SyncRetryPolicy], not by re-enqueue. */
    val uniqueWorkName: String = "sync-${accountId.value}",
)
