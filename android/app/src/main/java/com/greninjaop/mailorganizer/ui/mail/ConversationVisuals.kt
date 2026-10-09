package com.greninjaop.mailorganizer.ui.mail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.greninjaop.mailorganizer.core.conversation.ConversationAnalysisResult
import com.greninjaop.mailorganizer.core.conversation.ConversationState
import com.greninjaop.mailorganizer.core.conversation.ParticipantRole
import com.greninjaop.mailorganizer.ui.theme.MoSpacing

/**
 * Visual styling and colors for conversation state badges (Phase 21).
 */
data class ConversationStateVisuals(
    val label: String,
    val containerColor: Color,
    val contentColor: Color,
    val description: String,
)

@Composable
fun ConversationState.visuals(): ConversationStateVisuals? {
    return when (this) {
        ConversationState.AWAITING_USER_REPLY -> ConversationStateVisuals(
            label = "Awaiting your reply",
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            description = "Conversation appears to be waiting for your reply",
        )
        ConversationState.AWAITING_OTHER_PARTY -> ConversationStateVisuals(
            label = "Waiting for response",
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            description = "Waiting for response from other party",
        )
        ConversationState.RECENTLY_REPLIED -> ConversationStateVisuals(
            label = "Recently replied",
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            description = "You replied to this conversation recently",
        )
        ConversationState.STALE_CONVERSATION -> ConversationStateVisuals(
            label = "Stale (no reply)",
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.outline,
            description = "Conversation has had no activity for over 7 days",
        )
        ConversationState.RESOLVED -> ConversationStateVisuals(
            label = "Resolved",
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            description = "Conversation was resolved",
        )
        ConversationState.NO_ACTION, ConversationState.UNKNOWN -> null
    }
}

/**
 * Compact conversation state pill for thread headers and lists (Phase 21 §37).
 */
@Composable
fun ConversationStateBadge(
    state: ConversationState,
    modifier: Modifier = Modifier,
) {
    val visuals = state.visuals() ?: return
    Surface(
        color = visuals.containerColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.semantics { contentDescription = visuals.description },
    ) {
        Text(
            text = visuals.label,
            style = MaterialTheme.typography.labelSmall,
            color = visuals.contentColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = MoSpacing.sm, vertical = 2.dp),
        )
    }
}

/**
 * Explainable conversation intelligence card and timeline shown at top of ThreadScreen (Phase 21 §34, §36).
 */
@Composable
fun ConversationIntelligenceSection(
    result: ConversationAnalysisResult,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val visuals = result.state.visuals()

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(horizontal = MoSpacing.md, vertical = MoSpacing.xs),
    ) {
        Column(modifier = Modifier.padding(MoSpacing.md)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (visuals != null) {
                    ConversationStateBadge(state = result.state)
                    Spacer(Modifier.width(MoSpacing.sm))
                }
                Text(
                    text = result.explanation.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = if (expanded) "Hide details" else "Show details",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(MoSpacing.md),
                )
            }

            if (result.isFollowUpCandidate && result.followUpReason != null) {
                Spacer(Modifier.height(MoSpacing.xs))
                Text(
                    text = "Suggestion: ${result.followUpReason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Medium,
                )
            }

            if (expanded) {
                Spacer(Modifier.height(MoSpacing.sm))
                if (result.explanation.reasons.isNotEmpty()) {
                    Text(
                        text = "Why:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(2.dp))
                    for (reason in result.explanation.reasons) {
                        Text(
                            text = "• $reason",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = MoSpacing.xs, top = 2.dp),
                        )
                    }
                }

                // Compact timeline (§36)
                if (result.timeline.size > 1) {
                    Spacer(Modifier.height(MoSpacing.sm))
                    Text(
                        text = "Conversation timeline:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(MoSpacing.xs))
                    for (entry in result.timeline) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (entry.role) {
                                            ParticipantRole.USER -> MaterialTheme.colorScheme.primary
                                            ParticipantRole.OTHER_PARTY -> MaterialTheme.colorScheme.secondary
                                            ParticipantRole.AUTOMATED_NO_REPLY -> MaterialTheme.colorScheme.outline
                                        }
                                    )
                            )
                            Spacer(Modifier.width(MoSpacing.sm))
                            Text(
                                text = if (entry.role == ParticipantRole.USER) "You" else entry.senderName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (entry.isLatest) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f),
                            )
                            val now = System.currentTimeMillis()
                            Text(
                                text = MailFormatting.relativeTime(entry.timestampEpochMs, now),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
