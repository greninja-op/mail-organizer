package com.greninjaop.mailorganizer.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import com.greninjaop.mailorganizer.core.search.SearchHighlight
import com.greninjaop.mailorganizer.core.search.SearchResult
import com.greninjaop.mailorganizer.ui.mail.MailFormatting
import com.greninjaop.mailorganizer.ui.theme.MoSpacing

/**
 * Search result rows (Phase 10).
 *
 * Highlighting operates on safe display text only (never raw HTML —
 * phase §45). The highlight color comes from the theme container so it
 * works in dark mode (phase §65).
 */
@Composable
fun HighlightedText(
    text: String,
    terms: List<String>,
    phrases: List<String>,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
) {
    val highlight = MaterialTheme.colorScheme.tertiaryContainer
    val annotated = remember(text, terms, phrases, highlight) {
        buildAnnotatedString {
            append(text)
            for (span in SearchHighlight.findSpans(text, terms, phrases)) {
                addStyle(SpanStyle(background = highlight), span.start, span.end)
            }
        }
    }
    Text(
        text = annotated,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** One message hit: sender, highlighted subject/snippet, timestamp. */
@Composable
fun SearchMessageRow(
    result: SearchResult.Message,
    terms: List<String>,
    phrases: List<String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val record = result.record
    val timeText = remember(record.timestampEpochMs) {
        MailFormatting.relativeTime(record.timestampEpochMs, System.currentTimeMillis())
    }
    ListItem(
        headlineContent = {
            HighlightedText(
                text = MailFormatting.senderDisplay(record.fromName, record.fromAddress),
                terms = terms,
                phrases = phrases,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
            )
        },
        supportingContent = {
            Column {
                HighlightedText(
                    text = MailFormatting.subjectDisplay(record.subject),
                    terms = terms,
                    phrases = phrases,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                )
                HighlightedText(
                    text = result.snippet,
                    terms = terms,
                    phrases = phrases,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        },
        trailingContent = {
            Text(
                text = timeText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
            ),
    )
}

/** One thread hit: subject, match count, newest matched preview. */
@Composable
fun SearchThreadRow(
    result: SearchResult.Thread,
    terms: List<String>,
    phrases: List<String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val preview = result.preview
    ListItem(
        headlineContent = {
            HighlightedText(
                text = MailFormatting.subjectDisplay(result.thread.subject),
                terms = terms,
                phrases = phrases,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
            )
        },
        supportingContent = {
            Text(
                text = if (result.matchedMessageCount == 1) {
                    "1 matching message · ${MailFormatting.senderDisplay(preview.fromName, preview.fromAddress)}"
                } else {
                    "${result.matchedMessageCount} matching messages · ${MailFormatting.senderDisplay(preview.fromName, preview.fromAddress)}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
}

/**
 * Sender/company suggestion chips (phase §8): tapping one applies it as a
 * search filter. These preserve the result-type distinction without
 * building a universal search engine.
 */
@Composable
fun PeopleCompanySuggestions(
    senders: List<com.greninjaop.mailorganizer.data.local.SenderRecord>,
    companies: List<com.greninjaop.mailorganizer.data.local.CompanyRecord>,
    onSenderClick: (com.greninjaop.mailorganizer.data.local.SenderRecord) -> Unit,
    onCompanyClick: (com.greninjaop.mailorganizer.data.local.CompanyRecord) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (senders.isEmpty() && companies.isEmpty()) return
    Column(modifier = modifier.padding(horizontal = MoSpacing.md)) {
        Spacer(Modifier.height(MoSpacing.xs))
        for (sender in senders) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { onSenderClick(sender) })
                    .padding(vertical = MoSpacing.xs),
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(MoSpacing.sm))
                Text(
                    text = sender.displayName?.takeIf { it.isNotBlank() }
                        ?: sender.emailAddress,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        for (company in companies) {
            // No company glyph in material-icons-core: honest text row
            // (editor rule — never invent glyphs).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { onCompanyClick(company) })
                    .padding(vertical = MoSpacing.xs),
            ) {
                Spacer(Modifier.width(MoSpacing.md))
                Column {
                    Text(
                        text = company.userOverrideName ?: company.canonicalName,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "Company",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(MoSpacing.xs))
    }
}
