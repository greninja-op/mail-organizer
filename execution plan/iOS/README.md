# Mail Organizer — iOS Execution Plan

This directory is the complete execution plan for the **iPhone/iOS application only**.

## Platform isolation — mandatory

**This is the iOS plan. It is NOT the iPadOS plan, Android-tablet plan, macOS plan, Windows plan, or Linux plan.**

At the beginning of **every phase prompt**, the editor MUST read and enforce this boundary:

- Platform target: **iOS / iPhone**.
- All iOS-specific planning documents, configuration, assets, tests, scripts, generated platform code, and implementation artifacts created by these phases belong under the repository-root **`iOS/`** directory.
- Do not place iOS-specific files in `iPadOS/`, `AndroidTablet/`, `macOS/`, `Windows/`, or `Linux/`.
- Do not create those sibling platform directories while executing the iOS plan.
- Shared KMP code remains shared and is not duplicated merely to satisfy folder isolation.
- If an existing KMP/shared component must change to support iOS, change it only when the current phase explicitly requires it, document why, and do not fork the shared logic into an iOS-only copy.
- iOS-specific UI, lifecycle, entitlements, Info.plist configuration, signing, capabilities, tests, assets, and release configuration must remain inside the iOS boundary.
- Never touch an unrelated Android, Android-tablet, iPadOS, macOS, Windows, or Linux project.

This rule must be persisted in `editor-rules.md` as a permanent cross-platform repository rule during Phase 0.

## Execution model

The editor executes exactly one phase at a time:

1. Read the root project instructions.
2. Read `requirements.md`, `spec.md`, `design.md`, and `editor-rules.md`.
3. Determine the first incomplete iOS phase.
4. Read only that phase's prompt.
5. Inspect the actual repository before deciding what to implement.
6. Implement that phase only.
7. Build, test, run, inspect, and verify on an iPhone Simulator and, where available, a real iPhone.
8. Fix failures and repeat validation.
9. Update status and permanent editor rules when genuinely required.
10. Stop. Never begin the next phase automatically.

Build success alone never means a phase is complete.

## iOS architecture

Mail Organizer remains a Kotlin Multiplatform application:

- Shared KMP: domain, repositories, Gmail normalization, sync, classification, company intelligence, rules, search/indexing, priority, temporal/conversation intelligence, Action Engine, integration abstractions, persistence, automation, optional AI routing.
- iOS: SwiftUI presentation, iOS navigation/lifecycle, Apple platform integrations, Keychain, background execution, notifications, URL/auth presentation, entitlements, accessibility, iOS-specific performance and release configuration.
- Gmail remains the cloud source of truth.
- Local Mail Organizer state remains rebuildable from Gmail where possible.
- No iOS phase may silently redesign or fork the Android application.

## Phase map

0. iOS Platform Architecture Audit & Boundary Foundation
1. iOS Application Shell & KMP Integration
2. iOS Local Persistence & State Integration
3. Google OAuth & Gmail iOS Connection
4. Gmail Synchronization & iOS Lifecycle
5. iOS Mailbox, Navigation & Email Viewer
6. iOS Classification, Company Intelligence & Categories
7. iOS Search, Priority & Action Required
8. iOS Rules, Corrections, Temporal & Conversation Intelligence
9. iOS Action Engine, Calendar & Tasks
10. iOS Multi-Account & Unified Inbox
11. iOS Offline, Background Refresh & Notifications
12. iOS Privacy, Security & Data Protection
13. iOS Optional AI Fallback
14. iOS Advanced Automation
15. iOS Performance, Accessibility & Adaptive UX
16. iOS Comprehensive QA & Adversarial Testing
17. iOS Production Signing, App Store & OAuth Preparation
18. iOS Final Production Hardening & Release

**STOP AFTER PHASE 18.**
