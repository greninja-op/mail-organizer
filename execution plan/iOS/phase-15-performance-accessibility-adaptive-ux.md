# Phase 15 — iOS Performance, Accessibility & Adaptive UX

## MANDATORY PLATFORM ISOLATION — READ FIRST

This phase is **iOS / iPhone only**. All iOS-specific performance tooling, UI changes, tests, and documentation remain under `iOS/`. Never create or modify `iPadOS/`, `AndroidTablet/`, `macOS/`, `Windows/`, or `Linux/`.

## Mission

Optimize the complete iPhone experience for sustained responsiveness, memory, battery, accessibility, and thermal behavior.

## Required work

1. Profile launch, mailbox scrolling, search, sync, thread opening, account switching, and background/foreground transitions.
2. Fix SwiftUI rendering inefficiencies, unnecessary recomposition/state churn, image work, and unbounded lists.
3. Verify lazy/windowed rendering for large inboxes and long threads.
4. Verify cache limits and memory release under pressure.
5. Reduce unnecessary network, prefetch, background, and concurrent work.
6. Make expensive work cancellable and lifecycle-aware.
7. Respect Low Power Mode and Reduce Motion where applicable.
8. Complete VoiceOver labels, traits, focus order, Dynamic Type, large accessibility sizes, contrast, and hit targets.
9. Verify light/dark mode and common iPhone screen sizes.
10. Verify the UI remains usable during stale/offline/sync/error/auth states.
11. Avoid hard-coded assumptions about 60/90/120 Hz displays. Smoothness must be measured as sustained behavior, not claimed from hardware refresh rate.

## Validation

Use Xcode/Instruments or equivalent available profiling tools. Test realistic and large datasets, cold/warm launch, rapid navigation, repeated search, long threads, repeated sync, account switching, memory pressure, Low Power Mode, Reduce Motion, VoiceOver, and large Dynamic Type.

Fix only defects necessary for this phase; do not begin new product features.

**STOP AFTER PHASE 15.**
