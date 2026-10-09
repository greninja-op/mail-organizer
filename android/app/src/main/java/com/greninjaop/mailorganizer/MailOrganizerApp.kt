package com.greninjaop.mailorganizer

import android.app.Application
import android.content.pm.ApplicationInfo
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.di.AppContainer

/**
 * Application entry point.
 *
 * Phase 0 wires a manual [AppContainer] (constructor injection). A DI framework
 * (Hilt/Koin) is deliberately deferred: the graph is small enough for manual
 * wiring, and the decision is revisited in Phase 1 if the graph justifies it.
 */
class MailOrganizerApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        val isDebug = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        MoLogger.init(isDebug)
        container = AppContainer(this)
        container.syncScheduler.onAppStart()
        MoLogger.i(TAG, "Mail Organizer foundation initialized")
    }

    private companion object {
        const val TAG = "MailOrganizerApp"
    }
}
