package com.greninjaop.mailorganizer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.greninjaop.mailorganizer.ui.foundation.FoundationScreen
import com.greninjaop.mailorganizer.ui.foundation.PlaceholderScreen

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
        AppDestinations.all
            .filter { it != AppDestinations.HOME }
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
