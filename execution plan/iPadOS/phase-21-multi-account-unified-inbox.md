# Phase 21 — Multi-Account & Unified Inbox

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is iPadOS only. All iPadOS-specific implementation, tests, assets, configuration and documentation belong under iPadOS/. Never create or modify iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Shared KMP/mobile business and data logic remains common.

## MISSION

Complete account switching and unified All Inbox. Support current/connected/add-account, account-specific recovery, receiving-account identity, unified list/detail, account-scoped search/rules/company intelligence/actions, account removal and local cleanup. Adding an account must reuse OAuth→sync→recovery. Perform adversarial cross-account search/cache/notification/rule/action/company tests and verify fail-closed isolation.

## REQUIRED EXECUTION PROTOCOL

Read root instructions and all iPadOS master documents. Inspect the actual repository and prior phase evidence before implementation. Implement only this phase. Build and run targeted automated tests. Validate on iPad Simulator and a real iPad where available. Exercise success, empty, loading, offline, error, cancellation and interruption states relevant to the phase. Perform visual, accessibility, security, data-integrity and performance checks relevant to the feature. Fix failures, rebuild/reinstall/retest, record exact evidence, update status and permanent rules only when genuinely required.

Never claim unrun tests, screenshots, logs, device results or performance measurements.

**STOP AFTER THIS PHASE.**