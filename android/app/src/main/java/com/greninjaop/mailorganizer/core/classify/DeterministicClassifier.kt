package com.greninjaop.mailorganizer.core.classify

/**
 * Deterministic classification engine (Phase 7).
 *
 * Pipeline:
 * ```
 * ClassificationInput
 *        ↓  SignalExtractor.extract (bounded, total)
 * ExtractedSignals
 *        ↓  evaluate every registered rule
 * per-rule matches (rule → signals)
 *        ↓  group by category, sum weights
 * per-category scores
 *        ↓  resolve: highest score wins; ties → PRECEDENCE order
 * ClassificationResult
 * ```
 *
 * Conflict resolution (phase §13): scoring first, then an explicit
 * precedence list — never rule-list order, never randomness. Security
 * outranks everything; LOW_VALUE is gated so it can only win when no
 * other category scored (phase §41: never a universal unknown bucket).
 *
 * Confidence (phase §15) is deterministic rule strength, not statistics:
 * - HIGH: winning score ≥ 100 (a strong security/action/order rule fired).
 * - MEDIUM: winning score ≥ 50.
 * - LOW: winning score ≥ 20 (weak evidence only).
 * - Below 20: UNCLASSIFIED with LOW confidence — an honest "don't know".
 *
 * Versioning (phase §18): [VERSION] stamps every result and is persisted in
 * [com.greninjaop.mailorganizer.data.local.ClassificationRecord.version], so
 * future rule changes can reclassify ([ClassifyMailboxUseCase] pattern:
 * existing rows with an older version are eligible for re-run).
 *
 * Message-level classification (phase §35): this engine classifies one
 * message. Thread-level presentation aggregates later without destroying
 * per-message evidence.
 */
object DeterministicClassifier {

    /**
     * Classifier version (phase §18). Bump when rules change materially;
     * the use case reclassifies rows stamped with an older version.
     *
     * v1 (2026-10-09): initial 30-rule deterministic set.
     */
    const val VERSION = 1

    /**
     * Explicit conflict-resolution precedence, highest first (phase §13).
     * Used ONLY to break score ties and to document intent — the primary
     * decider is always the summed rule weight.
     */
    val PRECEDENCE: List<ClassifierCategory> = listOf(
        ClassifierCategory.SECURITY,
        ClassifierCategory.ACTION_REQUIRED,
        ClassifierCategory.RECEIPTS_ORDERS,
        ClassifierCategory.CAREER,
        ClassifierCategory.EDUCATION,
        ClassifierCategory.IMPORTANT,
        ClassifierCategory.NEWSLETTERS,
        ClassifierCategory.PROMOTIONS,
        ClassifierCategory.NOTIFICATIONS,
        ClassifierCategory.LOW_VALUE,
        ClassifierCategory.UNCLASSIFIED,
    )

    private const val HIGH_THRESHOLD = 100
    private const val MEDIUM_THRESHOLD = 50
    private const val MIN_SCORE = 20

    /**
     * Classifies one normalized email. Total: never throws — unexpected
     * failures degrade to UNCLASSIFIED/LOW instead of crashing the caller
     * (phase §56).
     *
     * @param clock injected timestamp source (tests fix it; production
     *   passes System::currentTimeMillis).
     */
    fun classify(
        input: ClassificationInput,
        clock: () -> Long = System::currentTimeMillis,
    ): ClassificationResult {
        return try {
            classifyOrThrow(input, clock)
        } catch (t: Throwable) {
            ClassificationResult(
                category = ClassifierCategory.UNCLASSIFIED,
                confidence = Confidence.LOW,
                matchedSignals = emptyList(),
                ruleId = null,
                firingRuleIds = emptyList(),
                classifierVersion = VERSION,
                classifiedAtEpochMs = safeClock(clock),
                explanation = "Classification failed safely; kept unclassified.",
            )
        }
    }

    private fun classifyOrThrow(
        input: ClassificationInput,
        clock: () -> Long,
    ): ClassificationResult {
        val signals = SignalExtractor.extract(input)
        val now = safeClock(clock)

        // 1. Evaluate every rule independently (order-independent).
        val firing = ClassificationRuleSet.all.mapNotNull { rule ->
            val matched = rule.match(signals)
            if (matched.isEmpty()) null else rule to matched
        }

        if (firing.isEmpty()) {
            return ClassificationResult(
                category = ClassifierCategory.UNCLASSIFIED,
                confidence = Confidence.LOW,
                matchedSignals = emptyList(),
                ruleId = null,
                firingRuleIds = emptyList(),
                classifierVersion = VERSION,
                classifiedAtEpochMs = now,
                explanation = "No classification rule matched; left unclassified.",
            )
        }

        // 2. Score per category (LinkedHashMap: deterministic iteration).
        val scores = LinkedHashMap<ClassifierCategory, Int>()
        val signalsByCategory = LinkedHashMap<ClassifierCategory, MutableList<MatchedSignal>>()
        val rulesByCategory = LinkedHashMap<ClassifierCategory, MutableList<ClassificationRule>>()
        for ((rule, matched) in firing) {
            scores[rule.category] = (scores[rule.category] ?: 0) + rule.weight
            signalsByCategory.getOrPut(rule.category) { mutableListOf() }.addAll(matched)
            rulesByCategory.getOrPut(rule.category) { mutableListOf() }.add(rule)
        }

        // 3. LOW_VALUE gate (phase §41): it may only win when nothing else
        //    scored — never as a dump for uncertain mail.
        val contenders = if (scores.size > 1) {
            scores.filterKeys { it != ClassifierCategory.LOW_VALUE }
        } else {
            scores
        }

        // 4. Resolve: highest score; ties → PRECEDENCE order (deterministic).
        val precedenceRank = PRECEDENCE.withIndex().associate { (i, c) -> c to i }
        val winner = contenders.entries
            .sortedWith(
                compareByDescending<Map.Entry<ClassifierCategory, Int>> { it.value }
                    .thenBy { precedenceRank[it.key] ?: Int.MAX_VALUE },
            )
            .first()
            .key

        val winningScore = scores[winner] ?: 0
        val confidence = when {
            winningScore >= HIGH_THRESHOLD -> Confidence.HIGH
            winningScore >= MEDIUM_THRESHOLD -> Confidence.MEDIUM
            winningScore >= MIN_SCORE -> Confidence.LOW
            else -> Confidence.LOW
        }

        // Below MIN_SCORE even the winner is too weak: stay unclassified.
        if (winningScore < MIN_SCORE) {
            return ClassificationResult(
                category = ClassifierCategory.UNCLASSIFIED,
                confidence = Confidence.LOW,
                matchedSignals = emptyList(),
                ruleId = null,
                firingRuleIds = emptyList(),
                classifierVersion = VERSION,
                classifiedAtEpochMs = now,
                explanation = "Only weak signals matched; left unclassified rather than guessing.",
            )
        }

        val winnerRules = (rulesByCategory[winner] ?: emptyList())
            .sortedByDescending { it.weight }
        val winnerSignals = (signalsByCategory[winner] ?: emptyList())
            .sortedWith(
                compareBy<MatchedSignal> { strengthRank(it.strength) }
                    .thenBy { it.name },
            )
        val primaryRule = winnerRules.firstOrNull()

        return ClassificationResult(
            category = winner,
            confidence = confidence,
            matchedSignals = winnerSignals,
            ruleId = primaryRule?.id,
            firingRuleIds = firing.sortedByDescending { it.first.weight }
                .map { it.first.id },
            classifierVersion = VERSION,
            classifiedAtEpochMs = now,
            explanation = buildExplanation(winner, primaryRule, winnerSignals),
        )
    }

    private fun strengthRank(s: SignalStrength): Int = when (s) {
        SignalStrength.STRONG -> 0
        SignalStrength.MEDIUM -> 1
        SignalStrength.WEAK -> 2
    }

    private fun buildExplanation(
        category: ClassifierCategory,
        primaryRule: ClassificationRule?,
        signals: List<MatchedSignal>,
    ): String {
        val label = category.name.lowercase().replace('_', ' ')
            .replaceFirstChar { it.uppercase() }
        val reasons = signals.take(3).joinToString("; ") { it.detail }
        val rulePart = if (primaryRule != null) " [${primaryRule.id}]" else ""
        return if (reasons.isEmpty()) {
            "Classified as $label$rulePart."
        } else {
            "Classified as $label$rulePart: $reasons."
        }
    }

    private fun safeClock(clock: () -> Long): Long = try {
        clock()
    } catch (t: Throwable) {
        0L
    }
}
