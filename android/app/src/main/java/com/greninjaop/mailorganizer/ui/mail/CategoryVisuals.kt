package com.greninjaop.mailorganizer.ui.mail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.greninjaop.mailorganizer.data.local.ClassificationRecord
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.ui.theme.MoDarkCategoryColors
import com.greninjaop.mailorganizer.ui.theme.MoLightCategoryColors
import com.greninjaop.mailorganizer.ui.theme.MoSpacing

/**
 * Category presentation (Phase 7 §52–55).
 *
 * Every category gets a text label (never color alone, §53), an accent
 * color from the centralized tokens (light + dark variants, §54), and a
 * content description for screen readers (§67).
 */
data class CategoryVisuals(
    val label: String,
    val accentLight: Color,
    val accentDark: Color,
    val contentDescription: String,
)

/** Maps a [MailCategory] to its visuals. */
fun MailCategory.visuals(): CategoryVisuals {
    val light = MoLightCategoryColors
    val dark = MoDarkCategoryColors
    return when (this) {
        MailCategory.ACTION_REQUIRED -> CategoryVisuals(
            "Action required", light.actionRequired, dark.actionRequired,
            "Category: action required",
        )
        MailCategory.IMPORTANT -> CategoryVisuals(
            "Important", light.important, dark.important,
            "Category: important",
        )
        MailCategory.CAREER -> CategoryVisuals(
            "Career", light.career, dark.career,
            "Category: career",
        )
        MailCategory.EDUCATION -> CategoryVisuals(
            "Education", light.education, dark.education,
            "Category: education",
        )
        MailCategory.RECEIPTS_ORDERS -> CategoryVisuals(
            "Receipts & orders", light.receiptsOrders, dark.receiptsOrders,
            "Category: receipts and orders",
        )
        MailCategory.SECURITY -> CategoryVisuals(
            "Security", light.security, dark.security,
            "Category: security",
        )
        MailCategory.NOTIFICATIONS -> CategoryVisuals(
            "Notifications", light.notifications, dark.notifications,
            "Category: notifications",
        )
        MailCategory.NEWSLETTERS -> CategoryVisuals(
            "Newsletters", light.newsletters, dark.newsletters,
            "Category: newsletters",
        )
        MailCategory.PROMOTIONS -> CategoryVisuals(
            "Promotions", light.promotions, dark.promotions,
            "Category: promotions",
        )
        MailCategory.LOW_VALUE -> CategoryVisuals(
            "Low value", light.lowValue, dark.lowValue,
            "Category: low value",
        )
        MailCategory.UNCLASSIFIED -> CategoryVisuals(
            "Unclassified", light.unclassified, dark.unclassified,
            "Category: unclassified",
        )
    }
}

/**
 * Small category chip: accent dot + text label. Color is never the only
 * indicator — the label text and content description carry the meaning.
 */
@Composable
fun CategoryChip(
    category: MailCategory,
    modifier: Modifier = Modifier,
) {
    val visuals = remember(category) { category.visuals() }
    val accent = if (isSystemInDarkTheme()) visuals.accentDark else visuals.accentLight
    Row(
        modifier = modifier.semantics {
            contentDescription = visuals.contentDescription
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(MoSpacing.sm)
                .clip(CircleShape)
                .background(accent),
        )
        Spacer(Modifier.width(MoSpacing.xs))
        Text(
            text = visuals.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Category chip with an expandable "Why this category?" explanation
 * (phase §55). The explanation comes from the persisted
 * [ClassificationRecord.explanation] — structured at classify time, so the
 * UI never needs raw email content to explain a decision (phase §62).
 */
@Composable
fun CategoryChipWithExplanation(
    classification: ClassificationRecord,
    modifier: Modifier = Modifier,
) {
    var showWhy by remember(classification.messageId) { mutableStateOf(false) }
    val visuals = remember(classification.category) { classification.category.visuals() }
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(MoSpacing.sm))
                .clickable { showWhy = !showWhy }
                .padding(vertical = MoSpacing.xs)
                .semantics {
                    contentDescription =
                        "${visuals.contentDescription}. " +
                            if (showWhy) "Hide explanation." else "Show why."
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryChip(category = classification.category)
            Spacer(Modifier.width(MoSpacing.xs))
            Icon(
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
            val explanation = classification.explanation
                ?: "No explanation was recorded for this classification."
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    start = MoSpacing.md,
                    top = MoSpacing.xs,
                    bottom = MoSpacing.xs,
                ),
            )
        }
    }
}
