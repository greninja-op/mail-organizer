# Mail Organizer iPadOS — Editor Rules

## FIRST RULE OF EVERY PHASE — PLATFORM ISOLATION
This is an iPadOS-only phase.
- iPadOS-specific source/config/assets/tests/docs belong under iPadOS/.
- Never create or modify iOS/, AndroidTablet/, macOS/, Windows/ or Linux/ during this plan.
- Shared KMP/common mobile source remains common.
- Do not copy common source into iPadOS merely because the UI differs.
- If common-source changes are necessary, make them in common source and document why.
- iPad presentation may differ substantially from iPhone/Android.
- Never touch unrelated platform projects.
- Persist this rule in root editor rules if applicable.

## Shared-core rules
Reuse shared sync, Gmail models, classification, company intelligence, search, rules, priority, temporal/conversation intelligence, Action Engine, integration abstractions, automation, AI routing and persistence/state contracts. No duplicate engines.

## iPad UI rules
Treat iPad as a workspace, not a phone. Use adaptive multi-column layouts, persistent context when useful, richer rows, selected-detail panes, sidebars, pointer/keyboard behavior and Stage Manager adaptation. Increase useful information density without sacrificing readability. Never assume a fixed resolution.

## Apple technology rules
Use public/documented Apple APIs and supported SDK capabilities. Apple-provided visual/system technologies may be used where appropriate. No private APIs or unofficial clones.

## Security
Official OAuth, least privilege, Keychain, sanitized email, no password collection, no email-authorized actions, strict account boundaries.

## Performance
Optimize sustained real behavior. Respect Low Power Mode, Reduce Motion, memory, battery, thermal constraints and display scheduling. ProMotion/120 Hz is not a 120 FPS promise.

## Phase protocol
Read root instructions and iPadOS master docs, identify the first incomplete phase, inspect the actual repository, implement only that phase, build/test/run/inspect, fix/retest, update documentation/status and STOP. Never claim unrun tests.
