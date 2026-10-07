# Windows Requirements

## Implementation architecture
- Rust is the primary language for the Windows application and reusable desktop core.
- The Rust core must be reusable by macOS.
- Rust owns domain, application/use cases, Gmail data handling, sync, classification, company intelligence, search/indexing, priority, temporal/conversation intelligence, rules, Action Engine, persistence, automation and AI routing.
- Windows presentation should remain Rust-first through a suitable Rust UI framework or safe Rust bindings to native Windows APIs.
- Do not duplicate business logic in C#, C++, JavaScript or another UI language.
- Windows-specific OS integration belongs behind small Rust adapters.

## Product
Mail Organizer is a privacy-first personal email organization and action layer over Gmail. Gmail remains cloud source of truth. Local processing is preferred; remote AI is optional and controlled.

## Mailbox
Primary destinations: All Inbox, Primary, Promotional, Social, Spam, Starred. Companies are filters inside the selected category. Unified Inbox preserves receiving-account identity per message. Individual message starring is separate from company pinning. Promotional/Social remain accessible; Spam is first-class with recovery.

## Security
Use official Gmail APIs/OAuth and least privilege. Never collect passwords, reuse cookies, scrape Gmail, or automate Gmail through accessibility. Protect credentials with appropriate Windows secure storage. Email content, HTML, URLs, attachments and model output are untrusted.

## Intelligence and actions
User correction > user rule > deterministic result > AI fallback > unknown. AI cannot authorize external effects. Calendar, Tasks, Gmail writes and automation are modular, explainable, account-scoped, confirmed and idempotent.

## Quality
Test runtime behavior beyond compilation, including fresh/upgrade/restart/offline/error/recovery, multi-account isolation, accessibility, keyboard/pointer, performance, data integrity and release configuration.