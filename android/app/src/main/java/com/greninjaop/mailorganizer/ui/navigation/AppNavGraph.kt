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
import com.greninjaop.mailorganizer.ui.actions.ActionsScreen
import com.greninjaop.mailorganizer.ui.actions.ActionsViewModel
import com.greninjaop.mailorganizer.ui.actions.ActionsViewModelFactory
import com.greninjaop.mailorganizer.ui.categories.CategoriesScreen
import com.greninjaop.mailorganizer.ui.categories.CategoriesViewModel
import com.greninjaop.mailorganizer.ui.categories.CategoriesViewModelFactory
import com.greninjaop.mailorganizer.ui.categories.CategoryDetailScreen
import com.greninjaop.mailorganizer.ui.companies.CompaniesScreen
import com.greninjaop.mailorganizer.ui.companies.CompaniesViewModel
import com.greninjaop.mailorganizer.ui.companies.CompaniesViewModelFactory
import com.greninjaop.mailorganizer.ui.companies.CompanyDetailScreen
import com.greninjaop.mailorganizer.ui.home.HomeScreen
import com.greninjaop.mailorganizer.ui.home.HomeViewModel
import com.greninjaop.mailorganizer.ui.home.HomeViewModelFactory
import com.greninjaop.mailorganizer.ui.mail.MailScreen
import com.greninjaop.mailorganizer.ui.mail.MailViewModel
import com.greninjaop.mailorganizer.ui.mail.MailViewModelFactory
import com.greninjaop.mailorganizer.ui.mail.ThreadScreen
import com.greninjaop.mailorganizer.ui.mail.ThreadViewModel
import com.greninjaop.mailorganizer.ui.mail.ThreadViewModelFactory
import com.greninjaop.mailorganizer.ui.rules.CorrectionViewModel
import com.greninjaop.mailorganizer.ui.rules.CorrectionViewModelFactory
import com.greninjaop.mailorganizer.ui.rules.RuleEditorScreen
import com.greninjaop.mailorganizer.ui.rules.RuleEditorViewModel
import com.greninjaop.mailorganizer.ui.rules.RuleEditorViewModelFactory
import com.greninjaop.mailorganizer.ui.rules.RulesScreen
import com.greninjaop.mailorganizer.ui.rules.RulesViewModel
import com.greninjaop.mailorganizer.ui.rules.RulesViewModelFactory
import com.greninjaop.mailorganizer.ui.search.SearchScreen
import com.greninjaop.mailorganizer.ui.search.SearchViewModel
import com.greninjaop.mailorganizer.ui.search.SearchViewModelFactory
import com.greninjaop.mailorganizer.ui.settings.AccountsScreen
import com.greninjaop.mailorganizer.ui.settings.AccountsViewModel
import com.greninjaop.mailorganizer.ui.settings.AccountsViewModelFactory
import com.greninjaop.mailorganizer.ui.settings.AppearanceViewModel
import com.greninjaop.mailorganizer.ui.settings.AppearanceViewModelFactory
import com.greninjaop.mailorganizer.ui.settings.IntegrationsScreen
import com.greninjaop.mailorganizer.ui.settings.PrivacyScreen
import com.greninjaop.mailorganizer.ui.settings.SettingsScreen

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

    /** Phase 12: rules list. */
    const val RULES = "rules"

    /**
     * Phase 12: rule editor. [ruleId] is null when creating a new rule.
     * Navigation arg validation (§34): a non-numeric ruleId is treated
     * as "create new" rather than crashing.
     */
    const val RULE_EDITOR = "rules/editor?ruleId={ruleId}"

    fun ruleEditorRoute(ruleId: Long? = null): String =
        if (ruleId == null) "rules/editor" else "rules/editor?ruleId=$ruleId"

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

    /**
     * Search route with an optional initial query (Phase 10): tapping the
     * mailbox search field's IME action opens the dedicated search screen
     * with the typed text (phase §10, §11).
     */
    const val SEARCH_ROUTE = "search?query={query}"

    fun searchRoute(query: String = ""): String =
        "search?query=" + android.net.Uri.encode(query)

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
        HOME -> "Phase 11"
        MAIL -> "Phase 6"
        CATEGORIES -> "Phase 11"
        COMPANIES -> "Phase 11"
        ACTIONS -> "Phase 11"
        SEARCH -> "Phase 10"
        INTEGRATIONS -> "Phase 11"
        SETTINGS -> "Phase 11"
        PRIVACY -> "Phase 11"
        ACCOUNTS -> "Phase 11"
        else -> "a later phase"
    }

    /** Category detail route: `category/{name}` (MailCategory.name). */
    const val CATEGORY_DETAIL = "category/{categoryName}"

    fun categoryRoute(category: com.greninjaop.mailorganizer.data.local.MailCategory): String =
        "category/${category.name}"

    /** Company detail route: `company/{companyId}`. */
    const val COMPANY_DETAIL = "company/{companyId}"

    fun companyRoute(companyId: String): String =
        "company/" + android.net.Uri.encode(companyId)
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
        // Phase 11: the real Home dashboard (replaces the Phase 1
        // foundation screen).
        composable(AppDestinations.HOME) {
            val vm: HomeViewModel = viewModel(factory = HomeViewModelFactory(container))
            HomeScreen(
                viewModel = vm,
                onNavigate = { route -> navController.navigate(route) },
                onOpenThread = { threadId ->
                    navController.navigate(AppDestinations.threadRoute(threadId))
                },
                onOpenCategory = { category ->
                    navController.navigate(AppDestinations.categoryRoute(category))
                },
                onOpenCompany = { companyId ->
                    navController.navigate(AppDestinations.companyRoute(companyId))
                },
                onOpenSearch = {
                    navController.navigate(AppDestinations.searchRoute())
                },
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
                // Phase 10: the mailbox search field's IME action opens the
                // dedicated search screen with the typed query.
                onOpenSearch = { query ->
                    navController.navigate(AppDestinations.searchRoute(query))
                },
                // Phase 11: primary-destination bottom bar.
                onNavigatePrimary = { route ->
                    navController.navigate(route) {
                        popUpTo(AppDestinations.HOME)
                        launchSingleTop = true
                    }
                },
            )
        }
        // Phase 11: category browsing + detail.
        composable(AppDestinations.CATEGORIES) {
            val vm: CategoriesViewModel =
                viewModel(factory = CategoriesViewModelFactory(container))
            CategoriesScreen(
                viewModel = vm,
                onNavigate = { route -> navController.navigate(route) },
                onOpenCategory = { category ->
                    navController.navigate(AppDestinations.categoryRoute(category))
                },
                onOpenThread = { threadId ->
                    navController.navigate(AppDestinations.threadRoute(threadId))
                },
                onOpenSearch = {
                    navController.navigate(AppDestinations.searchRoute())
                },
            )
        }
        composable(
            route = AppDestinations.CATEGORY_DETAIL,
            arguments = listOf(
                navArgument("categoryName") { type = NavType.StringType },
            ),
        ) { entry ->
            val categoryName = entry.arguments?.getString("categoryName")
            val category = runCatching {
                com.greninjaop.mailorganizer.data.local.MailCategory.valueOf(
                    categoryName ?: "",
                )
            }.getOrNull()
            if (category == null) {
                // Unknown category — never crash on a bad deep link.
                navController.popBackStack()
            } else {
                val vm: CategoriesViewModel =
                    viewModel(factory = CategoriesViewModelFactory(container))
                androidx.compose.runtime.LaunchedEffect(category) {
                    vm.selectCategory(category)
                }
                CategoryDetailScreen(
                    viewModel = vm,
                    category = category,
                    onOpenThread = { threadId ->
                        navController.navigate(AppDestinations.threadRoute(threadId))
                    },
                    onBack = { navController.popBackStack() },
                )
            }
        }
        // Phase 11: company browsing + detail.
        composable(AppDestinations.COMPANIES) {
            val vm: CompaniesViewModel =
                viewModel(factory = CompaniesViewModelFactory(container))
            CompaniesScreen(
                viewModel = vm,
                onNavigate = { route -> navController.navigate(route) },
                onOpenCompany = { companyId ->
                    navController.navigate(AppDestinations.companyRoute(companyId))
                },
                onOpenSearch = {
                    navController.navigate(AppDestinations.searchRoute())
                },
            )
        }
        composable(
            route = AppDestinations.COMPANY_DETAIL,
            arguments = listOf(
                navArgument("companyId") { type = NavType.StringType },
            ),
        ) { entry ->
            val companyId = entry.arguments?.getString("companyId")
            if (companyId.isNullOrEmpty()) {
                navController.popBackStack()
            } else {
                val vm: CompaniesViewModel =
                    viewModel(factory = CompaniesViewModelFactory(container))
                androidx.compose.runtime.LaunchedEffect(companyId) {
                    vm.selectCompany(companyId)
                }
                CompanyDetailScreen(
                    viewModel = vm,
                    onOpenThread = { threadId ->
                        navController.navigate(AppDestinations.threadRoute(threadId))
                    },
                    onBack = { navController.popBackStack() },
                )
            }
        }
        // Phase 11: actions destination (view-only).
        composable(AppDestinations.ACTIONS) {
            val vm: ActionsViewModel =
                viewModel(factory = ActionsViewModelFactory(container))
            ActionsScreen(
                viewModel = vm,
                onNavigate = { route -> navController.navigate(route) },
                onOpenThread = { threadId ->
                    navController.navigate(AppDestinations.threadRoute(threadId))
                },
                onOpenSearch = {
                    navController.navigate(AppDestinations.searchRoute())
                },
            )
        }
        // Phase 10: the real search experience (replaces the placeholder).
        composable(
            route = AppDestinations.SEARCH_ROUTE,
            arguments = listOf(
                navArgument("query") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = ""
                },
            ),
        ) { entry ->
            val initialQuery = entry.arguments?.getString("query").orEmpty()
            val vm: SearchViewModel = viewModel(
                factory = SearchViewModelFactory(container, initialQuery),
            )
            SearchScreen(
                viewModel = vm,
                onOpenThread = { threadId, focusMessageId ->
                    navController.navigate(
                        AppDestinations.threadRoute(threadId, focusMessageId),
                    )
                },
                onBack = { navController.popBackStack() },
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
            val correctionVm: CorrectionViewModel =
                viewModel(factory = CorrectionViewModelFactory(container))
            ThreadScreen(
                viewModel = vm,
                correctionViewModel = correctionVm,
                focusMessageId = focusMessageId,
                onBack = { navController.popBackStack() },
            )
        }
        // Phase 12: rules list + editor.
        composable(AppDestinations.RULES) {
            val vm: RulesViewModel =
                viewModel(factory = RulesViewModelFactory(container))
            RulesScreen(
                viewModel = vm,
                onAddRule = { navController.navigate(AppDestinations.ruleEditorRoute()) },
                onEditRule = { ruleId ->
                    navController.navigate(AppDestinations.ruleEditorRoute(ruleId))
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = AppDestinations.RULE_EDITOR,
            arguments = listOf(
                navArgument("ruleId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            // §34: non-numeric ruleId degrades to "create new", never a crash.
            val ruleId = entry.arguments?.getString("ruleId")?.toLongOrNull()
            val vm: RuleEditorViewModel =
                viewModel(factory = RuleEditorViewModelFactory(container, ruleId))
            RuleEditorScreen(
                viewModel = vm,
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        // Phase 11: secondary destinations — real, honest screens.
        composable(AppDestinations.INTEGRATIONS) {
            IntegrationsScreen(onBack = { navController.popBackStack() })
        }
        composable(AppDestinations.SETTINGS) {
            val vm: AppearanceViewModel =
                viewModel(factory = AppearanceViewModelFactory(container))
            SettingsScreen(
                appearanceViewModel = vm,
                onNavigate = { route -> navController.navigate(route) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(AppDestinations.PRIVACY) {
            PrivacyScreen(onBack = { navController.popBackStack() })
        }
        composable(AppDestinations.ACCOUNTS) {
            val vm: AccountsViewModel =
                viewModel(factory = AccountsViewModelFactory(container))
            AccountsScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
