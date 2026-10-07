# Phase 13 — iPadOS Optional AI Fallback

## MANDATORY PLATFORM ISOLATION — READ FIRST

iPadOS only. All iPadOS-specific work belongs under iPadOS/. Never modify iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Keep business/data logic in shared KMP.

## Mission

Expose existing optional AI architecture; deterministic-first routing, confidence, structured validation, prompt-injection defense, context minimization, sensitive-data controls, provider disclosure, failure/offline fallback, usage visibility and provenance. AI cannot authorize Gmail/Calendar/Tasks/automation or external actions. Test disabled/unavailable/malformed/adversarial/offline/timeout cases.

## iPad workspace requirement

Use the larger viewport for meaningful contextual information, not decoration. Prefer adaptive sidebar/list/detail/inspector layouts when available. Preserve a usable fallback at narrow split widths. Pointer, keyboard, touch, Dynamic Type and VoiceOver must remain first-class.

## Completion protocol

Inspect actual repository. Implement only this phase. Build and test. Exercise multiple window sizes, orientations and relevant input modes. Fix and retest. Record exact evidence. Update documentation/status and permanent rules only when required.

**STOP AFTER THIS PHASE.**