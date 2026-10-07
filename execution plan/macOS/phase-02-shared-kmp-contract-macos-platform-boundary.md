## Rust-first desktop requirement

Rust is the primary implementation language for this phase and for the reusable Windows/macOS desktop core. Swift/SwiftUI is limited to native macOS UI and OS integration.

## Mandatory Rust desktop architecture

**Rust is the primary implementation language for this macOS phase.** All platform-neutral business/data/application logic must live in the reusable Rust desktop core. Swift/SwiftUI is reserved for native macOS UI and OS-facing integration. Swift must not duplicate Rust business rules. The Rust core must be designed for reuse by Windows.

# Phase 02 — Shared Rust desktop core Contract & macOS Platform Boundary

## Mission
Implement and verify **only Phase 02** of the Mail Organizer macOS execution plan. This phase is part of a sequential 31-phase roadmap. Do not implement Phase 03 or any later phase early.

## Mandatory macOS isolation
All macOS-specific implementation, SwiftUI UI, resources, tests, entitlements, configuration, scripts, and release artifacts must remain inside the macOS platform boundary. Never create, edit, delete, rename, or reorganize `iOS/`, `iPadOS/`, `AndroidTablet/`, `Android/`, `Windows/`, `Linux/`, or another sibling platform area during this phase. Shared Rust desktop core changes are allowed only when they are genuinely platform-neutral and directly required by this phase.

## Required reading and state discovery
Before editing:
1. Read the root project instructions.
2. Read `execution plan/macOS/README.md`, `requirements.md`, `spec.md`, `design.md`, and `editor-rules.md`.
3. Inspect the actual repository and current implementation.
4. Determine the real current phase/status; do not assume the repository matches this prompt.
5. Review existing architecture, tests, build configuration, and previous evidence relevant to this phase.
6. Preserve newer repository decisions. Do not replace master documents with stale external copies.

## Phase objective
Define and enforce the boundary between shared Rust desktop core and native macOS. Map shared repositories/use cases/models/sync/classification/search/action/integration abstractions to Swift-facing APIs. Prevent duplicated business logic and document ownership of lifecycle, UI, Keychain, notifications, filesystem, and windowing.

## Implementation requirements
- Keep the implementation production-oriented rather than a mock or screenshot-only prototype.
- Reuse existing shared Rust desktop core/domain/data infrastructure where it is correct.
- Do not duplicate business logic in SwiftUI merely for convenience.
- Define explicit boundaries between UI state, application/use-case state, domain models, persistence, and external APIs.
- Handle loading, success, empty, error, cancellation, offline, stale-data, and recovery states wherever applicable.
- Preserve account isolation and Gmail-as-source-of-truth semantics.
- Treat all email content and external data as untrusted.
- Keep secrets out of source, logs, UI, ordinary database records, screenshots, and test fixtures.
- Avoid speculative abstractions for future phases unless a minimal interface is required by this phase.

## macOS UX requirements
Use native SwiftUI/macOS interaction patterns appropriate to the phase:
- resizable desktop windows;
- sidebar/split-view layouts where appropriate;
- toolbar and menu commands;
- keyboard navigation and shortcuts;
- pointer/trackpad interaction;
- context menus;
- VoiceOver/accessibility semantics;
- light/dark mode;
- reduced-motion support;
- responsive behavior across meaningful window sizes.

Do not present a stretched phone/tablet UI as the macOS implementation.

## Testing and verification
Build success alone is insufficient. After implementation:
1. Run targeted unit tests for changed shared and macOS code.
2. Run relevant integration tests.
3. Build the macOS application using the repository's actual supported toolchain.
4. Launch the real application.
5. Exercise the feature with realistic data/state and relevant failure states.
6. Verify persistence/state after restart where applicable.
7. Verify account isolation and security boundaries where applicable.
8. Check accessibility and keyboard/pointer interaction for UI work.
9. Check performance/memory behavior for data-heavy work.
10. Inspect logs for unexpected errors, secrets, tokens, or noisy failures.
11. Take screenshots/recordings only when they are genuinely useful evidence, and only claim them if actually captured.
12. Fix defects found.
13. Rebuild, relaunch/reinstall as appropriate, and rerun the affected tests.

For network/cloud features, test unavailable network, expired/invalid authentication, rate limiting, partial responses, cancellation, retry, and recovery where relevant. For data features, test fresh state, existing state, duplicates, malformed records, migration/restart, and large datasets where relevant.

## Evidence rules
Never say a test passed unless it was actually run. Never claim a Mac/device, OS version, simulator, signing identity, notarization result, performance metric, screenshot, or release artifact was verified unless it was actually observed. If an environment limitation prevents a required check, record it explicitly as blocked rather than inventing evidence.

## Documentation and rules
At completion:
- update the relevant phase status using the repository's existing status convention;
- update macOS documentation only when the phase genuinely changes the documented contract;
- add only genuinely permanent, reusable rules to `execution plan/macOS/editor-rules.md`;
- never delete or rewrite unrelated rules;
- record important unresolved limitations or follow-up work without implementing future phases.

## Completion gate
A phase is complete only when:
- the intended scope is implemented;
- relevant automated tests pass;
- the real macOS application is built and exercised when the environment permits;
- relevant runtime, UI, data, security, accessibility, and recovery behavior has been checked;
- discovered defects are fixed and retested;
- documentation/status/rules are updated accurately.

## Stop condition
After completing and verifying this phase, **STOP**. Do not continue into the next phase. Do not publish, distribute, notarize, or otherwise perform final release actions unless this exact phase explicitly includes them.

**Next phase:** Phase 03 — SwiftUI App Shell, Scenes & Lifecycle. Do not execute it now.
