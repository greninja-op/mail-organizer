# Phase 3 — Google OAuth & Gmail iPadOS Connection

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is an iPadOS-only phase. All iPadOS-specific work belongs under iPadOS/. Never create or modify iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Shared KMP/common source remains shared; never duplicate common business logic into iPadOS.

## Mission

Official Google OAuth/browser flow; iPad presentation; callback/deep-link boundary; Keychain; token refresh/expiry; account identity; least privilege/incremental scopes; disconnect/revoke; cancellation/retry; no password form; inspect entitlements/URL schemes/logs/account isolation; test success/cancel/error/expired token/revocation; do not implement broad sync; STOP.

## Required completion protocol

Inspect root instructions and iPadOS requirements/spec/design/editor-rules. Inspect actual repository before implementation. Implement only this phase. Build and run. Exercise realistic and failure states. Fix failures. Rebuild and retest. Update status and permanent rules only when genuinely required. Never claim unrun tests or fabricate evidence.

**STOP AFTER THIS PHASE.**