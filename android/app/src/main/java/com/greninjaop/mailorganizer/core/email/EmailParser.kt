package com.greninjaop.mailorganizer.core.email

import java.nio.charset.Charset
import java.time.DateTimeException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Base64
import java.util.Locale

/**
 * Gmail message parser (Phase 5).
 *
 * Converts a raw Gmail API payload ([RawGmailMessage], mirroring
 * `users.messages.get` with `format=FULL`) into the canonical [EmailMessage].
 *
 * Design contract:
 * - **Total**: [parse] never throws, on any input. Malformed messages yield a
 *   degraded model (empty fields, `dateEpochMs = 0` for "unknown") — one bad
 *   message must never break a sync batch.
 * - **Untrusted input**: HTML is sanitized before it reaches the model;
 *   nothing here executes, fetches, or authorizes anything.
 * - **Deterministic**: same bytes in → same model out. No randomness, no
 *   clock reads (timestamps come from the message, never `now`).
 * - Pure Kotlin, no Android imports.
 */
object EmailParser {

    /** Maximum body bytes decoded from a single part (DoS guard). */
    const val MAX_PART_BYTES = 4 * 1024 * 1024

    /** Fallback snippet length when Gmail provides none. */
    const val SNIPPET_LENGTH = 160

    fun parse(raw: RawGmailMessage): EmailMessage {
        try {
            return parseUnsafe(raw)
        } catch (_: Exception) {
            // Fail closed but total: return the identity shell with no content.
            return EmailMessage(
                gmailId = raw.id,
                gmailThreadId = raw.threadId,
                from = null,
                labelIds = raw.labelIds,
                sizeBytes = raw.sizeEstimate,
            )
        }
    }

    private fun parseUnsafe(raw: RawGmailMessage): EmailMessage {
        val payload = raw.payload
        val headers = payload?.headers.orEmpty()

        fun header(name: String): String? =
            headers.firstOrNull { it.name.equals(name, ignoreCase = true) }?.value

        val from = header("From")?.let(::parseAddress)
        val dateEpochMs = parseDateHeader(header("Date"))
            ?: raw.internalDateMs
            ?: 0L

        val walk = if (payload != null) walkParts(payload) else WalkResult()
        val bodyText = walk.plainTexts.firstOrNull()
            ?: walk.htmlTexts.firstOrNull()?.let(HtmlSanitizer::htmlToText)
        val bodyHtml = walk.htmlTexts.firstOrNull()?.let(HtmlSanitizer::sanitize)

        val snippet = raw.snippet?.takeIf { it.isNotBlank() }
            ?: bodyText?.let { makeSnippet(it) }.orEmpty()

        return EmailMessage(
            gmailId = raw.id,
            gmailThreadId = raw.threadId,
            from = from,
            to = parseAddressList(header("To")),
            cc = parseAddressList(header("Cc")),
            bcc = parseAddressList(header("Bcc")),
            replyTo = parseAddressList(header("Reply-To")),
            subject = header("Subject")?.let(::decodeEncodedWords).orEmpty(),
            dateEpochMs = dateEpochMs,
            messageIdHeader = header("Message-ID")?.trim()?.takeIf { it.isNotEmpty() },
            inReplyTo = header("In-Reply-To")?.trim()?.takeIf { it.isNotEmpty() },
            references = header("References")
                ?.split(Regex("\\s+"))
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                .orEmpty(),
            snippet = snippet,
            bodyText = bodyText?.takeIf { it.isNotEmpty() },
            bodyHtml = bodyHtml?.takeIf { it.isNotEmpty() },
            attachments = walk.attachments,
            labelIds = raw.labelIds,
            sizeBytes = raw.sizeEstimate,
        )
    }

    // ------------------------------------------------------------------
    // MIME tree walk
    // ------------------------------------------------------------------

    private class WalkResult(
        val plainTexts: MutableList<String> = mutableListOf(),
        val htmlTexts: MutableList<String> = mutableListOf(),
        val attachments: MutableList<AttachmentMeta> = mutableListOf(),
    )

    private fun walkParts(part: RawGmailPart): WalkResult {
        val result = WalkResult()
        walkInto(part, result)
        return result
    }

    private fun walkInto(part: RawGmailPart, out: WalkResult) {
        val mime = part.mimeType.lowercase(Locale.ROOT)
        if (mime.startsWith("multipart/")) {
            // For multipart/alternative the children are renderings of the same
            // content — collect all, selection happens after the walk.
            // For multipart/mixed|related|digest, children are distinct parts.
            for (child in part.parts) walkInto(child, out)
            return
        }
        val body = part.body
        val filename = part.filename?.takeIf { it.isNotBlank() }
        val isAttachment = filename != null ||
            body?.attachmentId != null ||
            (!mime.startsWith("text/"))

        if (isAttachment) {
            // Attachments: metadata only — binaries are never fetched here.
            if (mime.startsWith("text/") && filename == null && body?.attachmentId == null) {
                // A text/* leaf without filename/attachmentId is body content,
                // not an attachment (falls through below).
            } else {
                out.attachments.add(
                    AttachmentMeta(
                        filename = filename,
                        mimeType = part.mimeType,
                        sizeBytes = body?.size ?: 0L,
                        attachmentId = body?.attachmentId,
                    ),
                )
                return
            }
        }

        if (mime == "text/plain" || mime == "text/html") {
            val text = decodeBody(part) ?: return
            if (mime == "text/plain") out.plainTexts.add(text) else out.htmlTexts.add(text)
        }
        // Other leaf types without attachment markers are ignored (not body,
        // not attachment — e.g. delivery-status notifications).
    }

    private fun decodeBody(part: RawGmailPart): String? {
        val data = part.body?.data ?: return null
        return try {
            val bytes = decodeBase64Url(data) ?: return null
            if (bytes.size > MAX_PART_BYTES) return null
            val charset = part.headers
                .firstOrNull { it.name.equals("Content-Type", ignoreCase = true) }
                ?.value
                ?.let(::extractCharset)
                ?: Charsets.UTF_8
            String(bytes, charset)
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeBase64Url(data: String): ByteArray? {
        return try {
            // Gmail uses unpadded base64url; the JDK decoder needs padding.
            val padded = data.trim().let { s ->
                s + "=".repeat((4 - s.length % 4) % 4)
            }
            Base64.getUrlDecoder().decode(padded)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun extractCharset(contentType: String): Charset {
        val match = Regex("charset\\s*=\\s*\"?([^\";\\s]+)\"?", RegexOption.IGNORE_CASE)
            .find(contentType) ?: return Charsets.UTF_8
        return try {
            Charset.forName(match.groupValues[1])
        } catch (_: Exception) {
            Charsets.UTF_8
        }
    }

    // ------------------------------------------------------------------
    // Headers: RFC 2047, addresses, dates
    // ------------------------------------------------------------------

    private val encodedWord = Regex("""=\?([^?\s]+)\?([bBqQ])\?([^?]*)\?=""")

    /**
     * Decodes RFC 2047 encoded-words (`=?charset?B|Q?text?=`). Unknown charsets
     * or malformed words are left as-is; never throws.
     */
    fun decodeEncodedWords(text: String): String {
        if (!text.contains("=?")) return text
        var out = text
        var guard = 0
        try {
            while (guard++ < 32) {
                val match = encodedWord.find(out) ?: break
                val decoded = decodeWord(
                    match.groupValues[1],
                    match.groupValues[2],
                    match.groupValues[3],
                ) ?: match.value
                out = out.replaceRange(match.range, decoded)
            }
        } catch (_: Exception) {
            // Return best effort.
        }
        return out
    }

    private fun decodeWord(charsetName: String, encoding: String, text: String): String? {
        return try {
            val charset = try {
                Charset.forName(charsetName)
            } catch (_: Exception) {
                return null
            }
            val bytes = when (encoding.uppercase(Locale.ROOT)) {
                "B" -> try {
                    Base64.getDecoder().decode(text.trim())
                } catch (_: IllegalArgumentException) {
                    return null
                }
                "Q" -> decodeQuotedPrintable(text)
                else -> return null
            }
            String(bytes, charset)
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeQuotedPrintable(text: String): ByteArray {
        val out = mutableListOf<Byte>()
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c == '_' -> {
                    out.add(' '.code.toByte())
                    i++
                }
                c == '=' && i + 2 < text.length -> {
                    val hex = text.substring(i + 1, i + 3)
                    val byte = hex.toIntOrNull(16)
                    if (byte != null) {
                        out.add(byte.toByte())
                        i += 3
                    } else {
                        out.add(c.code.toByte())
                        i++
                    }
                }
                else -> {
                    // Non-ASCII chars in Q-encoding are technically invalid;
                    // encode as UTF-8 bytes best-effort.
                    val bytes = c.toString().toByteArray(Charsets.UTF_8)
                    bytes.forEach { out.add(it) }
                    i++
                }
            }
        }
        return out.toByteArray()
    }

    /**
     * Parses one RFC 5322 mailbox (`"Name" <a@b.c>` / `Name <a@b.c>` /
     * `a@b.c`). Returns null when nothing address-like is present.
     */
    fun parseAddress(raw: String?): EmailAddress? {
        val text = raw?.trim() ?: return null
        if (text.isEmpty()) return null
        return try {
            val angle = Regex("""^\s*(?:"([^"]*)"|([^"<]*?))\s*<([^<>\s]+@[^<>\s]+)>\s*$""")
                .find(text)
            if (angle != null) {
                val name = (angle.groupValues[1].ifEmpty { angle.groupValues[2] })
                    .trim().trim('"')
                    .let(::decodeEncodedWords)
                    .takeIf { it.isNotEmpty() }
                EmailAddress(name = name, address = angle.groupValues[3].trim())
            } else {
                val addr = text.trim().trim('"', '\'', '<', '>')
                if (addr.contains("@")) {
                    EmailAddress(name = null, address = decodeEncodedWords(addr))
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Parses an address-list header. Splits on commas outside quotes/angles,
     * tolerates `Group: a@x, b@y;` syntax by unwrapping the group.
     */
    fun parseAddressList(header: String?): List<EmailAddress> {
        if (header.isNullOrBlank()) return emptyList()
        return try {
            var text = header.trim()
            // Unwrap RFC 5322 group syntax: "Group Name: a@x, b@y;"
            if (text.endsWith(";")) {
                val colon = text.indexOf(':')
                if (colon > 0 && !text.substring(0, colon).contains("@")) {
                    text = text.substring(colon + 1, text.length - 1)
                }
            }
            splitAddressTokens(text).mapNotNull(::parseAddress)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun splitAddressTokens(text: String): List<String> {
        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var angleDepth = 0
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c == '"' && (i == 0 || text[i - 1] != '\\') -> {
                    inQuotes = !inQuotes
                    current.append(c)
                }
                !inQuotes && c == '<' -> {
                    angleDepth++
                    current.append(c)
                }
                !inQuotes && c == '>' -> {
                    if (angleDepth > 0) angleDepth--
                    current.append(c)
                }
                !inQuotes && angleDepth == 0 && c == ',' -> {
                    tokens.add(current.toString())
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        tokens.add(current.toString())
        return tokens.map { it.trim() }.filter { it.isNotEmpty() }
    }

    // RFC 2822 date patterns (weekday stripped before matching; Locale.ENGLISH).
    private val datePatterns = listOf(
        "d MMM yyyy HH:mm:ss Z",
        "d MMM yyyy HH:mm Z",
        "d MMM yy HH:mm:ss Z",
        "d MMM yyyy HH:mm:ss z",
        "d MMM yyyy HH:mm Z",
    )

    /**
     * Parses an RFC 2822 `Date` header to epoch millis. Returns null when the
     * header is absent or unparseable — callers fall back to `internalDate`.
     */
    fun parseDateHeader(value: String?): Long? {
        val text = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return try {
            // Drop the weekday ("Wed, ") — it is frequently wrong and unused.
            val noWeekday = text.replace(Regex("^[A-Za-z]{3,9},\\s+"), "")
            // Normalize obsolete/odd zones: "-0000" → "+0000".
            val normalized = noWeekday.replace("-0000", "+0000")
            for (pattern in datePatterns) {
                try {
                    val formatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH)
                    return ZonedDateTime.parse(normalized, formatter)
                        .toInstant().toEpochMilli()
                } catch (_: DateTimeParseException) {
                    // Try next pattern.
                }
            }
            null
        } catch (_: DateTimeException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    // ------------------------------------------------------------------
    // Snippet
    // ------------------------------------------------------------------

    private fun makeSnippet(bodyText: String): String {
        val collapsed = bodyText.replace(Regex("\\s+"), " ").trim()
        if (collapsed.length <= SNIPPET_LENGTH) return collapsed
        // Cut at a word boundary when close to the limit.
        val cut = collapsed.lastIndexOf(' ', SNIPPET_LENGTH)
        return if (cut > SNIPPET_LENGTH - 30) collapsed.substring(0, cut) else collapsed.substring(
            0,
            SNIPPET_LENGTH,
        )
    }
}
