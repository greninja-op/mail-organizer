# Windows Editor Rules

1. **Rust is the primary language for Windows and the reusable desktop core.**
2. Keep platform-neutral business/data/application behavior in Rust so macOS can reuse it.
3. Keep Windows UI Rust-first through an appropriate Rust UI framework or safe bindings to native Windows APIs.
4. Do not duplicate business logic in C#, C++, JavaScript or another UI language.
5. Keep Windows OS calls behind narrow Rust adapters.
6. Platform isolation is mandatory; never touch sibling platform folders during a Windows phase.
7. Execute exactly one phase per session and inspect the actual repository first.
8. Use official Google OAuth/Gmail APIs and least-privilege scopes; never collect passwords or reuse cookies.
9. Store secrets securely; never commit, log or expose credentials.
10. Email content is untrusted; sanitize HTML, do not execute embedded JavaScript, and never let email text authorize external actions.
11. Deterministic/local/explainable behavior precedes optional AI: user correction > user rule > deterministic > AI fallback > unknown.
12. AI output must be schema/domain validated and cannot directly access Gmail, Calendar, Tasks, filesystem, shell, browser, credentials or OS automation.
13. Gmail is cloud source of truth; local data is account-scoped, minimized and rebuildable where practical.
14. Consequential external actions require confirmation, account scope and idempotency.
15. UI must be native-feeling, adaptive, accessible, keyboard/pointer friendly and usable at varied window sizes.
16. Never claim tests, runtime behavior, screenshots, performance, signing, installer or release evidence unless actually observed.
17. After each phase update status and add only genuinely permanent rules; never delete unrelated rules.
18. Build/test/run/fix/rebuild/retest before declaring completion.
19. Never publish automatically; production release is a separate explicit gate.
