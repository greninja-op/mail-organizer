package com.greninjaop.mailorganizer.core.actions

import com.greninjaop.mailorganizer.data.local.ActionType
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.Priority

/**
 * Deterministic action-candidate generator (Phase 14).
 *
 * [generate] is a pure function of ([ActionInput], [nowEpochMs]) and
 * [VERSION] — no randomness, no network, no device state. [nowEpochMs] is
 * injected (same discipline as Phase 13's clock) so urgency computation is
 * testable.
 *
 * Generation rules (each rule is documented with a stable id in KDoc):
 *
 * - ACT-TEMPORAL-INTERVIEW: UPCOMING INTERVIEW (confidence ≥ MEDIUM) →
 *   MEETING candidate with a proposed "Add to calendar" external effect.
 * - ACT-TEMPORAL-MEETING: UPCOMING MEETING/APPOINTMENT/EVENT (≥ MEDIUM) →
 *   MEETING candidate, calendar proposed.
 * - ACT-TEMPORAL-APPLICATION: APPLICATION_DEADLINE/SUBMISSION_DEADLINE →
 *   APPLICATION candidate (review-style; paying/submitting is never
 *   automated).
 * - ACT-TEMPORAL-PAYMENT: PAYMENT_DEADLINE → PAYMENT candidate
 *   (review-style — amounts are never inferred).
 * - ACT-TEMPORAL-REGISTRATION: REGISTRATION_DEADLINE → DEADLINE candidate.
 * - ACT-TEMPORAL-DEADLINE: DEADLINE → DEADLINE candidate.
 * - ACT-TEMPORAL-REMINDER: REMINDER_DATE → REMINDER candidate.
 * - ACT-TEMPORAL-TRAVEL: TRAVEL → TRAVEL candidate.
 * - ACT-CLASSIFY-REPLY: effective category ACTION_REQUIRED →
 *   REPLY_REQUIRED candidate ("Review email" — replying is a Phase 22
 *   Gmail write, so the suggestion stays internal and honest).
 * - ACT-PRIORITY-REVIEW: CRITICAL priority with no other candidate →
 *   OTHER candidate ("Needs review").
 *
 * Deliberately NOT generated: bare DATE_ONLY/TIME_ONLY/DATE_TIME/DATE_RANGE
 * items (no semantic trigger — inventing an action would be fabrication),
 * PAST temporal items (the expiry pass retires stale cards instead), and
 * LOW/UNKNOWN-confidence temporal items for "attend" proposals (they may
 * still surface as review-style cards through classification signals).
 *
 * Deduplication (phase §22): [ActionCandidate.id] is
 * `"$messageId|$actionType|$targetKey"`. Thread-level dedup across messages
 * is the domain use case's job — the generator only guarantees
 * per-message stability.
 */
object ActionCandidateGenerator {

    /** Bump when generation rules change; drives re-generation. */
    const val VERSION = 1

    private const val UPCOMING = "UPCOMING"
    private const val PAST = "PAST"
    private const val HIGH = "HIGH"
    private const val MEDIUM = "MEDIUM"

    private const val DAY_MS = 24L * 60 * 60 * 1000

    fun generate(input: ActionInput, nowEpochMs: Long): List<ActionCandidate> {
        if (input.messageId.isBlank() || input.accountId.isBlank()) return emptyList()
        val out = mutableListOf<ActionCandidate>()

        for (item in input.temporalItems) {
            temporalCandidate(input, item, nowEpochMs)?.let(out::add)
        }

        if (input.actionRequired && input.category == MailCategory.ACTION_REQUIRED) {
            out += ActionCandidate(
                id = key(input.messageId, ActionType.REPLY_REQUIRED, "general"),
                accountId = input.accountId,
                messageId = input.messageId,
                threadId = input.threadId,
                actionType = ActionType.REPLY_REQUIRED,
                title = "Reply needed: ${shortSubject(input.subject)}",
                description = "This email asks for a reply. Review it to decide " +
                    "what to send — nothing is sent automatically.",
                urgency = urgencyFor(input.priority, dueEpochMs = null, nowEpochMs),
                source = sourceFor(input.categorySourceName),
                confidence = ActionConfidence.HIGH,
                explanation = "Suggested because: email is classified as " +
                    "Action Required" +
                    (if (input.unread) "; it is unread" else "") + ".",
                signals = listOf(
                    ActionSignal("action_required", "Effective category is ACTION_REQUIRED"),
                ),
                externalEffect = ExternalEffect.NONE,
                missingInfo = emptyList(),
                dueDateEpochMs = null,
                targetKey = "general",
                version = VERSION,
            )
        }

        if (out.isEmpty() && input.priority == Priority.CRITICAL) {
            out += ActionCandidate(
                id = key(input.messageId, ActionType.OTHER, "general"),
                accountId = input.accountId,
                messageId = input.messageId,
                threadId = input.threadId,
                actionType = ActionType.OTHER,
                title = "Needs review: ${shortSubject(input.subject)}",
                description = "Marked critical priority. Review when ready.",
                urgency = ActionUrgency.CRITICAL,
                source = ActionSource.PRIORITY,
                confidence = ActionConfidence.MEDIUM,
                explanation = "Suggested because: email priority is CRITICAL.",
                signals = listOf(
                    ActionSignal("critical_priority", "Effective priority is CRITICAL"),
                ),
                externalEffect = ExternalEffect.NONE,
                missingInfo = emptyList(),
                dueDateEpochMs = null,
                targetKey = "general",
                version = VERSION,
            )
        }

        return out
    }

    private fun temporalCandidate(
        input: ActionInput,
        item: TemporalRef,
        nowEpochMs: Long,
    ): ActionCandidate? {
        // Past items never generate new candidates — the expiry pass
        // retires their cards instead (phase §27).
        if (item.statusName == PAST) return null
        val confident = item.confidenceName == HIGH || item.confidenceName == MEDIUM

        return when (item.itemTypeName) {
            "INTERVIEW" -> {
                if (!confident || item.statusName != UPCOMING) return null
                meetingCard(
                    input, item, nowEpochMs,
                    title = "Interview: ${item.title}",
                    description = "You have an interview coming up. Add it to " +
                        "your calendar so you don't miss it.",
                )
            }
            "MEETING", "APPOINTMENT", "EVENT" -> {
                if (!confident || item.statusName != UPCOMING) return null
                meetingCard(
                    input, item, nowEpochMs,
                    title = "Meeting: ${item.title}",
                    description = "A meeting is scheduled. Add it to your " +
                        "calendar or review the details.",
                )
            }
            "APPLICATION_DEADLINE", "SUBMISSION_DEADLINE" ->
                reviewCard(
                    input, item, nowEpochMs,
                    type = ActionType.APPLICATION,
                    title = "Application deadline: ${item.title}",
                    description = "A deadline is approaching. Review what needs " +
                        "to be submitted — nothing is submitted automatically.",
                    source = ActionSource.TEMPORAL_EXTRACTION,
                )
            "PAYMENT_DEADLINE" ->
                reviewCard(
                    input, item, nowEpochMs,
                    type = ActionType.PAYMENT,
                    title = "Payment due: ${item.title}",
                    description = "A payment deadline is approaching. Review " +
                        "the bill — amounts are never inferred and nothing " +
                        "is paid automatically.",
                    source = ActionSource.TEMPORAL_EXTRACTION,
                )
            "REGISTRATION_DEADLINE" ->
                reviewCard(
                    input, item, nowEpochMs,
                    type = ActionType.DEADLINE,
                    title = "Registration closes: ${item.title}",
                    description = "Registration closes soon. Review the " +
                        "details — nothing is registered automatically.",
                    source = ActionSource.TEMPORAL_EXTRACTION,
                )
            "DEADLINE" ->
                reviewCard(
                    input, item, nowEpochMs,
                    type = ActionType.DEADLINE,
                    title = "Deadline: ${item.title}",
                    description = "A deadline is approaching. Review what " +
                        "needs to be done — nothing happens automatically.",
                    source = ActionSource.TEMPORAL_EXTRACTION,
                )
            "REMINDER_DATE" ->
                reviewCard(
                    input, item, nowEpochMs,
                    type = ActionType.REMINDER,
                    title = "Reminder: ${item.title}",
                    description = "You asked to be reminded about this. " +
                        "Review the details.",
                    source = ActionSource.TEMPORAL_EXTRACTION,
                )
            "TRAVEL" ->
                reviewCard(
                    input, item, nowEpochMs,
                    type = ActionType.TRAVEL,
                    title = "Trip: ${item.title}",
                    description = "Travel is coming up. Review the itinerary.",
                    source = ActionSource.TEMPORAL_EXTRACTION,
                )
            // DATE_ONLY / TIME_ONLY / DATE_TIME / DATE_RANGE carry no
            // semantic trigger — generating an action would be fabrication.
            else -> null
        }
    }

    /** "Attend" proposal: calendar write is proposed, never performed (§19). */
    private fun meetingCard(
        input: ActionInput,
        item: TemporalRef,
        nowEpochMs: Long,
        title: String,
        description: String,
    ): ActionCandidate {
        val missing = mutableListOf<String>()
        if (item.isDateOnly) missing += "start time"
        return ActionCandidate(
            id = key(input.messageId, ActionType.MEETING, "temporal:${item.itemId}"),
            accountId = input.accountId,
            messageId = input.messageId,
            threadId = input.threadId,
            actionType = ActionType.MEETING,
            title = title,
            description = description,
            urgency = urgencyFor(input.priority, item.startEpochMs, nowEpochMs),
            source = ActionSource.TEMPORAL_EXTRACTION,
            confidence = confidenceFor(item.confidenceName),
            explanation = "Suggested because: ${item.title} is scheduled " +
                friendlyWhen(item.startEpochMs, nowEpochMs) + ".",
            signals = listOf(
                ActionSignal(
                    "temporal_${item.itemTypeName.lowercase()}",
                    "Extracted ${item.itemTypeName.lowercase()} (${item.confidenceName.lowercase()} confidence)",
                ),
            ),
            externalEffect = ExternalEffect.CALENDAR,
            missingInfo = missing,
            dueDateEpochMs = item.startEpochMs,
            targetKey = "temporal:${item.itemId}",
            version = VERSION,
        )
    }

    /** Review-style internal suggestion — nothing external is proposed. */
    private fun reviewCard(
        input: ActionInput,
        item: TemporalRef,
        nowEpochMs: Long,
        type: ActionType,
        title: String,
        description: String,
        source: ActionSource,
    ): ActionCandidate = ActionCandidate(
        id = key(input.messageId, type, "temporal:${item.itemId}"),
        accountId = input.accountId,
        messageId = input.messageId,
        threadId = input.threadId,
        actionType = type,
        title = title,
        description = description,
        urgency = urgencyFor(input.priority, item.startEpochMs, nowEpochMs),
        source = source,
        confidence = confidenceFor(item.confidenceName),
        explanation = "Suggested because: ${item.title} " +
            friendlyWhen(item.startEpochMs, nowEpochMs) + ".",
        signals = listOf(
            ActionSignal(
                "temporal_${item.itemTypeName.lowercase()}",
                "Extracted ${item.itemTypeName.lowercase()} (${item.confidenceName.lowercase()} confidence)",
            ),
        ),
        externalEffect = ExternalEffect.NONE,
        missingInfo = emptyList(),
        dueDateEpochMs = item.startEpochMs,
        targetKey = "temporal:${item.itemId}",
        version = VERSION,
    )

    private fun urgencyFor(
        priority: Priority?,
        dueEpochMs: Long?,
        nowEpochMs: Long,
    ): ActionUrgency {
        var u = when (priority) {
            Priority.CRITICAL -> ActionUrgency.CRITICAL
            Priority.HIGH -> ActionUrgency.HIGH
            Priority.LOW -> ActionUrgency.LOW
            else -> ActionUrgency.NORMAL
        }
        if (dueEpochMs != null) {
            val delta = dueEpochMs - nowEpochMs
            u = when {
                // Overdue: still needs attention (expiry retires it later).
                delta < 0 -> maxUrgency(u, ActionUrgency.HIGH)
                delta < DAY_MS -> maxUrgency(u, ActionUrgency.HIGH)
                delta < 7 * DAY_MS -> maxUrgency(u, ActionUrgency.NORMAL)
                else -> u
            }
        }
        return u
    }

    private fun maxUrgency(a: ActionUrgency, b: ActionUrgency): ActionUrgency =
        if (a.ordinal < b.ordinal) a else b

    private fun confidenceFor(name: String): ActionConfidence = when (name) {
        HIGH -> ActionConfidence.HIGH
        MEDIUM -> ActionConfidence.MEDIUM
        "LOW" -> ActionConfidence.LOW
        else -> ActionConfidence.UNKNOWN
    }

    private fun sourceFor(categorySourceName: String?): ActionSource = when (categorySourceName) {
        "USER_CORRECTION" -> ActionSource.USER_CORRECTION
        "USER_RULE" -> ActionSource.USER_RULE
        else -> ActionSource.CLASSIFICATION
    }

    private fun friendlyWhen(startEpochMs: Long, nowEpochMs: Long): String {
        val delta = startEpochMs - nowEpochMs
        return when {
            delta < 0 -> "which has passed"
            delta < DAY_MS -> "which is within the next 24 hours"
            delta < 7 * DAY_MS -> "which is in the next few days"
            else -> "which is upcoming"
        }
    }

    private fun shortSubject(subject: String): String {
        val s = subject.ifBlank { "(no subject)" }
        return if (s.length <= 80) s else s.take(77) + "…"
    }

    private fun key(messageId: String, type: ActionType, targetKey: String): String =
        "$messageId|${type.name}|$targetKey"
}
