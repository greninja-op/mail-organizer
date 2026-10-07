# Phase 0 — iPadOS Architecture, Shared-Core & Display Audit

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is an iPadOS-only phase. All iPadOS-specific implementation, tests and documents belong under iPadOS/. Never create or modify iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Shared KMP/mobile business and data logic remains common.

## MISSION

Audit the actual repository before implementation. Inventory KMP shared modules, Android, iOS and any existing iPad target; classify relevant components EXISTING/PARTIAL/MISSING/DUPLICATE/UNSAFE. Map exactly which source files must remain common and which presentation/OS integrations belong to iPadOS. Audit Kotlin/Native compatibility, Xcode/SwiftUI target, dependencies, persistence, OAuth, sync, notifications, Calendar/Tasks, tests and build tooling. Perform a display/viewport audit across portrait, landscape, narrow split, medium split, full screen and Stage Manager. Identify information that can be simultaneously exposed on iPad without clutter. Define content-driven layout states and risks. Do not implement later product features. Produce an evidence-based audit, baseline build/test commands, blockers and permanent platform-boundary rule.

## REQUIRED VALIDATION

Inspect the actual repository before deciding. Implement only this phase. Run exact relevant build and automated tests, then runtime validation on iPad Simulator and a real iPad when available. Test failure and recovery states relevant to the phase. Fix failures, rebuild/reinstall/retest, record evidence, update status and permanent editor rules only when genuinely required.

**STOP AFTER THIS PHASE.**