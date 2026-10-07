# Phase 13 — iOS Optional AI Fallback

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is **iOS / iPhone only**. All iOS-specific settings, UI, tests, and provider configuration remain under `iOS/`. No sibling platform work.

## Mission

Expose the established optional AI fallback architecture on iPhone without making AI authoritative.

## Scope

- AI enabled/disabled state
- provider capability/status UI
- deterministic-first routing
- confidence thresholds
- structured output validation
- prompt-injection defense
- context minimization
- sensitive-data controls
- provider privacy disclosures
- failure/offline behavior
- cost/usage visibility where supported
- AI-related privacy controls
- AI provenance/explanation

AI may assist classification/temporal/conversation/action suggestions but may not authorize Gmail, Calendar, Tasks, automation, filesystem, shell, browser, or external actions.

Never present fabricated AI output as verified fact.

## Validation

Test AI disabled, unavailable provider, malformed output, adversarial email prompt injection, sensitive content policy, offline operation, timeout/retry, and deterministic fallback.

**STOP AFTER PHASE 13.**
