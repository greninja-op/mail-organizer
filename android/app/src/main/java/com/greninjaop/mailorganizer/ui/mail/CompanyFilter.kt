package com.greninjaop.mailorganizer.ui.mail

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.greninjaop.mailorganizer.ui.theme.MoSpacing

/**
 * Company filter UI models (Phase 8).
 *
 * Company filtering lives INSIDE the selected category (requirements.md,
 * design.md §"Category company filtering") — it is a filter, never a
 * navigation destination. Selecting a company narrows the current
 * category's list; the category context stays visible.
 */



/** Models moved to MailUiState.kt (Compose-free) so JVM unit tests can use them. */

/**
 * Horizontally scrolling company filter row (Phase 8).
 *
 * Entries arrive pinned-first (DAO ordering); the ViewModel re-sorts
 * pinned-first, then by count. Tapping a chip selects/deselects the
 * company filter. The Pin/Unpin text affordance toggles pinning —
 * pinning only reorders this list (requirements.md); it never moves
 * mail, never stars messages, never creates navigation.
 *
 * No pin glyph exists in the declared `material-icons-core` artifact, so
 * pinning uses an honest text affordance (editor rule: closest declared
 * icon or honest text indicator, never an invented glyph).
 */
@Composable
fun CompanyFilterRow(
    uiState: CompanyFilterUiState,
    onSelect: (companyId: String?) -> Unit,
    onTogglePin: (companyId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!uiState.visible || uiState.entries.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Companies in this mailbox",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                start = MoSpacing.md,
                end = MoSpacing.md,
                top = MoSpacing.sm,
            ),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(
                    start = MoSpacing.md,
                    end = MoSpacing.md,
                    top = MoSpacing.xs,
                    bottom = MoSpacing.sm,
                ),
            horizontalArrangement = Arrangement.spacedBy(MoSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (entry in uiState.entries) {
                CompanyFilterChip(
                    entry = entry,
                    selected = entry.companyId == uiState.selectedCompanyId,
                    onSelect = onSelect,
                    onTogglePin = onTogglePin,
                )
            }
        }
    }
}

@Composable
private fun CompanyFilterChip(
    entry: CompanyFilterEntry,
    selected: Boolean,
    onSelect: (companyId: String?) -> Unit,
    onTogglePin: (companyId: String) -> Unit,
) {
    val selectedDescription =
        "${entry.displayName}, ${entry.messageCount} messages" +
            (if (entry.pinned) ", pinned" else "") +
            (if (selected) ", selected filter" else "")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics {
            contentDescription = selectedDescription
        },
    ) {
        Surface(
            selected = selected,
            onClick = { onSelect(if (selected) null else entry.companyId) },
            shape = RoundedCornerShape(MoSpacing.md),
            color = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            tonalElevation = 1.dp,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(
                    horizontal = MoSpacing.sm,
                    vertical = MoSpacing.xs,
                ),
            ) {
                if (entry.pinned) {
                    Text(
                        text = "Pinned · ",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = entry.displayName,
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.width(MoSpacing.xs))
                Text(
                    text = entry.messageCount.toString(),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        TextButton(
            onClick = { onTogglePin(entry.companyId) },
            modifier = Modifier.semantics {
                contentDescription = if (entry.pinned) {
                    "Unpin ${entry.displayName}"
                } else {
                    "Pin ${entry.displayName} to the top of this list"
                }
            },
        ) {
            Text(
                text = if (entry.pinned) "Unpin" else "Pin",
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}
