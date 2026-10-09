package com.greninjaop.mailorganizer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.greninjaop.mailorganizer.di.AppContainer
import com.greninjaop.mailorganizer.ui.foundation.FoundationScreen
import com.greninjaop.mailorganizer.ui.foundation.PlaceholderScreen
import com.greninjaop.mailorganizer.ui.mail.MailScreen
import com.greninjaop.mailorganizer.ui.mail.MailViewModel
import com.greninjaop.mailorganizer.ui.mail.MailViewModelFactory
import com.greninjaop.mailorganizer.ui.mail.ThreadScreen
import com.greninjaop.mailorganizer.ui.mail.ThreadViewModel
import com.greninjaop.mailorganizer.ui.mail.ThreadViewModelFactory

/**
 * Navigation foundation — Phase 1 (phase-01 §25).
 *
 * Minimal foundation destinations for the full information architecture.
 * Each destination is a real route in the graph; non-home destinations
 * render an honest foundation placeholder until their phase builds them.
 * Later phases add real screens to these routes without a rewrite.
 */
object AppDestinations {
    const val HOME = "home"
    const val MAIL = "mail"
    const val CATEGORIES = "categories"
    const val COMPANIES = "companies"
    const val ACTIONS = "actions"

    const val SEARCH = "search"
    const val INTEGRATIONS = "integrations"
    const val SETTINGS = "settings"
    const val PRIVACY = "privacy"
    const val ACCOUNTS = "accounts"

    /**
     * Thread route with an optional deep-link focus arg (Phase 6, phase §37):
     * `mail/thread/{threadId}?focusMessageId={messageId}`. Future features
     * (notifications, widgets) can open a specific message without a rewrite.
     */
    const val THREAD = "mail/thread/{threadId}?focusMessageId={focusMessageId}"

    fun threadRoute(threadId: String, focusMessageId: String? = null): String =
        buildString {
            append("mail/thread/").append(threadId)
            if (focusMessageId != null) {
                append("?focusMessageId=").append(focusMessageId)
            }
        }

    /** Primary destinations, in display order. */
    val primary: List<String> = listOf(HOME, MAIL, CATEGORIES, COMPANIES, ACTIONS)

    /** Secondary destinations, in display order. */
    val secondary: List<String> = listOf(SEARCH, INTEGRATIONS, SETTINGS, PRIVACY, ACCOUNTS)

    /** Every route registered in the graph. */
    val all: List<String> = primary + secondary

    /** Human-readable title for a route. */
    fun titleFor(route: String): String = when (route) {
        HOME -> "Home"
        MAIL -> "Mail"
        CATEGORIES -> "Categories"
        COMPANIES -> "Companies"
        ACTIONS -> "Actions"
        SEARCH -> "Search"
        INTEGRATIONS -> "Integrations"
        SETTINGS -> "Settings"
        PRIVACY -> "Privacy"
        ACCOUNTS -> "Accounts"
        else -> route
    }

    /** The future phase that builds this destination's real UI. */
    fun phaseFor(route: String): String = when (route) {
        HOME -> "Phase 1"
        MAIL -> "Phase 6"
        CATEGORIES -> "Phase 9"
        COMPANIES -> "Phase 8"
        ACTIONS -> "Phase 14"
        SEARCH -> "Phase 10"
        INTEGRATIONS -> "Phase 17"
        SETTINGS -> "a later phase"
        PRIVACY -> "Phase 23"
        ACCOUNTS -> "Phase 18"
        else -> "a later phase"
    }
}

@Composable
fun AppNavGraph(
    container: AppContainer,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = AppDestinations.HOME,
    ) {
        composable(AppDestinations.HOME) {
            FoundationScreen(
                onNavigate = { route -> navController.navigate(route) },
            )
        }
        // Phase 6: the real mailbox. Other destinations keep their honest
        // placeholders until their phase builds them.
        composable(AppDestinations.MAIL) {
            val vm: MailViewModel = viewModel(factory = MailViewModelFactory(container))
            MailScreen(
                viewModel = vm,
                onOpenThread = { threadId, focusMessageId ->
                    navController.navigate(
                        AppDestinations.threadRoute(threadId, focusMessageId),
                    )
                },
                onOpenAccounts = { navController.navigate(AppDestinations.ACCOUNTS) },
            )
        }
        composable(
            route = AppDestinations.THREAD,
            arguments = listOf(
                navArgument("threadId") { type = NavType.StringType },
                navArgument("focusMessageId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            val threadId = entry.arguments?.getString("threadId") ?: return@composable
            val focusMessageId = entry.arguments?.getString("focusMessageId")
            val vm: ThreadViewModel =
                viewModel(factory = ThreadViewModelFactory(container, threadId))
            ThreadScreen(
                viewModel = vm,
                focusMessageId = focusMessageId,
                onBack = { navController.popBackStack() },
            )
        }
        AppDestinations.all
            .filter { it != AppDestinations.HOME && it != AppDestinations.MAIL }
            .forEach { route ->
                composable(route) {
                    PlaceholderScreen(
                        route = route,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
    }
}
