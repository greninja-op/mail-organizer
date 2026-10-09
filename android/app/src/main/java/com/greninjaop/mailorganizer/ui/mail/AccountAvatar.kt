package com.greninjaop.mailorganizer.ui.mail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.ui.theme.MoSpacing
import kotlin.math.abs

/**
 * Compact circular account indicator (Phase 6, design.md §All Inbox account
 * identity).
 *
 * Represents the *receiving* Gmail account — never the sender/company avatar.
 * The container color is derived deterministically from the account id and
 * always comes from the theme's container palette, so it stays readable in
 * light and dark mode.
 */
@Composable
fun AccountAvatar(
    account: AccountRecord,
    modifier: Modifier = Modifier,
    size: Dp = MoSpacing.xl,
) {
    val containers = listOf(
        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer,
        MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer,
    )
    val index = remember(account.accountId) {
        abs(account.accountId.hashCode()) % containers.size
    }
    val initial = remember(account.displayName, account.emailAddress) {
        MailFormatting.avatarInitial(account.displayName, account.emailAddress)
    }
    val (container, onContainer) = containers[index]
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(container)
            .semantics { contentDescription = "Account: ${account.emailAddress}" },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.labelMedium,
            color = onContainer,
        )
    }
}
