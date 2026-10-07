# Phase 13 — Mailbox Data Model & Message Presentation

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is iPadOS only. All iPadOS-specific artifacts belong under iPadOS/. Never touch iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Shared KMP business/data logic remains common.

## MISSION

Build the iPad mailbox data/presentation layer over synchronized shared data. Define list/detail view models without duplicating domain logic. Render sender, subject, preview, date/time, receiving Gmail account, unread state, star state and relevant labels. Implement sanitized HTML/plain-text message rendering, safe URL handling, attachment metadata boundaries and thread navigation. Test long subjects, malformed HTML, missing sender data, huge threads, deleted messages, empty states and VoiceOver. Do not implement advanced category/company intelligence yet.

## ACCEPTANCE / VALIDATION

Inspect the actual repository and prior evidence. Implement only this phase. Run targeted automated tests, then iPad Simulator and real iPad validation where available. Test multiple viewport states relevant to the feature and exercise failure/recovery paths. Fix, rebuild/reinstall/retest and record exact evidence. Update status/rules only when genuinely required.

**STOP AFTER THIS PHASE.**