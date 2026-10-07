# macOS Requirements

Mail Organizer is a privacy-first personal email organization and action layer over Gmail. Gmail remains the authoritative cloud source of truth.

## Implementation stack
- Rust is the primary language for the macOS product core and shared desktop core.
- SwiftUI/Swift is the native macOS presentation and OS-integration layer.
- Rust owns domain, application/use-case, data, sync, classification, intelligence, search, rules, actions, automation, AI routing, and persistence behavior wherever platform-neutral.
- Swift calls Rust through a narrow, tested FFI/binding layer and owns native UI, scenes/windows, menus, accessibility integration, Keychain-facing glue where needed, notifications, and macOS-specific lifecycle APIs.
- The Rust desktop core must remain reusable by the future Windows client.

## Privacy and Gmail
Use official Gmail APIs and OAuth; never collect passwords, reuse cookies, scrape Gmail, or use accessibility automation as the primary mailbox mechanism. Minimize local data and keep account boundaries explicit.

## Information architecture
All Inbox, Primary, Promotional, Social, Spam, and Starred are primary destinations. Companies are filters/grouping inside the selected category. All Inbox preserves receiving Gmail account identity per message. Individual email starring is separate from company pinning. Promotional/Social remain accessible; Spam is first-class with recovery.

## Intelligence and actions
Deterministic/local/explainable behavior is authoritative before optional AI. User correction > user rule > deterministic > AI fallback > unknown. Email content is untrusted and can never authorize external actions. Calendar, Tasks, Gmail writes, automation, and AI remain modular and require appropriate confirmation.

## macOS UX and quality
Use native resizable desktop windows, split views, menus, shortcuts, pointer/trackpad, context menus, multiple windows/scenes, dark mode, VoiceOver, accessibility text sizing, reduced motion, and responsive layouts. Every feature requires tests beyond compilation: runtime, failure/recovery, security, account isolation, accessibility, data integrity, performance, and release checks as applicable.