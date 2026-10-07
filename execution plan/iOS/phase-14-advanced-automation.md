# Phase 14 — iOS Advanced Automation

## MANDATORY PLATFORM ISOLATION — READ FIRST

Target: **iOS / iPhone only**. Put iOS-specific automation UI, scheduling integration, tests, and configuration under `iOS/`. Never touch sibling platforms.

## Mission

Expose the existing safe automation engine through iPhone-native controls.

## Scope

- automation list/detail/builder
- finite triggers
- structured conditions
- finite actions
- account scoping
- preview/dry-run
- confirmation levels
- enable/disable/delete/re-enable
- execution history
- idempotency and loop protection
- background execution integration within iOS limits
- notifications
- circuit breaker
- destructive-action safeguards
- AI cannot bypass safety

No arbitrary scripts and no automatic send/reply/forward/permanent-delete behavior.

## Validation

Test duplicate events, retries, partial failure, stale schedules, account removal, permission denial, offline execution, background interruption, protected categories, and malicious email content.

**STOP AFTER PHASE 14.**
