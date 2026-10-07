# Phase 17 — iPadOS Production Signing, App Store & OAuth Preparation

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is iPadOS only. All iPadOS-specific artifacts belong under iPadOS/. Never create or modify iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Shared KMP business/data source remains common.

## Mission and required work

Prepare production iPad release without publishing. Verify bundle identity, release configuration, Apple Developer/App Store Connect, signing/provisioning, capabilities/entitlements, production Google OAuth release identity, least-privilege scopes, URL/deep links, privacy/data declarations, permission strings, iPad screenshots/assets, version/build numbers, crash/logging configuration, network security, AI disclosure, backup/data behavior and archive reproducibility. Remove debug/test bypasses. Never commit signing keys, certificates, OAuth secrets or API keys. Inspect clean archive and release runtime.

## Completion protocol

Read root instructions and iPadOS master documents. Inspect actual repository and previous phase evidence. Implement only this phase. Build, automated-test, run on iPad Simulator and real iPad where available, inspect logs/screenshots and perform viewport/accessibility/input/security QA. Fix failures, rebuild and retest. Update status and permanent rules only when genuinely required. Never claim unrun tests.

**STOP AFTER THIS PHASE.**