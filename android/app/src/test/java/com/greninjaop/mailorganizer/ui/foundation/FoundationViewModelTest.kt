package com.greninjaop.mailorganizer.ui.foundation

import app.cash.turbine.test
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.data.prefs.ThemeMode
import com.greninjaop.mailorganizer.data.prefs.ThemePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

private class FakeThemePreferences(initial: ThemeMode = ThemeMode.SYSTEM) : ThemePreferences {
    private val state = MutableStateFlow(initial)
    override val themeMode: Flow<ThemeMode> = state
    override suspend fun setThemeMode(mode: ThemeMode) {
        state.value = mode
    }

    fun current(): ThemeMode = state.value
}

class FoundationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var prefs: FakeThemePreferences
    private lateinit var viewModel: FoundationViewModel

    private fun testDispatchers() = AppDispatchers(
        io = testDispatcher,
        default = testDispatcher,
        main = testDispatcher,
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        prefs = FakeThemePreferences()
        viewModel = FoundationViewModel(prefs, testDispatchers())
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `setThemeMode persists the choice`() = runTest(testDispatcher) {
        viewModel.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()
        assertEquals(ThemeMode.DARK, prefs.current())
    }

    @Test
    fun `themeMode emits the stored preference`() = runTest(testDispatcher) {
        prefs.setThemeMode(ThemeMode.LIGHT)
        val vm = FoundationViewModel(prefs, testDispatchers())
        vm.themeMode.test {
            var last = awaitItem()
            while (last != ThemeMode.LIGHT) {
                last = awaitItem()
            }
            assertEquals(ThemeMode.LIGHT, last)
        }
    }
}
