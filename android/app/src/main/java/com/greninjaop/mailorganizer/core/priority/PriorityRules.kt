package com.greninjaop.mailorganizer.core.priority

import com.greninjaop.mailorganizer.core.classify.ClassifierCategory
import com.greninjaop.mailorganizer.core.classify.Confidence
import com.greninjaop.mailorganizer.core.classify.SignalStrength

/**
 * Deterministic priority rules (Phase 9).
 *
 * Each rule votes for one [PriorityLevel] with a weight when its matcher
 * fires. The engine sums weights per level; the highest total wins (ties
 * break by explicit precedence). NORMAL carries a base score so that weak
 * or absent evidence keeps ordinary mail at NORMAL — priority is never
 * inflated or deflated on a hunch.
 *
 * Rules are registered data with stable ids (never PR_1), documented once
 * here. All matchers are total and bounded — hostile input degrades, never
 * crashes.
 */
data class PriorityRule(
    val id: String,
    val level: PriorityLevel,
    val weight: Int,
    val description: String,
    val explanationTemplate: String,
    val match: (PriorityInput) -> List<MatchedPrioritySignal>,
)

/** Builds the production rule set (v1). */
object PriorityRuleSet {

    /**
     * Rule ID: PRIORITY_CRITICAL_SECURITY
     * Level: CRITICAL, weight 105. Fires when the message is classified
     * SECURITY with HIGH confidence — e.g. a likely account-breach alert.
     * CRITICAL is deliberately rare: it must clear a high bar.
     */
    private val CRITICAL_SECURITY = PriorityRule(
        id = "PRIORITY_CRITICAL_SECURITY",
        level = PriorityLevel.CRITICAL,
        weight = 105,
        description = "High-confidence security alert (possible breach, OTP abuse).",
        explanationTemplate = "Critical — high-confidence security alert.",
        match = { input ->
            if (input.category == ClassifierCategory.SECURITY &&
                input.confidence == Confidence.HIGH
            ) {
                listOf(
                    MatchedPrioritySignal(
                        name = "category_security_high_confidence",
                        detail = "Classified as Security with high confidence",
                        strength = SignalStrength.STRONG,
                    ),
                )
            } else emptyList()
        },
    )

    /**
     * Rule ID: PRIORITY_CRITICAL_ACTION
     * Level: CRITICAL, weight 100. Fires when the message is classified
     * ACTION_REQUIRED with HIGH confidence — an urgent call to act.
     */
    private val CRITICAL_ACTION = PriorityRule(
        id = "PRIORITY_CRITICAL_ACTION",
        level = PriorityLevel.CRITICAL,
        weight = 100,
        description = "High-confidence action-required item (payment due, deadline).",
        explanationTemplate = "Critical — urgent action required.",
        match = { input ->
            if (input.category == ClassifierCategory.ACTION_REQUIRED &&
                input.confidence == Confidence.HIGH
            ) {
                listOf(
                    MatchedPrioritySignal(
                        name = "category_action_required_high_confidence",
                        detail = "Classified as Action Required with high confidence",
                        strength = SignalStrength.STRONG,
                    ),
                )
            } else emptyList()
        },
    )

    /**
     * Rule ID: PRIORITY_ACTION_REQUIRED
     * Level: HIGH, weight 70. Any ACTION_REQUIRED classification deserves
     * attention even at lower confidence — the category itself is the
     * signal (Phase 7's ACTION_* rules are explicit calls to act).
     */
    private val ACTION_REQUIRED = PriorityRule(
        id = "PRIORITY_ACTION_REQUIRED",
        level = PriorityLevel.HIGH,
        weight = 70,
        description = "Classified as Action Required (any confidence).",
        explanationTemplate = "Action required.",
        match = { input ->
            if (input.category == ClassifierCategory.ACTION_REQUIRED) {
                listOf(
                    MatchedPrioritySignal(
                        name = "category_action_required",
                        detail = "Classified as Action Required",
                        strength = SignalStrength.STRONG,
                    ),
                )
            } else emptyList()
        },
    )

    /**
     * Rule ID: PRIORITY_SECURITY
     * Level: HIGH, weight 60. SECURITY at any confidence — security mail
     * should not sit unread.
     */
    private val SECURITY = PriorityRule(
        id = "PRIORITY_SECURITY",
        level = PriorityLevel.HIGH,
        weight = 60,
        description = "Classified as Security (any confidence).",
        explanationTemplate = "Security-related.",
        match = { input ->
            if (input.category == ClassifierCategory.SECURITY) {
                listOf(
                    MatchedPrioritySignal(
                        name = "category_security",
                        detail = "Classified as Security",
                        strength = SignalStrength.STRONG,
                    ),
                )
            } else emptyList()
        },
    )

    /**
     * Rule ID: PRIORITY_IMPORTANT
     * Level: HIGH, weight 45. The IMPORTANT category is an explicit
     * importance signal from the classifier.
     */
    private val IMPORTANT = PriorityRule(
        id = "PRIORITY_IMPORTANT",
        level = PriorityLevel.HIGH,
        weight = 45,
        description = "Classified as Important.",
        explanationTemplate = "Marked important.",
        match = { input ->
            if (input.category == ClassifierCategory.IMPORTANT) {
                listOf(
                    MatchedPrioritySignal(
                        name = "category_important",
                        detail = "Classified as Important",
                        strength = SignalStrength.MEDIUM,
                    ),
                )
            } else emptyList()
        },
    )

    /**
     * Rule ID: PRIORITY_RECURRING_CAREER
     * Level: HIGH, weight 50. A recurring sender writing about career or
     * education is usually a real person with real stakes (recruiter,
     * professor) — worth attention.
     */
    private val RECURRING_CAREER = PriorityRule(
        id = "PRIORITY_RECURRING_CAREER",
        level = PriorityLevel.HIGH,
        weight = 50,
        description = "Recurring sender on a career/education thread.",
        explanationTemplate = "Recurring sender about career or education.",
        match = { input ->
            if (input.isRecurringSender &&
                (input.category == ClassifierCategory.CAREER ||
                    input.category == ClassifierCategory.EDUCATION)
            ) {
                listOf(
                    MatchedPrioritySignal(
                        name = "recurring_sender_career_education",
                        detail = "Recurring sender about career or education",
                        strength = SignalStrength.MEDIUM,
                    ),
                )
            } else emptyList()
        },
    )

    /**
     * Rule ID: PRIORITY_DIRECT_PERSONAL
     * Level: HIGH, weight 30. An unread, non-bulk, non-newsletter message
     * is likely a real person writing to the user. The weight is modest on
     * purpose: on its own it does not outrank NORMAL's base score — it
     * reinforces other HIGH signals rather than inventing urgency.
     */
    private val DIRECT_PERSONAL = PriorityRule(
        id = "PRIORITY_DIRECT_PERSONAL",
        level = PriorityLevel.HIGH,
        weight = 30,
        description = "Unread personal mail (not bulk, no unsubscribe marker).",
        explanationTemplate = "Personal mail, still unread.",
        match = { input ->
            val personalCategory = input.category == null ||
                input.category == ClassifierCategory.UNCLASSIFIED ||
                input.category == ClassifierCategory.IMPORTANT ||
                input.category == ClassifierCategory.ACTION_REQUIRED
            if (input.unread && !input.isBulkSender && !input.hasUnsubscribeMarker &&
                personalCategory
            ) {
                listOf(
                    MatchedPrioritySignal(
                        name = "direct_personal_unread",
                        detail = "Unread personal mail",
                        strength = SignalStrength.WEAK,
                    ),
                )
            } else emptyList()
        },
    )

    /**
     * Rule ID: PRIORITY_ORDER_ACTIVE
     * Level: NORMAL, weight 25. Receipts and order updates are ordinary
     * mail — this rule anchors them at NORMAL against LOW noise signals.
     */
    private val ORDER_ACTIVE = PriorityRule(
        id = "PRIORITY_ORDER_ACTIVE",
        level = PriorityLevel.NORMAL,
        weight = 25,
        description = "Order/receipt update — ordinary mail.",
        explanationTemplate = "Order or receipt update.",
        match = { input ->
            if (input.category == ClassifierCategory.RECEIPTS_ORDERS) {
                listOf(
                    MatchedPrioritySignal(
                        name = "category_receipts_orders",
                        detail = "Order or receipt update",
                        strength = SignalStrength.WEAK,
                    ),
                )
            } else emptyList()
        },
    )

    /**
     * Rule ID: PRIORITY_NEWSLETTER
     * Level: LOW, weight 55. Newsletters (or any mail with an unsubscribe
     * marker) are skimmable by design.
     */
    private val NEWSLETTER = PriorityRule(
        id = "PRIORITY_NEWSLETTER",
        level = PriorityLevel.LOW,
        weight = 55,
        description = "Newsletter or unsubscribe-marker mail.",
        explanationTemplate = "Newsletter.",
        match = { input ->
            if (input.category == ClassifierCategory.NEWSLETTERS ||
                input.hasUnsubscribeMarker
            ) {
                listOf(
                    MatchedPrioritySignal(
                        name = "newsletter_or_unsubscribe",
                        detail = "Newsletter or unsubscribe marker",
                        strength = SignalStrength.MEDIUM,
                    ),
                )
            } else emptyList()
        },
    )

    /**
     * Rule ID: PRIORITY_PROMOTION
     * Level: LOW, weight 55. Promotional mail must not dominate attention
     * (requirements.md).
     */
    private val PROMOTION = PriorityRule(
        id = "PRIORITY_PROMOTION",
        level = PriorityLevel.LOW,
        weight = 55,
        description = "Promotional mail.",
        explanationTemplate = "Promotional mail.",
        match = { input ->
            if (input.category == ClassifierCategory.PROMOTIONS) {
                listOf(
                    MatchedPrioritySignal(
                        name = "category_promotions",
                        detail = "Classified as Promotions",
                        strength = SignalStrength.MEDIUM,
                    ),
                )
            } else emptyList()
        },
    )

    /**
     * Rule ID: PRIORITY_LOW_VALUE
     * Level: LOW, weight 65. The classifier's LOW_VALUE verdict is a
     * strong deprioritization signal.
     */
    private val LOW_VALUE = PriorityRule(
        id = "PRIORITY_LOW_VALUE",
        level = PriorityLevel.LOW,
        weight = 65,
        description = "Classifier verdict: low value.",
        explanationTemplate = "Low-value mail.",
        match = { input ->
            if (input.category == ClassifierCategory.LOW_VALUE) {
                listOf(
                    MatchedPrioritySignal(
                        name = "category_low_value",
                        detail = "Classified as Low Value",
                        strength = SignalStrength.MEDIUM,
                    ),
                )
            } else emptyList()
        },
    )

    /**
     * Rule ID: PRIORITY_NOTIFICATION
     * Level: LOW, weight 35. Plain notifications skew low, but the weight
     * stays under NORMAL's base score: a lone notification is ordinary
     * mail, not noise — only combined LOW signals demote it.
     */
    private val NOTIFICATION = PriorityRule(
        id = "PRIORITY_NOTIFICATION",
        level = PriorityLevel.LOW,
        weight = 35,
        description = "Notification mail (weak deprioritization).",
        explanationTemplate = "Notification.",
        match = { input ->
            if (input.category == ClassifierCategory.NOTIFICATIONS) {
                listOf(
                    MatchedPrioritySignal(
                        name = "category_notifications",
                        detail = "Classified as Notifications",
                        strength = SignalStrength.WEAK,
                    ),
                )
            } else emptyList()
        },
    )

    /**
     * Rule ID: PRIORITY_SOCIAL_LABEL
     * Level: LOW, weight 35. Gmail's social bucket is low-attention mail.
     */
    private val SOCIAL_LABEL = PriorityRule(
        id = "PRIORITY_SOCIAL_LABEL",
        level = PriorityLevel.LOW,
        weight = 35,
        description = "Gmail social-category mail.",
        explanationTemplate = "Social mail.",
        match = { input ->
            if (input.labelIds.any { it == "CATEGORY_SOCIAL" }) {
                listOf(
                    MatchedPrioritySignal(
                        name = "label_category_social",
                        detail = "Gmail social label",
                        strength = SignalStrength.WEAK,
                    ),
                )
            } else emptyList()
        },
    )

    /**
     * Rule ID: PRIORITY_SPAM_LABEL
     * Level: LOW, weight 85. Spam is the lowest-attention mail there is.
     */
    private val SPAM_LABEL = PriorityRule(
        id = "PRIORITY_SPAM_LABEL",
        level = PriorityLevel.LOW,
        weight = 85,
        description = "Gmail spam label.",
        explanationTemplate = "Spam.",
        match = { input ->
            if (input.labelIds.any { it == "SPAM" }) {
                listOf(
                    MatchedPrioritySignal(
                        name = "label_spam",
                        detail = "Gmail spam label",
                        strength = SignalStrength.STRONG,
                    ),
                )
            } else emptyList()
        },
    )

    /**
     * Rule ID: PRIORITY_BULK_SENDER
     * Level: LOW, weight 30. Automated bulk senders (noreply et al.) are
     * weak deprioritization on their own — they reinforce, not decide.
     */
    private val BULK_SENDER = PriorityRule(
        id = "PRIORITY_BULK_SENDER",
        level = PriorityLevel.LOW,
        weight = 30,
        description = "Automated bulk sender.",
        explanationTemplate = "Automated sender.",
        match = { input ->
            if (input.isBulkSender) {
                listOf(
                    MatchedPrioritySignal(
                        name = "bulk_sender",
                        detail = "Automated bulk sender",
                        strength = SignalStrength.WEAK,
                    ),
                )
            } else emptyList()
        },
    )

    /** All registered rules (v1: 15 rules). */
    val ALL: List<PriorityRule> = listOf(
        CRITICAL_SECURITY,
        CRITICAL_ACTION,
        ACTION_REQUIRED,
        SECURITY,
        IMPORTANT,
        RECURRING_CAREER,
        DIRECT_PERSONAL,
        ORDER_ACTIVE,
        NEWSLETTER,
        PROMOTION,
        LOW_VALUE,
        NOTIFICATION,
        SOCIAL_LABEL,
        SPAM_LABEL,
        BULK_SENDER,
    )

    init {
        check(ALL.map { it.id }.toSet().size == ALL.size) {
            "Duplicate priority rule ids"
        }
    }
}
