# Phase 17 — Priority, Action Required & Explainability

## Mandatory Rust architecture
Rust is the primary implementation language. All platform-neutral domain/application/data behavior belongs in the reusable Rust desktop core for macOS and Windows. Swift/SwiftUI is limited to native macOS presentation and OS integration and must not duplicate business logic.

## Isolation
Modify only the macOS boundary and genuinely shared Rust code required here. Never modify sibling platform areas.

## Required reading
Read root instructions and all macOS master documents. Inspect the actual repository before editing.

## Scope
Implement deterministic Rust-owned priority and Action Required intelligence using explainable signals, user corrections/rules, sender/company context and confidence. SwiftUI presents explanations but must not reimplement the decision engine.

## Verification
Run targeted tests, build and launch the real macOS application when possible, exercise realistic success and failure states, verify account isolation, persistence, accessibility and performance as relevant, inspect logs, fix defects, rebuild and retest. Never invent evidence.

## Documentation
Update status and add only permanent rules.

**STOP AFTER PHASE 17.**