package com.greninjaop.mailorganizer.ui.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.greninjaop.mailorganizer.di.AppContainer

/** Factory for PrivacyViewModel. */
class PrivacyViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PrivacyViewModel(
            privacyUseCase = container.privacyUseCase,
            accountRepository = container.accountRepository,
            dispatchers = container.dispatchers,
        ) as T
    }
}
