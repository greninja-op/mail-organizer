# Phase 1 — iOS Application Shell & KMP Integration

## MANDATORY PLATFORM ISOLATION — READ FIRST

This phase targets **iOS / iPhone only**. All iOS-specific source, configuration, assets, tests, and documents must live under `iOS/`. Never create or modify `iPadOS/`, `AndroidTablet/`, `macOS/`, `Windows/`, or `Linux/`. Shared KMP changes must remain shared and must be justified by this phase.

## Mission

Create the real iPhone application shell and establish a clean SwiftUI ↔ KMP integration boundary without implementing the complete mailbox yet.

## Scope

- iOS application entry point and scene lifecycle
- SwiftUI navigation shell
- dependency/bootstrap boundary
- KMP framework/XCFramework integration
- environment/configuration separation
- centralized iOS design tokens
- basic loading/error/empty/offline state primitives
- development diagnostics that do not leak secrets
- simulator build and launch

## Requirements

The shell must be capable of hosting later mailbox screens without requiring a rewrite. Shared use cases must be injected through stable interfaces rather than directly coupling SwiftUI views to repositories.

Use iOS-native lifecycle and state handling. Keep platform concerns out of shared business logic unless represented through explicit interfaces.

## Validation

- clean iOS build
- unit tests for any new boundary logic
- Simulator install/launch/terminate/relaunch
- verify orientation and common iPhone sizes
- verify light/dark mode
- verify Dynamic Type basics
- inspect logs for accidental tokens or email content

Do not implement OAuth, Gmail sync, mailbox intelligence, Calendar, Tasks, automation, or AI beyond stubs/contracts required for compilation.

**STOP AFTER PHASE 1.**
