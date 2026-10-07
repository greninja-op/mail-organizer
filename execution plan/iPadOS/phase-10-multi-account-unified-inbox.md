# Phase 10 — iPadOS Multi-Account & Unified Inbox

## MANDATORY PLATFORM ISOLATION — READ FIRST

iPadOS only. All iPadOS-specific work belongs under iPadOS/. Never modify iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Keep business/data logic in shared KMP.

## Mission

Build tablet account workspace: current/connected/add account, unified All Inbox, source receiving-account identity, account-specific sync/recovery/search/rules/company/actions, account removal cleanup, cross-account action protection. Use the real OAuth→sync→recovery path. Test cross-account search/cache/notifications/rules/actions/company filters and unified selection/detail state; fail closed.

## iPad workspace requirement

Use the larger viewport for meaningful contextual information, not decoration. Prefer adaptive sidebar/list/detail/inspector layouts when available. Preserve a usable fallback at narrow split widths. Pointer, keyboard, touch, Dynamic Type and VoiceOver must remain first-class.

## Completion protocol

Inspect actual repository. Implement only this phase. Build and test. Exercise multiple window sizes, orientations and relevant input modes. Fix and retest. Record exact evidence. Update documentation/status and permanent rules only when required.

**STOP AFTER THIS PHASE.**