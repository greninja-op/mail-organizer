## Mandatory Rust desktop architecture

**Rust is the primary implementation language for this macOS phase.** Platform-neutral business/data/application logic belongs in the reusable Rust desktop core, which must also serve Windows. Swift/SwiftUI is for native macOS UI and OS integration only and must not duplicate Rust business rules.

# Phase 18 — Rules & User Corrections

## Mission
Implement and verify only Phase 18. Do not implement later phases.

## Isolation
Modify only the macOS platform boundary and genuinely shared Rust desktop core code required by this phase. Never modify sibling platform folders.

## Required reading
Read root instructions and all macOS master documents. Inspect the actual repository before editing.

## Scope
Implement user corrections and safe rules with explicit precedence, scopes, matching, versioning, enable/disable, conflict handling, history, and predictable reclassification. Rules must remain structured data and must not become a general-purpose execution mechanism.

## Product contract
Preserve Gmail as source of truth and explicit account boundaries. User correction and explicit user rules must outrank deterministic classification; AI remains a fallback. Rules cannot silently perform consequential external actions.

## Verification
Run targeted tests; build and launch the macOS application when possible; exercise create/edit/disable/re-enable/conflict/invalid-input/reclassification/restart states; verify persistence, accessibility, security and account isolation; inspect logs; fix defects; rebuild and retest. Never claim unrun evidence.

## Documentation
Update status accurately and add only permanent reusable rules.

**STOP AFTER PHASE 18.**