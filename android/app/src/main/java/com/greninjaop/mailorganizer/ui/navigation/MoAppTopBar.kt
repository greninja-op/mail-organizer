package com.greninjaop.mailorganizer.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.ui.mail.AccountAvatar

/**
 * Shared top app bar for the Phase 11 primary destinations (phase §7,
 * §34–§36): title, a prominent search entry point, the account identity
 * (tapping it opens Accounts), and an overflow menu for the secondary
 * destinations (Integrations, Settings, Privacy, Accounts).
 *
 * Search is always one tap away (§34); the account avatar always shows
 * which account's data is on screen (§36).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoAppTopBar(
    title: String,
    account: AccountRecord?,
    onOpenSearch: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenSecondary: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text(title) },
        modifier = modifier,
        actions = {
            IconButton(onClick = onOpenSearch) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search mail",
                )
            }
            if (account != null) {
                IconButton(onClick = onOpenAccounts) {
                    AccountAvatar(
                        account = account,
                        modifier = Modifier.clip(CircleShape).clickable(
                            onClick = onOpenAccounts,
                        ),
                    )
                }
            }
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "More options",
                )
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
            ) {
                AppDestinations.secondary
                    .filter { it != AppDestinations.SEARCH }
                    .forEach { route ->
                        DropdownMenuItem(
                            text = { Text(AppDestinations.titleFor(route)) },
                            onClick = {
                                menuOpen = false
                                onOpenSecondary(route)
                            },
                        )
                    }
            }
        },
    )
}
