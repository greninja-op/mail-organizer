package com.greninjaop.mailorganizer.core.privacy

/**
 * Data sensitivity classification taxonomy (Phase 23 §7).
 */
enum class DataSensitivity {
    /**
     * Highly sensitive assets: OAuth secrets, tokens, credentials, raw message body.
     * Must never be logged, backed up to cloud, or leaked to third parties.
     */
    HIGHLY_SENSITIVE,

    /**
     * Sensitive information: account identities, sender/recipient emails, temporal dates.
     * Stored locally, account-isolated, safe-logged only with redaction/omission.
     */
    SENSITIVE,

    /**
     * Derived intelligence: categories, priorities, action suggestions, company attributions.
     * Computed locally and deterministically.
     */
    DERIVED_INTELLIGENCE,

    /**
     * Non-sensitive application configuration: UI themes, layout toggles, notification preferences.
     * Eligible for standard local backup and settings export.
     */
    NON_SENSITIVE,
}

/**
 * Audit record representing an inspected data category in Mailstack (Phase 23 §8).
 */
data class DataInventoryItem(
    val categoryName: String,
    val description: String,
    val sensitivity: DataSensitivity,
    val storageLocation: String,
    val isAccountScoped: Boolean,
    val leavesDevice: Boolean,
    val destinationIfLeaves: String?,
    val retentionPolicy: String,
    val deletionBehavior: String,
)

/**
 * Security status and audit findings for the device and application state (Phase 23 §40, §41).
 */
data class SecurityAuditSnapshot(
    val credentialStorageMechanism: String,
    val plainSecretsInDatabase: Boolean = false,
    val databaseEncryptionStatus: String,
    val loggingSanitizationActive: Boolean = true,
    val backupRulesActive: Boolean = true,
    val networkSecurityStatus: String,
    val thirdPartyAiTelemetry: Boolean = false,
    val totalConnectedAccounts: Int,
    val totalLocalMessagesCount: Int,
    val activeIntegrationCount: Int,
)

/**
 * Technical scope disclosure model translated for transparent user understanding (Phase 23 §43, §44).
 */
data class PermissionScopeDisclosure(
    val scopeId: String,
    val technicalName: String,
    val userFriendlyDescription: String,
    val accessLevel: String,
    val whyNeeded: String,
    val isGranted: Boolean,
    val isWritePermission: Boolean,
)
