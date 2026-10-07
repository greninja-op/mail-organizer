# Phase 3 — Google OAuth & Gmail iOS Connection

## MANDATORY PLATFORM ISOLATION — READ FIRST

Target: **iOS / iPhone only**. All iOS-specific OAuth/browser/configuration/tests/docs belong under `iOS/`. Never touch sibling platform plans or implementations. Shared OAuth/domain abstractions may change only when required.

## Mission

Implement production-shaped Google authentication for iPhone using official Google OAuth/browser mechanisms and least privilege.

## Scope

- official OAuth authorization flow
- secure callback/deep-link/universal-link boundary as appropriate
- Keychain-backed token handling
- account identity normalization
- token refresh/expiry/error states
- disconnect/revoke behavior
- least-privilege Gmail scopes
- incremental authorization architecture
- authentication UI states
- no-password rule
- account cancellation/retry

Never create a fake Google login form or ask users to enter Gmail passwords.

## Security validation

Inspect redirect handling, state/nonce where applicable, token storage, logs, screenshots, error messages, and account isolation. Confirm OAuth errors do not expose credentials.

Do not implement broad Gmail synchronization yet.

**STOP AFTER PHASE 3.**
