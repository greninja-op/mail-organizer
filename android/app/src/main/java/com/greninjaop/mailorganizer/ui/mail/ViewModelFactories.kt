package com.greninjaop.mailorganizer.ui.mail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.greninjaop.mailorganizer.di.AppContainer

/** Factory for the Phase 6 mailbox screen. */
class MailViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MailViewModel(
            accounts = container.accountRepository,
            mail = container.mailRepository,
            intelligence = container.intelligenceRepository,
            classifyMailbox = container.classifyMailboxUseCase,
            companyIntelligence = container.companyIntelligenceUseCase,
            prioritizeMailbox = container.prioritizeMailboxUseCase,
            extractMailbox = container.extractMailboxUseCase,
            syncCoordinator = container.syncCoordinator,
            connectivity = container.connectivityObserver,
            dispatchers = container.dispatchers,
            samplePolicy = container.sampleDataPolicy,
            seeder = container.sampleMailboxSeeder,
        ) as T
    }
}

/** Factory for the Phase 6 thread screen (threadId comes from navigation). */
class ThreadViewModelFactory(
    private val container: AppContainer,
    private val threadId: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ThreadViewModel(
            threadId = threadId,
            mail = container.mailRepository,
            intelligence = container.intelligenceRepository,
            dispatchers = container.dispatchers,
        ) as T
    }
}
