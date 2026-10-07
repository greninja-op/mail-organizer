# Mail Organizer — Product Requirements

## Purpose
Mail Organizer is a privacy-first personal email organization and action layer over Gmail. Gmail remains the authoritative cloud source of truth. Mail Organizer adds local organization, intelligence, search, prioritization, action suggestions and optional integrations without replacing Gmail.

## Platforms
The product is intended for Android first and future iOS, iPadOS, macOS and Windows. Shared business/data logic must use Kotlin Multiplatform. Android uses Jetpack Compose; future Apple UI uses SwiftUI. The local database must be KMP-compatible, preferably Room Multiplatform or another justified SQLite-backed KMP solution.

## Gmail and privacy
Use official Google OAuth and Gmail APIs. Start with read-only Gmail access and least privilege. Never use passwords, cookies, scraping or AccessibilityService as the primary Gmail mechanism. All Gmail-derived data is account-scoped. Local processing is preferred; remote AI is optional and opt-in.

## Categories
Action Required; Important; Career; Education; Receipts & Orders; Security; Notifications; Newsletters; Promotions; Low Value.

## Intelligence
Deterministic classification first; sender/company intelligence; independent priority; Action Required detection; deadline/meeting extraction; conversation intelligence; local search; rules/corrections; analytics; optional AI fallback.

## Actions and integrations
Action suggestions are explainable, traceable to source mail, deduplicated and confirmed before external effects. Calendar and Tasks are modular integrations. Gmail writes are introduced only in the dedicated write phase.

## Local data
Persist only what Mail Organizer needs for offline use, search, derived intelligence, user corrections/rules, sync state and action relationships. Do not unnecessarily duplicate every Gmail attachment. Local state is rebuildable from Gmail; Mail Organizer-only rules/corrections require a later backup/sync strategy if they must survive uninstall.

## Quality
Accessibility, dark mode, responsive layouts, performance, battery safety, offline behavior, multi-account isolation, security, privacy, automated tests and real-device verification are mandatory. Build success alone is never sufficient.