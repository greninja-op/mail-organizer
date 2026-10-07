## Mandatory Rust desktop architecture

**Rust is the primary implementation language for this macOS phase.** The reusable Rust desktop core owns platform-neutral business/data/application behavior and must be designed for Windows reuse. Swift/SwiftUI is only the native macOS presentation and OS integration layer; it must not duplicate Rust logic.

# Phase 29 — Security, Adversarial, Migration & Recovery QA

## Mission
Implement and verify only Phase 29. This is a sequential roadmap. Do not implement any later phase.

## Mandatory macOS isolation
Modify only the macOS platform boundary and genuinely shared Rust desktop core/common code required by this phase. Never create, modify, delete, rename, or reorganize iOS, iPadOS, AndroidTablet, Android, Windows, Linux, or other sibling platform areas.

## Required reading and inspection
Read root instructions and execution plan/macOS/README.md, requirements.md, spec.md, design.md, and editor-rules.md. Determine the real current status and inspect the actual repository before editing. Preserve newer repository decisions and do not replace master documents with stale copies.

## Scope
Run adversarial and recovery QA for malicious email/HTML/URLs/prompt injection, cross-account attacks, token leakage, destructive actions, automation loops, stale cursors, database corruption, migrations, interrupted writes, offline conflicts, process death, sleep/wake, partial sync and account removal.

## Engineering contract
Use native macOS/SwiftUI patterns and the shared Rust desktop core core where appropriate. Preserve Gmail as source of truth, explicit account boundaries, least-privilege OAuth, secure credentials, untrusted-email handling, deterministic/local/explainable behavior before AI, and confirmation/idempotency for consequential external actions.

## Verification gate
Run targeted automated tests and relevant integration tests. Build the real macOS application with the supported toolchain. Launch and exercise it when the environment permits. Test realistic success, empty, loading, error, cancellation, offline, restart, sleep/wake, recovery and partial-failure states relevant to this phase. Check accessibility, keyboard/pointer behavior, security, data integrity, logs, memory/performance and release configuration as applicable. Fix defects, rebuild/relaunch/reinstall where appropriate, and retest.

Never claim tests, screenshots, runtime results, performance metrics, signing, notarization, or device/OS checks that were not actually performed. Record blockers honestly.

## Documentation
Update phase status accurately. Add only genuinely permanent reusable rules to macOS editor-rules.md and never delete unrelated rules.

**STOP AFTER PHASE 29.**