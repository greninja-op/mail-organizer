# Phase 17 — iOS Production Signing, App Store & OAuth Preparation

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is **iOS / iPhone only**. All iOS release configuration, signing references, App Store metadata, screenshots, entitlements, and tests belong under `iOS/`. Do not touch iPadOS, Android tablet, macOS, Windows, or Linux.

## Mission

Prepare a production-quality iPhone build and release configuration without publishing it.

## Required work

- Verify bundle identifier and release application identity.
- Configure release build settings and environment separation.
- Verify Apple Developer/App Store Connect identity, signing, provisioning, and capabilities.
- Audit entitlements and remove unnecessary capabilities.
- Configure production Google OAuth client identity and release redirect/deep-link behavior.
- Verify Gmail/Calendar/Tasks scopes are least-privilege and match actual product behavior.
- Prepare privacy policy and accurate App Store privacy/data declarations.
- Verify permissions strings are truthful and understandable.
- Prepare icon, screenshots, version/build numbers, support metadata, and release notes.
- Remove debug menus, test bypasses, test accounts, development endpoints, verbose logging, and fake data.
- Verify crash/error reporting configuration does not capture secrets or unnecessary email content.
- Verify network/security configuration and production API endpoints.
- Verify AI disclosure and optionality.
- Verify local data/backup behavior and account deletion/disconnect behavior.
- Produce a reproducible release archive.

Never commit signing certificates, private keys, OAuth secrets, API keys, or provisioning secrets.

Do not publish, submit, or release automatically.

## Validation

Build a clean release archive. Inspect its bundle identifier, signing state, entitlements, embedded configuration, linked frameworks, assets, permissions, deep links, OAuth release identity, and absence of debug/test code. Run the release build on Simulator and a real iPhone when available.

**STOP AFTER PHASE 17.**
