# Mail Organizer — Linux Execution Plan

## Purpose
This directory is the Linux-specific execution plan for Mail Organizer. Linux is a first-class desktop target and must have a native-feeling desktop experience while sharing the reusable desktop Rust core with macOS and Windows.

## Architecture
- Rust is the primary implementation language for the reusable desktop core and Linux application.
- The shared desktop Rust core owns domain, application, Gmail, sync, classification, company intelligence, search/indexing, priority, temporal/conversation intelligence, rules, actions, automation, AI routing, persistence, and integration abstractions.
- Linux-specific UI, windowing, desktop integration, packaging, permissions, notifications, keyring/secret-service integration, and filesystem/platform adapters remain Linux-specific.
- Do not duplicate business logic in another language or framework.
- Linux must not modify macOS, Windows, Android, AndroidTablet, iOS, or iPadOS implementation folders.
- Shared desktop Rust-core changes are allowed only where a feature is genuinely shared with macOS/Windows and must preserve their contracts.

## UI
Linux should use the same Mail Organizer product design language and information architecture as the other desktop targets, while adapting to Linux desktop conventions, window managers, scaling, keyboard/mouse, accessibility, and distribution differences. Do not merely ship a scaled mobile UI.

## Phase sequence
00 Architecture & shared Rust-core audit
01 Linux target & toolchain foundation
02 Shared Rust contract & Linux platform boundary
03 Application shell, lifecycle & state restoration
04 Desktop navigation & multi-column workspace
05 Visual system, Material-inspired product language, tokens & theming
06 Rust core integration & dependency boundary
07 Local persistence & state hydration
08 Secure storage, keyring & account state
09 Google OAuth & account connection
10 Gmail data access & API adapter
11 Initial Gmail sync
12 Incremental sync, pagination & recovery
13 Mailbox data model & message presentation
14 Linux mail workspace & conversation viewer
15 Categories, company intelligence & company workspace
16 Search & local index
17 Priority, Action Required & explainability
18 Rules & user corrections
19 Temporal intelligence & conversation intelligence
20 Action cards, Calendar & Tasks
21 Multi-account & unified inbox
22 Offline, background work & notifications
23 Privacy, security & data protection
24 Optional AI fallback
25 Advanced automation
26 Desktop interaction, keyboard, menus & multi-window
27 Performance, accessibility & reliability hardening
28 Comprehensive functional & integration QA
29 Security, adversarial, migration & recovery QA
30 Production packaging, signing/distribution & final release hardening

**STOP AFTER PHASE 30.**