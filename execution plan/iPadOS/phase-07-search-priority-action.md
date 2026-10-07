# Phase 7 — Local Persistence & State Hydration

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is iPadOS only. All iPadOS-specific artifacts belong under iPadOS/. Never touch iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Shared KMP business/data logic remains common.

## MISSION

Connect the existing common persistence/state layer to iPadOS. Define startup hydration, account-scoped caches, schema/migration handling, state invalidation and corruption recovery. Secrets must remain outside ordinary DB state. Verify restart, process termination, migration, corrupted DB, partial writes, account removal and reinstall semantics. Test that the iPad workspace can restore list/detail/navigation state without confusing persisted data with live Gmail truth. Do not implement Gmail sync yet.

## ACCEPTANCE / VALIDATION

Inspect actual repository and prior phase evidence. Implement only this phase. Run targeted unit/integration tests and runtime tests on iPad Simulator and real iPad where available. Exercise success, failure, cancellation and recovery paths appropriate to the phase. Inspect logs and persisted state. Fix, rebuild/reinstall/retest. Record exact evidence. Update status and permanent rules only when genuinely required.

**STOP AFTER THIS PHASE.**