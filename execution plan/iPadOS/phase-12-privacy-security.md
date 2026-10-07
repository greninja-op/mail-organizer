# Phase 12 — Incremental Sync, Pagination & Recovery

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is iPadOS only. All iPadOS-specific artifacts belong under iPadOS/. Never touch iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Shared KMP business/data logic remains common.

## MISSION

Implement incremental Gmail history/cursor processing using the common sync engine. Handle history gaps, token invalidation, deleted/changed messages, pagination, retries/backoff, rate limits, partial account failure and full-resync fallback. Verify idempotency and atomic cursor advancement: never advance a cursor beyond data that was not safely committed. Test interruption at every major boundary, duplicate events, out-of-order events and process termination.

## ACCEPTANCE / VALIDATION

Inspect the actual repository and prior evidence. Implement only this phase. Run targeted automated tests, then iPad Simulator and real iPad validation where available. Test multiple viewport states relevant to the feature and exercise failure/recovery paths. Fix, rebuild/reinstall/retest and record exact evidence. Update status/rules only when genuinely required.

**STOP AFTER THIS PHASE.**