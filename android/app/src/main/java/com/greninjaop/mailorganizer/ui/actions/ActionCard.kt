package com.greninjaop.mailorganizer.ui.actions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.greninjaop.mailorganizer.core.actions.ActionStatus
import com.greninjaop.mailorganizer.core.actions.ActionUrgency
import com.greninjaop.mailorganizer.core.actions.ExternalEffect
import com.greninjaop.mailorganizer.data.local.ActionItemRecord
import com.greninjaop.mailorganizer.data.local.ActionType
import com.greninjaop.mailorganizer.ui.mail.MailFormatting
import com.greninjaop.mailorganizer.ui.theme.MoSpacing

/**
 * Display model for one action card. Carries only display fields plus the
 * persisted explanation — the UI never needs raw email content to explain
 * a card (same discipline as Phases 7/9/13).
 */
data class ActionCardUi(
    val id: Long,
    val messageId: String,
    val threadId: String,
    val actionType: ActionType,
    val title: String,
    val description: String?,
    val urgency: ActionUrgency,
    val dueDateEpochMs: Long?,
    val senderName: String,
    val subject: String,
    val explanation: String?,
    val externalEffect: ExternalEffect,
    val status: ActionStatus,
) {
    /** Primary suggested-action label (phase §12). */
    val suggestedActionLabel: String
        get() = when {
            actionType == ActionType.MEETING && externalEffect == ExternalEffect.CALENDAR ->
                "Add to calendar"
            actionType == ActionType.REPLY_REQUIRED -> "Review email"
            else -> "Review"
        }

    /** Whether [Act] needs the confirmation dialog (phase §9, §31). */
    val requiresConfirmation: Boolean get() = externalEffect != ExternalEffect.NONE
}

fun ActionItemRecord.toCardUi(senderName: String, subject: String): ActionCardUi =
    ActionCardUi(
        id = id,
        messageId = messageId,
        threadId = threadId,
        actionType = actionType,
        title = title.ifBlank { defaultTitle(actionType) },
        description = description,
        urgency = urgency,
        dueDateEpochMs = dueDateEpochMs,
        senderName = senderName,
        subject = subject.ifBlank { "(no subject)" },
        explanation = explanation,
        externalEffect = externalEffect,
        status = status,
    )

private fun defaultTitle(type: ActionType): String = when (type) {
    ActionType.REPLY_REQUIRED -> "Reply needed"
    ActionType.MEETING -> "Meeting"
    ActionType.DEADLINE -> "Deadline"
    ActionType.PAYMENT -> "Payment due"
    ActionType.TRAVEL -> "Trip"
    ActionType.APPLICATION -> "Application"
    ActionType.REMINDER -> "Reminder"
    ActionType.OTHER -> "Needs review"
}

private fun ActionType.label(): String = when (this) {
    ActionType.REPLY_REQUIRED -> "Reply"
    ActionType.MEETING -> "Meeting"
    ActionType.DEADLINE -> "Deadline"
    ActionType.PAYMENT -> "Payment"
    ActionType.TRAVEL -> "Travel"
    ActionType.APPLICATION -> "Application"
    ActionType.REMINDER -> "Reminder"
    ActionType.OTHER -> "Review"
}

private fun ActionUrgency.label(): String = when (this) {
    ActionUrgency.CRITICAL -> "Critical"
    ActionUrgency.HIGH -> "High"
    ActionUrgency.NORMAL -> "Normal"
    ActionUrgency.LOW -> "Low"
}

/**
 * Reusable action card (Phase 14, phase §12–§15).
 *
 * Information hierarchy (§13): what needs attention → why → when → from
 * whom → what can I do → what happens on Act. Progressive disclosure for
 * the explanation. The source email is tappable; the card never duplicates
 * the email body (§14).
 */
@Composable
fun ActionCard(
    card: ActionCardUi,
    nowEpochMs: Long,
    onOpenSource: () -> Unit,
    onReview: () -> Unit,
    onAct: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showWhy by remember(card.id) { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "${card.actionType.label()}: ${card.title}. " +
                    "${card.urgency.label()} urgency."
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(MoSpacing.md)) {
            // 1. What needs attention?
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MoSpacing.xs),
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text(card.actionType.label()) },
                )
                AssistChip(
                    onClick = {},
                    label = { Text(card.urgency.label()) },
                )
                card.dueDateEpochMs?.let { due ->
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = MailFormatting.relativeTime(due, nowEpochMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(MoSpacing.xs))
            Text(
                text = card.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!card.description.isNullOrBlank()) {
                Spacer(Modifier.height(MoSpacing.xs))
                Text(
                    text = card.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // 2. Why? (progressive disclosure)
            if (!card.explanation.isNullOrBlank()) {
                TextButton(
                    onClick = { showWhy = !showWhy },
                    modifier = Modifier.semantics {
                        contentDescription =
                            if (showWhy) "Hide explanation." else "Show why."
                    },
                ) {
                    Text(if (showWhy) "Hide why" else "Why this?")
                }
                if (showWhy) {
                    Text(
                        text = card.explanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = MoSpacing.md),
                    )
                }
            }

            // 4. From whom + source email (tappable, §14).
            Spacer(Modifier.height(MoSpacing.xs))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenSource)
                    .semantics { contentDescription = "Open source email." }
                    .padding(vertical = MoSpacing.xs),
            ) {
                Text(
                    text = "From: ${card.senderName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Source: “${card.subject}”",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // 5–6. What can I do? What happens on Act?
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.semantics {
                        contentDescription = "Dismiss this suggestion."
                    },
                ) {
                    Text("Dismiss")
                }
                Spacer(Modifier.width(MoSpacing.xs))
                TextButton(
                    onClick = onReview,
                    modifier = Modifier.semantics {
                        contentDescription = "Review details."
                    },
                ) {
                    Text("Review")
                }
                Spacer(Modifier.width(MoSpacing.xs))
                androidx.compose.material3.FilledTonalButton(
                    onClick = onAct,
                    modifier = Modifier.semantics {
                        contentDescription = "${card.suggestedActionLabel}." +
                            if (card.requiresConfirmation) {
                                " Confirmation required."
                            } else {
                                ""
                            }
                    },
                ) {
                    Text(card.suggestedActionLabel)
                }
            }
            if (card.requiresConfirmation) {
                Text(
                    text = "Confirmation required — nothing happens automatically.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = MoSpacing.xs),
                )
            }
        }
    }
}
