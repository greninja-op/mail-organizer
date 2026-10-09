package com.greninjaop.mailorganizer.ui.mail

/**
 * Render-model for safely displaying email bodies (Phase 6).
 *
 * The pipeline is:
 * ```
 * wire HTML → [com.greninjaop.mailorganizer.core.email.HtmlSanitizer] (strips
 *               scripts, event handlers, dangerous URLs, styles)
 *           → [SafeHtmlRenderer] (this package: parses the *sanitized* HTML
 *               into [BodyBlock]s — no WebView, so JavaScript execution and
 *               remote resource loading are impossible by construction)
 *           → Compose [androidx.compose.material3.Text] rendering
 * ```
 *
 * Only `http`/`https` links survive as clickable ([BodyRun.link]); images
 * become [BodyBlock.ImagePlaceholder] (never fetched — phase §24); inline
 * styles never survive the sanitizer, so email HTML cannot force colors and
 * dark mode stays readable (phase §44).
 *
 * Pure Kotlin — fully unit-testable on the JVM.
 */

/** One styled run of text inside a block. */
data class BodyRun(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    /** Clickable URL, or null. Only ever http/https (validated at parse). */
    val link: String? = null,
)

sealed interface BodyBlock {
    data class Paragraph(val runs: List<BodyRun>) : BodyBlock
    data class Heading(val level: Int, val runs: List<BodyRun>) : BodyBlock
    data class ListBlock(
        val ordered: Boolean,
        val items: List<List<BodyRun>>,
    ) : BodyBlock
    data class Quote(val runs: List<BodyRun>) : BodyBlock
    data class Code(val text: String) : BodyBlock
    /** Remote images are never loaded (phase §24): shown as a labeled box. */
    data class ImagePlaceholder(val alt: String) : BodyBlock
}
