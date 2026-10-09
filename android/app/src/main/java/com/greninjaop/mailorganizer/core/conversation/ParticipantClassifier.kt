package com.greninjaop.mailorganizer.core.conversation

import java.util.Locale

/**
 * Account-aware participant modeling (Phase 21 §10, §11, §20, §21).
 *
 * Distinguishes user-sent messages from counterparty messages and automated/no-reply senders.
 * Always evaluated in the context of the specific [userAccountEmail].
 */
object ParticipantClassifier {

    private val NO_REPLY_PREFIXES = listOf(
        "no-reply",
        "noreply",
        "donotreply",
        "do-not-reply",
        "notifications",
        "notification",
        "mailer-daemon",
        "postmaster",
        "bounce",
        "automated",
        "system",
        "alert",
        "alerts",
        "support-auto",
        "billing-notice",
        "orders",
        "newsletters",
        "updates",
    )

    private val NO_REPLY_DOMAINS = listOf(
        "notifications.google.com",
        "mail.instagram.com",
        "postmaster.twitter.com",
        "bounce.linkedin.com",
    )

    /**
     * Extracts pure email address from potential name/bracket formatted string:
     * e.g. "Alex Rivera <alex@example.com>" -> "alex@example.com"
     */
    fun extractEmailAddress(raw: String): String {
        val trimmed = raw.trim()
        val start = trimmed.indexOf('<')
        val end = trimmed.lastIndexOf('>')
        if (start >= 0 && end > start) {
            return trimmed.substring(start + 1, end).trim()
        }
        return trimmed
    }

    /**
     * Extracts a display name from an address header or falls back to local part:
     * e.g. "Sarah Chen <sarah@chen.org>" -> "Sarah Chen"
     * e.g. "sarah.chen@example.com" -> "sarah.chen"
     */
    fun displayName(fromHeader: String): String {
        val trimmed = fromHeader.trim()
        val start = trimmed.indexOf('<')
        if (start > 0) {
            val name = trimmed.substring(0, start).trim().removeSurrounding("\"")
            if (name.isNotBlank()) return name
        }
        val email = extractEmailAddress(trimmed)
        return email.substringBefore('@')
    }

    /**
     * Determines whether an email address is an automated / no-reply address.
     * Note: Reply-To headers should also be inspected by callers (§21).
     */
    fun isAutomatedNoReply(emailAddress: String): Boolean {
        val clean = extractEmailAddress(emailAddress).lowercase(Locale.ROOT)
        if (clean.isBlank()) return false

        val localPart = clean.substringBefore('@')
        val domain = clean.substringAfter('@', "")

        for (prefix in NO_REPLY_PREFIXES) {
            if (localPart == prefix || localPart.startsWith("$prefix+") || localPart.startsWith("$prefix.") || localPart.startsWith("$prefix-")) {
                return true
            }
        }

        for (d in NO_REPLY_DOMAINS) {
            if (domain == d || domain.endsWith(".$d")) {
                return true
            }
        }

        return false
    }

    /**
     * Classifies the participant role for a message sender with respect to the user's account.
     * Supports both [userAccountEmail] and [userEmail] parameter names.
     */
    fun classifyRole(
        fromAddress: String,
        userAccountEmail: String? = null,
        userEmail: String? = userAccountEmail,
        replyToAddress: String? = null,
    ): ParticipantRole {
        val targetUserEmail = (userEmail ?: userAccountEmail).orEmpty()
        val cleanFrom = extractEmailAddress(fromAddress).lowercase(Locale.ROOT)
        val cleanUser = extractEmailAddress(targetUserEmail).lowercase(Locale.ROOT)

        if (cleanFrom.isNotBlank() && cleanUser.isNotBlank() && cleanFrom == cleanUser) {
            return ParticipantRole.USER
        }

        if (isAutomatedNoReply(cleanFrom)) {
            return ParticipantRole.AUTOMATED_NO_REPLY
        }

        return ParticipantRole.OTHER_PARTY
    }
}
