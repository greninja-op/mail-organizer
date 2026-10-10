package com.greninjaop.mailorganizer.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.greninjaop.mailorganizer.data.prefs.ActiveAccountPreferences
import com.greninjaop.mailorganizer.data.repository.AccountRepository
import com.greninjaop.mailorganizer.domain.analytics.AnalyticsUseCase

class AnalyticsViewModelFactory(
    private val analyticsUseCase: AnalyticsUseCase,
    private val accountRepository: AccountRepository,
    private val activeAccountPreferences: ActiveAccountPreferences,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnalyticsViewModel::class.java)) {
            return AnalyticsViewModel(
                analyticsUseCase = analyticsUseCase,
                accountRepository = accountRepository,
                activeAccountPreferences = activeAccountPreferences,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
