# Phase 10 — iOS Multi-Account & Unified Inbox

## MANDATORY PLATFORM ISOLATION — READ FIRST

Target: **iOS / iPhone only**. All iOS-specific work belongs under `iOS/`. Never touch sibling platform implementations.

## Mission

Complete the multi-account iPhone experience with strict isolation and unified inbox behavior.

## Scope

- current/connected/add-account UI
- account-specific sync/recovery
- account switching
- swipe/profile account navigation where appropriate for iPhone
- unified All Inbox
- receiving-account identity on each row
- account-scoped search/rules/company intelligence/actions
- account removal and local data cleanup
- external object cleanup where supported
- cross-account action protection

Adding an account must reuse the real Gmail OAuth → sync → recovery flow.

## Adversarial validation

Attempt cross-account search, rule, action, company filter, cache, notification, and automation leakage. Verify the system fails closed.

**STOP AFTER PHASE 10.**
