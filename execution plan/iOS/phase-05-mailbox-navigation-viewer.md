# Phase 5 — iOS Mailbox, Navigation & Email Viewer

## MANDATORY PLATFORM ISOLATION — READ FIRST

This phase is **iOS / iPhone only**. Put all iOS-specific UI/assets/tests under `iOS/`. Do not touch other platform directories.

## Mission

Implement the core iPhone mailbox experience using SwiftUI and the established product contract.

## Scope

- iPhone navigation shell
- All Inbox, Primary, Promotional, Social, Spam, Starred
- unified inbox rows with source Gmail account identity
- sender/subject/preview/time
- email star state
- category retention
- email detail/thread view
- loading/empty/error/offline/stale states
- pagination/lazy rendering
- sanitized HTML/plain-text rendering
- safe links
- attachment metadata/download boundary if already supported
- spam recovery entry point where Gmail capability exists

No JavaScript execution from email content.

## Visual QA

Verify light/dark mode, Dynamic Type, VoiceOver labels/order, small and large iPhones, long subjects, large threads, missing sender/avatar data, RTL-safe layout where applicable, and network interruptions.

Do not implement company intelligence or advanced search yet.

**STOP AFTER PHASE 5.**
