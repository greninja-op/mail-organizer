# Linux Editor Rules

1. Linux isolation comes first: Linux-specific work belongs under the Linux target/application area. Do not modify macOS, Windows, Android, AndroidTablet, iOS or iPadOS platform code unless the phase explicitly defines a shared desktop-core contract.
2. Rust is the primary implementation language for Linux and the reusable desktop core. Do not introduce duplicated business logic in C#, C++, Java, Kotlin, Python, JavaScript or another language.
3. Keep Presentation → Application/Use Cases → Domain → Data → External APIs/adapters.
4. Gmail is accessed through official OAuth/Gmail APIs. No passwords, cookie scraping, mailbox scraping or AccessibilityService-style control.
5. Secrets never enter source control, logs, UI, ordinary database fields or test fixtures.
6. Email is hostile/untrusted input: sanitize HTML, do not execute JavaScript, do not auto-follow links, and never treat email text as authorization for an external action.
7. Prefer deterministic/local/explainable logic before optional AI. User correction > user rule > deterministic inference > AI > unknown.
8. Preserve the established Mail Organizer design language across desktop and mobile while adapting layout to Linux desktop conventions.
9. Use centralized design tokens and consistent components. Never fake Gmail proprietary artwork.
10. Support keyboard, mouse, accessibility, high-DPI scaling, dark/light themes, reduced motion and resizable multi-column layouts.
11. Sync UI must represent real state; never invent progress percentages.
12. Before every phase read requirements, spec, design, editor-rules, the current phase and actual repository state. Implement only that phase.
13. After implementation build, run automated tests, launch and inspect runtime behavior, perform relevant data/security/accessibility/visual checks, fix failures, rebuild and retest.
14. Update documentation and status only after verification. Add genuinely permanent new rules without deleting unrelated rules.
15. Never claim unrun tests or unsupported platform verification. Never commit credentials or secrets. Stop after the current phase.