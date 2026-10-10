package com.greninjaop.mailorganizer.ui.automation

import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.automation.AutomationAccountScope
import com.greninjaop.mailorganizer.core.automation.AutomationAction
import com.greninjaop.mailorganizer.core.automation.AutomationActionType
import com.greninjaop.mailorganizer.core.automation.AutomationConditionGroup
import com.greninjaop.mailorganizer.core.automation.AutomationConfirmationPolicy
import com.greninjaop.mailorganizer.core.automation.AutomationLifecycleState
import com.greninjaop.mailorganizer.core.automation.AutomationRule
import com.greninjaop.mailorganizer.core.automation.AutomationScopeType
import com.greninjaop.mailorganizer.core.automation.AutomationTrigger
import com.greninjaop.mailorganizer.core.automation.AutomationTriggerType
import com.greninjaop.mailorganizer.data.prefs.AccountSelection
import com.greninjaop.mailorganizer.data.prefs.ActiveAccountPreferences
import com.greninjaop.mailorganizer.domain.automation.AutomationEngine
import com.greninjaop.mailorganizer.domain.automation.AutomationUseCase
import com.greninjaop.mailorganizer.domain.automation.FakeAutomationRepository
import com.greninjaop.mailorganizer.ui.mail.FakeAccountRepository
import com.greninjaop.mailorganizer.ui.mail.FakeIntelligenceRepository
import com.greninjaop.mailorganizer.ui.mail.FakeMailRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeActiveAccountPreferences : ActiveAccountPreferences {
    val selectionFlow = MutableStateFlow<AccountSelection>(AccountSelection.Single("acc-1"))
    override val activeSelection = selectionFlow

    override suspend fun setActiveSelection(selection: AccountSelection) {
        selectionFlow.value = selection
    }
}

class AutomationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers = AppDispatchers(testDispatcher, testDispatcher, testDispatcher)

    private lateinit var automationRepo: FakeAutomationRepository
    private lateinit var accountRepo: FakeAccountRepository
    private lateinit var mailRepo: FakeMailRepository
    private lateinit var intelligenceRepo: FakeIntelligenceRepository
    private lateinit var activePrefs: FakeActiveAccountPreferences
    private lateinit var engine: AutomationEngine
    private lateinit var useCase: AutomationUseCase
    private lateinit var viewModel: AutomationViewModel

    @Before
    fun setUp() {
        automationRepo = FakeAutomationRepository()
        accountRepo = FakeAccountRepository()
        mailRepo = FakeMailRepository()
        intelligenceRepo = FakeIntelligenceRepository()
        activePrefs = FakeActiveAccountPreferences()

        engine = AutomationEngine(
            automationRepository = automationRepo,
            mailRepository = mailRepo,
            intelligenceRepository = intelligenceRepo,
            integrationManager = null,
            executorRegistry = null,
            dispatchers = dispatchers,
        )
        useCase = AutomationUseCase(
            repository = automationRepo,
            engine = engine,
            dispatchers = dispatchers,
        )
        viewModel = AutomationViewModel(
            automationUseCase = useCase,
            accountRepository = accountRepo,
            activeAccountPreferences = activePrefs,
            dispatchers = dispatchers,
        )
    }

    @Test
    fun saveRule_addsRuleToRepository() = runTest(testDispatcher) {
        var callbackInvoked = false
        viewModel.saveRule(
            ruleId = null,
            name = "Test Automation",
            description = "Description",
            scopeType = AutomationScopeType.SPECIFIC_ACCOUNT,
            targetAccountId = "acc-1",
            triggerType = AutomationTriggerType.NEW_EMAIL_SYNCED,
            triggerParam = null,
            conditionGroup = AutomationConditionGroup(),
            actions = listOf(AutomationAction(AutomationActionType.MARK_AS_READ)),
            confirmationPolicy = AutomationConfirmationPolicy.ALWAYS_CONFIRM,
            onSuccess = { callbackInvoked = true }
        )
        advanceUntilIdle()

        assertTrue(callbackInvoked)
        val rules = automationRepo.getAllRules().first()
        assertEquals(1, rules.size)
        assertEquals("Test Automation", rules[0].name)
    }

    @Test
    fun toggleRule_updatesRuleState() = runTest(testDispatcher) {
        val rule = AutomationRule(
            id = "rule-1",
            name = "Toggle Me",
            scope = AutomationAccountScope(AutomationScopeType.SPECIFIC_ACCOUNT, "acc-1"),
            state = AutomationLifecycleState.ENABLED,
            trigger = AutomationTrigger(AutomationTriggerType.NEW_EMAIL_SYNCED),
            actions = listOf(AutomationAction(AutomationActionType.MARK_AS_READ)),
            confirmationPolicy = AutomationConfirmationPolicy.PRE_APPROVED,
            createdAtEpochMs = 1000L,
            updatedAtEpochMs = 1000L,
        )
        automationRepo.saveRule(rule)

        viewModel.toggleRule("rule-1", false)
        advanceUntilIdle()

        val updated = automationRepo.getRuleById("rule-1")
        assertNotNull(updated)
        assertEquals(AutomationLifecycleState.DISABLED, updated?.state)
    }

    @Test
    fun deleteRule_removesFromRepository() = runTest(testDispatcher) {
        val rule = AutomationRule(
            id = "rule-del",
            name = "Delete Me",
            scope = AutomationAccountScope(AutomationScopeType.SPECIFIC_ACCOUNT, "acc-1"),
            state = AutomationLifecycleState.ENABLED,
            trigger = AutomationTrigger(AutomationTriggerType.NEW_EMAIL_SYNCED),
            actions = listOf(AutomationAction(AutomationActionType.MARK_AS_READ)),
            confirmationPolicy = AutomationConfirmationPolicy.PRE_APPROVED,
            createdAtEpochMs = 1000L,
            updatedAtEpochMs = 1000L,
        )
        automationRepo.saveRule(rule)

        viewModel.deleteRule("rule-del")
        advanceUntilIdle()

        val rules = automationRepo.getAllRules().first()
        assertTrue(rules.isEmpty())
    }
}
