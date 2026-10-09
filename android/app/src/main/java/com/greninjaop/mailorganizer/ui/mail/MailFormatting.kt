package com.greninjaop.mailorganizer.ui.mail

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Pure-Kotlin display formatting for the mail UI (Phase 6).
 *
 * No Android imports: fully unit-testable on the JVM. All functions are
 * total — malformed input degrades to a safe fallback, never an exception.
 */
object MailFormatting {

    private val monthDay: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMM d", Locale.US)
    private val monthDayYear: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)

    /**
     * Gmail-style relative timestamp.
     *
     * - `timestampEpochMs <= 0` means "unknown" (Phase 5 never fabricates
     *   dates) → returns "" and callers fall back to no timestamp display.
     * - < 1 min → "Now"; < 1 h → "42m"; < 24 h → "3h"; yesterday → "Yesterday";
     *   < 7 d → "4d"; < 1 y → "Mar 3"; else → "Mar 3, 2025".
     */
    fun relativeTime(timestampEpochMs: Long, nowEpochMs: Long): String {
        if (timestampEpochMs <= 0L) return ""
        val diffMs = nowEpochMs - timestampEpochMs
        if (diffMs < 0) return "Now"
        val minutes = diffMs / 60_000
        if (minutes < 1) return "Now"
        if (minutes < 60) return "${minutes}m"
        val hours = minutes / 60
        if (hours < 24) return "${hours}h"
        val zone = ZoneId.systemDefault()
        // LocalDate.ofInstant is API 34+; atZone().toLocalDate() works on 26+.
        val then = Instant.ofEpochMilli(timestampEpochMs).atZone(zone).toLocalDate()
        val now = Instant.ofEpochMilli(nowEpochMs).atZone(zone).toLocalDate()
        if (then == now.minusDays(1)) return "Yesterday"
        val days = java.time.temporal.ChronoUnit.DAYS.between(then, now)
        if (days < 7) return "${days}d"
        return if (then.year == now.year) {
            then.format(monthDay)
        } else {
            then.format(monthDayYear)
        }
    }

    /**
     * Human-readable byte count for attachment metadata, e.g. "1.2 MB".
     * Null or negative input → "" (caller hides the size line).
     */
    fun formatBytes(bytes: Long?): String {
        if (bytes == null || bytes < 0) return ""
        if (bytes < 1_024) return "$bytes B"
        val kb = bytes / 1_024.0
        if (kb < 1_024) return "${kb.toLong()} KB"
        val mb = kb / 1_024.0
        if (mb < 1_024) return String.format(Locale.US, "%.1f MB", mb)
        return String.format(Locale.US, "%.1f GB", mb / 1_024.0)
    }

    /**
     * Avatar initial: first letter of the display name, else of the email
     * local-part, upper-cased. Never blank — falls back to "?".
     */
    fun avatarInitial(displayName: String?, emailAddress: String): String {
        val fromName = displayName?.trim()?.takeIf { it.isNotEmpty() }
        if (fromName != null) return fromName.first().uppercaseChar().toString()
        val local = emailAddress.substringBefore('@').trim()
        if (local.isNotEmpty()) return local.first().uppercaseChar().toString()
        return "?"
    }

    /**
     * Sender line for list rows: display name when present, else the address.
     * Blank/blank → "Unknown sender" (phase §28: safe fallback, never crash).
     */
    fun senderDisplay(fromName: String?, fromAddress: String): String {
        val name = fromName?.trim()?.takeIf { it.isNotEmpty() }
        if (name != null) return name
        val addr = fromAddress.trim()
        return addr.ifEmpty { "Unknown sender" }
    }

    /** Subject line fallback (phase §28). */
    fun subjectDisplay(subject: String): String =
        subject.trim().ifEmpty { "No subject" }
}
