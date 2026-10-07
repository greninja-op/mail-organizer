## Mandatory Rust desktop architecture

**Rust is the primary implementation language for this macOS phase.** Platform-neutral business/data/application logic belongs in the reusable Rust desktop core, which must also serve Windows. Swift/SwiftUI is for native macOS UI and OS integration only and must not duplicate Rust business rules.

# Phase 21 — Multi-Account & Unified Inbox

## Mission
Implement and verify only Phase 21. Do not implement later phases.

## macOS isolation
Keep all macOS-specific source, UI, tests, assets, entitlements and configuration in the macOS boundary. Never modify sibling platforms. Shared Rust desktop core changes must be genuinely platform-neutral and required by this phase.

## Required reading
Read root instructions and all macOS master documents. Inspect the actual repository, current status and existing architecture before editing.

## Scope
Implement connected/current/add account state, account-specific sync and recovery, receiving-account identity per message, Unified Inbox, global Starred semantics, category preservation, account removal cleanup and cross-account isolation.

## Safety and architecture
Preserve Gmail as cloud source of truth and explicit account scoping. Keep business logic shared where platform-neutral. External actions require appropriate authorization, confirmation and idempotency. Treat email, URLs, attachments and external model output as untrusted.

## Verification
Run targeted unit/integration tests; build and launch the actual macOS app when possible; test realistic success and failure states including offline, cancellation, restart, recovery and partial failure as applicable; verify accessibility, security, data integrity and performance; inspect logs for unexpected errors or secrets; fix defects; rebuild and retest. Never invent evidence.

## Documentation
Update status accurately and add only permanent rules.

**STOP AFTER PHASE 21.**