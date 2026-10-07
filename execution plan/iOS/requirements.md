# Mail Organizer iOS — Requirements

## 1. Purpose

The iPhone application is the iOS presentation and platform layer of Mail Organizer: a privacy-first personal email organization and action layer over Gmail.

Gmail is the authoritative cloud source of truth. Mail Organizer adds local organization, intelligence, search, prioritization, action suggestions, automation, and optional integrations without becoming a second mail server.

## 2. Platform boundary

This document applies to **iOS / iPhone only**.

All iOS-specific documents, source, assets, tests, scripts, configuration, entitlements, signing material references, and release artifacts created by this plan belong under `iOS/`.

iPadOS is a separate future execution plan and must not be implemented as part of this plan. Android tablet, macOS, Windows, and Linux are separate plans.

## 3. Architecture

Use Kotlin Multiplatform for shared business/data logic and SwiftUI for iOS presentation.

Shared KMP owns, where appropriate:
- Gmail data models and normalization
- repositories and sync state
- local persistence abstraction
- classification and company/sender intelligence
- priority and Action Required
- local search/indexing
- rules and user corrections
- temporal and conversation intelligence
- Action Engine
- integration abstractions
- automation
- optional AI provider/routing abstractions

iOS owns:
- SwiftUI screens and navigation
- iOS lifecycle and scene handling
- Apple authentication presentation boundaries
- Keychain-backed secrets
- background execution and refresh scheduling
- notifications
- Calendar/Reminders integration adapters where required
- iOS permissions/entitlements
- accessibility and Dynamic Type presentation
- iOS performance, packaging, signing, App Store configuration

Do not duplicate shared business logic in Swift merely because the UI is native.

## 4. Mail behavior

Required mailbox destinations:
- All Inbox
- Primary
- Promotional
- Social
- Spam
- Starred

All Inbox is unified across connected Gmail accounts. Each message row identifies the receiving Gmail account with a compact source-account identity. This is distinct from sender/company identity.

Promotional and Social are organized, not silently deleted. Spam is first-class, visibly distinguished, and supports recovery/Not Spam through the appropriate Gmail capability.

Starred is global. Starring an email does not remove it from its original category.

Company grouping/filtering occurs inside the selected category. Company pinning is separate from email starring and moves a pinned company to the top of that category's company filter.

## 5. Accounts and privacy

Support multiple Gmail accounts with explicit account boundaries. Account switching must not leak messages, rules, actions, cached data, or credentials between accounts.

OAuth uses official Google authentication/browser flows. Never collect Gmail passwords. Start with least privilege and request additional scopes only when a feature genuinely needs them.

Tokens and secrets never appear in source, ordinary logs, UI, screenshots, or ordinary database fields. Use Keychain or the appropriate secure mechanism.

Email content is untrusted input. HTML must be sanitized. JavaScript must not execute from email content. Email text must never be treated as authorization for an external action.

## 6. Offline and synchronization

The application must provide truthful local cached state where available and clearly distinguish fresh, stale, syncing, failed, offline, and authenticated states.

Background work must respect iOS scheduling constraints. Never claim that a background task will run continuously or at an exact cadence that iOS does not guarantee.

Gmail remains authoritative. Local state must reconcile safely after interruptions, process termination, account removal, and reinstall.

## 7. Intelligence

Use deterministic/local/explainable logic first. User corrections and rules have higher authority than deterministic classifiers; deterministic logic has higher authority than optional AI.

AI is optional and must never be required for core mailbox access, search, classification, or safety. AI cannot authorize Gmail, Calendar, Tasks, filesystem, shell, browser, or external actions.

## 8. Actions and automation

Actions must be explainable, traceable, deduplicated, account-scoped, and confirmed before external effects when required.

Automation is finite and policy-controlled. No arbitrary scripts. Destructive actions are conservative. Email content cannot create or approve an automation.

## 9. Quality

Every phase requires appropriate unit/integration/UI/runtime testing. The final product must be verified on iPhone Simulator and a real iPhone when available, in light/dark mode, Dynamic Type sizes, poor/offline network conditions, account changes, process termination, and realistic data volumes.

Build success alone is never sufficient.
