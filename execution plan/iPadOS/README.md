# Mail Organizer — iPadOS Execution Plan

This is the complete execution plan for the iPadOS application only.

MANDATORY PLATFORM ISOLATION — READ FIRST IN EVERY PHASE:
- Target: iPadOS.
- All iPadOS-specific documents, UI, assets, tests, configuration, entitlements, scripts and release artifacts belong under iPadOS/.
- Do not create or modify iOS/, Android/, AndroidTablet/, macOS/, Windows/, or Linux/ during this plan.
- Shared KMP/common mobile source must remain shared; do not duplicate it merely for folder separation.
- Android phone, iPhone and iPad share the same product/domain/data source of truth wherever practical. Platform presentation and OS integration are separate.
- The iPad UI is NOT an enlarged iPhone UI.
- Persist this boundary as a permanent repository rule in Phase 0.

Architecture:
COMMON KMP CORE → domain, data, Gmail models/normalization, sync, classification, company intelligence, search, rules, priority, temporal/conversation intelligence, Action Engine, integration abstractions, persistence, automation and optional AI routing.
ANDROID → Android presentation/platform integration.
iOS → iPhone SwiftUI presentation/platform integration.
iPadOS → iPad SwiftUI presentation, adaptive multi-column layout, pointer/keyboard/windowing and iPad platform integration.

The common product core should remain as identical as practical. Differences should primarily be UI and genuine OS integration. Do not artificially duplicate source files.

The iPad is a larger information workspace. It must expose substantially more useful information at a glance than iPhone:
- navigation/category context
- company/category filters
- message list
- richer message metadata
- selected message/conversation detail
- priority/action state
- contextual actions/inspector where useful

Use adaptive multi-column layouts, split views, Stage Manager/resizable windows, pointer, keyboard and contextual menus. Never assume one fixed resolution.

Execution:
1. Read root instructions and all iPadOS master documents.
2. Determine the first incomplete iPadOS phase.
3. Read only that phase.
4. Inspect actual repository before implementation.
5. Implement only that phase.
6. Build, test, run and inspect on iPad Simulator and real iPad where available.
7. Fix, rebuild and retest.
8. Update status and permanent rules when genuinely required.
9. Stop. Never start the next phase.

Build success alone never means completion.

Phase map:
0 Architecture, Shared-Core & Display Audit
1 Application Shell & Shared KMP Integration
2 Persistence & State Integration
3 Google OAuth & Gmail Connection
4 Gmail Sync & iPadOS Lifecycle
5 Multi-Column Mailbox & Email Viewer
6 Classification, Company Intelligence & Category Workspace
7 Search, Priority & Action Required
8 Rules, Temporal & Conversation Workspace
9 Action Engine, Calendar & Tasks
10 Multi-Account & Unified Inbox
11 Offline, Background Refresh & Notifications
12 Privacy, Security & Data Protection
13 Optional AI Fallback
14 Advanced Automation
15 Adaptive Layout, Performance, Accessibility & Input
16 Comprehensive QA & Adversarial Testing
17 Production Signing, App Store & OAuth Preparation
18 Final Production Hardening & Release

STOP AFTER PHASE 18.
