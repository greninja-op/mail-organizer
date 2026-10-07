# Phase 5 — Design Tokens, Theming & iPad Visual System

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is iPadOS only. All iPadOS-specific artifacts belong under iPadOS/. Never touch iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Shared KMP business/data logic remains common.

## MISSION

Establish shared semantic visual tokens without duplicating business constants. Define typography, spacing, colors, surfaces, shapes, icon semantics, motion and state tokens so Android/iPhone/iPad share product meaning while iPad can express its own layout. Implement light/dark and accessibility variants. Evaluate supported Apple visual materials, including Liquid Glass-era public APIs where appropriate, without private APIs or unofficial clones. Validate token consistency across sidebar/list/detail, Dynamic Type, contrast, Reduce Motion and narrow widths. Do not implement mailbox data.

## ACCEPTANCE / VALIDATION

Inspect actual repository and prior phase evidence. Implement only this phase. Run targeted unit/integration tests and runtime tests on iPad Simulator and real iPad where available. Exercise success, failure, cancellation and recovery paths appropriate to the phase. Inspect logs and persisted state. Fix, rebuild/reinstall/retest. Record exact evidence. Update status and permanent rules only when genuinely required.

**STOP AFTER THIS PHASE.**