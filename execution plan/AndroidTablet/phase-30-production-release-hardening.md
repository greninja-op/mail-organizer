# Phase 30 — Production Signing, Play Store Preparation & Final Release Hardening

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is an **Android tablet-only** phase. All tablet-specific source, resources, tests, configuration and documentation belong under `AndroidTablet/`. Never create or modify `Android/`, `iOS/`, `iPadOS/`, `macOS/`, `Windows/` or `Linux/`.

Shared KMP/Android business and data logic remains common. Do not duplicate common engines merely because the tablet UI differs.

## Mission

Final gate. Verify all phase evidence and master docs. Audit application identity, signing, Play configuration, OAuth release identity/scopes, permissions, data/privacy declarations, assets, versioning, release config, dependency/security/secret scans, debug/test bypass removal, reproducible artifact and repository hygiene. Clean production build/install and smoke path: launch→OAuth→sync→multi-pane All Inbox→detail→search→company filter→star→Action Required→account switch→offline→reconnect. Verify multiple tablet sizes/window modes and accessibility. RELEASE READY only with evidence; otherwise RELEASE BLOCKED with severity/evidence/remediation. Never publish automatically or claim approval without evidence.

## Required execution protocol

Read the root execution instructions and all AndroidTablet master documents. Determine the first incomplete AndroidTablet phase and read only this prompt. Inspect the actual repository and previous evidence before implementation.

Implement **only this phase**. Do not begin later phases.

Run targeted automated tests, then Android tablet emulator validation and a physical tablet where available. Test relevant success, empty, loading, offline, error, cancellation, process-death and recovery states. For UI phases, test multiple widths/orientations and input modes. Perform relevant accessibility, security, data-integrity and performance checks.

Fix failures. Rebuild/reinstall/retest. Record exact commands and evidence. Update phase status and add only genuinely permanent new editor rules without deleting unrelated rules.

Never claim a test, screenshot, log, device result or performance measurement that was not actually performed.

**STOP AFTER PHASE 30.**