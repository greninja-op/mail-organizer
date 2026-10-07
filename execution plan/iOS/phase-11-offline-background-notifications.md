# Phase 11 — iOS Offline, Background Refresh & Notifications

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is **iOS / iPhone only**. All iOS-specific background, notification, and UI artifacts belong under `iOS/`. No sibling platforms.

## Mission

Make the iPhone application resilient across offline periods, suspension, termination, and notification delivery.

## Scope

- cached mailbox state
- offline search/read behavior where available
- stale indicators
- reconnect/reconcile
- BGTaskScheduler/background refresh where justified
- notification categories/actions
- notification privacy
- duplicate suppression
- deep links
- authentication-required notifications
- cancellation and retry
- battery-aware scheduling

Do not promise exact background execution times.

Notifications must not reveal sensitive email content on a locked screen beyond the user's configured privacy policy.

## Validation

Test airplane mode, flaky network, app suspension, force termination, reboot where practical, delayed background execution, duplicate notifications, notification permission denial, and stale cache.

**STOP AFTER PHASE 11.**
