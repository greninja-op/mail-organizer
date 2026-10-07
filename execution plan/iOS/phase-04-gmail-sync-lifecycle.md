# Phase 4 — Gmail Synchronization & iOS Lifecycle

## MANDATORY PLATFORM ISOLATION — READ FIRST

Target is **iOS / iPhone only**. iOS-specific sync lifecycle code/config/tests stay under `iOS/`. Do not touch iPadOS, Android tablet, macOS, Windows, or Linux.

## Mission

Connect the established Gmail synchronization engine to iOS while respecting iOS lifecycle and background-execution constraints.

## Scope

- initial account sync
- incremental sync/cursors/history where supported by existing architecture
- pagination
- retry/backoff
- cancellation
- partial failure recovery
- stale/fresh/syncing/offline states
- foreground refresh
- iOS background refresh/task integration where justified
- per-account and aggregate sync state
- honest sync progress
- sync diagnostics without sensitive logging

Use the existing KMP sync engine rather than creating an iOS-specific duplicate.

## UX

Implement the established Gmail → Mail Organizer retrieval concept. Determinate progress only when trustworthy; otherwise indeterminate. Never fake a percentage.

## Validation

Test first sync, incremental sync, interruption, process termination, expired auth, offline start, network recovery, duplicate prevention, account isolation, and background scheduling behavior. Do not claim exact background execution cadence.

**STOP AFTER PHASE 4.**
