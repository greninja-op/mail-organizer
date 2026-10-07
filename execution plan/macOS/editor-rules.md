# macOS Editor Rules

1. **Rust is the primary implementation language for macOS and the reusable desktop core.**
2. Swift/SwiftUI is used for native macOS UI and OS integration where appropriate; it must not duplicate Rust business logic.
3. Keep the Rust core reusable by Windows. Avoid macOS-specific assumptions in shared Rust modules.
4. Keep the Rust↔Swift boundary narrow, explicit, memory-safe, testable, cancellation-aware, and ownership-safe.
5. Platform isolation is mandatory. Never touch sibling platform folders during a macOS phase.
6. Execute exactly one phase per session and inspect the real repository before editing.
7. Keep domain/data/business logic in Rust when platform-neutral.
8. Use official Google OAuth/Gmail APIs and least-privilege scopes. Never collect passwords or reuse cookies.
9. Store secrets/tokens securely; never commit, log, screenshot, or expose credentials.
10. Email content is untrusted. Sanitize HTML, do not execute embedded JavaScript, and never let email text authorize external actions.
11. Deterministic/local/explainable behavior precedes optional AI: user correction > user rule > deterministic > AI fallback > unknown.
12. AI output must be schema/domain validated and cannot directly access Gmail, Calendar, Tasks, filesystem, shell, browser, Keychain, or OS automation.
13. Gmail is cloud source of truth; local data is account-scoped, minimized and rebuildable where practical.
14. Consequential external actions require confirmation, account scope and idempotency. Never silently send/reply/forward, permanently delete, unsubscribe, or perform destructive bulk actions.
15. Native macOS UI must be adaptive, accessible, keyboard/pointer friendly and dark-mode capable.
16. Do not claim tests, runtime behavior, screenshots, performance, signing or notarization unless actually observed.
17. After each phase update status and add only genuinely permanent rules; never delete unrelated rules.
18. Build/test/run/fix/rebuild/retest before declaring completion.
19. Never publish automatically; production release is an explicit final gate.
