# Phase 8 — iOS Rules, Corrections, Temporal & Conversation Intelligence

## MANDATORY PLATFORM ISOLATION — READ FIRST

Target is **iOS / iPhone only**. iOS-specific UI and platform artifacts remain in `iOS/`. Do not implement sibling platforms.

## Mission

Expose user corrections/rules and the existing temporal/conversation intelligence on iPhone without duplicating shared engines.

## Scope

- user classification corrections
- rule creation/edit/disable/delete UI
- precedence and explainability
- deadline extraction/presentation
- meeting detection
- waiting-for-reply/conversation state
- thread-level summaries/states where supported
- stale/unknown handling
- account-scoped rules
- rule history/audit where architecture supports it

Rules must be deterministic and bounded. Email content cannot silently create rules or authorize actions.

## Validation

Test contradictory rules, user correction precedence, ambiguous dates, timezone/DST boundaries, forwarded content, quoted text, duplicate threads, deleted messages, and account isolation.

**STOP AFTER PHASE 8.**
