# Phase 30 — Production Signing, Installer & Final Release Hardening

## Mission
Implement and verify only Phase 30. Do not implement later phases.

## Mandatory Windows isolation
Modify only the Windows platform boundary and genuinely shared Rust desktop code required by this phase. Never touch macOS, iOS, iPadOS, AndroidTablet, Android, Linux or other sibling areas.

## Required reading
Read root instructions and execution plan/Windows/README.md, requirements.md, spec.md, design.md and editor-rules.md. Inspect the actual repository and current status before editing.

## Objective
Perform final Windows release hardening: release configuration, application identity, signing certificates, secure packaging, installer, OAuth release configuration, privacy/data disclosures, dependency review, clean production build, install/smoke test, artifact inspection, versioning, rollback/update documentation and explicit RELEASE READY or RELEASE BLOCKED decision. Never publish automatically.

## Rust-first contract
Rust is the authoritative product/core implementation and must remain reusable by macOS. Keep Windows UI Rust-first. Native Windows SDK calls belong behind narrow Rust adapters. Never move business logic into another language merely for convenience.

## Security and trust
Gmail remains cloud source of truth. Preserve account isolation, least-privilege OAuth, secure credentials, deterministic/local/explainable precedence and untrusted-email rules. Consequential external effects require appropriate confirmation and idempotency.

## Verification
Run targeted and integration tests; build and launch the real Windows application when possible; exercise realistic success, empty, loading, error, cancellation, offline, restart, sleep/wake and recovery states as relevant; verify accessibility, keyboard/pointer behavior, security, data integrity, memory/performance and release configuration where applicable; inspect logs; fix defects; rebuild and retest. Never claim evidence that was not actually observed.

## Documentation
Update status accurately and add only genuinely permanent reusable rules. Record blockers honestly.

**STOP AFTER PHASE 30.** The Windows roadmap ends here; do not create Phase 31.