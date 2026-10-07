# Mail Organizer — macOS Execution Plan

This folder is the complete macOS-specific execution plan for Mail Organizer.

## Mandatory isolation
All macOS-specific source, SwiftUI views, Rust desktop bindings, assets, tests, entitlements, configuration, scripts, signing/notarization material, and release artifacts must remain within the macOS platform boundary. Never modify iOS, iPadOS, AndroidTablet, Android, Windows, Linux, or other sibling platform areas.

## Cross-platform desktop architecture
The desktop product uses a shared Rust core for business/data behavior so the core can be reused by both macOS and Windows. Swift/SwiftUI is the macOS-native UI and OS integration layer. Rust owns platform-neutral application behavior; Swift must not duplicate it.


## macOS implementation architecture — Rust first, Swift UI only where required

Rust is the **primary implementation language for macOS**. Platform-neutral business logic, application/use-case orchestration, Gmail data handling, normalization, synchronization, classification, company intelligence, search/indexing, priority, temporal and conversation intelligence, rules, Action Engine, persistence abstractions, automation, AI routing, networking adapters, and security-sensitive non-UI logic must be implemented in Rust wherever technically appropriate.

Swift is permitted for the **native macOS presentation and OS-facing UI/integration layer** where Swift/SwiftUI/AppKit is the correct platform mechanism. Swift must not become a second business-logic implementation. SwiftUI views should call Rust-owned application/domain APIs through a deliberate FFI boundary.

The Rust core must be designed so the same core can be reused by the future Windows desktop implementation. Avoid macOS-only Rust abstractions in the shared desktop core. Keep the Rust/Swift boundary explicit, narrow, testable, memory-safe, and cancellation-aware. Prefer opaque handles/value objects or generated bindings over duplicated models and logic.

The macOS application is therefore: **Rust core + Swift/SwiftUI native desktop shell**. Do not replace the Rust core with Swift merely because a macOS API is convenient.

## Execution doctrine
At the beginning of every session: read root instructions and macOS master docs; determine the first incomplete phase; read only that phase; inspect the actual repository; implement only that phase; run targeted tests; build and launch the real macOS app; exercise relevant runtime states; perform visual/accessibility/security/data/performance/recovery checks; fix defects; rebuild/retest; update status and genuinely permanent rules; stop.

Never treat compilation as completion. Never claim unrun tests, runtime checks, screenshots, signing, notarization, or performance evidence.

## Phase map
00 — macOS Architecture, Shared-Core & Desktop Audit
01 — macOS Target & Toolchain Foundation
02 — Shared KMP Contract & macOS Platform Boundary
03 — SwiftUI App Shell, Scenes & Lifecycle
04 — Desktop Navigation & Multi-Column Workspace
05 — macOS Visual System, Tokens & Theming
06 — KMP Framework Integration & Dependency Boundary
07 — Local Persistence & State Hydration
08 — Secure Storage, Keychain & Account State
09 — Google OAuth & Account Connection
10 — Gmail Data Access & API Adapter
11 — Initial Gmail Sync
12 — Incremental Sync, Pagination & Recovery
13 — Mailbox Data Model & Message Presentation
14 — macOS Mail Workspace & Conversation Viewer
15 — Categories, Company Intelligence & Company Workspace
16 — Search & Local Index
17 — Priority, Action Required & Explainability
18 — Rules & User Corrections
19 — Temporal Intelligence & Conversation Intelligence
20 — Action Cards, Calendar & Tasks
21 — Multi-Account & Unified Inbox
22 — Offline, Background Refresh & Notifications
23 — Privacy, Security & Data Protection
24 — Optional AI Fallback
25 — Advanced Automation
26 — Desktop Interaction, Keyboard, Menus & Multi-Window
27 — Performance, Accessibility & Reliability Hardening
28 — Comprehensive Functional & Integration QA
29 — Security, Adversarial, Migration & Recovery QA
30 — Production Signing, Notarization & Final Release Hardening

**STOP AFTER PHASE 30.**