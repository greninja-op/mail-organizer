# macOS Requirements

## Product role
Mail Organizer is a privacy-first personal email organization and action layer over Gmail. Gmail remains the authoritative cloud source of truth; Mail Organizer provides local organization, intelligence, search, prioritization, action suggestions, and optional integrations.

## macOS scope
- Native macOS desktop application using SwiftUI.
- Shared KMP core wherever business/data behavior is platform-neutral.
- Native macOS presentation and OS integration rather than stretched mobile layouts.
- Gmail official APIs and OAuth; no password collection, cookie reuse, scraping, or AccessibilityService-style mailbox control.
- Local persistence for Mail Organizer state: classification, corrections, rules, company intelligence, Action Required state, deadlines/meetings, conversation intelligence, sync cursors, local search/index data, analytics, automation state, and integration relationships as appropriate.
- Gmail remains cloud source of truth.
- Local processing is preferred. Remote AI is optional and explicitly controlled.

## Information architecture
Primary destinations include All Inbox, Primary, Promotional, Social, Spam, and Starred. Companies are filters/grouping inside the selected category, not a replacement for mailbox navigation.

All Inbox unifies connected Gmail accounts. Each message must expose the receiving Gmail account identity separately from sender/company identity. Individual email starring remains independent of company pinning and does not remove the email from its category.

Promotional and Social mail remain organized and accessible. Spam is first-class and supports recovery/Not Spam. Search should be local-index-first and support sender, subject, body/thread, company/domain, category, priority, Action Required, Gmail labels, and other supported indexed fields.

## Intelligence and actions
Deterministic/local/explainable logic is authoritative before optional AI. User corrections and explicit rules outrank deterministic classification; AI is fallback, never authority for external actions. Action Engine operations are explainable, traceable, account-scoped, deduplicated, and confirmed before consequential external effects.

Calendar, Tasks, Gmail writes, automation, AI, and future integrations remain modular. Email content is untrusted input and can never itself authorize sending, deletion, calendar/task creation, automation escalation, shell commands, browser actions, or credential use.

## macOS interaction
Support resizable windows, large desktop layouts, keyboard navigation, menu commands, shortcuts, pointer/trackpad, context menus, drag/drop only where safe and intentional, multiple windows/scenes where useful, dark mode, Dynamic Type/accessibility text sizing, VoiceOver, reduced motion, and responsive split layouts.

## Quality
Every feature must be tested beyond build success: automated tests, runtime behavior, realistic loading/empty/error/offline/cancellation/recovery states, account isolation, privacy/security, accessibility, performance, battery/power behavior where applicable, data integrity, and release configuration. Production signing/notarization must be a dedicated final gate and must never be faked.
