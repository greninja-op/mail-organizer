package com.greninjaop.mailorganizer.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Bottom navigation bar for the five primary destinations (Phase 11,
 * phase §7): Home, Mail, Categories, Companies, Actions.
 *
 * Icon note: `material-icons-core` (the only icon artifact declared) has
 * no `Business` glyph, so Companies uses `AccountBox` (organization card
 * metaphor) — the text label and content description carry the meaning,
 * never the icon alone.
 */
@Composable
fun MoNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        PrimaryDestination.entries.forEach { dest ->
            val selected = currentRoute == dest.route
            NavigationBarItem(
                selected = selected,
                onClick = { if (!selected) onNavigate(dest.route) },
                icon = {
                    Icon(
                        imageVector = dest.icon,
                        contentDescription = null,
                    )
                },
                label = { Text(dest.label) },
                modifier = Modifier.semantics {
                    contentDescription = "${dest.label} tab"
                },
            )
        }
    }
}

/** Primary destinations in display order (phase §7). */
enum class PrimaryDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    HOME(AppDestinations.HOME, "Home", Icons.Filled.Home),
    MAIL(AppDestinations.MAIL, "Mail", Icons.Filled.Email),
    CATEGORIES(AppDestinations.CATEGORIES, "Categories", Icons.Filled.List),
    COMPANIES(AppDestinations.COMPANIES, "Companies", Icons.Filled.AccountBox),
    ACTIONS(AppDestinations.ACTIONS, "Actions", Icons.Filled.CheckCircle),
}
