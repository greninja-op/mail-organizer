# Phase 1 — iPadOS Project Target & Toolchain Foundation

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is an iPadOS-only phase. All iPadOS-specific implementation, tests and documents belong under iPadOS/. Never create or modify iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Shared KMP/mobile business and data logic remains common.

## MISSION

Establish the real iPadOS project target and toolchain without implementing the mailbox. Configure bundle/module boundaries, Swift/KMP framework consumption path, Debug/Release separation, simulator/device destinations, build settings and dependency resolution. Verify Apple Silicon simulator and physical-device architecture where available. Keep all iPad-specific files under iPadOS/. Do not duplicate common KMP code. Add only scaffolding required to make later phases deterministic. Validate clean build, install, launch and termination on multiple simulator configurations. Inspect logs for secrets.

## REQUIRED VALIDATION

Inspect the actual repository before deciding. Implement only this phase. Run exact relevant build and automated tests, then runtime validation on iPad Simulator and a real iPad when available. Test failure and recovery states relevant to the phase. Fix failures, rebuild/reinstall/retest, record evidence, update status and permanent editor rules only when genuinely required.

**STOP AFTER THIS PHASE.**