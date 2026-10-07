# Phase 3 — iPadOS SwiftUI Shell & Scene Lifecycle

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is an iPadOS-only phase. All iPadOS-specific implementation, tests and documents belong under iPadOS/. Never create or modify iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Shared KMP/mobile business and data logic remains common.

## MISSION

Build the iPad SwiftUI application shell and scene lifecycle. Establish app entry, scene/window state, dependency/bootstrap boundary, navigation root, selection state and restoration foundation. Create reusable loading/empty/error/offline/auth-required state primitives. Keep business logic outside views and consume shared KMP use cases through explicit interfaces. Do not implement Gmail/OAuth/mail features beyond compile-time contracts. Validate cold launch, warm launch, terminate/relaunch, scene activation/deactivation, orientation changes, Dynamic Type, dark mode and multiple iPad simulator sizes.

## REQUIRED VALIDATION

Inspect the actual repository before deciding. Implement only this phase. Run exact relevant build and automated tests, then runtime validation on iPad Simulator and a real iPad when available. Test failure and recovery states relevant to the phase. Fix failures, rebuild/reinstall/retest, record evidence, update status and permanent editor rules only when genuinely required.

**STOP AFTER THIS PHASE.**