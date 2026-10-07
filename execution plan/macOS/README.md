# Mail Organizer — macOS Execution Plan

This folder is the complete macOS-specific execution plan for Mail Organizer.

## Mandatory isolation
All macOS-specific source, SwiftUI views, assets, tests, entitlements, configuration, scripts, signing/notarization material, and release artifacts must live under the macOS platform boundary established by the actual repository. Never create or modify `iOS/`, `iPadOS/`, `AndroidTablet/`, `Android/`, `Windows/`, or other sibling platform folders while executing a macOS phase.

Shared KMP/common business and data logic should remain shared when it is genuinely platform-neutral. Do not duplicate shared domain, Gmail normalization, sync, classification, company intelligence, search, rules, priority, temporal/conversation intelligence, Action Engine, persistence contracts, automation, or AI-routing logic merely to make macOS convenient.

macOS is a native desktop experience, not an enlarged iPad or phone UI. Use SwiftUI and macOS-native interaction patterns, including menu bar commands, keyboard shortcuts, pointer behavior, resizable windows, split views, toolbars, context menus, multiple windows/scenes, and accessibility semantics.

## Execution doctrine
At the beginning of every session:
1. Read the root project instructions and the macOS execution-plan documents.
2. Read `requirements.md`, `spec.md`, `design.md`, and `editor-rules.md`.
3. Determine the first incomplete macOS phase from the actual status.
4. Read only that phase prompt.
5. Inspect the real repository and existing implementation before changing anything.
6. Implement only that phase.
7. Run targeted automated tests, then build and run the macOS application.
8. Exercise meaningful runtime states on a supported Mac/macOS environment or the strongest available equivalent.
9. Perform visual, accessibility, security, data-integrity, performance, and recovery checks relevant to the phase.
10. Fix discovered defects, rebuild, reinstall/relaunch where applicable, and retest.
11. Update phase status and add only genuinely permanent new rules to `editor-rules.md`.
12. Stop.

Never treat compilation as completion. Never claim tests, screenshots, runtime checks, device checks, signing, notarization, or performance measurements that were not actually performed.

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
