package com.greninjaop.mailorganizer.ui.mail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.greninjaop.mailorganizer.core.temporal.ExtractedTemporal
import com.greninjaop.mailorganizer.core.temporal.TemporalItemType
import com.greninjaop.mailorganizer.core.temporal.TemporalStatus
import com.greninjaop.mailorganizer.ui.theme.MoSpacing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * "Deadlines & meetings" section for the expanded message view (Phase 13).
 *
 * Same explainability discipline as Phase 7/9: the "why" text was recorded
 * at extraction time, so the UI never needs raw email content to explain
 * an item. Meeting URLs open externally through the caller's
 * [onOpenLink] (re-validated there); they are never fetched or validated
 * here (phase §30).
 */
@Composable
fun TemporalSection(
    items: List<ExtractedTemporal>,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MoSpacing.xs),
    ) {
        Text(
            text = "Deadlines & meetings",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        items.forEach { item ->
            TemporalRow(item = item, onOpenLink = onOpenLink)
        }
    }
}

@Composable
private fun TemporalRow(
    item: ExtractedTemporal,
    onOpenLink: (String) -> Unit,
) {
    var showWhy by remember(item.startEpochMs, item.type) { mutableStateOf(false) }
    Column {
        Row(
            modifier = Modifier
                .clickable { showWhy = !showWhy }
                .padding(vertical = MoSpacing.xs)
                .semantics {
                    contentDescription =
                        "${item.title}, ${formatInstant(item)}, " +
                            "${item.status.name.lowercase()}. " +
                            if (showWhy) "Hide explanation." else "Show why."
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.Icon(
                imageVector = item.type.icon(),
                contentDescription = null,
                tint = if (item.status == TemporalStatus.PAST) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(MoSpacing.md),
            )
            Spacer(Modifier.width(MoSpacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = formatInstant(item),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                item.location?.let { location ->
                    Text(
                        text = "· $location",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            androidx.compose.material3.Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(MoSpacing.md),
            )
            Spacer(Modifier.width(MoSpacing.xs))
            Text(
                text = if (showWhy) "Hide why" else "Why?",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (showWhy) {
            Text(
                text = item.explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    start = MoSpacing.md,
                    top = MoSpacing.xs,
                    bottom = MoSpacing.xs,
                ),
            )
        }
        item.meetingUrl?.let { url ->
            Text(
                text = "Join meeting",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { onOpenLink(url) }
                    .padding(vertical = MoSpacing.xs)
                    .semantics { contentDescription = "Join meeting, opens externally" },
            )
        }
    }
}

/**
 * Deadlines get the warning glyph; everything else uses the info glyph.
 * Only material-icons-core glyphs are used (project rule since Phase 6).
 */
private fun TemporalItemType.icon(): ImageVector = when (this) {
    TemporalItemType.DEADLINE,
    TemporalItemType.SUBMISSION_DEADLINE,
    TemporalItemType.APPLICATION_DEADLINE,
    TemporalItemType.PAYMENT_DEADLINE,
    TemporalItemType.REGISTRATION_DEADLINE,
    TemporalItemType.REMINDER_DATE -> Icons.Filled.Warning
    else -> Icons.Filled.Info
}

/**
 * Formats the instant in the item's own timezone (never converts silently).
 * Date-only items show the date; others show date + time + zone.
 */
private fun formatInstant(item: ExtractedTemporal): String {
    val zone = runCatching { ZoneId.of(item.timezoneId) }
        .getOrDefault(ZoneId.of("UTC"))
    val zdt = Instant.ofEpochMilli(item.startEpochMs).atZone(zone)
    val datePart = zdt.format(
        DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US),
    )
    if (item.isDateOnly) return datePart
    val timePart = zdt.format(
        DateTimeFormatter.ofPattern("h:mm a", Locale.US),
    )
    val endPart = item.endEpochMs?.let { end ->
        val endZdt = Instant.ofEpochMilli(end).atZone(zone)
        " – " + endZdt.format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))
    }.orEmpty()
    return "$datePart, $timePart$endPart ${zone.id}"
}
