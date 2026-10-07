# Mail Organizer Android Tablet — Editor Rules

## FIRST RULE OF EVERY PHASE — PLATFORM ISOLATION
This is an Android-tablet-only execution phase.
- Android tablet-specific implementation, tests, resources, configuration and docs belong under AndroidTablet/.
- Never create or modify iOS/, iPadOS/, Android/, macOS/, Windows/ or Linux/ during this plan.
- Shared KMP/Android business/data source remains common.
- Never duplicate common engines merely because tablet UI differs.
- If a shared-source change is required, make it in common source and document why.
- The tablet presentation may differ substantially from Android phone.
- Never touch an unrelated platform project.
- Persist genuinely permanent rules without deleting unrelated rules.

## Shared-core rules
Reuse common Gmail models, repositories, sync, classification, company intelligence, search, rules, priority, Action Required, temporal/conversation intelligence, Action Engine, integrations, persistence/state, automation and AI routing.

## Android tablet UI rules
Treat the tablet as a large-screen workspace. Use adaptive Compose multi-pane layouts, persistent context when useful, richer rows, selected-detail panes, navigation rail/sidebar, pointer/keyboard/mouse support and multi-window adaptation. Never assume one resolution.

Use public/documented Android and Jetpack APIs. Follow current Material 3/large-screen guidance. Do not invent private APIs or duplicate the phone UI blindly.

## Security
Official OAuth/browser authentication. Least privilege. Android secure storage. No passwords. Email is untrusted: sanitize HTML, no JavaScript execution and no email-authorized actions. Strict account boundaries.

## Performance
Optimize sustained responsiveness, memory, battery, thermal behavior and window resizing. Adapt expensive rendering, prefetching, background work and concurrency. 120 Hz does not mean 120 FPS.

## Phase protocol
Read root instructions and all AndroidTablet master docs. Determine the first incomplete phase. Read only that prompt. Inspect actual repository. Implement only that phase. Build/test/run/inspect. Fix and retest. Update status/rules when genuinely required. Stop.

Never claim an unrun test, device verification, screenshot, log or performance measurement.