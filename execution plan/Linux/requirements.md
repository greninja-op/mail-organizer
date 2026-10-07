# Linux Requirements

Mail Organizer on Linux is a privacy-first desktop email organization and action layer over Gmail.

## Platform
- Linux is a first-class desktop target.
- Reuse the shared Rust desktop core with macOS and Windows.
- Linux-specific UI and OS integration may differ, but product behavior and core contracts remain consistent.
- Support common modern Linux desktop environments without assuming a single distro or window manager where practical.
- Distribution/packaging strategy must be explicit before release.

## Gmail and privacy
- Use official Google OAuth and Gmail APIs.
- Never use passwords, cookie theft, scraping, or AccessibilityService-style mailbox control.
- Start with least-privilege/read-only access where possible.
- Treat email content as untrusted input.
- Local processing is preferred; remote AI is optional and opt-in.
- Secrets belong in the platform secret store/keyring, never source control or ordinary logs/database.

## Core product
Support Action Required, Important, Career, Education, Receipts & Orders, Security, Notifications, Newsletters, Promotions, Low Value, unified inbox, company grouping, company pinning, individual message starring, Gmail-style search, deterministic classification, priority, deadlines/meetings, conversation intelligence, rules/corrections, Calendar/Tasks, multi-account, offline behavior, analytics, optional AI fallback, and advanced automation.

## Desktop UX
Use a native desktop workspace with resizable windows, multi-column layouts, keyboard/mouse support, accessible controls, dark/light themes, high-DPI scaling, and Linux desktop conventions while preserving Mail Organizer's established visual language.

## Quality
Build success is insufficient. Verify automated tests, real runtime behavior, data isolation, accessibility, visual states, offline/recovery paths, performance, packaging and installation on supported Linux environments.