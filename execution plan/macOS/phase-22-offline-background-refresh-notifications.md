# Phase 22 — Offline, Background Refresh & Notifications

## Mission
Implement and verify only Phase 22. Do not implement later phases.

## macOS isolation
Keep all macOS-specific source, UI, tests, assets, entitlements and configuration in the macOS boundary. Never modify sibling platforms. Shared KMP changes must be genuinely platform-neutral and required by this phase.

## Required reading
Read root instructions and all macOS master documents. Inspect the actual repository, current status and existing architecture before editing.

## Scope
Implement offline behavior, macOS-appropriate refresh/background work and notifications. Handle network loss, sleep/wake, termination, relaunch, stale work, cancellation, duplicate notifications, notification authorization/privacy and power constraints.

## Safety and architecture
Preserve Gmail as cloud source of truth and explicit account scoping. Keep business logic shared where platform-neutral. External actions require appropriate authorization, confirmation and idempotency. Treat email, URLs, attachments and external model output as untrusted.

## Verification
Run targeted unit/integration tests; build and launch the actual macOS app when possible; test realistic success and failure states including offline, cancellation, restart, recovery and partial failure as applicable; verify accessibility, security, data integrity and performance; inspect logs for unexpected errors or secrets; fix defects; rebuild and retest. Never invent evidence.

## Documentation
Update status accurately and add only permanent rules.

**STOP AFTER PHASE 22.**