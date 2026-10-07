# Phase 2 — iPadOS Persistence & State Integration

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is an iPadOS-only phase. All iPadOS-specific work belongs under iPadOS/. Never create or modify iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Shared KMP/common source remains shared; never duplicate common business logic into iPadOS.

## Mission

Reuse common KMP persistence; connect iPad lifecycle/state hydration; account-scoped cache; migrations; corruption handling; uninstall/reinstall semantics; no duplicate Swift database; verify state survives restart but not unauthorized account transitions; fresh install/restart/process termination/migration/corruption/account separation/reinstall tests; secrets excluded from ordinary storage/logs; STOP.

## Required completion protocol

Inspect root instructions and iPadOS requirements/spec/design/editor-rules. Inspect actual repository before implementation. Implement only this phase. Build and run. Exercise realistic and failure states. Fix failures. Rebuild and retest. Update status and permanent rules only when genuinely required. Never claim unrun tests or fabricate evidence.

**STOP AFTER THIS PHASE.**