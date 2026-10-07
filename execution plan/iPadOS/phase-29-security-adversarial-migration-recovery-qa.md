# Phase 29 — Security, Adversarial, Migration & Recovery QA

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is iPadOS only. All iPadOS-specific implementation, tests, assets, configuration and documentation belong under iPadOS/. Never create or modify iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Shared KMP/mobile business and data logic remains common.

## MISSION

Perform a dedicated hostile/recovery pass. Test malicious email HTML/URLs, prompt injection, malformed provider responses, OAuth callback attacks, token/log leakage, cross-account access, database corruption, migration failures, interrupted sync, cursor/history gaps, process death, reboot, storage pressure, stale cache, notification privacy, account removal and partial external-action failures. Verify fail-closed behavior and data integrity. No feature expansion except fixes required by findings.

## REQUIRED EXECUTION PROTOCOL

Read root instructions and all iPadOS master documents. Inspect the actual repository and prior phase evidence before implementation. Implement only this phase. Build and run targeted automated tests. Validate on iPad Simulator and a real iPad where available. Exercise success, empty, loading, offline, error, cancellation and interruption states relevant to the phase. Perform visual, accessibility, security, data-integrity and performance checks relevant to the feature. Fix failures, rebuild/reinstall/retest, record exact evidence, update status and permanent rules only when genuinely required.

Never claim unrun tests, screenshots, logs, device results or performance measurements.

**STOP AFTER THIS PHASE.**