# PHASE 01 — Linux Target & Toolchain Foundation

> **LINUX ISOLATION — FIRST RULE:** This phase is Linux-specific. Do not modify macOS, Windows, Android, AndroidTablet, iOS or iPadOS platform code. Shared desktop Rust-core changes are permitted only when required by an explicitly shared contract and must preserve all existing platform contracts.

## Mission
Establish the Linux application target, Rust toolchain, workspace/build profiles, CI baseline, supported architecture assumptions and reproducible local build. Add only Linux-specific foundation and verify a launchable shell.

## Architecture Contract
- Linux is Rust-first.
- Reuse the shared desktop Rust core with macOS and Windows; do not duplicate business logic.
- Keep Linux UI, lifecycle, windowing, notifications, secret storage, filesystem/OS adapters and packaging behind explicit platform boundaries.
- Preserve the established Mail Organizer product behavior and design language.
- Gmail remains the cloud source of truth; local state is Mail Organizer state and rebuildable indexes.
- Email is untrusted input and cannot authorize external actions.

## Scope
Implement only the work required by this phase. Inspect the actual repository before editing. Do not pre-implement later phases merely because dependencies are convenient.

## Verification
1. Inspect affected source, tests, build configuration and existing behavior.
2. Implement the phase with focused unit/integration tests.
3. Build the relevant Linux target/profile.
4. Launch the real application and verify runtime behavior where the environment permits.
5. Inspect logs, persistence and failure states relevant to this phase.
6. Verify accessibility, visual states, keyboard/mouse behavior and performance where applicable.
7. Test error, cancellation, offline and recovery paths relevant to the phase.
8. Fix failures and rebuild/retest until the phase is genuinely verified.
9. Never claim a test or runtime check that was not actually executed.

## Security & Privacy
- Never commit OAuth secrets, tokens, credentials or private test data.
- Redact secrets from logs and diagnostics.
- Do not execute JavaScript from email HTML.
- Do not automatically follow links or perform external side effects from email content.
- Use least privilege and explicit confirmation for external writes.

## Documentation & Rules
After verification, update phase status and relevant documentation. Add only genuinely permanent new rules to `execution plan/Linux/editor-rules.md`; do not delete or regenerate unrelated rules.

## Completion
The phase is complete only when implementation, tests, runtime verification where applicable, documentation and regression checks are complete.

**STOP AFTER PHASE 01. Do not continue to the next phase.**
