# Phase 11 — Initial Gmail Sync

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is iPadOS only. All iPadOS-specific artifacts belong under iPadOS/. Never touch iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Shared KMP business/data logic remains common.

## MISSION

Implement first-account/full synchronization using the existing shared sync architecture. Fetch in bounded pages, persist atomically, expose per-account progress/state and support cancellation/resume. Define honest determinate progress only where a trustworthy denominator exists; otherwise indeterminate. Test empty mailbox, large mailbox, pagination boundaries, duplicate prevention, partial failure, expired auth, cancellation and restart. Validate unified data does not leak between accounts.

## ACCEPTANCE / VALIDATION

Inspect the actual repository and prior evidence. Implement only this phase. Run targeted automated tests, then iPad Simulator and real iPad validation where available. Test multiple viewport states relevant to the feature and exercise failure/recovery paths. Fix, rebuild/reinstall/retest and record exact evidence. Update status/rules only when genuinely required.

**STOP AFTER THIS PHASE.**