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

## Android-first UI milestone
Before broad backend/integration expansion, the Android UI foundation must be treated as a first-class product milestone. It must establish:
- Gmail-familiar information hierarchy with Mail Organizer differentiation.
- Material 3 / Material 3 Expressive based component and motion system.
- Centralized design tokens and UI consistency rules.
- Adaptive performance profiling and a capability-based PerformanceProfile.
- Adaptive motion/visual complexity that prioritizes sustained smoothness, thermals and battery.
- Google OAuth sign-in experience that uses the official authentication flow rather than a fake credential form.
- Honest initial Gmail recovery/sync progress UX, including the Gmail → Mail Organizer mail-transfer animation when real synchronization takes long enough to need it.
- Real-data, loading, empty, error, offline and partial-sync states.
- Repeatable visual QA and frame/jank/performance checks.

### Android mailbox UX contract
The Android mailbox information architecture must also establish:
- top app bar with navigation drawer affordance, Gmail-familiar search affordance and account/profile affordance;
- drawer destinations: All Inbox, Primary, Promotional, Social, Spam and Starred;
- All Inbox rows expose the receiving/source Gmail account identity through a compact circular account indicator;
- account switcher supports connected accounts, add-account flow and immediate swipe-to-next-account interaction where supported;
- adding an account reuses the real Gmail → Mail Organizer recovery/sync UX;
- Promotional is organized and accessible but should not dominate the Primary experience;
- Spam is visible as a dedicated destination and may show a red new/unread indicator;
- Starred is a global view of individually starred messages;
- starring a message does not change its category;
- company grouping/filtering is inside a selected category rather than a drawer destination;
- company pinning moves a company to the top of that category's company filter list, not into the global navigation;
- company pinning and email starring are independent concepts;
- company filters preserve category context.

Do not interpret this as permission to implement later Gmail synchronization phases early. UI may use clearly labeled fixtures during the UI-only phase, but production screens must switch to real data as the relevant data phases become available.

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


## Phase status (2026-10-09)
- [x] 0 Project Audit & Development Foundation — complete, on main
- [x] 1 Android Application Foundation — complete, on main
- [x] 2 Local Data Architecture — complete, on main
- [ ] 3 Google OAuth & Gmail Connection — DEFERRED to the end (user decision)
- [x] 4 Gmail Synchronization Engine — complete, on main
- [x] 5 Email Data Model & Parsing — complete, on main
- [x] 6 Core Inbox & Email Viewer — complete, on main
- [x] 7 Deterministic Classification Engine — complete, on main
  (local 30-rule engine v1; Promotional/Social/Spam destinations wired;
  category chip + "why" explanation on message detail)
- [x] 8 Company & Sender Intelligence — complete, on main
  (deterministic company detection; sender profiles; company filter chips;
  classifier v2 with recurring-sender rule)
- [x] 9 Priority & Action-Required Engine — complete, on main
  (deterministic priority engine v1: core/priority/ 15 rules,
  LOW/NORMAL/HIGH/CRITICAL independent from category; domain/priority/
  use cases with manual-override safety; action-required filter chip +
  priority badges + "why this priority?" in UI)
- [ ] 10–30 — not started

Phase 30 is the final roadmap phase. A fully written roadmap is not evidence that implementation is complete.