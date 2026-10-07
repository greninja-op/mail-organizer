# Phase 14 — macOS Mail Workspace & Conversation Viewer

## Mission
Implement and verify only Phase 14. Do not implement later phases.

## Isolation
Modify only macOS-specific code and genuinely shared KMP code required by this phase. Never modify iOS, iPadOS, AndroidTablet, Android, Windows, Linux, or other sibling areas.

## Read first
Read root instructions and all files in `execution plan/macOS/` master docs. Inspect the actual repository, status, architecture, existing tests, and previous evidence before editing.

## Objective
Build the native desktop mail list/detail and conversation viewer with selection, thread navigation, safe HTML rendering, attachment metadata, loading/empty/error/offline states, keyboard movement, context actions, and large-thread performance.

## Product rules
Preserve Gmail as source of truth, explicit account boundaries, category semantics, and deterministic/local/explainable behavior. Individual email star is independent from company pinning. Do not silently delete or hide promotional/social mail. Do not make companies a replacement for mailbox navigation.

## Verification
Run targeted automated tests; build and run the actual macOS app when possible; exercise realistic success, empty, loading, error, cancellation, offline, restart and recovery states; test large datasets where relevant; verify accessibility, keyboard/pointer behavior, data integrity and account isolation; inspect logs; fix defects; rebuild and retest. Never claim unrun evidence.

## Documentation
Update status accurately and add only genuinely permanent rules. Do not delete unrelated rules.

**STOP AFTER PHASE 14.**
