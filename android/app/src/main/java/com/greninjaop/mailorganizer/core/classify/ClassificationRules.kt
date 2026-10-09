package com.greninjaop.mailorganizer.core.classify

/**
 * Structured classification rules (Phase 7 §17, §37, §61).
 *
 * Architecture (phase §37): rules are registered data — a list of
 * [ClassificationRule] objects with stable ids, weights, and pure matcher
 * functions — never a giant if/else chain. Adding a rule means appending one
 * entry; precedence and scoring stay centralized in [DeterministicClassifier].
 *
 * Rule documentation convention (phase §61): each rule's KDoc carries
 * Rule ID / Purpose / Signals / Strength / Category / Precedence /
 * Explanation / Known limitations. The doc lives here, once — it is not
 * duplicated elsewhere.
 *
 * Signal-strength guide (phase §39):
 * - STRONG: the pattern is specific (multi-word phrase, known domain).
 * - MEDIUM: indicative but broader (single keyword in the right context).
 * - WEAK: generic hint that only corroborates (unsubscribe footer alone).
 *
 * Negative-signal handling (phase §40): narrow multi-word phrases are used
 * instead of single generic words, so "sale" inside an unrelated sentence
 * cannot fire promotion rules and a lone "security" cannot fire security
 * rules. Gmail-label rules require corroborating content signals.
 */

/**
 * One deterministic rule.
 *
 * @param id stable rule id, e.g. SECURITY_OTP (phase §17 — never RULE_1).
 * @param category category awarded when this rule fires.
 * @param weight scoring weight (phase §38); higher = stronger evidence.
 * @param description one-line purpose.
 * @param explanationTemplate human-readable "why", with {detail} slots filled
 *   from matched signals.
 * @param match pure function of [ExtractedSignals]; returns the matched
 *   signals or empty when the rule does not fire. Must be total.
 */
data class ClassificationRule(
    val id: String,
    val category: ClassifierCategory,
    val weight: Int,
    val description: String,
    val explanationTemplate: String,
    val match: (ExtractedSignals) -> List<MatchedSignal>,
)

/**
 * Builds the production rule set (v2: 31 rules).
 *
 * Known-domain lists are intentionally small and architectural (phase §20):
 * [DomainSignals] is the seam where a future sender-intelligence layer
 * (Phase 8) can inject richer data without changing rule structure.
 */
object ClassificationRuleSet {

    /** Domains with well-known roles, used by several rules. */
    object DomainSignals {
        /** Job/recruiting platforms. */
        val CAREER = setOf(
            "linkedin.com", "indeed.com", "naukri.com", "monster.com",
            "glassdoor.com", "ziprecruiter.com", "wellfound.com",
        )

        /** Learning platforms. */
        val EDUCATION_PLATFORMS = setOf(
            "coursera.org", "udemy.com", "edx.org", "khanacademy.org",
            "udacity.com", "skillshare.com", "masterclass.com",
        )

        /** Social networks (notification context). */
        val SOCIAL = setOf(
            "facebook.com", "instagram.com", "twitter.com", "x.com",
            "tiktok.com", "snapchat.com", "pinterest.com", "reddit.com",
            "youtube.com",
        )

        /** Developer/service notification senders. */
        val SERVICE_NOTIFICATIONS = setOf(
            "github.com", "gitlab.com", "bitbucket.org", "stackoverflow.com",
        )
    }

    private fun phraseSignal(
        name: String,
        phrases: List<String>,
        signals: ExtractedSignals,
        fields: List<String>,
        strength: SignalStrength,
        detail: (String) -> String,
    ): List<MatchedSignal> {
        val out = mutableListOf<MatchedSignal>()
        for (field in fields) {
            for (phrase in phrases) {
                if (field.contains(phrase)) {
                    out.add(MatchedSignal(name, detail(phrase), strength))
                    break
                }
            }
        }
        return out
    }

    private fun subjectOrBody(
        name: String,
        phrases: List<String>,
        signals: ExtractedSignals,
        strength: SignalStrength,
        subjectDetail: (String) -> String = { "Subject contains \"$it\"" },
        bodyDetail: (String) -> String = { "Body mentions \"$it\"" },
    ): List<MatchedSignal> =
        phraseSignal(name, phrases, signals, listOf(signals.subjectNorm), strength, subjectDetail) +
            phraseSignal(name, phrases, signals, listOf(signals.bodyNorm), strength, bodyDetail)

    // ------------------------------------------------------------------
    // SECURITY (precedence 1 — security-sensitive matches outrank everything)
    // ------------------------------------------------------------------

    /**
     * Rule ID: SECURITY_OTP
     * Purpose: one-time passcodes / verification codes.
     * Signals: multi-word OTP phrases in subject or body.
     * Strength: STRONG (weight 100). Category: SECURITY. Precedence: 1.
     * Explanation: "Subject/body contains a verification-code phrase".
     * Limitations: codes sent as images (no text) are missed.
     */
    private val SECURITY_OTP = ClassificationRule(
        id = "SECURITY_OTP",
        category = ClassifierCategory.SECURITY,
        weight = 100,
        description = "One-time passcode / verification code",
        explanationTemplate = "Contains a verification-code phrase",
        match = { s ->
            subjectOrBody(
                "otp_phrase",
                listOf(
                    "verification code", "security code", "one-time passcode",
                    "one time passcode", "your otp", "otp is",
                ),
                s, SignalStrength.STRONG,
            )
        },
    )

    /**
     * Rule ID: SECURITY_LOGIN_ALERT
     * Purpose: new-login / sign-in attempt alerts.
     * Signals: login-attempt phrases. Strength: STRONG (100). Precedence: 1.
     * Limitations: "login" alone (single word) never fires — phrases required.
     */
    private val SECURITY_LOGIN_ALERT = ClassificationRule(
        id = "SECURITY_LOGIN_ALERT",
        category = ClassifierCategory.SECURITY,
        weight = 100,
        description = "New login / sign-in attempt alert",
        explanationTemplate = "Reports a login or sign-in attempt",
        match = { s ->
            subjectOrBody(
                "login_alert_phrase",
                listOf(
                    "new login", "new sign-in", "new sign in", "sign-in attempt",
                    "sign in attempt", "login attempt", "unrecognized device",
                    "new device", "was just used to sign in",
                ),
                s, SignalStrength.STRONG,
            )
        },
    )

    /**
     * Rule ID: SECURITY_PASSWORD_RESET
     * Purpose: password reset / change flows.
     * Signals: reset phrases. Strength: STRONG (100). Precedence: 1.
     */
    private val SECURITY_PASSWORD_RESET = ClassificationRule(
        id = "SECURITY_PASSWORD_RESET",
        category = ClassifierCategory.SECURITY,
        weight = 100,
        description = "Password reset or change request",
        explanationTemplate = "Concerns a password reset or change",
        match = { s ->
            subjectOrBody(
                "password_reset_phrase",
                listOf(
                    "password reset", "reset your password", "change your password",
                    "forgot your password",
                ),
                s, SignalStrength.STRONG,
            )
        },
    )

    /**
     * Rule ID: SECURITY_SUSPICIOUS
     * Purpose: suspicious/unusual activity warnings.
     * Signals: suspicious-activity phrases. Strength: STRONG (95).
     */
    private val SECURITY_SUSPICIOUS = ClassificationRule(
        id = "SECURITY_SUSPICIOUS",
        category = ClassifierCategory.SECURITY,
        weight = 95,
        description = "Suspicious or unusual account activity warning",
        explanationTemplate = "Warns about suspicious account activity",
        match = { s ->
            subjectOrBody(
                "suspicious_phrase",
                listOf(
                    "suspicious activity", "unusual activity", "we detected",
                    "unauthorized access",
                ),
                s, SignalStrength.STRONG,
            )
        },
    )

    /**
     * Rule ID: SECURITY_VERIFY_ACCOUNT
     * Purpose: account / email / identity verification requests.
     * Signals: verification phrases. Strength: STRONG (90). Precedence: 1.
     * Limitations: distinct from OTP (no code delivered) — kept separate so
     * explanations stay precise.
     */
    private val SECURITY_VERIFY_ACCOUNT = ClassificationRule(
        id = "SECURITY_VERIFY_ACCOUNT",
        category = ClassifierCategory.SECURITY,
        weight = 90,
        description = "Account/email/identity verification request",
        explanationTemplate = "Asks to verify the account or email",
        match = { s ->
            subjectOrBody(
                "verify_account_phrase",
                listOf(
                    "verify your account", "verify your email",
                    "confirm your email", "verify your identity",
                    "email verification",
                ),
                s, SignalStrength.STRONG,
            )
        },
    )

    // ------------------------------------------------------------------
    // ACTION REQUIRED (precedence 2 — explicit calls to act)
    // ------------------------------------------------------------------

    /**
     * Rule ID: ACTION_DEADLINE
     * Purpose: deadlines / required responses.
     * Signals: deadline phrases. Strength: STRONG (90). Category:
     * ACTION_REQUIRED. Precedence: 2.
     * Limitations: full deadline extraction is Phase 13 — this rule only
     * detects that action language is present.
     */
    private val ACTION_DEADLINE = ClassificationRule(
        id = "ACTION_DEADLINE",
        category = ClassifierCategory.ACTION_REQUIRED,
        weight = 90,
        description = "Deadline or required-response language",
        explanationTemplate = "Contains deadline / action-required language",
        match = { s ->
            subjectOrBody(
                "deadline_phrase",
                listOf(
                    "deadline", "due by", "due on", "action required",
                    "response required", "respond by", "needs your response",
                ),
                s, SignalStrength.STRONG,
            )
        },
    )

    /**
     * Rule ID: ACTION_PAYMENT_DUE
     * Purpose: payments due / overdue.
     * Signals: payment-due phrases. Strength: STRONG (90).
     */
    private val ACTION_PAYMENT_DUE = ClassificationRule(
        id = "ACTION_PAYMENT_DUE",
        category = ClassifierCategory.ACTION_REQUIRED,
        weight = 90,
        description = "Payment due or overdue",
        explanationTemplate = "Indicates a payment is due",
        match = { s ->
            subjectOrBody(
                "payment_due_phrase",
                listOf(
                    "payment due", "pay by", "past due", "amount due",
                    "invoice due", "payment overdue",
                ),
                s, SignalStrength.STRONG,
            )
        },
    )

    /**
     * Rule ID: ACTION_DOC_REQUEST
     * Purpose: document / information requests.
     * Signals: request phrases. Strength: STRONG (85).
     */
    private val ACTION_DOC_REQUEST = ClassificationRule(
        id = "ACTION_DOC_REQUEST",
        category = ClassifierCategory.ACTION_REQUIRED,
        weight = 85,
        description = "Document or information request",
        explanationTemplate = "Requests documents or information",
        match = { s ->
            subjectOrBody(
                "doc_request_phrase",
                listOf(
                    "document request", "please upload", "upload your",
                    "provide your", "submit your", "documents required",
                ),
                s, SignalStrength.STRONG,
            )
        },
    )

    // ------------------------------------------------------------------
    // RECEIPTS & ORDERS (precedence 3)
    // ------------------------------------------------------------------

    /**
     * Rule ID: ORDER_CONFIRMATION — order confirmation language.
     * Strength: STRONG (85). Signals: confirmation phrases.
     */
    private val ORDER_CONFIRMATION = ClassificationRule(
        id = "ORDER_CONFIRMATION",
        category = ClassifierCategory.RECEIPTS_ORDERS,
        weight = 85,
        description = "Order confirmation",
        explanationTemplate = "Confirms a placed order",
        match = { s ->
            subjectOrBody(
                "order_confirmation_phrase",
                listOf(
                    "order confirmed", "order confirmation",
                    "thank you for your order", "we've received your order",
                    "your order has been received",
                ),
                s, SignalStrength.STRONG,
            )
        },
    )

    /**
     * Rule ID: ORDER_SHIPPED — shipment / tracking notifications.
     * Strength: STRONG (80).
     */
    private val ORDER_SHIPPED = ClassificationRule(
        id = "ORDER_SHIPPED",
        category = ClassifierCategory.RECEIPTS_ORDERS,
        weight = 80,
        description = "Shipment / tracking notification",
        explanationTemplate = "Notifies that an order shipped",
        match = { s ->
            subjectOrBody(
                "shipped_phrase",
                listOf(
                    "has shipped", "your package", "out for delivery",
                    "tracking number", "on its way", "shipped:",
                ),
                s, SignalStrength.STRONG,
            )
        },
    )

    /**
     * Rule ID: ORDER_DELIVERED — delivery confirmations. Strength: STRONG (80).
     */
    private val ORDER_DELIVERED = ClassificationRule(
        id = "ORDER_DELIVERED",
        category = ClassifierCategory.RECEIPTS_ORDERS,
        weight = 80,
        description = "Delivery confirmation",
        explanationTemplate = "Confirms delivery",
        match = { s ->
            subjectOrBody(
                "delivered_phrase",
                listOf(
                    "has been delivered", "delivery confirmed",
                    "successfully delivered", "your delivery",
                ),
                s, SignalStrength.STRONG,
            )
        },
    )

    /**
     * Rule ID: ORDER_INVOICE — invoices / receipts / refunds.
     * Strength: STRONG (85). Signals: invoice phrases + INVOICE_LIKE
     * attachment hint (corroborating, MEDIUM).
     */
    private val ORDER_INVOICE = ClassificationRule(
        id = "ORDER_INVOICE",
        category = ClassifierCategory.RECEIPTS_ORDERS,
        weight = 85,
        description = "Invoice, receipt, or refund record",
        explanationTemplate = "Contains invoice / receipt / refund evidence",
        match = { s ->
            val out = subjectOrBody(
                "invoice_phrase",
                listOf(
                    "invoice", "your receipt", "payment receipt",
                    "receipt for your", "payment received", "refund",
                    "refunded", "refund confirmation",
                ),
                s, SignalStrength.STRONG,
            ).toMutableList()
            if ("INVOICE_LIKE" in s.attachmentHints) {
                out.add(
                    MatchedSignal(
                        "invoice_attachment",
                        "Attachment looks like an invoice or receipt",
                        SignalStrength.MEDIUM,
                    ),
                )
            }
            out
        },
    )

    // ------------------------------------------------------------------
    // CAREER (precedence 4)
    // ------------------------------------------------------------------

    /**
     * Rule ID: CAREER_INTERVIEW — interview invitations/scheduling.
     * Strength: STRONG (90). Signals: interview + scheduling phrases.
     */
    private val CAREER_INTERVIEW = ClassificationRule(
        id = "CAREER_INTERVIEW",
        category = ClassifierCategory.CAREER,
        weight = 90,
        description = "Interview invitation or scheduling",
        explanationTemplate = "Concerns a job interview",
        match = { s ->
            val text = s.subjectNorm + " " + s.bodyNorm
            val out = mutableListOf<MatchedSignal>()
            if (text.contains("interview")) {
                val schedulers = listOf(
                    "invitation", "schedule", "scheduled", "request",
                    "invite", "round",
                )
                val hit = schedulers.firstOrNull { text.contains(it) }
                if (hit != null) {
                    out.add(
                        MatchedSignal(
                            "interview_scheduling",
                            "Mentions \"interview\" with \"$hit\"",
                            SignalStrength.STRONG,
                        ),
                    )
                }
            }
            out
        },
    )

    /**
     * Rule ID: CAREER_RECRUITER — recruiter outreach.
     * Strength: STRONG (80). Signals: recruiter phrases or career-platform
     * sender domain. Limitations: domain list is intentionally small (phase
     * §20); Phase 8 enriches sender intelligence.
     */
    private val CAREER_RECRUITER = ClassificationRule(
        id = "CAREER_RECRUITER",
        category = ClassifierCategory.CAREER,
        weight = 80,
        description = "Recruiter outreach",
        explanationTemplate = "Comes from a recruiter or hiring contact",
        match = { s ->
            val out = subjectOrBody(
                "recruiter_phrase",
                listOf(
                    "recruiter", "talent acquisition", "hiring manager",
                    "reached out", "opportunity for you",
                ),
                s, SignalStrength.STRONG,
            ).toMutableList()
            if (s.senderDomain in DomainSignals.CAREER) {
                out.add(
                    MatchedSignal(
                        "career_platform_sender",
                        "Sender domain is a known job platform (${s.senderDomain})",
                        SignalStrength.STRONG,
                    ),
                )
            }
            out
        },
    )

    /**
     * Rule ID: CAREER_JOB_POSTING — job openings / alerts.
     * Strength: STRONG (75).
     */
    private val CAREER_JOB_POSTING = ClassificationRule(
        id = "CAREER_JOB_POSTING",
        category = ClassifierCategory.CAREER,
        weight = 75,
        description = "Job opening or job alert",
        explanationTemplate = "Describes a job opening",
        match = { s ->
            subjectOrBody(
                "job_posting_phrase",
                listOf(
                    "job alert", "job opening", "we're hiring", "we are hiring",
                    "new position", "job opportunity", "open role",
                ),
                s, SignalStrength.STRONG,
            )
        },
    )

    /**
     * Rule ID: CAREER_APPLICATION — application status updates.
     * Strength: STRONG (80).
     */
    private val CAREER_APPLICATION = ClassificationRule(
        id = "CAREER_APPLICATION",
        category = ClassifierCategory.CAREER,
        weight = 80,
        description = "Job application status",
        explanationTemplate = "Updates a job application",
        match = { s ->
            subjectOrBody(
                "application_phrase",
                listOf(
                    "application received", "application status",
                    "thank you for applying", "your application",
                    "applied for",
                ),
                s, SignalStrength.STRONG,
            )
        },
    )

    // ------------------------------------------------------------------
    // EDUCATION (precedence 5)
    // ------------------------------------------------------------------

    /**
     * Rule ID: EDUCATION_INSTITUTION — .edu or learning-platform sender.
     * Strength: STRONG (75). Negative-signal note (phase §40): a bare
     * promotional keyword ("sale") in the body does not override this —
     * institution senders outscore promotion rules by weight.
     */
    private val EDUCATION_INSTITUTION = ClassificationRule(
        id = "EDUCATION_INSTITUTION",
        category = ClassifierCategory.EDUCATION,
        weight = 75,
        description = "Educational institution / learning platform sender",
        explanationTemplate = "Sender is an educational institution or learning platform",
        match = { s ->
            val out = mutableListOf<MatchedSignal>()
            if (s.senderDomain.endsWith(".edu")) {
                out.add(
                    MatchedSignal(
                        "edu_domain",
                        "Sender domain is educational (${s.senderDomain})",
                        SignalStrength.STRONG,
                    ),
                )
            }
            if (s.senderDomain in DomainSignals.EDUCATION_PLATFORMS) {
                out.add(
                    MatchedSignal(
                        "learning_platform_sender",
                        "Sender domain is a learning platform (${s.senderDomain})",
                        SignalStrength.STRONG,
                    ),
                )
            }
            out
        },
    )

    /**
     * Rule ID: EDUCATION_ASSIGNMENT — assignments / homework.
     * Strength: STRONG (80).
     */
    private val EDUCATION_ASSIGNMENT = ClassificationRule(
        id = "EDUCATION_ASSIGNMENT",
        category = ClassifierCategory.EDUCATION,
        weight = 80,
        description = "Assignment / homework notice",
        explanationTemplate = "Concerns an assignment or homework",
        match = { s ->
            val out = subjectOrBody(
                "assignment_phrase",
                listOf("assignment", "homework", "problem set"),
                s, SignalStrength.STRONG,
            ).toMutableList()
            if ("ACADEMIC_LIKE" in s.attachmentHints) {
                out.add(
                    MatchedSignal(
                        "academic_attachment",
                        "Attachment looks like coursework",
                        SignalStrength.MEDIUM,
                    ),
                )
            }
            out
        },
    )

    /**
     * Rule ID: EDUCATION_EXAM — exams / quizzes. Strength: STRONG (80).
     */
    private val EDUCATION_EXAM = ClassificationRule(
        id = "EDUCATION_EXAM",
        category = ClassifierCategory.EDUCATION,
        weight = 80,
        description = "Exam / quiz notice",
        explanationTemplate = "Concerns an exam or quiz",
        match = { s ->
            subjectOrBody(
                "exam_phrase",
                listOf("exam", "midterm", "final exam", "quiz"),
                s, SignalStrength.STRONG,
            )
        },
    )

    /**
     * Rule ID: EDUCATION_COURSE — course / lecture / enrollment.
     * Strength: STRONG (75).
     */
    private val EDUCATION_COURSE = ClassificationRule(
        id = "EDUCATION_COURSE",
        category = ClassifierCategory.EDUCATION,
        weight = 75,
        description = "Course / lecture / enrollment update",
        explanationTemplate = "Concerns a course or enrollment",
        match = { s ->
            subjectOrBody(
                "course_phrase",
                listOf(
                    "course", "lecture", "enrollment", "syllabus",
                    "semester", "class schedule",
                ),
                s, SignalStrength.STRONG,
            )
        },
    )

    // ------------------------------------------------------------------
    // IMPORTANT (precedence 6 — material but unspecific)
    // ------------------------------------------------------------------

    /**
     * Rule ID: IMPORTANT_PERSONAL — direct personal correspondence.
     * Strength: MEDIUM (50). Signals: a named human sender, no
     * noreply/unsubscribe/list markers, message addressed to the user.
     * This is intentionally the weakest "positive" bucket: anything more
     * specific wins by weight (phase §39).
     */
    private val IMPORTANT_PERSONAL = ClassificationRule(
        id = "IMPORTANT_PERSONAL",
        category = ClassifierCategory.IMPORTANT,
        weight = 50,
        description = "Direct personal correspondence",
        explanationTemplate = "Looks like direct correspondence from a person",
        match = { s ->
            val out = mutableListOf<MatchedSignal>()
            val addr = s.senderAddress
            val isNoReply = addr.startsWith("noreply") ||
                addr.startsWith("no-reply") ||
                addr.startsWith("donotreply") ||
                addr.startsWith("do-not-reply")
            val isPersonLike = !isNoReply &&
                s.senderNameNorm.isNotBlank() &&
                !s.hasUnsubscribe &&
                "CATEGORY_PROMOTIONS" !in s.gmailCategories
            if (isPersonLike) {
                out.add(
                    MatchedSignal(
                        "personal_sender",
                        "From a named sender with no bulk-mail markers",
                        SignalStrength.MEDIUM,
                    ),
                )
            }
            out
        },
    )

    /**
     * Rule ID: IMPORTANT_RECURRING_SENDER — mail from a sender the user
     * actually corresponds with (Phase 8).
     * Strength: MEDIUM (45) — deliberately below IMPORTANT_PERSONAL (50)
     * so direct personal correspondence wins ties; above nothing else in
     * the IMPORTANT bucket. Signals: the real recurring-sender flag from
     * local sender frequency ([SenderIntelligence]), with bulk-mail
     * markers as a veto — a recurring newsletter is still a newsletter,
     * and noreply senders are never "correspondents".
     * This is a frequency heuristic, documented as such in the
     * explanation — never a probability or a learned model.
     */
    private val IMPORTANT_RECURRING_SENDER = ClassificationRule(
        id = "IMPORTANT_RECURRING_SENDER",
        category = ClassifierCategory.IMPORTANT,
        weight = 45,
        description = "Mail from a recurring sender",
        explanationTemplate = "From someone who emails you regularly",
        match = { s ->
            val out = mutableListOf<MatchedSignal>()
            val addr = s.senderAddress
            val isNoReply = addr.startsWith("noreply") ||
                addr.startsWith("no-reply") ||
                addr.startsWith("donotreply") ||
                addr.startsWith("do-not-reply")
            val isBulkLike = s.hasUnsubscribe ||
                "CATEGORY_PROMOTIONS" in s.gmailCategories
            if (s.isRecurringSender && !isNoReply && !isBulkLike) {
                out.add(
                    MatchedSignal(
                        "recurring_sender",
                        "Sender has emailed repeatedly (local frequency signal)",
                        SignalStrength.MEDIUM,
                    ),
                )
            }
            out
        },
    )

    // ------------------------------------------------------------------
    // NEWSLETTERS (precedence 7)
    // ------------------------------------------------------------------

    /**
     * Rule ID: NEWSLETTER_UNSUBSCRIBE — unsubscribe footer + newsletter
     * indicators. Strength: MEDIUM (60). Unsubscribe alone is WEAK and never
     * decides by itself (phase §25) — explicit newsletter self-identification
     * must corroborate. Words like "monthly"/"weekly" alone are NOT enough
     * (a bank's "monthly statement" is not a newsletter); the
     * NEWSLETTER_FORMAT rule covers "weekly digest"-style phrasing.
     */
    private val NEWSLETTER_UNSUBSCRIBE = ClassificationRule(
        id = "NEWSLETTER_UNSUBSCRIBE",
        category = ClassifierCategory.NEWSLETTERS,
        weight = 60,
        description = "Newsletter with unsubscribe mechanism",
        explanationTemplate = "Has newsletter markers and an unsubscribe option",
        match = { s ->
            val out = mutableListOf<MatchedSignal>()
            if (s.hasUnsubscribe) {
                out.add(
                    MatchedSignal(
                        "unsubscribe_footer",
                        "Contains an unsubscribe option",
                        SignalStrength.WEAK,
                    ),
                )
                val markers = listOf("newsletter", "digest")
                val text = s.subjectNorm + " " + s.bodyNorm
                val hit = markers.firstOrNull { text.contains(it) }
                if (hit != null) {
                    out.add(
                        MatchedSignal(
                            "newsletter_marker",
                            "Mentions \"$hit\"",
                            SignalStrength.MEDIUM,
                        ),
                    )
                }
                // Require the corroborating marker: unsubscribe alone fires nothing.
                if (out.none { it.name == "newsletter_marker" }) out.clear()
            }
            out
        },
    )

    /**
     * Rule ID: NEWSLETTER_FORMAT — explicit newsletter wording.
     * Strength: MEDIUM (55).
     */
    private val NEWSLETTER_FORMAT = ClassificationRule(
        id = "NEWSLETTER_FORMAT",
        category = ClassifierCategory.NEWSLETTERS,
        weight = 55,
        description = "Explicit newsletter format",
        explanationTemplate = "Identifies itself as a newsletter or digest",
        match = { s ->
            subjectOrBody(
                "newsletter_format_phrase",
                listOf(
                    "newsletter", "weekly digest", "daily digest",
                    "monthly roundup", "this week in",
                ),
                s, SignalStrength.MEDIUM,
            )
        },
    )

    // ------------------------------------------------------------------
    // PROMOTIONS (precedence 8)
    // ------------------------------------------------------------------

    /**
     * Rule ID: PROMOTION_DISCOUNT — discount / coupon language.
     * Strength: MEDIUM (50). Phrases are multi-word so a lone "sale" inside
     * an unrelated sentence cannot fire this rule (phase §21).
     */
    private val PROMOTION_DISCOUNT = ClassificationRule(
        id = "PROMOTION_DISCOUNT",
        category = ClassifierCategory.PROMOTIONS,
        weight = 50,
        description = "Discount / coupon promotion",
        explanationTemplate = "Offers a discount or coupon",
        match = { s ->
            subjectOrBody(
                "discount_phrase",
                listOf(
                    "discount", "% off", "percent off", "promo code",
                    "coupon", "save 20", "save 50", "use code",
                ),
                s, SignalStrength.MEDIUM,
            )
        },
    )

    /**
     * Rule ID: PROMOTION_SALE — sale / limited-offer language.
     * Strength: MEDIUM (50).
     */
    private val PROMOTION_SALE = ClassificationRule(
        id = "PROMOTION_SALE",
        category = ClassifierCategory.PROMOTIONS,
        weight = 50,
        description = "Sale / limited-time offer",
        explanationTemplate = "Advertises a sale or limited offer",
        match = { s ->
            subjectOrBody(
                "sale_phrase",
                listOf(
                    "clearance", "limited time", "offer ends",
                    "big sale", "sale ends", "on sale now",
                ),
                s, SignalStrength.MEDIUM,
            )
        },
    )

    /**
     * Rule ID: PROMOTION_GMAIL_CATEGORY — Gmail's PROMOTIONS tab + marketing
     * call-to-action. Strength: MEDIUM (45). The Gmail label alone never
     * decides the Mail Organizer category (phase §23) — marketing wording
     * must corroborate.
     */
    private val PROMOTION_GMAIL_CATEGORY = ClassificationRule(
        id = "PROMOTION_GMAIL_CATEGORY",
        category = ClassifierCategory.PROMOTIONS,
        weight = 45,
        description = "Gmail Promotions tab with marketing call-to-action",
        explanationTemplate = "Gmail filed it under Promotions and it reads like marketing",
        match = { s ->
            val out = mutableListOf<MatchedSignal>()
            if ("CATEGORY_PROMOTIONS" in s.gmailCategories) {
                out.add(
                    MatchedSignal(
                        "gmail_promotions_tab",
                        "Gmail filed it under Promotions",
                        SignalStrength.WEAK,
                    ),
                )
                val ctas = listOf("shop now", "buy now", "exclusive offer", "order now")
                val text = s.subjectNorm + " " + s.bodyNorm
                val hit = ctas.firstOrNull { text.contains(it) }
                if (hit != null) {
                    out.add(
                        MatchedSignal(
                            "marketing_cta",
                            "Contains marketing call-to-action \"$hit\"",
                            SignalStrength.MEDIUM,
                        ),
                    )
                }
                if (out.none { it.name == "marketing_cta" }) out.clear()
            }
            out
        },
    )

    // ------------------------------------------------------------------
    // NOTIFICATIONS (precedence 9 — routine service pings)
    // ------------------------------------------------------------------

    /**
     * Rule ID: NOTIFICATION_SOCIAL — social-network activity.
     * Strength: MEDIUM (40). Signals: CATEGORY_SOCIAL label or known social
     * domain. Drives the Social drawer destination.
     */
    private val NOTIFICATION_SOCIAL = ClassificationRule(
        id = "NOTIFICATION_SOCIAL",
        category = ClassifierCategory.NOTIFICATIONS,
        weight = 40,
        description = "Social-network notification",
        explanationTemplate = "Notifies about social-network activity",
        match = { s ->
            val out = mutableListOf<MatchedSignal>()
            if ("CATEGORY_SOCIAL" in s.gmailCategories) {
                out.add(
                    MatchedSignal(
                        "gmail_social_tab",
                        "Gmail filed it under Social",
                        SignalStrength.MEDIUM,
                    ),
                )
            }
            if (s.senderDomain in DomainSignals.SOCIAL) {
                out.add(
                    MatchedSignal(
                        "social_sender",
                        "Sender is a social network (${s.senderDomain})",
                        SignalStrength.MEDIUM,
                    ),
                )
            }
            val socialWords = listOf(
                "liked your", "started following", "tagged you",
                "mentioned you", "new follower", "friend request",
            )
            val text = s.subjectNorm + " " + s.bodyNorm
            val hit = socialWords.firstOrNull { text.contains(it) }
            if (hit != null) {
                out.add(
                    MatchedSignal(
                        "social_activity_phrase",
                        "Mentions \"$hit\"",
                        SignalStrength.MEDIUM,
                    ),
                )
            }
            out
        },
    )

    /**
     * Rule ID: NOTIFICATION_SERVICE — routine service/application alerts.
     * Strength: WEAK+ (35). Signals: CATEGORY_UPDATES, known service senders,
     * generic notification wording.
     */
    private val NOTIFICATION_SERVICE = ClassificationRule(
        id = "NOTIFICATION_SERVICE",
        category = ClassifierCategory.NOTIFICATIONS,
        weight = 35,
        description = "Routine service / application notification",
        explanationTemplate = "Routine notification from a service",
        match = { s ->
            val out = mutableListOf<MatchedSignal>()
            if ("CATEGORY_UPDATES" in s.gmailCategories) {
                out.add(
                    MatchedSignal(
                        "gmail_updates_tab",
                        "Gmail filed it under Updates",
                        SignalStrength.WEAK,
                    ),
                )
            }
            if (s.senderDomain in DomainSignals.SERVICE_NOTIFICATIONS) {
                out.add(
                    MatchedSignal(
                        "service_sender",
                        "Sender is a developer/service platform (${s.senderDomain})",
                        SignalStrength.MEDIUM,
                    ),
                )
            }
            val text = s.subjectNorm + " " + s.bodyNorm
            val hit = listOf("[github]", "notification:", "build succeeded", "build failed")
                .firstOrNull { text.contains(it) }
            if (hit != null) {
                out.add(
                    MatchedSignal(
                        "service_notification_phrase",
                        "Mentions \"$hit\"",
                        SignalStrength.WEAK,
                    ),
                )
            }
            out
        },
    )

    // ------------------------------------------------------------------
    // LOW VALUE (precedence 10 — conservative noise bucket, phase §41)
    // ------------------------------------------------------------------

    /**
     * Rule ID: LOW_VALUE_NOREPLY_NOISE — automated noise with no other
     * evidence. Strength: WEAK (20). Conservative by design: it only fires
     * when NO other rule fired (enforced in the classifier, not here) and
     * the mail is noreply-automated. Never a universal unknown bucket.
     */
    private val LOW_VALUE_NOREPLY_NOISE = ClassificationRule(
        id = "LOW_VALUE_NOREPLY_NOISE",
        category = ClassifierCategory.LOW_VALUE,
        weight = 20,
        description = "Automated noreply noise with no other signal",
        explanationTemplate = "Automated bulk mail with no meaningful signal",
        match = { s ->
            val out = mutableListOf<MatchedSignal>()
            val addr = s.senderAddress
            val isNoReply = addr.startsWith("noreply") ||
                addr.startsWith("no-reply") ||
                addr.startsWith("donotreply") ||
                addr.startsWith("do-not-reply")
            if (isNoReply) {
                val noise = listOf(
                    "do not reply", "this is an automated", "auto-generated",
                    "system generated",
                )
                val text = s.subjectNorm + " " + s.bodyNorm
                val hit = noise.firstOrNull { text.contains(it) }
                if (hit != null) {
                    out.add(
                        MatchedSignal(
                            "noreply_noise",
                            "Automated noreply mail (\"$hit\")",
                            SignalStrength.WEAK,
                        ),
                    )
                }
            }
            out
        },
    )

    /**
     * The registered rule set, in evaluation order. Order here does NOT
     * decide conflicts — scoring plus [DeterministicClassifier.PRECEDENCE]
     * does (phase §13). Kept as a list so rules stay independently
     * testable and append-only.
     */
    val all: List<ClassificationRule> = listOf(
        // Security — precedence 1
        SECURITY_OTP,
        SECURITY_LOGIN_ALERT,
        SECURITY_PASSWORD_RESET,
        SECURITY_SUSPICIOUS,
        SECURITY_VERIFY_ACCOUNT,
        // Action Required — precedence 2
        ACTION_DEADLINE,
        ACTION_PAYMENT_DUE,
        ACTION_DOC_REQUEST,
        // Receipts & Orders — precedence 3
        ORDER_CONFIRMATION,
        ORDER_SHIPPED,
        ORDER_DELIVERED,
        ORDER_INVOICE,
        // Career — precedence 4
        CAREER_INTERVIEW,
        CAREER_RECRUITER,
        CAREER_JOB_POSTING,
        CAREER_APPLICATION,
        // Education — precedence 5
        EDUCATION_INSTITUTION,
        EDUCATION_ASSIGNMENT,
        EDUCATION_EXAM,
        EDUCATION_COURSE,
        // Important — precedence 6
        IMPORTANT_PERSONAL,
        IMPORTANT_RECURRING_SENDER,
        // Newsletters — precedence 7
        NEWSLETTER_UNSUBSCRIBE,
        NEWSLETTER_FORMAT,
        // Promotions — precedence 8
        PROMOTION_DISCOUNT,
        PROMOTION_SALE,
        PROMOTION_GMAIL_CATEGORY,
        // Notifications — precedence 9
        NOTIFICATION_SOCIAL,
        NOTIFICATION_SERVICE,
        // Low Value — precedence 10 (conservative; see classifier gating)
        LOW_VALUE_NOREPLY_NOISE,
    )
}
