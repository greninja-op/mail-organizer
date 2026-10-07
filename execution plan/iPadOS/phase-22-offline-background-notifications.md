# Phase 22 — Offline, Background Refresh & Notifications

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is iPadOS only. All iPadOS-specific implementation, tests, assets, configuration and documentation belong under iPadOS/. Never create or modify iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Shared KMP/mobile business and data logic remains common.

## MISSION

Implement truthful offline/stale behavior, reconnect/reconcile, iPadOS background refresh where justified, notification categories/actions, privacy-safe notification content, duplicate suppression, deep links, auth-required states and battery-aware scheduling. Respect OS scheduling; never promise exact cadence. Test airplane mode, flaky network, suspension, termination, reboot where practical, delayed background execution, denied permissions, duplicate notifications and stale cache.

## REQUIRED EXECUTION PROTOCOL

Read root instructions and all iPadOS master documents. Inspect the actual repository and prior phase evidence before implementation. Implement only this phase. Build and run targeted automated tests. Validate on iPad Simulator and a real iPad where available. Exercise success, empty, loading, offline, error, cancellation and interruption states relevant to the phase. Perform visual, accessibility, security, data-integrity and performance checks relevant to the feature. Fix failures, rebuild/reinstall/retest, record exact evidence, update status and permanent rules only when genuinely required.

Never claim unrun tests, screenshots, logs, device results or performance measurements.

**STOP AFTER THIS PHASE.**