package com.greninjaop.mailorganizer.domain.rules

import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.rules.RuleAction
import com.greninjaop.mailorganizer.core.rules.RuleActionType
import com.greninjaop.mailorganizer.core.rules.RuleCondition
import com.greninjaop.mailorganizer.core.rules.RuleConditionField
import com.greninjaop.mailorganizer.core.rules.RuleOperator
import com.greninjaop.mailorganizer.data.local.ActionItemRecord
import com.greninjaop.mailorganizer.data.local.ActionType
import com.greninjaop.mailorganizer.data.local.ClassificationRecord
import com.greninjaop.mailorganizer.data.local.ClassificationSource
import com.greninjaop.mailorganizer.data.local.CompanyRecord
import com.greninjaop.mailorganizer.data.local.CorrectionField
import com.greninjaop.mailorganizer.data.local.CorrectionScope
import com.greninjaop.mailorganizer.data.local.ExtractedItemRecord
import com.greninjaop.mailorganizer.data.local.ExtractedItemType
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.local.Priority
import com.greninjaop.mailorganizer.data.local.PriorityRecord
import com.greninjaop.mailorganizer.data.local.RuleSource
import com.greninjaop.mailorganizer.data.local.SenderRecord
import com.greninjaop.mailorganizer.data.local.ThreadRecord
import com.greninjaop.mailorganizer.data.local.UserCorrectionRecord
import com.greninjaop.mailorganizer.data.local.UserRuleRecord
import com.greninjaop.mailorganizer.data.repository.CorrectionLookup as RepoCorrectionLookup
import com.greninjaop.mailorganizer.data.repository.IntelligenceRepository
import com.greninjaop.mailorganizer.data.repository.MailRepository
import com.greninjaop.mailorganizer.data.repository.RuleRepository
import com.greninjaop.mailorganizer.domain.classify.ClassifyMessageUseCase
import com.greninjaop.mailorganizer.domain.priority.PrioritizeMessageUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Use-case tests for the Phase 12 effective-intelligence pipeline.
 *
 * Covers precedence (correction > rule > deterministic), idempotency,
 * undo/restore, account isolation, and reprocessing — with in-memory
 * fakes (no Robolectric). The deterministic engines run for real through
 * the actual [ClassifyMessageUseCase]/[PrioritizeMessageUseCase].
 */
class ApplyRulesUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers = AppDispatchers(
        io = testDispatcher,
        default = testDispatcher,
        main = testDispatcher,
    )

    private lateinit var mail: FakeRulesMailRepository
    private lateinit var intelligence: FakeRulesIntelligenceRepository
    private lateinit var rules: FakeRuleRepository
    private lateinit var apply: ApplyRulesUseCase
    private lateinit var corrections: RecordCorrectionUseCase
    private lateinit var management: RuleManagementUseCase

    @Before
    fun setup() {
        mail = FakeRulesMailRepository()
        intelligence = FakeRulesIntelligenceRepository()
        rules = FakeRuleRepository()
        val classify = ClassifyMessageUseCase(mail, intelligence, dispatchers) { 1_700_000_000_000L }
        val prioritize = PrioritizeMessageUseCase(mail, intelligence, dispatchers) { 1_700_000_000_000L }
        apply = ApplyRulesUseCase(
            mail, intelligence, rules, classify, prioritize, dispatchers,
        ) { 1_700_000_000_000L }
        corrections = RecordCorrectionUseCase(
            mail, intelligence, rules, apply, dispatchers,
        ) { 1_700_000_000_000L }
        management = RuleManagementUseCase(
            mail, rules, apply, dispatchers,
        ) { 1_700_000_000_000L }
    }

    private fun message(
        id: String,
        account: String = "acc1",
        from: String = "sender@example.com",
        subject: String = "Hello world",
    ) = MessageRecord(
        messageId = id,
        gmailMessageId = "g-$id",
        threadId = "t-$id",
        accountId = account,
        fromAddress = from,
        fromName = null,
        subject = subject,
        snippet = null,
        bodyText = "Body of $id",
        timestampEpochMs = 1_000L,
    )

    private fun categoryRule(
        name: String,
        domain: String,
        category: MailCategory,
        order: Int = 0,
        account: String = "acc1",
    ) = UserRule(
        accountId = account,
        name = name,
        order = order,
        conditions = listOf(
            RuleCondition(RuleConditionField.SENDER_DOMAIN, RuleOperator.EQUALS, domain),
        ),
        actions = listOf(RuleAction(RuleActionType.SET_CATEGORY, category.name)),
        createdAtEpochMs = 1L,
        updatedAtEpochMs = 1L,
    )

    // ---- Precedence ----

    @Test
    fun `no user input leaves the deterministic result untouched`() = runTest(testDispatcher) {
        mail.add(message("m1"))
        // Base classification first.
        apply.refreshMessage("m1", "acc1")
        val base = intelligence.getClassification("m1")!!
        assertEquals(ClassificationSource.DETERMINISTIC, base.source)
        assertFalse(base.overridden)

        // Second refresh with no rules/corrections writes nothing.
        assertFalse(apply.refreshMessage("m1", "acc1"))
        val again = intelligence.getClassification("m1")!!
        assertEquals(base.category, again.category)
        assertEquals(ClassificationSource.DETERMINISTIC, again.source)
    }

    @Test
    fun `message correction overrides the category`() = runTest(testDispatcher) {
        mail.add(message("m1"))
        assertTrue(corrections.correctMessage("acc1", "m1", CorrectionField.CATEGORY, "CAREER"))

        val row = intelligence.getClassification("m1")!!
        assertEquals(MailCategory.CAREER, row.category)
        assertEquals(ClassificationSource.USER_CORRECTION, row.source)
        assertTrue(row.overridden)
        assertTrue(row.explanation!!.contains("You set"))

        // The deterministic engine must not clobber it on re-run.
        assertFalse(apply.refreshMessage("m1", "acc1"))
        assertEquals(MailCategory.CAREER, intelligence.getClassification("m1")!!.category)
    }

    @Test
    fun `sender correction applies to all their messages`() = runTest(testDispatcher) {
        mail.add(message("m1", from = "boss@example.com"))
        mail.add(message("m2", from = "boss@example.com"))
        mail.add(message("m3", from = "other@example.com"))

        val refreshed = corrections.correctSender("acc1", "boss@example.com", CorrectionField.CATEGORY, "IMPORTANT")
        assertEquals(2, refreshed)
        assertEquals(MailCategory.IMPORTANT, intelligence.getClassification("m1")!!.category)
        assertEquals(MailCategory.IMPORTANT, intelligence.getClassification("m2")!!.category)
        assertEquals(
            ClassificationSource.USER_CORRECTION,
            intelligence.getClassification("m2")!!.source,
        )
        // Unrelated sender untouched (still deterministic or absent, never USER_CORRECTION).
        val other = intelligence.getClassification("m3")
        assertTrue(other == null || other.source != ClassificationSource.USER_CORRECTION)
    }

    @Test
    fun `correction outranks rule`() = runTest(testDispatcher) {
        mail.add(message("m1", from = "boss@example.com"))
        val ruleId = management.createRule(
            categoryRule("Example → Promotions", "example.com", MailCategory.PROMOTIONS),
        )
        assertTrue(ruleId > 0)
        assertEquals(MailCategory.PROMOTIONS, intelligence.getClassification("m1")!!.category)

        corrections.correctMessage("acc1", "m1", CorrectionField.CATEGORY, "CAREER")
        val row = intelligence.getClassification("m1")!!
        assertEquals(MailCategory.CAREER, row.category)
        assertEquals(ClassificationSource.USER_CORRECTION, row.source)
    }

    @Test
    fun `rule applies when no correction exists`() = runTest(testDispatcher) {
        mail.add(message("m1", from = "news@example.com", subject = "Weekly deals"))
        val ruleId = management.createRule(
            categoryRule("Example → Newsletters", "example.com", MailCategory.NEWSLETTERS),
        )
        assertTrue(ruleId > 0)

        val row = intelligence.getClassification("m1")!!
        assertEquals(MailCategory.NEWSLETTERS, row.category)
        assertEquals(ClassificationSource.USER_RULE, row.source)
        assertTrue(row.overridden)
        assertTrue(row.explanation!!.contains("Example → Newsletters"))
    }

    @Test
    fun `lower rule order wins conflicts`() = runTest(testDispatcher) {
        mail.add(message("m1", from = "a@example.com"))
        management.createRule(categoryRule("Broad", "example.com", MailCategory.PROMOTIONS, order = 10))
        management.createRule(categoryRule("Specific", "example.com", MailCategory.CAREER, order = 1))

        assertEquals(MailCategory.CAREER, intelligence.getClassification("m1")!!.category)
    }

    @Test
    fun `disabled rule is ignored`() = runTest(testDispatcher) {
        mail.add(message("m1", from = "a@example.com"))
        val id = management.createRule(categoryRule("R", "example.com", MailCategory.CAREER))
        assertEquals(MailCategory.CAREER, intelligence.getClassification("m1")!!.category)

        assertTrue(management.setRuleEnabled("acc1", id, false))
        val row = intelligence.getClassification("m1")!!
        // Falls back to the deterministic base — the rule no longer applies.
        assertEquals(ClassificationSource.DETERMINISTIC, row.source)
    }

    // ---- Undo / restore ----

    @Test
    fun `undoing a correction restores the deterministic base`() = runTest(testDispatcher) {
        mail.add(message("m1", from = "a@example.com", subject = "Hello world"))
        apply.refreshMessage("m1", "acc1")
        val baseCategory = intelligence.getClassification("m1")!!.category

        corrections.correctMessage("acc1", "m1", CorrectionField.CATEGORY, "CAREER")
        assertEquals(MailCategory.CAREER, intelligence.getClassification("m1")!!.category)

        corrections.undoCorrection("acc1", CorrectionScope.MESSAGE, "m1", CorrectionField.CATEGORY)
        val restored = intelligence.getClassification("m1")!!
        assertEquals(baseCategory, restored.category)
        assertEquals(ClassificationSource.DETERMINISTIC, restored.source)
        assertFalse(restored.overridden)
    }

    @Test
    fun `deleting a rule restores the deterministic base`() = runTest(testDispatcher) {
        mail.add(message("m1", from = "a@example.com", subject = "Hello world"))
        apply.refreshMessage("m1", "acc1")
        val baseCategory = intelligence.getClassification("m1")!!.category

        val id = management.createRule(categoryRule("R", "example.com", MailCategory.CAREER))
        assertEquals(MailCategory.CAREER, intelligence.getClassification("m1")!!.category)

        assertTrue(management.deleteRule("acc1", id))
        val restored = intelligence.getClassification("m1")!!
        assertEquals(baseCategory, restored.category)
        assertEquals(ClassificationSource.DETERMINISTIC, restored.source)
    }

    // ---- Priority ----

    @Test
    fun `priority correction sets manual override with explanation`() = runTest(testDispatcher) {
        mail.add(message("m1"))
        assertTrue(corrections.correctMessage("acc1", "m1", CorrectionField.PRIORITY, "HIGH"))

        val row = intelligence.getPriority("m1")!!
        assertEquals(Priority.HIGH, row.priority)
        assertTrue(row.manualOverride)
        assertTrue(row.reason!!.contains("You set"))

        // Idempotent: second refresh writes nothing.
        assertFalse(apply.refreshMessage("m1", "acc1"))
    }

    @Test
    fun `priority rule applies and is explained`() = runTest(testDispatcher) {
        mail.add(message("m1", from = "vip@example.com"))
        val rule = UserRule(
            accountId = "acc1",
            name = "VIPs are high priority",
            conditions = listOf(
                RuleCondition(RuleConditionField.SENDER_EMAIL, RuleOperator.EQUALS, "vip@example.com"),
            ),
            actions = listOf(RuleAction(RuleActionType.SET_PRIORITY, "HIGH")),
            createdAtEpochMs = 1L,
            updatedAtEpochMs = 1L,
        )
        assertTrue(management.createRule(rule) > 0)
        val row = intelligence.getPriority("m1")!!
        assertEquals(Priority.HIGH, row.priority)
        assertTrue(row.manualOverride)
        assertTrue(row.reason!!.contains("VIPs are high priority"))
    }

    // ---- Account isolation ----

    @Test
    fun `corrections never leak across accounts`() = runTest(testDispatcher) {
        mail.add(message("m1", account = "acc1", from = "boss@example.com"))
        mail.add(message("m2", account = "acc2", from = "boss@example.com"))

        corrections.correctSender("acc1", "boss@example.com", CorrectionField.CATEGORY, "CAREER")
        assertEquals(MailCategory.CAREER, intelligence.getClassification("m1")!!.category)

        val other = intelligence.getClassification("m2")
        assertTrue(other == null || other.category != MailCategory.CAREER ||
            other.source != ClassificationSource.USER_CORRECTION)
    }

    @Test
    fun `rules never affect other accounts`() = runTest(testDispatcher) {
        mail.add(message("m1", account = "acc1", from = "a@example.com"))
        mail.add(message("m2", account = "acc2", from = "a@example.com"))
        management.createRule(categoryRule("R", "example.com", MailCategory.CAREER, account = "acc1"))

        assertEquals(MailCategory.CAREER, intelligence.getClassification("m1")!!.category)
        val other = intelligence.getClassification("m2")
        assertTrue(other == null || other.source != ClassificationSource.USER_RULE)
    }

    // ---- Preview & conflicts ----

    @Test
    fun `rule preview counts local matches honestly`() = runTest(testDispatcher) {
        mail.add(message("m1", from = "a@example.com"))
        mail.add(message("m2", from = "b@example.com"))
        mail.add(message("m3", from = "c@other.com"))
        val draft = categoryRule("D", "example.com", MailCategory.CAREER)

        val preview = management.previewRule("acc1", draft)
        assertEquals(2, preview.matchCount)
        assertTrue(preview.isExactCount)
        assertEquals(2, preview.samples.size)
    }

    @Test
    fun `conflicting rules are detected`() = runTest(testDispatcher) {
        management.createRule(categoryRule("A", "example.com", MailCategory.CAREER, order = 1))
        management.createRule(categoryRule("B", "example.com", MailCategory.EDUCATION, order = 2))

        val conflicts = management.detectConflicts("acc1")
        assertEquals(1, conflicts.size)
        // Lower order wins.
        assertEquals("CAREER", conflicts[0].valueA)
        assertEquals("EDUCATION", conflicts[0].valueB)
    }

    @Test
    fun `malformed rule is rejected`() = runTest(testDispatcher) {
        val bad = UserRule(
            accountId = "acc1",
            name = "",
            conditions = emptyList(),
            actions = emptyList(),
            createdAtEpochMs = 1L,
            updatedAtEpochMs = 1L,
        )
        assertEquals(-1L, management.createRule(bad))
    }

    @Test
    fun `rule from correction builds a sender rule`() {
        val rule = ruleFromCorrection(
            accountId = "acc1",
            ruleName = "Boss → Important",
            senderEmail = "Boss@Example.com",
            senderDomain = "example.com",
            preferDomain = false,
            field = CorrectionField.CATEGORY,
            value = "IMPORTANT",
            nowEpochMs = 5L,
        )
        assertNotNull(rule)
        assertEquals(1, rule!!.conditions.size)
        assertEquals("boss@example.com", rule.conditions[0].value)
        assertEquals(RuleSource.FROM_CORRECTION, rule.source)
        assertTrue(rule.isWellFormed())
    }

    // ================= Fakes =================

    private class FakeRulesMailRepository : MailRepository {
        private val messages = mutableMapOf<String, MessageRecord>()

        fun add(m: MessageRecord) {
            messages[m.messageId] = m
        }

        override suspend fun getMessage(messageId: String) = messages[messageId]
        override suspend fun getMessagesByIds(ids: List<String>) = ids.mapNotNull { messages[it] }
        override suspend fun getMessageIdsByAccount(accountId: String, limit: Int) =
            messages.values.filter { it.accountId == accountId }.map { it.messageId }.take(limit)
        override suspend fun getMessageIdsBySender(accountId: String, email: String, limit: Int) =
            messages.values.filter {
                it.accountId == accountId && it.fromAddress.equals(email, ignoreCase = true)
            }.map { it.messageId }.take(limit)
        override suspend fun getMessageIdsByDomain(accountId: String, domain: String, limit: Int) =
            messages.values.filter {
                it.accountId == accountId &&
                    it.fromAddress.substringAfter('@', "").equals(domain, ignoreCase = true)
            }.map { it.messageId }.take(limit)
        override suspend fun getMessageIdsByCompany(accountId: String, companyId: String, limit: Int) =
            messages.values.filter {
                it.accountId == accountId && it.companyId == companyId
            }.map { it.messageId }.take(limit)

        // Unused in these tests.
        override suspend fun saveThreadWithMessages(thread: ThreadRecord, messages: List<MessageRecord>) = Unit
        override fun observeThreads(accountId: String, limit: Int): Flow<List<ThreadRecord>> = MutableStateFlow(emptyList())
        override fun observeMessages(threadId: String, limit: Int): Flow<List<MessageRecord>> = MutableStateFlow(emptyList())
        override fun observeUnread(accountId: String, limit: Int): Flow<List<MessageRecord>> = MutableStateFlow(emptyList())
        override fun observeStarred(accountId: String, limit: Int): Flow<List<MessageRecord>> = MutableStateFlow(emptyList())
        override suspend fun searchByText(accountId: String, query: String, limit: Int) = emptyList<MessageRecord>()
        override suspend fun setRead(messageId: String, read: Boolean) = Unit
        override suspend fun setStarred(messageId: String, starred: Boolean) = Unit
        override suspend fun countByAccount(accountId: String) = messages.values.count { it.accountId == accountId }
        override suspend fun getThreadByGmailId(accountId: String, gmailThreadId: String): ThreadRecord? = null
        override suspend fun updateThreadAggregates(threadId: String) = Unit
        override suspend fun existingGmailIds(accountId: String, gmailIds: List<String>) = emptySet<String>()
        override suspend fun deleteMessagesByGmailIds(accountId: String, gmailIds: List<String>) = emptyList<String>()
        override suspend fun getUnclassifiedMessages(accountId: String, limit: Int) = emptyList<MessageRecord>()
        override suspend fun getUnprioritizedMessages(accountId: String, limit: Int) = emptyList<MessageRecord>()
        override fun observeByLabel(accountId: String, label: String, limit: Int): Flow<List<MessageRecord>> = MutableStateFlow(emptyList())
        override suspend fun setMessageCompanyId(messageId: String, companyId: String?) = Unit
        override suspend fun getMessagesWithoutCompany(accountId: String, limit: Int) = emptyList<MessageRecord>()
        override fun observeMessagesByCompany(accountId: String, companyId: String, limit: Int): Flow<List<MessageRecord>> = MutableStateFlow(emptyList())
        override fun observeMessagesByCompanyAndCategory(accountId: String, category: MailCategory, companyId: String, limit: Int): Flow<List<MessageRecord>> = MutableStateFlow(emptyList())
        override fun observeMessagesByCompanyAndLabel(accountId: String, companyId: String, label: String, limit: Int): Flow<List<MessageRecord>> = MutableStateFlow(emptyList())
        override suspend fun companyCountsForCategory(accountId: String, category: MailCategory) = emptyMap<String, Int>()
        override suspend fun companyCountsForLabel(accountId: String, label: String) = emptyMap<String, Int>()
        override suspend fun companyMessageCounts(accountId: String) = emptyMap<String, Int>()
    }

    private class FakeRulesIntelligenceRepository : IntelligenceRepository {
        private val classifications = mutableMapOf<String, ClassificationRecord>()
        private val priorities = mutableMapOf<String, PriorityRecord>()
        private val companies = mutableMapOf<String, CompanyRecord>()

        override suspend fun setClassification(record: ClassificationRecord) {
            classifications[record.messageId] = record
        }
        override suspend fun getClassification(messageId: String) = classifications[messageId]
        override suspend fun deleteClassification(messageId: String) {
            classifications.remove(messageId)
        }
        override suspend fun getClassifications(messageIds: List<String>) =
            messageIds.mapNotNull { classifications[it] }.associateBy { it.messageId }
        override fun observeByCategory(accountId: String, category: MailCategory, limit: Int) =
            MutableStateFlow(emptyList<ClassificationRecord>())
        override suspend fun categoryCounts(accountId: String) = emptyMap<MailCategory, Int>()

        override suspend fun setPriority(record: PriorityRecord) {
            priorities[record.messageId] = record
        }
        override suspend fun getPriority(messageId: String) = priorities[messageId]
        override suspend fun deletePriority(messageId: String) {
            priorities.remove(messageId)
        }
        override suspend fun getPriorities(messageIds: List<String>) =
            messageIds.mapNotNull { priorities[it] }.associateBy { it.messageId }
        override fun observeByPriority(accountId: String, priority: Priority, limit: Int) =
            MutableStateFlow(emptyList<PriorityRecord>())

        override suspend fun getCompanyByDomain(accountId: String, normalizedDomain: String) =
            companies.values.firstOrNull {
                it.accountId == accountId && it.normalizedDomain == normalizedDomain
            }
        override suspend fun upsertCompany(company: CompanyRecord) {
            companies[company.companyId] = company
        }

        // Unused in these tests.
        override suspend fun upsertSender(sender: SenderRecord) = Unit
        override fun observeTopSenders(accountId: String, limit: Int) = MutableStateFlow(emptyList<SenderRecord>())
        override suspend fun getSenderByEmail(accountId: String, normalizedEmail: String): SenderRecord? = null
        override suspend fun recordSenderMessage(accountId: String, emailAddress: String, normalizedEmail: String, displayName: String?, domain: String): SenderRecord =
            SenderRecord("s", accountId, emailAddress, normalizedEmail, displayName, domain, 1L, 1L)
        override fun observeCompanyFilterList(accountId: String, limit: Int) = MutableStateFlow(emptyList<CompanyRecord>())
        override suspend fun setCompanyPinned(companyId: String, pinned: Boolean) = Unit
        override suspend fun addActionItem(item: ActionItemRecord) = 1L
        override fun observeOpenActionItems(accountId: String, limit: Int) = MutableStateFlow(emptyList<ActionItemRecord>())
        override suspend fun completeActionItem(id: Long) = Unit
        override suspend fun dismissActionItem(id: Long) = Unit
        override suspend fun addExtractedItem(item: ExtractedItemRecord) = 1L
        override fun observeOpenExtracted(accountId: String, type: ExtractedItemType, limit: Int) = MutableStateFlow(emptyList<ExtractedItemRecord>())
    }

    private class FakeRuleRepository : RuleRepository {
        private val ruleRows = mutableListOf<UserRuleRecord>()
        private val correctionRows = mutableListOf<UserCorrectionRecord>()
        private var nextId = 1L
        private val flows = MutableStateFlow(0)

        override suspend fun addRule(rule: UserRuleRecord): Long {
            val id = nextId++
            ruleRows += rule.copy(id = id)
            flows.value++
            return id
        }
        override suspend fun deleteRule(id: Long) {
            ruleRows.removeAll { it.id == id }
            flows.value++
        }
        override fun observeEnabledRules(accountId: String): Flow<List<UserRuleRecord>> =
            flows.map { ruleRows.filter { it.accountId == accountId && it.enabled } }
        override suspend fun getEnabledRules(accountId: String) =
            ruleRows.filter { it.accountId == accountId && it.enabled }
        override fun observeAllRules(): Flow<List<UserRuleRecord>> = flows.map { ruleRows.toList() }
        override suspend fun updateRule(rule: UserRuleRecord) {
            val i = ruleRows.indexOfFirst { it.id == rule.id }
            if (i >= 0) ruleRows[i] = rule
            flows.value++
        }
        override suspend fun getRule(id: Long) = ruleRows.firstOrNull { it.id == id }
        override suspend fun setRuleEnabled(id: Long, enabled: Boolean) {
            val i = ruleRows.indexOfFirst { it.id == id }
            if (i >= 0) ruleRows[i] = ruleRows[i].copy(enabled = enabled)
            flows.value++
        }
        override suspend fun getAllRules(accountId: String) =
            ruleRows.filter { it.accountId == accountId }.sortedWith(compareBy({ it.ruleOrder }, { it.id }))

        override suspend fun recordCorrection(correction: UserCorrectionRecord) {
            correctionRows.removeAll {
                it.accountId == correction.accountId && it.scope == correction.scope &&
                    it.scopeKey == correction.scopeKey && it.field == correction.field
            }
            correctionRows += correction
        }
        override suspend fun getCorrection(
            accountId: String,
            scope: CorrectionScope,
            scopeKey: String,
            field: CorrectionField,
        ) = correctionRows.firstOrNull {
            it.accountId == accountId && it.scope == scope && it.scopeKey == scopeKey && it.field == field
        }
        override suspend fun getCorrections(
            accountId: String,
            lookups: List<RepoCorrectionLookup>,
        ): Map<RepoCorrectionLookup, UserCorrectionRecord> {
            val wanted = lookups.filter { it.accountId == accountId }.toSet()
            val byKey = correctionRows.filter { it.accountId == accountId }.associateBy {
                RepoCorrectionLookup(it.accountId, it.scope, it.scopeKey, it.field)
            }
            return wanted.mapNotNull { l -> byKey[l]?.let { l to it } }.toMap()
        }
        override suspend fun deleteCorrection(
            accountId: String,
            scope: CorrectionScope,
            scopeKey: String,
            field: CorrectionField,
        ) {
            correctionRows.removeAll {
                it.accountId == accountId && it.scope == scope && it.scopeKey == scopeKey && it.field == field
            }
        }
    }
}
