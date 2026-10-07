# Phase 14 — iPad Mail Workspace & Conversation Viewer

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is iPadOS only. All iPadOS-specific artifacts belong under iPadOS/. Never touch iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Shared KMP business/data logic remains common.

## MISSION

Turn the mailbox into the full iPad workspace. Build adaptive sidebar/category context + persistent list + selected conversation/detail where width permits. Preserve list context while reading. Add thread grouping, conversation navigation, message-level actions already supported by shared contracts, attachment presentation, loading/stale/offline/error states and selection restoration. Verify narrow split fallback, landscape/wide layouts, Stage Manager resizing, large Dynamic Type, pointer/keyboard navigation, large threads and rapid selection changes. Do not implement new backend intelligence in this phase.

## ACCEPTANCE / VALIDATION

Inspect the actual repository and prior evidence. Implement only this phase. Run targeted automated tests, then iPad Simulator and real iPad validation where available. Test multiple viewport states relevant to the feature and exercise failure/recovery paths. Fix, rebuild/reinstall/retest and record exact evidence. Update status/rules only when genuinely required.

**STOP AFTER THIS PHASE.**