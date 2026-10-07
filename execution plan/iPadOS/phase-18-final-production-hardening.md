# Phase 18 — Rules & User Corrections

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is iPadOS only. All iPadOS-specific artifacts belong under iPadOS/. Never touch iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Shared KMP business/data logic remains common.

## MISSION

Implement iPad rule/correction management over the common rules engine. Support create/edit/disable/delete, scope, precedence, explanation, history/audit where available and immediate correction feedback. User correction > user rule > deterministic intelligence > AI > unknown. Rules are bounded and cannot execute arbitrary scripts. Test contradictory rules, correction rollback, deleted messages, account isolation, malformed rule input and email prompt-injection attempts. Do not build temporal/conversation extraction yet.

## ACCEPTANCE / VALIDATION

Inspect actual repository and prior evidence. Implement only this phase. Run targeted unit/integration tests plus iPad Simulator and real iPad validation where available. Test multiple viewport modes, accessibility and relevant failure/recovery paths. Fix, rebuild/reinstall/retest and record exact evidence. Update status/rules only when genuinely required.

**STOP AFTER THIS PHASE.**