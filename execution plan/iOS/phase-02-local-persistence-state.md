# Phase 2 — iOS Local Persistence & State Integration

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is **iOS / iPhone only**. All iOS-specific artifacts belong under `iOS/`. Do not touch iPadOS, Android tablet, macOS, Windows, or Linux. Do not duplicate KMP persistence logic in Swift.

## Mission

Connect the existing KMP local persistence/state architecture to the iOS application safely.

## Scope

- inspect and reuse the existing KMP database/state layer
- iOS initialization and lifecycle integration
- account-scoped local state
- migrations and schema version handling
- cache/state hydration
- corruption/failure handling
- secure separation of secrets from ordinary state
- test data isolation
- clean uninstall/reinstall semantics

The Gmail cloud remains authoritative. Local state must be rebuildable where possible.

## Validation

Test fresh install, restart, process termination, migration, corrupted/invalid local state handling, account separation, and reinstall. Verify no tokens or sensitive credentials are placed in ordinary database records or logs.

Do not implement the Gmail sync engine yet.

**STOP AFTER PHASE 2.**
