package com.greninjaop.mailorganizer.ui.mail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.Priority
import com.greninjaop.mailorganizer.ui.theme.MoSpacing

/**
 * One row in the mail list (Phase 6, phase §9–11).
 *
 * A flat row — not a card (design.md: no unnecessary shadows). Hierarchy:
 * sender / subject / preview / timestamp, with a clear unread distinction
 * (bold sender+subject). Long text is ellipsis-bounded so one rogue email
 * can't break the layout (§11). Minimum 48dp touch target (§42).
 *
 * The star is a *display* of synced starred state only — read-only phase,
 * no toggle (§6).
 */
@Composable
fun ThreadRow(
    item: ThreadItem,
    account: AccountRecord?,
    showAccountIndicator: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val now = System.currentTimeMillis()
    val timeText = remember(item.timestampEpochMs, now / 60_000) {
        MailFormatting.relativeTime(item.timestampEpochMs, now)
    }
    val description = remember(item) {
        buildString {
            append(if (item.unread) "Unread email" else "Email")
            append(" from ${item.senderDisplay}")
            append(", subject: ${MailFormatting.subjectDisplay(item.subject)}")
            if (item.snippet.isNotBlank()) append(", ${item.snippet}")
            if (timeText.isNotBlank()) append(", $timeText")
            if (item.messageCount > 1) append(", ${item.messageCount} messages")
            item.priority?.visuals()?.let { append(", ${it.contentDescription}") }
        }
    }
    MailRowLayout(
        sender = item.senderDisplay,
        subject = MailFormatting.subjectDisplay(item.subject),
        snippet = item.snippet,
        timeText = timeText,
        unread = item.unread,
        showStar = item.anyStarred,
        showAttachment = item.hasAttachment,
        messageCount = item.messageCount,
        priority = item.priority,
        account = account,
        showAccountIndicator = showAccountIndicator,
        onClick = onClick,
        modifier = modifier.semantics { contentDescription = description },
    )
}

/** Row variant for the Starred destination (message-centric). */
@Composable
fun StarredMessageRow(
    item: MessageItem,
    account: AccountRecord?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val now = System.currentTimeMillis()
    val timeText = remember(item.timestampEpochMs, now / 60_000) {
        MailFormatting.relativeTime(item.timestampEpochMs, now)
    }
    val description = remember(item) {
        "Starred email from ${
            MailFormatting.senderDisplay(item.fromName, item.fromAddress)
        }, subject: ${MailFormatting.subjectDisplay(item.subject)}, $timeText"
    }
    MailRowLayout(
        sender = MailFormatting.senderDisplay(item.fromName, item.fromAddress),
        subject = MailFormatting.subjectDisplay(item.subject),
        snippet = item.snippet,
        timeText = timeText,
        unread = item.unread,
        showStar = true,
        showAttachment = item.attachments.isNotEmpty(),
        messageCount = 1,
        priority = item.priority,
        account = account,
        showAccountIndicator = false,
        onClick = onClick,
        modifier = modifier.semantics { contentDescription = description },
    )
}

@Composable
private fun MailRowLayout(
    sender: String,
    subject: String,
    snippet: String,
    timeText: String,
    unread: Boolean,
    showStar: Boolean,
    showAttachment: Boolean,
    messageCount: Int,
    priority: Priority?,
    account: AccountRecord?,
    showAccountIndicator: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val emphasis = if (unread) FontWeight.Bold else FontWeight.Normal
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MoSpacing.huge)
            .clickable(onClick = onClick)
            .padding(horizontal = MoSpacing.md, vertical = MoSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showAccountIndicator && account != null) {
            AccountAvatar(account = account, size = MoSpacing.xxl)
            Spacer(Modifier.width(MoSpacing.sm))
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = sender,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = emphasis),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (timeText.isNotBlank()) {
                    Spacer(Modifier.width(MoSpacing.xs))
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (unread) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = subject,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = emphasis),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (showStar) {
                    Spacer(Modifier.width(MoSpacing.xs))
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Starred",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(MoSpacing.md),
                    )
                }
                if (showAttachment) {
                    Spacer(Modifier.width(MoSpacing.xs))
                    // No paperclip in the bundled core icon set: a compact text chip
                    // is honest and needs no invented icon.
                    Text(
                        text = "Files",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (priority != null && priority.visuals() != null) {
                    Spacer(Modifier.width(MoSpacing.xs))
                    PriorityBadge(priority = priority)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = snippet.ifBlank { "—" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (messageCount > 1) {
                    Spacer(Modifier.width(MoSpacing.xs))
                    Text(
                        text = "$messageCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.semantics {
                            contentDescription = "$messageCount messages in thread"
                        },
                    )
                }
            }
        }
    }
}
