package com.greninjaop.mailorganizer.ui.mail

import com.greninjaop.mailorganizer.core.email.AttachmentMeta
import com.greninjaop.mailorganizer.core.email.HtmlSanitizer
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.ConnectionState
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.local.ThreadRecord
import com.greninjaop.mailorganizer.data.repository.AccountRepository
import com.greninjaop.mailorganizer.data.repository.MailRepository
import kotlinx.coroutines.flow.first

/**
 * Clearly-labeled fixture data for UI development (Phase 6).
 *
 * The spec permits clearly-labeled fixtures during the UI-only phase; there
 * is no live Gmail access (Phase 3 deferred), so the inbox would otherwise
 * be permanently empty and untestable. Rules:
 *
 * - Every fixture id starts with `fixture-` ([FIXTURE_ID_PREFIX]); fixture
 *   rows are trivially distinguishable from real synced rows in the DB.
 * - Fixture `gmailMessageId`s are null: these rows were never synced from
 *   Gmail, and nothing pretends otherwise.
 * - [seedIfEmpty] only runs when the database has **zero** accounts — it
 *   never touches real user data.
 * - Fixture HTML is passed through [HtmlSanitizer], exactly like production
 *   mail, so the viewer exercises the real safe-rendering path.
 * - The UI shows a "Sample data" banner whenever fixture accounts exist.
 *
 * Covers the phase §49/§50 test cases: normal, long subject, HTML (with a
 * hostile `javascript:` link that must NOT become clickable, and a remote
 * image that must NOT load), attachment metadata, Unicode (Malayalam +
 * emoji), missing fields, a multi-message thread, and a starred message.
 */
class SampleMailboxSeeder(
    private val accounts: AccountRepository,
    private val mail: MailRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    companion object {
        const val FIXTURE_ID_PREFIX = "fixture-"

        /** True for fixture account ids — drives the "Sample data" UI banner. */
        fun isFixtureAccount(accountId: String): Boolean =
            accountId.startsWith(FIXTURE_ID_PREFIX)

        const val ACCT_ALEX = "${FIXTURE_ID_PREFIX}acct-alex"
        const val ACCT_PRIYA = "${FIXTURE_ID_PREFIX}acct-priya"
    }

    /**
     * Seeds the fixture mailbox. Returns true when seeding happened.
     * No-op when any account already exists (real or fixture).
     */
    suspend fun seedIfEmpty(): Boolean {
        if (accounts.observeAll().first().isNotEmpty()) return false
        val now = clock()

        accounts.upsert(
            AccountRecord(
                accountId = ACCT_ALEX,
                emailAddress = "alex.reader@example.com",
                displayName = "Alex Reader",
                createdAtEpochMs = now,
                connectionState = ConnectionState.DISCONNECTED,
            ),
        )
        accounts.upsert(
            AccountRecord(
                accountId = ACCT_PRIYA,
                emailAddress = "priya.dev@example.com",
                displayName = "Priya Nair",
                createdAtEpochMs = now,
                connectionState = ConnectionState.DISCONNECTED,
            ),
        )

        seedThread(
            threadId = "${FIXTURE_ID_PREFIX}thread-t1",
            accountId = ACCT_ALEX,
            subject = "Your account information",
            messages = listOf(
                msg(
                    id = "m1", accountId = ACCT_ALEX, threadId = "${FIXTURE_ID_PREFIX}thread-t1",
                    fromName = "Google", fromAddress = "no-reply@accounts.example.com",
                    subject = "Your account information",
                    snippet = "Here's a summary of your recent account activity…",
                    bodyText = "Hi Alex,\n\nHere's a summary of recent activity on your " +
                        "Google Account.\n\n— The Accounts team",
                    timestamp = now - 5 * 60_000,
                    unread = true,
                ),
            ),
        )

        seedThread(
            threadId = "${FIXTURE_ID_PREFIX}thread-t2",
            accountId = ACCT_ALEX,
            subject = "This is an intentionally very long subject line designed to " +
                "exercise ellipsis and line limits in the mail list row rendering " +
                "pipeline without breaking the layout",
            messages = listOf(
                msg(
                    id = "m2", accountId = ACCT_ALEX, threadId = "${FIXTURE_ID_PREFIX}thread-t2",
                    fromName = "Long Subject Tester", fromAddress = "long@example.com",
                    subject = "This is an intentionally very long subject line designed to " +
                        "exercise ellipsis and line limits in the mail list row rendering " +
                        "pipeline without breaking the layout",
                    snippet = "Body.",
                    bodyText = "Body.",
                    timestamp = now - 2 * 3_600_000,
                    unread = false,
                ),
            ),
        )

        seedThread(
            threadId = "${FIXTURE_ID_PREFIX}thread-t3",
            accountId = ACCT_ALEX,
            subject = "Your weekly digest",
            messages = listOf(
                msg(
                    id = "m3", accountId = ACCT_ALEX, threadId = "${FIXTURE_ID_PREFIX}thread-t3",
                    fromName = "Weekly Digest", fromAddress = "digest@example.com",
                    subject = "Your weekly digest",
                    snippet = "Hi Alex, here are your highlights…",
                    bodyText = "Hi Alex, here are your highlights: Item one, Item two. " +
                        "Read more online.",
                    bodyHtml = HtmlSanitizer.sanitize(
                        "<h1>This week</h1>" +
                            "<p>Hi Alex, here are your <b>highlights</b>:</p>" +
                            "<ul><li>Item one</li><li>Item <i>two</i></li></ul>" +
                            "<p>Read more <a href=\"https://example.com/digest\">online</a>.</p>" +
                            "<p><a href=\"javascript:alert('xss')\">do not click</a></p>" +
                            "<img src=\"https://example.com/pixel.png\" alt=\"Weekly banner\">",
                    ),
                    attachments = listOf(
                        AttachmentMeta(
                            filename = "invoice.pdf",
                            mimeType = "application/pdf",
                            sizeBytes = 1_258_291,
                            attachmentId = "${FIXTURE_ID_PREFIX}att-1",
                        ),
                    ),
                    timestamp = now - 26 * 3_600_000,
                    unread = true,
                ),
            ),
        )

        seedThread(
            threadId = "${FIXTURE_ID_PREFIX}thread-t4",
            accountId = ACCT_ALEX,
            subject = "വാർത്താക്കുറിപ്പ് 🎉",
            messages = listOf(
                msg(
                    id = "m4a", accountId = ACCT_ALEX, threadId = "${FIXTURE_ID_PREFIX}thread-t4",
                    fromName = "പ്രിയ", fromAddress = "priya@example.com",
                    subject = "വാർത്താക്കുറിപ്പ് 🎉",
                    snippet = "ഹായ്! 🎉",
                    bodyText = "ഹായ്! 🎉",
                    timestamp = now - 3 * 86_400_000,
                    unread = false,
                ),
                msg(
                    id = "m4b", accountId = ACCT_ALEX, threadId = "${FIXTURE_ID_PREFIX}thread-t4",
                    fromName = "Alex Reader", fromAddress = "alex.reader@example.com",
                    subject = "വാർത്താക്കുറിപ്പ് 🎉",
                    snippet = "Thanks, Priya! Looks great. 👍",
                    bodyText = "Thanks, Priya! Looks great. 👍",
                    timestamp = now - 3 * 86_400_000 + 3_600_000,
                    unread = false,
                ),
                msg(
                    id = "m4c", accountId = ACCT_ALEX, threadId = "${FIXTURE_ID_PREFIX}thread-t4",
                    fromName = "പ്രിയ", fromAddress = "priya@example.com",
                    subject = "വാർത്താക്കുറിപ്പ് 🎉",
                    snippet = "ശരി, നമുക്ക് തുടരാം.",
                    bodyText = "ശരി, നമുക്ക് തുടരാം.",
                    timestamp = now - 2 * 86_400_000,
                    unread = false,
                ),
                msg(
                    id = "m4d", accountId = ACCT_ALEX, threadId = "${FIXTURE_ID_PREFIX}thread-t4",
                    fromName = "Priya", fromAddress = "priya@example.com",
                    subject = "വാർത്താക്കുറിപ്പ് 🎉",
                    snippet = "Latest update — all green ✅",
                    bodyText = "Latest update — all green ✅",
                    timestamp = now - 3_600_000,
                    unread = true,
                ),
            ),
        )

        seedThread(
            threadId = "${FIXTURE_ID_PREFIX}thread-t5",
            accountId = ACCT_ALEX,
            subject = "",
            messages = listOf(
                msg(
                    id = "m5", accountId = ACCT_ALEX, threadId = "${FIXTURE_ID_PREFIX}thread-t5",
                    fromName = null, fromAddress = "",
                    subject = "",
                    snippet = null,
                    bodyText = null,
                    timestamp = now - 8 * 86_400_000,
                    unread = true,
                ),
            ),
        )

        seedThread(
            threadId = "${FIXTURE_ID_PREFIX}thread-t6",
            accountId = ACCT_ALEX,
            subject = "Quarterly report",
            messages = listOf(
                msg(
                    id = "m6", accountId = ACCT_ALEX, threadId = "${FIXTURE_ID_PREFIX}thread-t6",
                    fromName = "Finance", fromAddress = "finance@example.com",
                    subject = "Quarterly report",
                    snippet = "Attached is the quarterly report.",
                    bodyText = "Attached is the quarterly report.",
                    timestamp = now - 40L * 86_400_000,
                    unread = false,
                    starred = true,
                ),
            ),
        )

        seedThread(
            threadId = "${FIXTURE_ID_PREFIX}thread-t7",
            accountId = ACCT_PRIYA,
            subject = "Sprint planning",
            messages = listOf(
                msg(
                    id = "m7a", accountId = ACCT_PRIYA, threadId = "${FIXTURE_ID_PREFIX}thread-t7",
                    fromName = "Sam", fromAddress = "sam@example.com",
                    subject = "Sprint planning",
                    snippet = "Planning at 10?",
                    bodyText = "Planning at 10?",
                    timestamp = now - 5 * 86_400_000,
                    unread = false,
                ),
                msg(
                    id = "m7b", accountId = ACCT_PRIYA, threadId = "${FIXTURE_ID_PREFIX}thread-t7",
                    fromName = "Priya Nair", fromAddress = "priya.dev@example.com",
                    subject = "Sprint planning",
                    snippet = "10 works.",
                    bodyText = "10 works.",
                    timestamp = now - 5 * 86_400_000 + 1_800_000,
                    unread = true,
                ),
            ),
        )

        return true
    }

    private fun msg(
        id: String,
        accountId: String,
        threadId: String,
        fromName: String?,
        fromAddress: String,
        subject: String,
        snippet: String?,
        bodyText: String?,
        bodyHtml: String? = null,
        attachments: List<AttachmentMeta> = emptyList(),
        timestamp: Long,
        unread: Boolean,
        starred: Boolean = false,
    ): MessageRecord = MessageRecord(
        messageId = "$FIXTURE_ID_PREFIX" + "msg-$id",
        gmailMessageId = null, // fixtures were never synced — honest null
        threadId = threadId,
        accountId = accountId,
        fromAddress = fromAddress,
        fromName = fromName,
        subject = subject,
        snippet = snippet,
        bodyText = bodyText,
        bodyHtml = bodyHtml,
        attachments = attachments,
        timestampEpochMs = timestamp,
        unread = unread,
        starred = starred,
    )

    private suspend fun seedThread(
        threadId: String,
        accountId: String,
        subject: String,
        messages: List<MessageRecord>,
    ) {
        val latest = messages.maxByOrNull { it.timestampEpochMs }
        val participants = messages
            .mapNotNull { it.fromName?.takeIf { n -> n.isNotBlank() } ?: it.fromAddress.takeIf { a -> a.isNotBlank() } }
            .distinct()
        mail.saveThreadWithMessages(
            ThreadRecord(
                threadId = threadId,
                gmailThreadId = null, // fixtures were never synced
                accountId = accountId,
                subject = subject,
                participantDisplayNames = participants,
                messageCount = messages.size,
                unreadCount = messages.count { it.unread },
                latestMessageId = latest?.messageId,
                latestMessageEpochMs = latest?.timestampEpochMs ?: 0L,
                updatedAtEpochMs = clock(),
            ),
            messages,
        )
    }
}
