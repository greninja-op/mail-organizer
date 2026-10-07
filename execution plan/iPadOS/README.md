# Mail Organizer — iPadOS Execution Plan

This is the complete iPadOS / iPad execution program. It deliberately uses many small, independently verifiable phases rather than compressing product features into a few large prompts.

## MANDATORY PLATFORM ISOLATION — READ FIRST IN EVERY PHASE

This phase targets iPadOS only. Every iPadOS-specific document, UI source, asset, test, configuration, entitlement, script and release artifact belongs under repository-root iPadOS/.

Never create or modify iOS/, AndroidTablet/, macOS/, Windows/, or Linux/ while executing this plan. Shared KMP/common mobile code remains shared. Never copy common business logic merely to satisfy folder separation.

Android phone, iPhone and iPad share the same product/domain/data source of truth wherever technically appropriate. The primary divergence is presentation and genuine OS integration.

The iPad is not a large iPhone. Its larger viewport, split configurations, Stage Manager, pointer, keyboard and multi-column navigation are first-class product requirements.

## EXECUTION DOCTRINE

Each phase is intentionally narrow enough to implement, test, diagnose and stabilize independently.

For every phase: read root instructions and iPadOS master documents; determine the first incomplete phase; read only that phase; inspect actual repository and previous evidence; implement only that phase; build; run targeted automated tests; run iPad Simulator and real iPad when available; exercise relevant success, empty, loading, offline, error and interruption states; perform relevant visual, accessibility, security, data-integrity and performance checks; fix; rebuild/reinstall/retest; update status and permanent rules only when genuinely required; stop.

A compiling application is not a completed phase. Never claim tests, screenshots, device runs, logs or performance measurements that were not actually performed.

## SHARED-CORE DOCTRINE

Keep common product behavior in the shared KMP/mobile core: domain models, repositories, Gmail normalization, synchronization, classification, company intelligence, search/indexing, rules, priority, temporal/conversation intelligence, Action Engine, integration abstractions, persistence, automation and AI routing.

Do not fork these systems into iPadOS merely because the UI is different.

## IPAD UI DOCTRINE

The UI may differ substantially from iPhone and Android while preserving the same product behavior, semantic design tokens and source-of-truth rules.

At sufficient width, the product should be able to show navigation/category context + company/category context + message list + selected conversation/detail + contextual actions/inspector. The layout must adapt to actual available width rather than a fixed device resolution.

## PHASE MAP — 31 INDEPENDENTLY EXECUTABLE PHASES

00. iPadOS Architecture, Shared-Core & Display Audit
01. iPadOS Project Target & Toolchain Foundation
02. Shared Mobile Contract & Platform Boundary
03. iPadOS SwiftUI Shell & Scene Lifecycle
04. Adaptive Navigation & Multi-Column Workspace
05. Design Tokens, Theming & iPad Visual System
06. KMP Framework Integration & Dependency Boundary
07. Local Persistence & State Hydration
08. Secure Storage & Account State
09. Google OAuth & Account Connection
10. Gmail Data Access & API Adapter
11. Initial Gmail Sync
12. Incremental Sync, Pagination & Recovery
13. Mailbox Data Model & Message Presentation
14. iPad Mail Workspace & Conversation Viewer
15. Categories, Company Intelligence & Company Workspace
16. Search & Local Index
17. Priority, Action Required & Explainability
18. Rules & User Corrections
19. Temporal Intelligence & Conversation Intelligence
20. Action Cards, Calendar & Tasks
21. Multi-Account & Unified Inbox
22. Offline, Background Refresh & Notifications
23. Privacy, Security & Data Protection
24. Optional AI Fallback
25. Advanced Automation
26. Adaptive Layout, Pointer, Keyboard & Stage Manager
27. Performance, Accessibility & Reliability Hardening
28. Comprehensive Functional & Integration QA
29. Security, Adversarial, Migration & Recovery QA
30. Production Signing, App Store Preparation & Final Release Hardening

STOP AFTER PHASE 30. There is no Phase 31.