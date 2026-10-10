package com.greninjaop.mailorganizer.domain.privacy

import com.greninjaop.mailorganizer.core.privacy.DataSensitivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacyModelsTest {

    @Test
    fun dataSensitivity_enumHasAllExpectedTiers() {
        assertEquals(4, DataSensitivity.entries.size)
        assertTrue(DataSensitivity.entries.contains(DataSensitivity.HIGHLY_SENSITIVE))
        assertTrue(DataSensitivity.entries.contains(DataSensitivity.SENSITIVE))
        assertTrue(DataSensitivity.entries.contains(DataSensitivity.DERIVED_INTELLIGENCE))
        assertTrue(DataSensitivity.entries.contains(DataSensitivity.NON_SENSITIVE))
    }

    @Test
    fun permissionScopeDisclosure_tracksWritePermissionsTruthfully() {
        val readonly = com.greninjaop.mailorganizer.core.privacy.PermissionScopeDisclosure(
            scopeId = "gmail.readonly",
            technicalName = "https://www.googleapis.com/auth/gmail.readonly",
            userFriendlyDescription = "Read email messages",
            accessLevel = "Read-Only",
            whyNeeded = "Needed for synchronization",
            isGranted = true,
            isWritePermission = false,
        )
        assertFalse(readonly.isWritePermission)

        val modify = com.greninjaop.mailorganizer.core.privacy.PermissionScopeDisclosure(
            scopeId = "gmail.modify",
            technicalName = "https://www.googleapis.com/auth/gmail.modify",
            userFriendlyDescription = "Modify email messages",
            accessLevel = "Read & Write",
            whyNeeded = "Needed for label changes",
            isGranted = false,
            isWritePermission = true,
        )
        assertTrue(modify.isWritePermission)
        assertFalse(modify.isGranted)
    }

    @Test
    fun securityAuditSnapshot_initializesWithSafeDefaults() {
        val snapshot = com.greninjaop.mailorganizer.core.privacy.SecurityAuditSnapshot(
            credentialStorageMechanism = "Android Keystore Seam",
            plainSecretsInDatabase = false,
            databaseEncryptionStatus = "App-private database sandbox",
            loggingSanitizationActive = true,
            backupRulesActive = true,
            networkSecurityStatus = "TLS 1.3 Strict HTTPS",
            thirdPartyAiTelemetry = false,
            totalConnectedAccounts = 2,
            totalLocalMessagesCount = 150,
            activeIntegrationCount = 1,
        )
        assertFalse(snapshot.plainSecretsInDatabase)
        assertTrue(snapshot.loggingSanitizationActive)
        assertTrue(snapshot.backupRulesActive)
        assertFalse(snapshot.thirdPartyAiTelemetry)
        assertEquals(2, snapshot.totalConnectedAccounts)
    }
}
