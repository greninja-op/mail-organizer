# Phase 18 — iPadOS Final Production Hardening & Release

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is iPadOS only. All iPadOS-specific artifacts belong under iPadOS/. Never create or modify iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Shared KMP business/data source remains common.

## Mission and required work

Final audit of every iPadOS phase and master document. Recheck shared-core boundary, Gmail source of truth, sync/account isolation, OAuth/scopes/security, unified inbox, category/company workspace, search, classification/priority/action, rules/temporal/conversation, Action Engine, Calendar/Tasks, offline/background/notifications, AI boundary, automation safety, privacy/Keychain/entitlements, accessibility, all viewport/window modes, pointer/keyboard, performance/memory/battery/thermal, large datasets, migrations/recovery, release archive/signing, App Store metadata/data declarations, OAuth production configuration, dependencies/secret scan and removal of debug bypasses. Run release smoke path: launch→OAuth→sync→multi-column All Inbox→select message/detail→search→company filter→star→Action Required→account switch→offline→reconnect. Decision RELEASE READY only with evidence; otherwise RELEASE BLOCKED with severity/evidence/remediation. Do not claim App Store approval or publish automatically. STOP AFTER PHASE 18.

## Completion protocol

Read root instructions and iPadOS master documents. Inspect actual repository and previous phase evidence. Implement only this phase. Build, automated-test, run on iPad Simulator and real iPad where available, inspect logs/screenshots and perform viewport/accessibility/input/security QA. Fix failures, rebuild and retest. Update status and permanent rules only when genuinely required. Never claim unrun tests.

**STOP AFTER THIS PHASE.**