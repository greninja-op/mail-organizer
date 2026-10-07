# Mail Organizer — Product Requirements

## Purpose
Mail Organizer is a privacy-first personal email organization and action layer over Gmail. Gmail remains the authoritative cloud source of truth. Mail Organizer adds local organization, intelligence, search, prioritization, action suggestions and optional integrations without replacing Gmail.

## Platforms
The product is intended for Android first and future iOS, iPadOS, macOS and Windows. Shared business/data logic must use Kotlin Multiplatform. Android uses Jetpack Compose; future Apple UI uses SwiftUI. The local database must be KMP-compatible, preferably Room Multiplatform or another justified SQLite-backed KMP solution.

## Gmail and privacy
Use official Google OAuth and Gmail APIs. Start with read-only Gmail access and least privilege. Never use passwords, cookies, scraping or AccessibilityService as the primary Gmail mechanism. All Gmail-derived data is account-scoped. Local processing is preferred; remote AI is optional and opt-in.

## Categories
Action Required; Important; Career; Education; Receipts & Orders; Security; Notifications; Newsletters; Promotions; Low Value.

## Required Android mailbox navigation
The Android navigation drawer must provide these core destinations:
- All Inbox
- Primary
- Promotional
- Social
- Spam
- Starred

The drawer is a navigation/filter surface, not a permanent list of every detected company.

All Inbox is a unified presentation across connected Gmail accounts. Each message shown in All Inbox must retain visible source-mailbox identity, such as a compact circular account indicator, so users can distinguish which Gmail address received the message.

Promotional and Social remain organized destinations even when the product suppresses their noise from the primary experience. Promotional mail must not be silently deleted merely because it is promotional.

Spam is a first-class destination. New/unread spam should have a visible unread indicator (for example, a red dot), and folder counts may be shown when useful. Users must be able to recover a legitimate message from Spam. Exact aging/count rules are implementation details to be defined later.

Starred is a global view of individually starred emails. Starring an email must not remove it from its original category.

## Account switching
Multiple Gmail accounts are first-class. The Android UI must provide a familiar Google-style account switcher with:
- current account identity
- connected account list
- add another account
- account-specific sync/recovery state

Swiping on the profile/account affordance should switch to the next account immediately where the gesture is supported. Adding a new account must reuse the real Gmail → Mail Organizer synchronization/recovery experience when data retrieval takes meaningful time.

## Company grouping and filtering
Company/sender intelligence must support grouping messages inside a selected mailbox/category.

Example:
Promotional → companies → Google / Facebook / Amazon → selected company mail.

Company selection is a filter within the currently selected category; it is not a new top-level navigation destination.

Company pinning is separate from email starring. Users may pin a detected company so that company appears at the top of the company filter list for the relevant category. Pinning a company must not move its emails out of their category and must not create a global navigation item.

## Individual email starring
Each message row should expose a familiar Star control. A starred message remains in its existing category and additionally appears in Starred. Starred state is user intent and must be independent of automated category classification.

## Search
The Android top app bar must include a Gmail-familiar search affordance at the top. Search should eventually operate over the local Mail Organizer index where possible, with sender, subject, body, thread, company/domain, category, priority, action and Gmail-label filters as supported by the search phase.

## Intelligence
Deterministic classification first; sender/company intelligence; independent priority; Action Required detection; deadline/meeting extraction; conversation intelligence; local search; rules/corrections; analytics; optional AI fallback.

## Actions and integrations
Action suggestions are explainable, traceable to source mail, deduplicated and confirmed before external effects. Calendar and Tasks are modular integrations. Gmail writes are introduced only in the dedicated write phase.

## Local data
Persist only what Mail Organizer needs for offline use, search, derived intelligence, user corrections/rules, sync state and action relationships. Do not unnecessarily duplicate every Gmail attachment. Local state is rebuildable from Gmail; Mail Organizer-only rules/corrections require a later backup/sync strategy if they must survive uninstall.

## Quality
Accessibility, dark mode, responsive layouts, performance, battery safety, offline behavior, multi-account isolation, security, privacy, automated tests and real-device verification are mandatory. Build success alone is never sufficient.