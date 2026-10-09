package com.greninjaop.mailorganizer.data.integrations

import com.greninjaop.mailorganizer.MoError
import com.greninjaop.mailorganizer.MoResult
import com.greninjaop.mailorganizer.core.integrations.IntegrationAdapter
import com.greninjaop.mailorganizer.core.integrations.IntegrationCapability
import com.greninjaop.mailorganizer.core.integrations.IntegrationId
import com.greninjaop.mailorganizer.core.integrations.IntegrationSnapshot
import com.greninjaop.mailorganizer.core.integrations.IntegrationStatus
import com.greninjaop.mailorganizer.core.integrations.PermissionDescription
import com.greninjaop.mailorganizer.data.repository.AccountRepository
import com.greninjaop.mailorganizer.data.sync.GmailSyncApi
import com.greninjaop.mailorganizer.data.sync.SyncApiException

/**
 * Gmail integration adapter (Phase 17).
 *
 * Wraps Phase 4's [GmailSyncApi] seam — the app's local sync engine — and
 * reports its status honestly:
 * - No account in scope → [IntegrationStatus.DISCONNECTED].
 * - Account exists but Google authorization is missing (Phase 3 deferred)
 *   → [IntegrationStatus.AUTH_REQUIRED] with an explicit reason. The user
 *   cannot fix this yet, and the reason says so.
 * - [IntegrationStatus.CONNECTED] is never reported: without Phase 3's
 *   OAuth there is no verified connection, and the manager must not
 *   claim one.
 *
 * Declared capabilities (READ_EMAIL, SYNC_EMAIL) describe what Gmail will
 * provide once connected; they are not usable until then. Least-privilege
 * permissions name only the readonly scope — modify/send are explicitly
 * out of scope for this phase (phase §23).
 */
class GmailIntegrationAdapter(
    private val syncApi: GmailSyncApi,
    private val accounts: AccountRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) : IntegrationAdapter {

    override val id: IntegrationId = IntegrationId.GMAIL
    override val displayName: String = "Gmail"
    override val provider: String = "Google"

    override val declaredCapabilities: Set<IntegrationCapability> = setOf(
        IntegrationCapability.READ_EMAIL,
        IntegrationCapability.SYNC_EMAIL,
    )

    override val requiredPermissions: List<PermissionDescription> = listOf(
        PermissionDescription(
            scope = "gmail.readonly",
            purpose = "Read and sync your Gmail messages. Mail Organizer " +
                "never modifies or sends mail with this permission.",
            required = true,
        ),
    )

    override suspend fun snapshot(accountId: String?): IntegrationSnapshot {
        val account = accountId?.let { accounts.getById(it) }
        val (status, reason) = when {
            account == null -> IntegrationStatus.DISCONNECTED to
                "No account added yet."
            account.googleAccountId.isNullOrBlank() -> IntegrationStatus.AUTH_REQUIRED to
                "Gmail isn't connected yet — Google sign-in arrives with " +
                    "Phase 3. Your mail stays on your device until then."
            else -> probeSyncApi(accountId)
        }
        return IntegrationSnapshot(
            id = id,
            displayName = displayName,
            provider = provider,
            accountId = accountId,
            accountEmail = account?.emailAddress,
            status = status,
            statusReason = reason,
            declaredCapabilities = declaredCapabilities,
            requiredPermissions = requiredPermissions,
            lastCheckedEpochMs = clock(),
        )
    }

    override suspend fun connect(): MoResult<Unit> =
        MoResult.Failure(
            MoError.InvalidConfiguration(
                "Google sign-in isn't available yet — it arrives with " +
                    "Phase 3. Nothing was connected.",
            ),
        )

    /**
     * Idempotent safe disconnect (phase §18): with no live connection
     * there is nothing to revoke; succeeds without touching mail,
     * classifications, rules, or action history.
     */
    override suspend fun disconnect(accountId: String?): MoResult<Unit> =
        MoResult.Success(Unit)

    override suspend fun refresh(accountId: String?): IntegrationSnapshot =
        snapshot(accountId)

    /**
     * Probes the sync seam to distinguish a configured client from the
     * fail-closed deferred stand-in. Any failure keeps the honest
     * AUTH_REQUIRED state — a probe must never promote to CONNECTED.
     */
    private suspend fun probeSyncApi(accountId: String): Pair<IntegrationStatus, String> {
        return try {
            syncApi.latestHistoryCursor(com.greninjaop.mailorganizer.core.AccountId(accountId))
            // A real cursor means a configured client — but without OAuth
            // verification this path is unreachable today; kept for Phase 3.
            IntegrationStatus.AUTH_REQUIRED to
                "Gmail authorization still needs verification (Phase 3)."
        } catch (e: SyncApiException.NotConfigured) {
            IntegrationStatus.AUTH_REQUIRED to
                "Gmail isn't connected yet — Google sign-in arrives with " +
                    "Phase 3. Your mail stays on your device until then."
        } catch (e: Exception) {
            IntegrationStatus.ERROR to
                "Couldn't check Gmail status: ${e.message ?: "unknown error"}"
        }
    }
}
