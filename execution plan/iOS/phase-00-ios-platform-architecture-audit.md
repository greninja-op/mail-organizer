# Phase 0 — iOS Platform Architecture Audit & Boundary Foundation

## MANDATORY PLATFORM ISOLATION — READ FIRST

This phase targets **iOS / iPhone only**.

All iOS-specific files created by this phase must live under repository-root `iOS/`. Do not create or modify `iPadOS/`, `AndroidTablet/`, `macOS/`, `Windows/`, or `Linux/`. Do not duplicate shared KMP logic into an iOS copy. Shared KMP changes are allowed only when evidence shows they are required for iOS.

Persist this isolation rule as a permanent repository rule.

## Mission

Audit the actual Mail Organizer repository and establish a verified implementation boundary for iOS without prematurely implementing later product functionality.

## Required work

1. Read the root project instructions and all iOS master documents.
2. Inspect the complete repository structure.
3. Identify the existing KMP shared module, Android application, persistence, networking, Gmail/OAuth, domain models, use cases, sync, classification, search, Action Engine, integrations, automation, AI, tests, and build tooling.
4. Determine whether an iOS target already exists and classify every relevant component as EXISTING, PARTIAL, MISSING, DUPLICATE, or UNSAFE.
5. Identify exactly where iOS-specific source/config/assets/tests can live under `iOS/` without breaking the existing KMP architecture.
6. Inspect Kotlin/Native/iOS compatibility and current dependency versions.
7. Inspect existing Apple project configuration, if any, without assuming it is production-ready.
8. Map KMP shared APIs that SwiftUI will consume.
9. Identify authentication, secure storage, lifecycle, notification, background-refresh, Calendar/Reminders, and networking boundaries.
10. Record architecture risks and blockers.

## Do not

- Do not implement Gmail features.
- Do not redesign the Android application.
- Do not implement iPadOS.
- Do not create Windows/Linux/macOS projects.
- Do not introduce a second architecture.
- Do not claim iOS support merely because KMP can compile.

## Required deliverables

Create/update only iOS planning/audit material under `iOS/` and genuinely necessary permanent root rules.

Report:
- repository inventory
- current iOS readiness
- shared KMP integration map
- iOS-specific folder boundary
- dependency/toolchain findings
- security/privacy risks
- blockers
- exact validation performed
- files changed
- next phase recommendation

Build only enough to establish a trustworthy baseline; do not implement later-phase functionality.

## Definition of done

The repository's real iOS readiness and boundaries are documented, the iOS folder rule is permanent, no unrelated platform work occurred, and baseline checks are reproducible.

**STOP AFTER PHASE 0.**
