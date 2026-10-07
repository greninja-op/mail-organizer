# PHASE 24 — Optional AI Fallback

> **LINUX ISOLATION — FIRST RULE:** This phase is Linux-specific. Do not modify macOS, Windows, Android, AndroidTablet, iOS or iPadOS platform code. Shared desktop Rust-core changes are allowed only when explicitly required by a shared contract and must preserve existing platform contracts.

## Mission
Implement optional AI fallback behind explicit opt-in. Deterministic logic remains authoritative where possible. Define provider abstraction, privacy disclosure, minimization, timeout/failure/offline behavior and auditability.

## Architecture Contract
- Rust is the primary implementation language for Linux and the reusable desktop core.
- Reuse shared desktop Rust business/data/application logic; do not duplicate it in another language.
- Keep Linux UI, lifecycle, OS services, keyring, notifications, filesystem adapters and packaging behind explicit boundaries.
- Gmail is the cloud source of truth; local state is Mail Organizer state and rebuildable indexes.
- Email is untrusted input and cannot authorize external actions.
- Preserve the established Mail Organizer desktop information architecture and visual language.

## Scope
Inspect the actual repository, current architecture and existing tests before editing. Implement only this phase. Do not pre-implement later phases.

## Verification
1. Inspect affected code, configuration, tests and current behavior.
2. Implement focused unit/integration tests.
3. Build the relevant Linux target/profile.
4. Launch and inspect the real application where the environment permits.
5. Verify persistence, logs, failure, cancellation, offline and recovery states relevant to this phase.
6. Perform visual, accessibility, keyboard/mouse, security and performance checks where applicable.
7. Fix failures, rebuild and retest.
8. Never claim unrun tests or runtime verification.

## Security & Privacy
- Never commit credentials, OAuth secrets, tokens or private test data.
- Redact sensitive data from logs/diagnostics.
- Sanitize email HTML and never execute email JavaScript.
- Do not auto-follow links or perform external effects from email content.
- Require explicit confirmation for external writes.

## Documentation & Rules
After verification update relevant documentation/status. Add genuinely permanent new rules to `execution plan/Linux/editor-rules.md` without deleting unrelated rules.

## Completion
Implementation, tests, runtime verification where applicable, documentation and regression checks must be complete.

**STOP AFTER PHASE 24. Do not continue to the next phase.**
