# Phase 8 — Secure Storage & Account State

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is iPadOS only. All iPadOS-specific artifacts belong under iPadOS/. Never touch iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Shared KMP business/data logic remains common.

## MISSION

Implement/verify iPad Keychain-backed credential/token state and account metadata boundaries. Define secure storage adapter through common interface, token refresh state, logout/disconnect cleanup and lock/privacy behavior. Verify no secrets enter logs, screenshots, analytics, ordinary DB or SwiftUI state dumps. Test account add/remove, revoked credentials, corrupted secure state, duplicate accounts and cross-account access attempts. Do not build the OAuth UI yet.

## ACCEPTANCE / VALIDATION

Inspect actual repository and prior phase evidence. Implement only this phase. Run targeted unit/integration tests and runtime tests on iPad Simulator and real iPad where available. Exercise success, failure, cancellation and recovery paths appropriate to the phase. Inspect logs and persisted state. Fix, rebuild/reinstall/retest. Record exact evidence. Update status and permanent rules only when genuinely required.

**STOP AFTER THIS PHASE.**