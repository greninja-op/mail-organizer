package com.greninjaop.mailorganizer.ui.mail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import com.greninjaop.mailorganizer.ui.theme.MoSpacing

/**
 * Renders [BodyBlock]s produced by [SafeHtmlRenderer] (Phase 6).
 *
 * All color comes from the Material theme — email HTML carries no styles
 * past the sanitizer, so content stays readable in light and dark mode
 * (phase §44). Links are clickable only for http/https (re-validated here);
 * tapping any other scheme is impossible.
 */
@Composable
fun SafeHtmlText(
    blocks: List<BodyBlock>,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MoSpacing.xs),
    ) {
        blocks.forEach { block ->
            when (block) {
                is BodyBlock.Paragraph ->
                    LinkedRunsText(
                        runs = block.runs,
                        style = MaterialTheme.typography.bodyLarge,
                        onOpenLink = onOpenLink,
                    )
                is BodyBlock.Heading ->
                    LinkedRunsText(
                        runs = block.runs,
                        style = when (block.level) {
                            1 -> MaterialTheme.typography.titleLarge
                            2 -> MaterialTheme.typography.titleMedium
                            else -> MaterialTheme.typography.titleSmall
                        },
                        onOpenLink = onOpenLink,
                    )
                is BodyBlock.ListBlock ->
                    Column(verticalArrangement = Arrangement.spacedBy(MoSpacing.xxs)) {
                        block.items.forEachIndexed { index, runs ->
                            Row {
                                Text(
                                    text = if (block.ordered) "${index + 1}." else "•",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.width(MoSpacing.xs))
                                LinkedRunsText(
                                    runs = runs,
                                    style = MaterialTheme.typography.bodyLarge,
                                    onOpenLink = onOpenLink,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                is BodyBlock.Quote ->
                    Row {
                        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.width(MoSpacing.sm))
                        LinkedRunsText(
                            runs = block.runs,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            onOpenLink = onOpenLink,
                            modifier = Modifier.weight(1f),
                        )
                    }
                is BodyBlock.Code ->
                    Text(
                        text = block.text,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = MoSpacing.xs),
                    )
                is BodyBlock.ImagePlaceholder ->
                    // Text-only placeholder: the bundled core icon set has no
                    // image glyph, and remote images are never fetched (§25).
                    Text(
                        text = block.alt.ifBlank { "Image not loaded" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(vertical = MoSpacing.xs)
                            .semantics {
                                contentDescription =
                                    "Image not loaded${if (block.alt.isNotBlank()) ": ${block.alt}" else ""}"
                            },
                    )
            }
        }
    }
}

@Composable
private fun LinkedRunsText(
    runs: List<BodyRun>,
    style: TextStyle,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val annotated = remember(runs, linkColor) {
        buildAnnotatedString {
            runs.forEach { run ->
                val span = SpanStyle(
                    fontWeight = if (run.bold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (run.italic) FontStyle.Italic else FontStyle.Normal,
                    textDecoration = if (run.underline) TextDecoration.Underline else TextDecoration.None,
                )
                val url = run.link
                if (url != null && isOpenableUrl(url)) {
                    withLink(
                        LinkAnnotation.Clickable(
                            tag = "mo-link",
                            styles = TextLinkStyles(
                                style = SpanStyle(
                                    color = linkColor,
                                    textDecoration = TextDecoration.Underline,
                                ),
                            ),
                            linkInteractionListener = { onOpenLink(url) },
                        ),
                    ) {
                        append(run.text)
                    }
                    // Merge emphasis into the link span.
                    addStyle(span, length - run.text.length, length)
                } else {
                    withStyle(span) { append(run.text) }
                }
            }
        }
    }
    Text(text = annotated, style = style, modifier = modifier)
}

/** Defense in depth: only http/https ever open (phase §26). */
private fun isOpenableUrl(url: String): Boolean {
    val scheme = url.substringBefore(':').lowercase()
    return url.contains(':') &&
        (scheme == "http" || scheme == "https") &&
        url.none { it.isWhitespace() || it.code < 0x20 }
}
