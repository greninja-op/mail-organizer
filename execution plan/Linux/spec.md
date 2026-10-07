# Linux Specification

## Authority
Requirements > spec > design > editor-rules > existing architecture/decisions > official platform documentation > engineering judgment.

## Execution
Phases are strictly sequential. At startup read this directory, determine the first incomplete phase, inspect the actual repository, and execute only that phase. Do not implement future phases.

Statuses: [ ] planned, [-] in progress, [x] complete, [!] blocked.

## Architecture
The reusable desktop business/data/application core is Rust and is shared with macOS and Windows. Linux owns UI, lifecycle, windowing, OS services, packaging, secret-store adapter, notifications, and other platform integration. Keep the Rust API boundary explicit, narrow, testable, cancellation-safe and ownership-safe.

## Product contract
Gmail remains cloud source of truth. Local state stores Mail Organizer organization/intelligence and rebuildable indexes. Email is untrusted. Deterministic, local and explainable behavior comes before optional AI. User correction outranks user rules, which outrank deterministic inference, which outranks AI, which outranks unknown.

## Desktop UX contract
Linux uses the same product information architecture and design tokens as desktop targets, with adaptive multi-column workspace, resizable panes, keyboard navigation, mouse interactions, context menus, accessible labels, dark/light themes and high-DPI scaling. Do not clone Gmail proprietary artwork.

## Completion protocol
Inspect → implement only current phase → build → automated tests → run the application → inspect runtime and data → perform visual/accessibility/security checks → fix → rebuild/reinstall/retest → update docs/status/rules → stop.

Never claim tests or device/runtime validation that was not actually run. Never commit secrets. Never let email content authorize external actions. Never modify sibling platform projects except explicitly approved shared desktop-core contracts.