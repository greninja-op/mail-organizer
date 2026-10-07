# Mail Organizer — Android Tablet Execution Plan

This is the complete **Android tablet** execution program. It is deliberately split into many independently verifiable phases rather than compressing major features into a few prompts.

## MANDATORY PLATFORM ISOLATION — READ FIRST IN EVERY PHASE

This is an **Android tablet** phase. Every Android-tablet-specific document, UI source, asset, test, configuration, resource, manifest change and release artifact belongs under repository-root `AndroidTablet/`.

Never create or modify `iOS/`, `iPadOS/`, `Android/`, `macOS/`, `Windows/`, or `Linux/` while executing this plan.

The Android phone and Android tablet share the same Android/KMP product core wherever technically appropriate. Do not duplicate business/data engines merely because the tablet UI is different.

The Android tablet is **not an enlarged phone UI**. It is an adaptive large-screen workspace with more information visible simultaneously.

## SHARED-CORE DOCTRINE

Keep common behavior shared:
- KMP domain/data/application logic
- Gmail models and normalization
- repositories and synchronization
- classification/company intelligence
- search/indexing
- priority/Action Required
- rules/corrections
- temporal/conversation intelligence
- Action Engine
- integrations
- persistence/state contracts
- automation
- optional AI routing

Android tablet-specific presentation and Android large-screen behavior belong in `AndroidTablet/`.

## LARGE-SCREEN DOCTRINE

The tablet should use available width for:
- navigation/category context
- company/category context
- richer message list
- selected message/conversation detail
- contextual actions/inspector where useful

Use adaptive Compose layouts, window size classes, panes, drawers/rails, keyboard/mouse/pointer input, resizable/multi-window behavior and appropriate large-screen patterns. Never assume one tablet resolution.

## EXECUTION

For every phase:
1. Read root instructions and AndroidTablet master documents.
2. Determine the first incomplete AndroidTablet phase.
3. Read only that phase.
4. Inspect the actual repository and previous evidence.
5. Implement only that phase.
6. Build.
7. Run targeted automated tests.
8. Run emulator and physical tablet validation when available.
9. Exercise relevant success/empty/loading/offline/error/interruption states.
10. Perform relevant visual/accessibility/security/data/performance checks.
11. Fix.
12. Rebuild/reinstall/retest.
13. Update status and genuinely permanent rules.
14. Stop.

Build success alone never means completion.

## PHASE MAP — 31 PHASES

00. Android Tablet Architecture, Shared-Core & Large-Screen Audit
01. Android Tablet Target & Toolchain Foundation
02. Shared Android Mobile Contract & Platform Boundary
03. Compose Tablet Shell & Lifecycle
04. Adaptive Navigation & Multi-Pane Workspace
05. Material 3 Large-Screen Visual System
06. Shared KMP/Android Core Integration Boundary
07. Local Persistence & State Hydration
08. Secure Storage & Account State
09. Google OAuth & Account Connection
10. Gmail Data Access & API Adapter
11. Initial Gmail Sync
12. Incremental Sync, Pagination & Recovery
13. Mailbox Data Model & Message Presentation
14. Tablet Mail Workspace & Conversation Viewer
15. Categories, Company Intelligence & Company Workspace
16. Search & Local Index
17. Priority, Action Required & Explainability
18. Rules & User Corrections
19. Temporal Intelligence & Conversation Intelligence
20. Action Cards, Calendar & Tasks
21. Multi-Account & Unified Inbox
22. Offline, Background Sync & Notifications
23. Privacy, Security & Data Protection
24. Optional AI Fallback
25. Advanced Automation
26. Adaptive Layout, Pointer, Keyboard & Multi-Window
27. Performance, Accessibility & Reliability Hardening
28. Comprehensive Functional & Integration QA
29. Security, Adversarial, Migration & Recovery QA
30. Production Signing, Play Store Preparation & Final Release Hardening

**STOP AFTER PHASE 30. There is no Phase 31.**