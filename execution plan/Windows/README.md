# Mail Organizer — Windows Execution Plan

This folder is the complete Windows-specific execution plan.

## Mandatory isolation
All Windows-specific Rust source, UI resources, tests, configuration, manifests, installers, signing material, scripts and release artifacts must remain inside the Windows platform boundary. Never modify macOS, iOS, iPadOS, AndroidTablet, Android, Linux or other sibling platform areas.

## Desktop architecture
Rust is the primary language for the Windows application and the reusable desktop core shared with macOS. The Rust core owns platform-neutral domain/data/application behavior: Gmail normalization and sync, classification, company intelligence, search, rules, priority, temporal/conversation intelligence, Action Engine, persistence, automation and AI routing.

Windows UI should remain Rust-first as well, using an appropriate Rust desktop UI framework or safe Rust bindings to native Windows UI APIs. Do not create a second business-logic implementation in another language. If a Windows SDK surface genuinely requires a foreign-language/native boundary, isolate it behind a small Rust adapter and keep the product logic in Rust.

Windows is a native desktop experience, not a scaled mobile UI.

## Execution doctrine
Every session: read root instructions and Windows master docs; determine the first incomplete Windows phase; read only that phase; inspect the actual repository; implement only that phase; run targeted tests; build and launch the real Windows app when available; exercise relevant states; perform visual/accessibility/security/data/performance/recovery checks; fix defects; rebuild/retest; update status and permanent rules; stop.

Never treat compilation as completion. Never claim unrun tests, screenshots, runtime checks, signing, installer, performance, or release evidence.

## Phase map
00 — Windows Architecture, Shared Rust Core & Desktop Audit
01 — Windows Target & Toolchain Foundation
02 — Shared Rust Contract & Windows Platform Boundary
03 — Windows Application Shell, Window Lifecycle & State Restoration
04 — Desktop Navigation & Multi-Column Workspace
05 — Windows Visual System, Tokens & Theming
06 — Rust Core Integration & Dependency Boundary
07 — Local Persistence & State Hydration
08 — Secure Storage, Windows Credential Protection & Account State
09 — Google OAuth & Account Connection
10 — Gmail Data Access & API Adapter
11 — Initial Gmail Sync
12 — Incremental Sync, Pagination & Recovery
13 — Mailbox Data Model & Message Presentation
14 — Windows Mail Workspace & Conversation Viewer
15 — Categories, Company Intelligence & Company Workspace
16 — Search & Local Index
17 — Priority, Action Required & Explainability
18 — Rules & User Corrections
19 — Temporal Intelligence & Conversation Intelligence
20 — Action Cards, Calendar & Tasks
21 — Multi-Account & Unified Inbox
22 — Offline, Background Work & Notifications
23 — Privacy, Security & Data Protection
24 — Optional AI Fallback
25 — Advanced Automation
26 — Desktop Interaction, Keyboard, Menus & Multi-Window
27 — Performance, Accessibility & Reliability Hardening
28 — Comprehensive Functional & Integration QA
29 — Security, Adversarial, Migration & Recovery QA
30 — Production Signing, Installer & Final Release Hardening

**STOP AFTER PHASE 30.**