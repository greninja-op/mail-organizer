# Phase 03 — Compose Tablet Shell & Lifecycle

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is an **Android tablet-only** phase. All tablet-specific source, resources, tests, configuration and documentation belong under `AndroidTablet/`. Never create or modify `Android/`, `iOS/`, `iPadOS/`, `macOS/`, `Windows/` or `Linux/`.

Shared KMP/Android business and data logic remains common. Do not duplicate common engines merely because the tablet UI differs.

## Mission

Build the tablet Compose shell, activity/lifecycle integration, root navigation state, dependency bootstrap, loading/empty/error/offline/auth-required primitives and state restoration foundation. Keep business logic out of composables. Validate cold/warm launch, recreation, process death, rotation, dark mode, large fonts and multiple tablet sizes. Do not implement Gmail features.

## Required execution protocol

Read the root execution instructions and all AndroidTablet master documents. Determine the first incomplete AndroidTablet phase and read only this prompt. Inspect the actual repository and previous evidence before implementation.

Implement **only this phase**. Do not begin later phases.

Run targeted automated tests, then Android tablet emulator validation and a physical tablet where available. Test the relevant success, empty, loading, offline, error, cancellation, process-death and recovery states. For UI phases, test multiple widths/orientations and input modes. Perform relevant accessibility, security, data-integrity and performance checks.

Fix failures. Rebuild/reinstall/retest. Record exact commands and evidence. Update phase status and add only genuinely permanent new editor rules without deleting unrelated rules.

Never claim a test, screenshot, log, device result or performance measurement that was not actually performed.

**STOP AFTER PHASE 03.**