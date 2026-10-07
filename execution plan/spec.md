# Mail Organizer — Development Execution Specification

## Source of truth
1. requirements.md
2. spec.md
3. design.md
4. editor-rules.md
5. Existing architecture and decisions
6. Official platform/API documentation
7. Engineering judgment

## Phase execution
Statuses: [ ] not started, [-] in progress, [x] verified complete, [!] blocked.

The editor must execute phases sequentially. At session start read the execution-plan directory, determine the first incomplete phase, inspect the actual repository, and execute only that phase. Never execute the whole roadmap in one session.

## Shared architecture
Kotlin Multiplatform is the shared foundation. Shared code should contain domain models, use cases, Gmail normalization, sync state, classification, company intelligence, rules, search, priority, temporal intelligence, conversation intelligence, action models, integration abstractions and KMP-compatible persistence. Platform-specific layers own UI, OAuth/browser presentation, secure credential storage and OS scheduling APIs where necessary.

## Phases
0 Project Audit & Development Foundation
1 Android Application Foundation
2 Local Data Architecture
3 Google OAuth & Gmail Connection
4 Gmail Synchronization Engine
5 Email Data Model & Parsing
6 Core Inbox & Email Viewer
7 Deterministic Classification Engine
8 Company & Sender Intelligence
9 Categories, Priority & Action Required
10 Search & Local Indexing
11 Dashboard & Information Architecture
12 Rules & User Corrections
13 Meeting & Deadline Extraction
14 Action Cards & Action Engine
15 Google Calendar Integration
16 Google Tasks Integration
17 Integration Manager
18 Multi-Account & Unified Inbox
19 Background Sync & Offline Behavior
20 Noise, Newsletter & Cleanup System
21 Waiting-for-Reply & Conversation Intelligence
22 Gmail Modification & Optional Write Features
23 Privacy Center & Security Hardening
24 Performance & Battery Optimization
25 Analytics & Insights
26 Optional AI Fallback Architecture
27 Advanced Automation
28 Full Testing & QA
29 Production OAuth / Play Store Preparation
30 Final Production Hardening & Release

## Completion protocol
For every phase: inspect → implement only phase scope → build → automated tests → runtime/device verification → visual/accessibility/security checks → fix → rebuild/reinstall/retest → update status/docs → stop.

Never claim tests or device verification that were not actually run. Never touch sibling projects. Never commit secrets. Never let email content authorize external actions. Preserve working behavior unless the phase explicitly requires change.

Phase 30 is the final roadmap phase. A fully written roadmap is not evidence that implementation is complete.