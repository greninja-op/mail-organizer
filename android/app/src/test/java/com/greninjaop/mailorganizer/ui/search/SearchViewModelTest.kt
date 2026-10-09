package com.greninjaop.mailorganizer.ui.search

import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.search.SearchIndexState
import com.greninjaop.mailorganizer.core.search.SearchOutcome
import com.greninjaop.mailorganizer.core.search.SearchQuery
import com.greninjaop.mailorganizer.core.search.SearchResult
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.repository.AccountRepository
import com.greninjaop.mailorganizer.data.repository.SearchRepository
import com.greninjaop.mailorganizer.domain.search.SearchIndexMaintenance
import com.greninjaop.mailorganizer.ui.mail.ConnectivityObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Phase 10 — SearchViewModel behavior.
 *
 * IMPORTANT: [SearchViewModel.state] uses SharingStarted.WhileSubscribed, so
 * nothing flows until something collects. Each test starts a background
 * collector on [backgroundScope] BEFORE advancing virtual time; assertions
 * then read [SearchViewModel.state].value directly.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var accounts: FakeAccountsRepository
    private lateinit var search: FakeSearchRepository
    private lateinit var indexMaintenance: FakeIndexMaintenance
    private lateinit var connectivity: FakeConnectivity

    private val dispatchers = AppDispatchers(
        io = testDispatcher,
        default = testDispatcher,
        main = testDispatcher,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        accounts = FakeAccountsRepository()
        search = FakeSearchRepository()
        indexMaintenance = FakeIndexMaintenance()
        connectivity = FakeConnectivity()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(initialQuery: String = "") = SearchViewModel(
        accounts = accounts,
        search = search,
        searchIndex = indexMaintenance,
        connectivity = connectivity,
        dispatchers = dispatchers,
        initialQuery = initialQuery,
    )

    @Test
    fun `empty query shows landing without running search`() = runTest(testDispatcher) {
        val vm = viewModel()
        backgroundScope.launch { vm.state.collect {} }
        runCurrent()

        val content = vm.state.value.content
        assertTrue(content is SearchContent.Landing)
        assertEquals(0, search.calls)
    }

    @Test
    fun `debounce delays search until 300ms quiet`() = runTest(testDispatcher) {
        val vm = viewModel()
        backgroundScope.launch { vm.state.collect {} }
        runCurrent()

        vm.setQueryText("interview")
        advanceTimeBy(100)
        runCurrent()
        // Debounce not elapsed — still landing, no search ran.
        assertTrue(vm.state.value.content is SearchContent.Landing)
        assertEquals("", vm.state.value.queryText)
        assertEquals(0, search.calls)

        advanceTimeBy(500)
        runCurrent()
        val content = vm.state.value.content
        assertTrue(content is SearchContent.Results)
        assertEquals(1, search.calls)
        assertEquals("interview", vm.state.value.queryText)
    }

    @Test
    fun `rapid typing only searches once`() = runTest(testDispatcher) {
        val vm = viewModel()
        backgroundScope.launch { vm.state.collect {} }
        runCurrent()

        vm.setQueryText("a")
        advanceTimeBy(100)
        vm.setQueryText("ab")
        advanceTimeBy(100)
        vm.setQueryText("abc")
        advanceTimeBy(500)
        runCurrent()

        assertEquals(1, search.calls)
        assertEquals("abc", search.lastQuery?.rawText)
    }

    @Test
    fun `no matches shows empty state`() = runTest(testDispatcher) {
        val vm = viewModel()
        backgroundScope.launch { vm.state.collect {} }
        runCurrent()

        vm.setQueryText("zzz-no-match")
        advanceTimeBy(500)
        runCurrent()

        val content = vm.state.value.content
        assertTrue(content is SearchContent.Empty)
        assertTrue((content as SearchContent.Empty).hint.isNotBlank())
    }

    @Test
    fun `account isolation - other accounts mail never returned`() = runTest(testDispatcher) {
        val vm = viewModel()
        backgroundScope.launch { vm.state.collect {} }
        runCurrent()

        vm.setQueryText("interview")
        advanceTimeBy(500)
        runCurrent()

        val content = vm.state.value.content
        assertTrue(content is SearchContent.Results)
        val results = (content as SearchContent.Results).outcome.results
        assertEquals(1, results.size)
        val msg = results[0] as SearchResult.Message
        assertEquals("a1", msg.record.accountId)
        // The engine itself only holds a1 mail; nothing from a2 can leak.
        assertEquals("a1", search.lastQuery?.accountId)
    }

    @Test
    fun `unread filter applies`() = runTest(testDispatcher) {
        val vm = viewModel()
        backgroundScope.launch { vm.state.collect {} }
        runCurrent()

        vm.setQueryText("interview")
        vm.toggleUnreadOnly()
        advanceTimeBy(500)
        runCurrent()

        assertTrue(search.lastQuery?.unreadOnly == true)
    }

    @Test
    fun `search failure shows error with rebuild action`() = runTest(testDispatcher) {
        search.fail = true
        val vm = viewModel()
        backgroundScope.launch { vm.state.collect {} }
        runCurrent()

        vm.setQueryText("interview")
        advanceTimeBy(500)
        runCurrent()

        val content = vm.state.value.content
        assertTrue(content is SearchContent.Error)
        val error = content as SearchContent.Error
        assertTrue(error.message.isNotBlank())
        assertFalse(vm.state.value.indexing)

        // Rebuild action re-runs the search (still failing here) and clears indexing.
        vm.rebuildIndex()
        runCurrent()
        assertFalse(vm.state.value.indexing)
        assertTrue(vm.state.value.content is SearchContent.Error)
        assertTrue(indexMaintenance.rebuildCalls >= 1)
    }

    @Test
    fun `offline banner state is exposed`() = runTest(testDispatcher) {
        connectivity.online.value = false
        val vm = viewModel()
        backgroundScope.launch { vm.state.collect {} }
        runCurrent()

        vm.setQueryText("interview")
        advanceTimeBy(500)
        runCurrent()

        assertTrue(vm.state.value.isOffline)
        // Search still runs locally while offline.
        assertTrue(vm.state.value.content is SearchContent.Results)
    }

    // ------------------------------------------------------------------
    // Fakes
    // ------------------------------------------------------------------

    private class FakeAccountsRepository : AccountRepository {
        private val all = MutableStateFlow(
            listOf(
                AccountRecord("a1", "a1@gmail.com", "Work", createdAtEpochMs = 1L),
                AccountRecord("a2", "a2@gmail.com", "Personal", createdAtEpochMs = 2L),
            ),
        )

        override fun observeAll(): Flow<List<AccountRecord>> = all
        override fun observeEnabled(): Flow<List<AccountRecord>> = all
        override suspend fun getById(accountId: String) =
            all.value.firstOrNull { it.accountId == accountId }
        override suspend fun upsert(account: AccountRecord) = Unit
        override suspend fun updateConnectionState(
            accountId: String,
            state: com.greninjaop.mailorganizer.data.local.ConnectionState,
        ) = Unit
        override suspend fun recordSync(accountId: String, syncEpochMs: Long) = Unit
        override suspend fun setEnabled(accountId: String, enabled: Boolean) = Unit
        override suspend fun deleteById(accountId: String) = Unit
    }

    private class FakeSearchRepository : SearchRepository {
        var calls = 0
        var lastQuery: SearchQuery? = null
        var fail = false

        override suspend fun search(query: SearchQuery): SearchOutcome {
            calls++
            lastQuery = query
            if (fail) throw IllegalStateException("boom")
            val results = if (query.rawText.contains("zzz-no-match")) {
                emptyList()
            } else {
                listOf(
                    SearchResult.Message(
                        record = MessageRecord(
                            messageId = "m1",
                            gmailMessageId = "g1",
                            threadId = "t1",
                            accountId = "a1",
                            fromAddress = "priya@example.com",
                            fromName = "Priya",
                            subject = "Interview tomorrow",
                            snippet = "Interview tomorrow at 10",
                            bodyText = "Interview tomorrow at 10",
                            timestampEpochMs = 1000L,
                        ),
                        rank = 1.0,
                        snippet = "Interview tomorrow at 10",
                        classification = null,
                        priority = null,
                    ),
                )
            }
            return SearchOutcome(
                results = results,
                query = query,
                parsed = com.greninjaop.mailorganizer.core.search.QueryParser.parse(query.rawText),
                latencyMs = 0L,
            )
        }

        override suspend fun indexState(accountId: String): SearchIndexState =
            SearchIndexState.READY
    }

    private class FakeIndexMaintenance : SearchIndexMaintenance {
        var ensureCalls = 0
        var rebuildCalls = 0

        override suspend fun ensureIndexed(accountId: String): SearchIndexState {
            ensureCalls++
            return SearchIndexState.READY
        }

        override suspend fun rebuild(accountId: String): Int {
            rebuildCalls++
            return 0
        }
    }

    private class FakeConnectivity : ConnectivityObserver {
        val online = MutableStateFlow(true)
        override val isOnline: Flow<Boolean> = online
    }
}
