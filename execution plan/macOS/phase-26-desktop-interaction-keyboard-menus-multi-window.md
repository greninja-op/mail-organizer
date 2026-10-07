# Phase 26 — Desktop Interaction, Keyboard, Menus & Multi-Window

## Mission
Implement and verify only Phase 26. This is a sequential roadmap. Do not implement any later phase.

## Mandatory macOS isolation
Modify only the macOS platform boundary and genuinely shared KMP/common code required by this phase. Never create, modify, delete, rename, or reorganize iOS, iPadOS, AndroidTablet, Android, Windows, Linux, or other sibling platform areas.

## Required reading and inspection
Read root instructions and execution plan/macOS/README.md, requirements.md, spec.md, design.md, and editor-rules.md. Determine the real current status and inspect the actual repository before editing. Preserve newer repository decisions and do not replace master documents with stale copies.

## Scope
Complete native desktop interaction: keyboard shortcuts, menu commands, toolbar validation, context menus, selection semantics, pointer/trackpad behavior, safe drag/drop, multiple windows/scenes, state restoration, focus management and workspace consistency.

## Engineering contract
Use native macOS/SwiftUI patterns and the shared KMP core where appropriate. Preserve Gmail as source of truth, explicit account boundaries, least-privilege OAuth, secure credentials, untrusted-email handling, deterministic/local/explainable behavior before AI, and confirmation/idempotency for consequential external actions.

## Verification gate
Run targeted automated tests and relevant integration tests. Build the real macOS application with the supported toolchain. Launch and exercise it when the environment permits. Test realistic success, empty, loading, error, cancellation, offline, restart, sleep/wake, recovery and partial-failure states relevant to this phase. Check accessibility, keyboard/pointer behavior, security, data integrity, logs, memory/performance and release configuration as applicable. Fix defects, rebuild/relaunch/reinstall where appropriate, and retest.

Never claim tests, screenshots, runtime results, performance metrics, signing, notarization, or device/OS checks that were not actually performed. Record blockers honestly.

## Documentation
Update phase status accurately. Add only genuinely permanent reusable rules to macOS editor-rules.md and never delete unrelated rules.

**STOP AFTER PHASE 26.**