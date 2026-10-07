# Phase 16 — Search & Local Index

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is iPadOS only. All iPadOS-specific artifacts belong under iPadOS/. Never touch iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Shared KMP business/data logic remains common.

## MISSION

Implement the iPad search workspace over the common local index. Support sender, subject, body, thread, company, domain, category, priority, action and Gmail labels where available. Provide keyboard-friendly search, cancellation, pagination, recent-search behavior with privacy controls, stale-index handling and rich result/detail context. Test special characters, partial terms, empty queries, huge datasets, deleted messages, stale index, offline search, account switching and performance. No second search database.

## ACCEPTANCE / VALIDATION

Inspect actual repository and prior evidence. Implement only this phase. Run targeted unit/integration tests plus iPad Simulator and real iPad validation where available. Test multiple viewport modes, accessibility and relevant failure/recovery paths. Fix, rebuild/reinstall/retest and record exact evidence. Update status/rules only when genuinely required.

**STOP AFTER THIS PHASE.**