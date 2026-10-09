package com.greninjaop.mailorganizer.ui.mail

import com.greninjaop.mailorganizer.BuildConfig

/**
 * Decides whether fixture mailbox data may be seeded (Phase 6).
 *
 * Fixtures exist so the inbox UI is developable and verifiable without live
 * Gmail access (Phase 3 deferred). They are strictly a development aid:
 * release builds never seed, and [SampleMailboxSeeder] never touches a
 * database that already holds accounts.
 */
interface SampleDataPolicy {
    fun shouldSeed(): Boolean
}

/** Debug builds may seed; release builds never do. */
class DebugSampleDataPolicy : SampleDataPolicy {
    override fun shouldSeed(): Boolean = BuildConfig.DEBUG
}
