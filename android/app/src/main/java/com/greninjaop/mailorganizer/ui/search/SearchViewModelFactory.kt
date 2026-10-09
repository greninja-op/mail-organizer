package com.greninjaop.mailorganizer.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.greninjaop.mailorganizer.di.AppContainer

/** Factory for the Phase 10 search screen (initial query comes from navigation). */
class SearchViewModelFactory(
    private val container: AppContainer,
    private val initialQuery: String = "",
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SearchViewModel(
            accounts = container.accountRepository,
            search = container.searchRepository,
            searchIndex = container.searchIndexUseCase,
            connectivity = container.connectivityObserver,
            dispatchers = container.dispatchers,
            initialQuery = initialQuery,
            activeAccountPreferences = container.activeAccountPreferences,
        ) as T
    }
}
