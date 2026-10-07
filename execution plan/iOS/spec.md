# Mail Organizer iOS — Execution Specification

## Authority

For the iOS execution plan, use this order when resolving decisions:

1. iOS requirements
2. iOS specification
3. iOS design contract
4. iOS editor rules
5. Existing repository architecture and verified decisions
6. Official Apple/Google/Kotlin documentation
7. Engineering judgment

Never invent repository state. Inspect it.

## Status

Use:
- `[ ]` not started
- `[-]` in progress
- `[x]` complete and verified
- `[!]` blocked

A phase may be marked complete only after its implementation and verification evidence exists.

## Sequential execution

The editor must execute the first incomplete phase only. A phase must not start work belonging to later phases merely because dependencies are convenient.

Every phase follows:

inspect → plan against actual code → implement only phase → build → automated tests → simulator/runtime tests → real-device tests where available → visual/accessibility/security checks → fix → rebuild/reinstall/retest → documentation/status → stop.

Never report a test, device run, screenshot, log, or validation that was not actually performed.

## iOS boundary

Every phase begins with the iOS isolation block from `README.md`. All iOS-specific work belongs under `iOS/`. No sibling platform work is permitted.

## Shared KMP boundary

Shared KMP changes are permitted only where required for iOS functionality. Prefer shared abstractions over duplicated logic. Do not move Android behavior into iOS-specific copies.

## Core iOS UI contract

SwiftUI must present a Gmail-familiar but original interface:
- top navigation/search/account affordances appropriate to iPhone
- clear drawer/sidebar-equivalent navigation appropriate to iPhone
- All Inbox, Primary, Promotional, Social, Spam, Starred
- account identity distinct from sender/company
- company filtering inside a category
- company pinning separate from email star
- honest sync state
- real loading/error/offline/empty states
- Dynamic Type and VoiceOver support
- light/dark mode
- no fake Google credential form
- no AI-generated functional UI artwork
- centralized design tokens

## Performance contract

Optimize for sustained responsiveness, battery, memory, storage, and thermal behavior rather than synthetic peak FPS.

Respect iOS scheduling and display behavior. Do not equate a 120 Hz display with a guaranteed 120 FPS application. Reduce expensive animation, prefetching, background work, cache pressure, and concurrency on constrained conditions.

## Release contract

Production release must use real Apple signing/provisioning/App Store configuration, production OAuth configuration, accurate privacy/data declarations, correct entitlements, no debug bypasses, no test credentials, no verbose sensitive logging, and a clean archive.

Do not publish automatically. Final release is a human-controlled decision.
