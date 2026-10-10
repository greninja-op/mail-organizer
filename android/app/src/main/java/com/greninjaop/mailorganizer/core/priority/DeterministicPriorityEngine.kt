package com.greninjaop.mailorganizer.core.priority

/**
 * Deterministic priority engine (Phase 9).
 *
 * Pipeline:
 * ```
 * PriorityInput
 *        ↓  evaluate every registered rule
 * per-rule matches (rule → signals)
 *        ↓  group by level, sum weights (NORMAL starts at NORMAL_BASE_SCORE)
 * per-level scores
 *        ↓  resolve: highest score wins; ties → PRECEDENCE order
 * PriorityResult
 * ```
 *
 * Scoring discipline (mirrors Phase 7's classifier):
 * - NORMAL carries a base score ([NORMAL_BASE_SCORE]): ordinary mail with no
 *   strong signal stays NORMAL. Priority is never invented.
 * - The winner is the level with the highest total; ties break by explicit
 *   precedence (CRITICAL > HIGH > NORMAL > LOW) — never rule-list order,
 *   never randomness.
 * - LOW_VALUE-style gating is implicit: LOW must outscore NORMAL's base, so
 *   weak noise signals alone cannot demote ordinary mail.
 *
 * Failure safety: [prioritize] is total — unexpected failures degrade to
 * NORMAL (the honest "couldn't determine": neither hiding mail nor crying
 * wolf), never crash the caller.
 *
 * Versioning: [VERSION] stamps every result and is persisted in
 * [com.greninjaop.mailorganizer.data.local.PriorityRecord.version], so
 * future rule changes can reprioritize ([PrioritizeMailboxUseCase] pattern:
 * existing rows with an older version are eligible for re-run).
 */
object DeterministicPriorityEngine {

    /**
     * Engine version. Bump when rules change materially; the use case
     * reprioritizes rows stamped with an older version.
     *
     * v1 (2026-10-09): initial 15-rule deterministic set.
     */
    const val VERSION = 1

    /**
     * Explicit conflict-resolution precedence, highest first.
     * Used ONLY to break score ties — the primary decider is always the
     * summed rule weight.
     */
    val PRECEDENCE: List<PriorityLevel> = listOf(
        PriorityLevel.CRITICAL,
        PriorityLevel.HIGH,
        PriorityLevel.NORMAL,
        PriorityLevel.LOW,
    )

    /**
     * NORMAL's base score: ordinary mail with no strong signal stays NORMAL.
     * A level must beat this to take the result.
     */
    const val NORMAL_BASE_SCORE = 40

    /**
     * Prioritizes one message. Total: never throws — unexpected failures
     * degrade to NORMAL instead of crashing the caller.
     *
     * @param clock injected timestamp source (tests fix it; production
     *   passes System::currentTimeMillis).
     */
    fun prioritize(
        input: PriorityInput,
        clock: () -> Long = System::currentTimeMillis,
    ): PriorityResult {
        return try {
            prioritizeOrThrow(input, clock)
        } catch (t: Throwable) {
            PriorityResult(
                priority = PriorityLevel.NORMAL,
                signals = emptyList(),
                ruleId = null,
                firingRuleIds = emptyList(),
                version = VERSION,
                computedAtEpochMs = safeClock(clock),
                explanation = "Priority computation failed safely; kept normal.",
            )
        }
    }

    private fun prioritizeOrThrow(
        input: PriorityInput,
        clock: () -> Long,
    ): PriorityResult {
        val firings = mutableListOf<Pair<PriorityRule, List<MatchedPrioritySignal>>>()
        val allSignals = mutableListOf<MatchedPrioritySignal>()
        val scores = mutableMapOf<PriorityLevel, Int>()
        scores[PriorityLevel.NORMAL] = NORMAL_BASE_SCORE

        for (rule in PriorityRuleSet.ALL) {
            val signals = rule.match(input)
            if (signals.isNotEmpty()) {
                firings.add(rule to signals)
                scores[rule.level] = (scores[rule.level] ?: 0) + rule.weight
                allSignals.addAll(signals)
            }
        }

        // Resolve: highest score wins; ties → explicit precedence.
        var maxScore = -1
        var winner = PriorityLevel.NORMAL
        for (level in PRECEDENCE) {
            val s = scores[level] ?: 0
            if (s > maxScore) {
                maxScore = s
                winner = level
            }
        }

        // Order firing ids by weight, highest first (stable for ties).
        val byWeight = if (firings.size <= 1) firings else firings.sortedByDescending { it.first.weight }
        val orderedIds = byWeight.map { it.first.id }

        val primaryRule = byWeight.firstOrNull()?.first
        val explanation = buildExplanation(winner, byWeight.map { it.first })

        return PriorityResult(
            priority = winner,
            signals = allSignals,
            ruleId = primaryRule?.id,
            firingRuleIds = orderedIds,
            version = VERSION,
            computedAtEpochMs = safeClock(clock),
            explanation = explanation,
        )
    }

    private fun buildExplanation(
        winner: PriorityLevel,
        rulesByWeight: List<PriorityRule>,
    ): String {
        val levelLabel = when (winner) {
            PriorityLevel.CRITICAL -> "Critical priority"
            PriorityLevel.HIGH -> "High priority"
            PriorityLevel.NORMAL -> "Normal priority"
            PriorityLevel.LOW -> "Low priority"
        }
        if (rulesByWeight.isEmpty()) {
            return "$levelLabel — no strong priority signals; ordinary mail."
        }
        val reasons = rulesByWeight.take(3).map { it.explanationTemplate }
        return "$levelLabel — ${reasons.joinToString("; ")}."
    }

    private fun safeClock(clock: () -> Long): Long = try {
        clock()
    } catch (t: Throwable) {
        0L
    }
}
