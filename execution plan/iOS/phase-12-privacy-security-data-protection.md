# Phase 12 — iOS Privacy, Security & Data Protection

## MANDATORY PLATFORM ISOLATION — READ FIRST

Target: **iOS / iPhone only**. All iOS-specific security/config/docs/tests remain under `iOS/`. Do not touch sibling platforms.

## Mission

Harden the iOS application and make privacy/data handling explicit and verifiable.

## Scope

- Keychain storage review
- token lifecycle
- data minimization
- account isolation
- local database protection
- sensitive notification handling
- logging/redaction
- URL/deep-link validation
- email HTML sanitization
- attachment handling
- pasteboard/privacy-sensitive surfaces
- screenshots/background snapshot privacy where appropriate
- exported URL schemes/universal links
- entitlements audit
- network transport security
- dependency/security audit
- privacy center hooks
- account disconnect/data cleanup

Do not make unsupported legal/compliance claims.

## Validation

Perform static inspection, dependency audit, secret scan, log review, entitlement review, network/config review, malicious HTML tests, hostile URLs, malformed data, and cross-account attack tests.

**STOP AFTER PHASE 12.**
