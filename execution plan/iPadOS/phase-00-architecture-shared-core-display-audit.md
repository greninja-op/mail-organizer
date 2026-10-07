# Phase 0 — iPadOS Platform Architecture, Shared-Core & Display Audit

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is an iPadOS-only phase. All iPadOS-specific work belongs under iPadOS/. Never create or modify iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Shared KMP/common source remains shared; never duplicate common business logic into iPadOS.

## Mission

Audit actual repo; classify shared KMP, Android, iOS and iPad targets EXISTING/PARTIAL/MISSING/DUPLICATE/UNSAFE; map common source vs UI; inspect SwiftUI/iPad target, SDK/dependencies, NavigationSplitView/window APIs, Stage Manager, pointer/keyboard, persistence, OAuth, sync, notifications, Calendar/Tasks; inventory every screen and determine what information can coexist at iPad widths; establish content-driven breakpoints; create permanent isolation/common-source rule; baseline build only; no later functionality; validate Simulator sizes and real iPad where available; report evidence, blockers and exact commands; STOP.

## Required completion protocol

Inspect root instructions and iPadOS requirements/spec/design/editor-rules. Inspect actual repository before implementation. Implement only this phase. Build and run. Exercise realistic and failure states. Fix failures. Rebuild and retest. Update status and permanent rules only when genuinely required. Never claim unrun tests or fabricate evidence.

**STOP AFTER THIS PHASE.**