# Phase 6 — KMP Framework Integration & Dependency Boundary

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is iPadOS only. All iPadOS-specific artifacts belong under iPadOS/. Never touch iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Shared KMP business/data logic remains common.

## MISSION

Integrate the actual shared KMP framework into iPadOS production-shaped dependency boundaries. Expose only required use cases/repositories/state flows to SwiftUI. Handle Kotlin/Native lifecycle, coroutine observation, threading and cancellation safely. Remove accidental duplicate business logic from the iPad layer where proven. Add integration tests around representative shared use cases and failure propagation. Validate Debug/Release, simulator/device architectures, cancellation, lifecycle restart and error mapping.

## ACCEPTANCE / VALIDATION

Inspect actual repository and prior phase evidence. Implement only this phase. Run targeted unit/integration tests and runtime tests on iPad Simulator and real iPad where available. Exercise success, failure, cancellation and recovery paths appropriate to the phase. Inspect logs and persisted state. Fix, rebuild/reinstall/retest. Record exact evidence. Update status and permanent rules only when genuinely required.

**STOP AFTER THIS PHASE.**