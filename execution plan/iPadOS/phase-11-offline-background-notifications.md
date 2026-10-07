# Phase 11 — iPadOS Offline, Background Refresh & Notifications

## MANDATORY PLATFORM ISOLATION — READ FIRST

iPadOS only. All iPadOS-specific work belongs under iPadOS/. Never modify iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Keep business/data logic in shared KMP.

## Mission

Make the workspace truthful through offline/suspension/termination. Cached list/detail, stale indicators, reconnect/reconcile, background refresh where justified, notification categories/actions, privacy, deduplication, deep links, auth-required states, battery-aware scheduling. Test airplane mode, flaky network, suspension, force termination, delayed background execution, duplicate notifications, denied notification permission and stale cache. Never promise exact background cadence.

## iPad workspace requirement

Use the larger viewport for meaningful contextual information, not decoration. Prefer adaptive sidebar/list/detail/inspector layouts when available. Preserve a usable fallback at narrow split widths. Pointer, keyboard, touch, Dynamic Type and VoiceOver must remain first-class.

## Completion protocol

Inspect actual repository. Implement only this phase. Build and test. Exercise multiple window sizes, orientations and relevant input modes. Fix and retest. Record exact evidence. Update documentation/status and permanent rules only when required.

**STOP AFTER THIS PHASE.**