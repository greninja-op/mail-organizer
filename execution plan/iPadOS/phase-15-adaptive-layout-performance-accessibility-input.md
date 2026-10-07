# Phase 15 — iPadOS Adaptive Layout, Performance, Accessibility & Input

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is iPadOS only. All iPadOS-specific artifacts belong under iPadOS/. Never create or modify iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Shared KMP business/data source remains common.

## Mission and required work

Perform a dedicated tablet optimization pass. Audit adaptive layout transitions, list virtualization, rendering/state churn, image/cache memory, search, large mailboxes/threads, window resizing, split-view transitions, Stage Manager, background work, battery and thermal behavior. Complete VoiceOver, Dynamic Type, Reduce Motion, pointer hover, keyboard shortcuts/focus, contextual menus and accessibility hit targets. Profile realistic workflows with Instruments/Xcode tools where available. Test cold/warm launch, scrolling, search, sync, thread opening, account switching, resizing and constrained memory. Never optimize only for benchmark FPS; ProMotion is not guaranteed 120 FPS.

## Completion protocol

Read root instructions and iPadOS master documents. Inspect actual repository and previous phase evidence. Implement only this phase. Build, automated-test, run on iPad Simulator and real iPad where available, inspect logs/screenshots and perform viewport/accessibility/input/security QA. Fix failures, rebuild and retest. Update status and permanent rules only when genuinely required. Never claim unrun tests.

**STOP AFTER THIS PHASE.**