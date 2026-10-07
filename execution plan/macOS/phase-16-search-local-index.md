## Mandatory Rust desktop architecture

**Rust is the primary implementation language for this macOS phase.** Platform-neutral business/data/application logic belongs in the reusable Rust desktop core, which must also serve Windows. Swift/SwiftUI is for native macOS UI and OS integration only and must not duplicate Rust business rules.

# Phase 16 — Search & Local Index

## Mission
Implement and verify only Phase 16. Do not implement later phases.

## Isolation
Modify only macOS-specific code and genuinely shared Rust desktop core code required by this phase. Never modify iOS, iPadOS, AndroidTablet, Android, Windows, Linux, or other sibling areas.

## Read first
Read root instructions and all files in `execution plan/macOS/` master docs. Inspect the actual repository, status, architecture, existing tests, and previous evidence before editing.

## Objective
Implement local-first search and indexing for sender, subject, body/thread, company/domain, category, priority, Action Required, Gmail labels, and supported metadata. Handle incremental updates, deletion, ranking, cancellation, empty results, malformed input, and large indexes.

## Product rules
Preserve Gmail as source of truth, explicit account boundaries, category semantics, and deterministic/local/explainable behavior. Individual email star is independent from company pinning. Do not silently delete or hide promotional/social mail. Do not make companies a replacement for mailbox navigation.

## Verification
Run targeted automated tests; build and run the actual macOS app when possible; exercise realistic success, empty, loading, error, cancellation, offline, restart and recovery states; test large datasets where relevant; verify accessibility, keyboard/pointer behavior, data integrity and account isolation; inspect logs; fix defects; rebuild and retest. Never claim unrun evidence.

## Documentation
Update status accurately and add only genuinely permanent rules. Do not delete unrelated rules.

**STOP AFTER PHASE 16.**
