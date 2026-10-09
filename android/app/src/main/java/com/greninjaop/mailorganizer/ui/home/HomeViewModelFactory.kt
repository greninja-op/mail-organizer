package com.greninjaop.mailorganizer.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.greninjaop.mailorganizer.di.AppContainer

/** Factory for the Phase 11 Home dashboard. */
class HomeViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HomeViewModel(
            accounts = container.accountRepository,
            mail = container.mailRepository,
            intelligence = container.intelligenceRepository,
            connectivity = container.connectivityObserver,
            dispatchers = container.dispatchers,
            samplePolicy = container.sampleDataPolicy,
            seeder = container.sampleMailboxSeeder,
            activeAccountPreferences = container.activeAccountPreferences,
        ) as T
    }
}
