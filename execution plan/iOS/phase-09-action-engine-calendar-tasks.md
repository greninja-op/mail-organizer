# Phase 9 — iOS Action Engine, Calendar & Tasks

## MANDATORY PLATFORM ISOLATION — READ FIRST

This phase targets **iOS / iPhone only**. Keep iOS-specific integrations and UI under `iOS/`. Do not touch other platform plans.

## Mission

Expose safe Action Cards and integrate calendar/task capabilities through explicit, user-controlled boundaries.

## Scope

- Action Cards
- action explanations/provenance
- deduplication/idempotency
- confirmation levels
- Calendar integration abstraction
- iOS Calendar/EventKit adapter where required
- task integration abstraction and supported iOS task target
- permission states
- account association
- failure/retry
- undo/recovery where possible

No email may authorize an external action. AI is not allowed to bypass the Action Engine.

## Validation

Test permission denial, revoked permission, duplicate action attempts, conflicting calendar/task state, offline behavior, account removal, and user cancellation.

**STOP AFTER PHASE 9.**
