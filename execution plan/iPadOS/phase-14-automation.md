# Phase 14 — iPadOS Advanced Automation

## MANDATORY PLATFORM ISOLATION — READ FIRST

iPadOS only. All iPadOS-specific work belongs under iPadOS/. Never modify iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Keep business/data logic in shared KMP.

## Mission

Expose safe automation builder/list/detail/history in a tablet workspace; finite triggers/conditions/actions, account scope, preview/dry-run, confirmation, enable/disable/delete/re-enable, idempotency/loop protection, background constraints, notifications, circuit breaker and destructive safeguards. No arbitrary scripts or automatic send/reply/forward/permanent delete. Test duplicate/retry/partial failure/stale schedule/account removal/offline/background interruption/malicious email.

## iPad workspace requirement

Use the larger viewport for meaningful contextual information, not decoration. Prefer adaptive sidebar/list/detail/inspector layouts when available. Preserve a usable fallback at narrow split widths. Pointer, keyboard, touch, Dynamic Type and VoiceOver must remain first-class.

## Completion protocol

Inspect actual repository. Implement only this phase. Build and test. Exercise multiple window sizes, orientations and relevant input modes. Fix and retest. Record exact evidence. Update documentation/status and permanent rules only when required.

**STOP AFTER THIS PHASE.**