# Phase 9 — Google OAuth & Account Connection

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is iPadOS only. All iPadOS-specific artifacts belong under iPadOS/. Never touch iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Shared KMP business/data logic remains common.

## MISSION

Implement official Google OAuth for iPadOS using browser/system authentication mechanisms. Configure callback/deep-link state, least-privilege Gmail scopes, incremental authorization, cancellation, retry, token refresh/expiry, account identity and disconnect/revoke. Never collect Gmail passwords or build a fake Google credential form. Validate success, denial, cancellation, invalid callback/state, expired token, revoked access, second-account connection and logs. Do not implement broad Gmail synchronization.

## ACCEPTANCE / VALIDATION

Inspect actual repository and prior phase evidence. Implement only this phase. Run targeted unit/integration tests and runtime tests on iPad Simulator and real iPad where available. Exercise success, failure, cancellation and recovery paths appropriate to the phase. Inspect logs and persisted state. Fix, rebuild/reinstall/retest. Record exact evidence. Update status and permanent rules only when genuinely required.

**STOP AFTER THIS PHASE.**