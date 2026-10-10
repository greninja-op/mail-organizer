package com.greninjaop.mailorganizer.ui.mail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import com.greninjaop.mailorganizer.core.temporal.ExtractedTemporal
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.greninjaop.mailorganizer.data.local.ClassificationRecord
import com.greninjaop.mailorganizer.data.local.PriorityRecord
import com.greninjaop.mailorganizer.ui.theme.MoSpacing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Expanded message detail (Phase 6, phase §20–21, §27–30).
 *
 * - Sender / recipients via progressive "Show details" disclosure (§21).
 * - Body: sanitized HTML rendered by [SafeHtmlText], plain-text fallback,
 *   safe empty state (§29). Bodies over [LARGE_BODY_CHARS] fall back to
 *   plain text so one enormous message can't stall the UI (§31).
 * - Attachment *metadata* only — never downloaded automatically (§27).
 * - Missing data uses safe fallbacks (§28); raw MIME headers are never
 *   shown (§20).
 */
@Composable
fun MessageCard(
    message: MessageItem,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
    /** Phase 7: message-level classification; null when not classified yet. */
    classification: ClassificationRecord? = null,
    /** Phase 9: message-level priority; null when not prioritized yet. */
    priority: PriorityRecord? = null,
    /**
     * Phase 12: correction entry point. When non-null, a "Correct" affordance
     * is shown next to the category/priority chips (§4).
     */
    onCorrect: (() -> Unit)? = null,
    /** Phase 13: extracted meetings/deadlines; empty when none extracted. */
    temporalItems: List<ExtractedTemporal> = emptyList(),
) {
    Column(modifier = modifier.fillMaxWidth()) {
        MessageHeader(
            message = message,
            expanded = expanded,
            onToggleExpanded = onToggleExpanded,
        )
        if (expanded) {
            MessageBody(
                message = message,
                classification = classification,
                priority = priority,
                onCorrect = onCorrect,
                temporalItems = temporalItems,
                onOpenLink = onOpenLink,
                modifier = Modifier.padding(
                    start = MoSpacing.md,
                    end = MoSpacing.md,
                    bottom = MoSpacing.md,
                ),
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun MessageHeader(
    message: MessageItem,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val now = System.currentTimeMillis()
    val timeText = remember(message.timestampEpochMs, now / 60_000) {
        MailFormatting.relativeTime(message.timestampEpochMs, now)
    }
    val sender = remember(message.fromName, message.fromAddress) {
        MailFormatting.senderDisplay(message.fromName, message.fromAddress)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MoSpacing.huge)
            .clickable(onClick = onToggleExpanded)
            .padding(horizontal = MoSpacing.md, vertical = MoSpacing.sm)
            .semantics {
                contentDescription =
                    "$sender, ${MailFormatting.subjectDisplay(message.subject)}, " +
                        "${if (expanded) "expanded" else "collapsed"}"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = sender,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = if (message.unread) FontWeight.Bold else FontWeight.Normal,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (timeText.isNotBlank()) {
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (message.attachments.isNotEmpty()) {
                    Spacer(Modifier.width(MoSpacing.xs))
                    // No paperclip in the bundled core icon set: an exact count
                    // is more informative than an icon anyway.
                    val count = message.attachments.size
                    Text(
                        text = if (count == 1) "1 file" else "$count files",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (!expanded) {
                Text(
                    text = message.snippet.ifBlank { "—" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = onToggleExpanded) {
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse message" else "Expand message",
            )
        }
    }
}

@Composable
private fun MessageBody(
    message: MessageItem,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
    classification: ClassificationRecord? = null,
    /** Phase 9: message-level priority; null when not prioritized yet. */
    priority: PriorityRecord? = null,
    /** Phase 12: correction entry point (§4). */
    onCorrect: (() -> Unit)? = null,
    /** Phase 13: extracted meetings/deadlines. */
    temporalItems: List<ExtractedTemporal> = emptyList(),
) {
    var showDetails by remember { mutableStateOf(false) }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MoSpacing.sm),
    ) {
        // Phase 7: category chip with explainable "why" (§52, §55).
        if (classification != null) {
            CategoryChipWithExplanation(classification = classification)
        }
        // Phase 9: priority with explainable "why" — same discipline as
        // classification: the reason was recorded at compute time, so the
        // UI never needs raw email content to explain it.
        if (priority != null) {
            PriorityWithExplanation(priority = priority)
        }
        // Phase 13: deadlines & meetings with explainable "why" — same
        // discipline: explanations were recorded at extraction time.
        if (temporalItems.isNotEmpty()) {
            TemporalSection(items = temporalItems, onOpenLink = onOpenLink)
        }
        // Phase 12: "Correct" affordance next to the chips, plus the
        // "Set by you" marker comes from the records' overridden flag (§7).
        if (onCorrect != null && (classification != null || priority != null)) {
            Text(
                text = "Correct",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(onClick = onCorrect)
                    .padding(vertical = MoSpacing.xs)
                    .semantics { contentDescription = "Correct category or priority" },
            )
        }
        // Progressive disclosure for full headers (§21).
        Text(
            text = if (showDetails) "Hide details" else "Show details",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable { showDetails = !showDetails }
                .padding(vertical = MoSpacing.xs)
                .semantics {
                    contentDescription =
                        if (showDetails) "Hide email details" else "Show email details"
                },
        )
        if (showDetails) {
            HeaderDetails(message)
        }

        val html = message.bodyHtml
        val text = message.bodyText
        when {
            !html.isNullOrBlank() && html.length <= LARGE_BODY_CHARS -> {
                val blocks = remember(html) { SafeHtmlRenderer.render(html) }
                if (blocks.isNotEmpty()) {
                    SafeHtmlText(blocks = blocks, onOpenLink = onOpenLink)
                } else {
                    EmptyBodyText()
                }
            }
            !html.isNullOrBlank() -> {
                // Enormous body: plain-text fallback keeps the UI responsive.
                val fallbackText = remember(html) {
                    com.greninjaop.mailorganizer.core.email.HtmlSanitizer.htmlToText(html)
                }
                Text(
                    text = fallbackText,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            !text.isNullOrBlank() -> {
                Text(text = text, style = MaterialTheme.typography.bodyLarge)
            }
            else -> EmptyBodyText()
        }

        if (message.attachments.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(MoSpacing.xs)) {
                message.attachments.forEach { attachment ->
                    AttachmentRow(
                        filename = attachment.filename,
                        mimeType = attachment.mimeType,
                        sizeBytes = attachment.sizeBytes,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyBodyText() {
    Text(
        text = "No content available",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun HeaderDetails(message: MessageItem) {
    val fullDate = remember(message.timestampEpochMs) {
        if (message.timestampEpochMs <= 0L) {
            "Unknown date"
        } else {
            DateTimeFormatter.ofPattern("EEE, MMM d, yyyy h:mm a", Locale.US)
                .format(
                    Instant.ofEpochMilli(message.timestampEpochMs)
                        .atZone(ZoneId.systemDefault()),
                )
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(MoSpacing.xxs)) {
        DetailRow(label = "From", value = detailAddress(message.fromName, message.fromAddress))
        if (message.toAddresses.isNotEmpty()) {
            DetailRow(label = "To", value = message.toAddresses.joinToString(", "))
        }
        if (message.ccAddresses.isNotEmpty()) {
            DetailRow(label = "Cc", value = message.ccAddresses.joinToString(", "))
        }
        DetailRow(label = "Date", value = fullDate)
    }
}

private fun detailAddress(name: String?, address: String): String {
    val n = name?.trim()?.takeIf { it.isNotEmpty() }
    val a = address.trim()
    return when {
        n != null && a.isNotEmpty() -> "$n <$a>"
        a.isNotEmpty() -> a
        else -> "Unknown sender"
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(MoSpacing.giant),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AttachmentRow(
    filename: String?,
    mimeType: String,
    sizeBytes: Long,
) {
    val size = remember(sizeBytes) { MailFormatting.formatBytes(sizeBytes) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics {
            contentDescription = "Attachment: ${filename ?: "unnamed"}, " +
                "$mimeType${if (size.isNotBlank()) ", $size" else ""}"
        },
    ) {
        // Text-only row: the bundled core icon set has no paperclip, and the
        // filename + size + type already identify the attachment.
        Column {
            Text(
                text = filename?.takeIf { it.isNotBlank() } ?: "Unnamed attachment",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val meta = listOf(
                mimeType.substringAfter('/').uppercase(Locale.US).takeIf { it.isNotBlank() },
                size.takeIf { it.isNotBlank() },
            ).filterNotNull().joinToString(" · ")
            if (meta.isNotBlank()) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private const val LARGE_BODY_CHARS = 500_000
