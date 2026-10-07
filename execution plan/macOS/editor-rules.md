# macOS Editor Rules

1. **Platform isolation is mandatory.** During a macOS phase, modify only macOS-specific files plus genuinely shared KMP/common code required by the phase. Never touch sibling platform folders.
2. Read the root instructions and all macOS master documents before implementation.
3. Execute exactly one phase per session. Do not implement future phases early.
4. Inspect the actual repository and existing architecture before creating abstractions.
5. Keep domain/data/business logic in KMP when platform-neutral; keep SwiftUI and macOS OS integration native.
6. Use official Google OAuth/Gmail APIs and least-privilege scopes. Never collect passwords or reuse cookies.
7. Store secrets/tokens in secure storage such as Keychain. Never commit, print, screenshot, or expose credentials.
8. Email content is untrusted. Sanitize HTML, do not execute embedded JavaScript, and never let email text authorize external actions.
9. Deterministic/local/explainable behavior precedes optional AI. User correction > user rule > deterministic result > AI fallback > unknown.
10. AI output must be schema-validated and domain-validated. AI cannot directly access Gmail, Calendar, Tasks, shell, filesystem, browser, Keychain, or OS automation.
11. Gmail is cloud source of truth; local data must be account-scoped, minimized, and rebuildable where practical.
12. All external actions require appropriate confirmation and idempotency. Never silently send/reply/forward, permanently delete, unsubscribe, or perform destructive bulk actions.
13. macOS UI must be native SwiftUI, adaptive, accessible, keyboard-friendly, pointer-friendly, dark-mode capable, and usable at varied window sizes.
14. Use native menus/commands/shortcuts and avoid pretending macOS is a phone or tablet.
15. Do not claim tests, runtime behavior, screenshots, performance, signing, or notarization unless actually executed and observed.
16. After each phase, update status and add only genuinely permanent new rules. Do not delete unrelated existing rules.
17. Build/test/run/fix/rebuild/retest before declaring completion.
18. Never publish automatically; production release is a separate explicit gate.
