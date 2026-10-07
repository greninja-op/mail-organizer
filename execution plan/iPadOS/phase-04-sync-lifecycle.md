# Phase 4 — Gmail Sync & iPadOS Lifecycle

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is an iPadOS-only phase. All iPadOS-specific work belongs under iPadOS/. Never create or modify iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Shared KMP/common source remains shared; never duplicate common business logic into iPadOS.

## Mission

Reuse common sync engine; initial/incremental sync, cursors/history, pagination, retry/backoff, cancellation, partial failure, per-account/aggregate state; foreground refresh and justified background scheduling; truthful determinate/indeterminate sync UX; Stage Manager/window changes must not corrupt sync; test interruption, process termination, auth expiry, offline, recovery, duplicates, multi-account and background constraints; STOP.

## Required completion protocol

Inspect root instructions and iPadOS requirements/spec/design/editor-rules. Inspect actual repository before implementation. Implement only this phase. Build and run. Exercise realistic and failure states. Fix failures. Rebuild and retest. Update status and permanent rules only when genuinely required. Never claim unrun tests or fabricate evidence.

**STOP AFTER THIS PHASE.**