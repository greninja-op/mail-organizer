# Phase 4 — Adaptive Navigation & Multi-Column Workspace

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is an iPadOS-only phase. All iPadOS-specific implementation, tests and documents belong under iPadOS/. Never create or modify iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Shared KMP/mobile business and data logic remains common.

## MISSION

Implement the adaptive navigation workspace foundation. Build sidebar → list → detail architecture using current native SwiftUI navigation APIs where appropriate. Define content-driven width thresholds rather than device-name checks. Preserve selection and context when moving between columns. Support narrow split fallback, medium two-column behavior and wide three-column behavior. Establish keyboard focus, pointer hover/selection, contextual menu and touch interaction foundations. Test portrait, landscape, split widths, full-screen and Stage Manager resizing repeatedly, including rapid resize while state changes. Do not implement real Gmail data yet.

## REQUIRED VALIDATION

Inspect the actual repository before deciding. Implement only this phase. Run exact relevant build and automated tests, then runtime validation on iPad Simulator and a real iPad when available. Test failure and recovery states relevant to the phase. Fix failures, rebuild/reinstall/retest, record evidence, update status and permanent editor rules only when genuinely required.

**STOP AFTER THIS PHASE.**