# Phase 01 — Android Application Foundation

## Execution contract

Read `requirements.md`, `spec.md`, `design.md`, `editor-rules.md`, and this phase file before editing.

This phase is the first serious Android product/UI foundation. Inspect the actual repository and preserve existing verified work.

Implement **Phase 01 only**.

Do not implement Gmail synchronization, production Gmail data, classification, company intelligence, local search, Calendar, Tasks, background sync, AI, automation, or any later roadmap feature.

Work only inside the Mail Organizer repository. Never modify sibling projects.

The required execution sequence is:

`inspect → plan → implement Phase 01 → build → tests → runtime/device QA → visual/accessibility/performance QA → fix → rebuild/reinstall/retest → update docs/status → stop`

---

## 1. Read and reconcile the product contract

Before implementation, establish the Android requirements from the source-of-truth documents.

The Android experience must:

- feel immediately familiar to Gmail/Google-app users;
- use Jetpack Compose and Material 3;
- use Material 3 Expressive/current Android guidance where the relevant APIs are stable and appropriate;
- remain distinctly Mail Organizer rather than becoming a Gmail clone;
- centralize design tokens and motion;
- support Android 12+ dynamic color where appropriate with a controlled fallback;
- support light/dark themes;
- establish accessible touch targets, typography and semantics;
- remain responsive across supported window sizes;
- establish adaptive performance behavior rather than assuming a fixed device class.

Do not copy proprietary Google source code, artwork or inaccessible assets.

Functional icons must use official Material Symbols, Android/system resources, permitted official assets, or deliberately authored vector assets.

---

## 2. Audit the current Android implementation first

Before changing code, inspect the actual repository and determine:

- current Android module(s);
- Compose setup;
- Material dependencies;
- navigation implementation;
- theme files;
- color/typography/shape definitions;
- existing activities;
- application class;
- manifest;
- resources;
- tests;
- KMP/shared module boundaries;
- Gradle/version catalog configuration;
- existing assets;
- current build state.

Do not rebuild the Android application from scratch if a usable foundation already exists.

Preserve working code unless it conflicts with this phase's requirements.

Record any important architectural mismatch before correcting it.

---

## 3. Establish the Android application shell

Create a stable Android entry point that can:

- start without a Gmail account;
- render a deterministic initial state;
- survive recreation;
- support navigation between the foundation-level destinations/shell states;
- expose a consistent theme;
- support loading, empty, error and offline foundation states;
- remain usable without network access.

If a full mailbox data layer does not yet exist, use clearly labeled development fixtures or empty-state models. Never present fabricated messages as live Gmail data.

The shell should be designed so later phases can replace fixtures with real repositories without rewriting the UI architecture.

Prefer a state-driven UI architecture:

`UI state → ViewModel/use case → repository/interface`

Do not put Gmail/network/database logic directly inside composables.

---

## 4. Establish centralized design tokens

Create one coherent design-token layer for Android.

Centralize, as appropriate:

### Color
- primary/accent colors;
- surface/background colors;
- content emphasis levels;
- error/warning/success states;
- unread/attention states;
- dark-theme equivalents;
- dynamic-color mapping.

### Typography
Define reusable styles for:

- app-level headings;
- section labels;
- sender/name emphasis;
- subject;
- preview/body;
- metadata/time;
- supporting/error text.

Typography must remain legible at large font scales.

### Shape
Centralize:

- cards/surfaces;
- search field;
- account identity circles;
- chips;
- menus;
- drawers;
- dialogs/sheets.

### Spacing
Use a consistent spacing system rather than arbitrary per-screen values.

### Elevation/surfaces
Use Material elevation/surface-container guidance consistently.

### Icon sizing
Define predictable icon and touch-target sizes.

Do not scatter raw colors, arbitrary paddings and typography values throughout composables.

---

## 5. Build the Gmail-familiar Android top bar

The main mailbox shell must establish the intended top-level hierarchy:

- three-line navigation affordance on the left;
- prominent Gmail-familiar search affordance;
- account/profile circle on the right.

The exact implementation can be an appropriate Material top app bar/search pattern rather than a pixel-for-pixel Gmail copy.

The top bar must:

- respect status-bar/inset behavior;
- remain accessible;
- expose meaningful content descriptions;
- preserve touch targets;
- behave correctly in light/dark themes;
- avoid unnecessary recomposition or animation;
- adapt to supported window widths.

The account/profile affordance is the entry point for the account switcher in this phase's shell.

The search affordance establishes the location and visual contract for the later local-search implementation; it must not implement the full search engine in Phase 01.

---

## 6. Establish the navigation drawer

Implement the reusable Material navigation drawer/sheet structure with the required core destinations:

1. All Inbox
2. Primary
3. Promotional
4. Social
5. Spam
6. Starred

The drawer must be compact and stable.

Do not turn the drawer into a company directory.

Company filtering belongs inside the selected category.

Folder/count indicators must be based on real state when shown. If the foundation has no real mail state yet, use empty/zero state rather than fake counts.

Spam must have a dedicated destination and the visual contract must allow a red new/unread indicator later.

Starred must be a global destination independent of the original category.

The drawer must support:

- keyboard/focus navigation where relevant;
- TalkBack semantics;
- correct selected-state semantics;
- large font behavior;
- light/dark themes;
- narrow and wider supported windows.

---

## 7. Establish the mailbox-row visual contract

Create a reusable message-row component or equivalent foundation component that can later accept real mail models.

It should establish:

- sender identity;
- subject;
- preview;
- timestamp;
- unread/emphasis state;
- Star control;
- optional source-account identity.

### All Inbox source-account identity

All Inbox is cross-account.

Every row in All Inbox must have a compact circular source/account indicator representing the **receiving Gmail account**, not the sender or company.

The component should be architected so the account identity is a separate model/value from:

- sender identity;
- company identity;
- message avatar.

Do not collapse these concepts into one avatar field.

The full Gmail address does not need to permanently occupy every row, but the user must be able to determine the receiving account without opening the message.

### Star

The row must provide a familiar Star control for the individual email.

Star state is separate from:

- category;
- company pinning;
- sender/company identity.

A starred message remains in its existing category and later appears in global Starred.

If the actual persistence/action engine does not yet exist, use a local UI-state fixture rather than implementing later Gmail write behavior.

---

## 8. Establish category/company filtering UI boundaries

The Android shell must reserve the correct information hierarchy for company organization.

Inside the selected category, provide the reusable surface/component boundary for:

- pinned companies;
- unpinned companies;
- company counts when real data exists;
- selecting a company to filter the current category.

Example conceptual structure:

`PROMOTIONAL → PINNED → Google / Facebook → ALL COMPANIES → Canva / Notion`

Company selection must preserve category context.

Company pinning:

- moves a company to the top of that category's company filter;
- does not create a drawer item;
- does not move email between categories;
- does not star messages;
- represents user intent separately from email starring.

Do not implement the real company-intelligence engine in this phase. Use interfaces/fixtures as necessary.

---

## 9. Account switcher foundation

Implement the visual and interaction shell for the account/profile menu.

It must provide:

- current account identity;
- connected-account list structure;
- add-account entry point;
- account-specific sync/recovery status slot;
- clear selected-account state.

The switcher should feel familiar to a Google-app user while using Mail Organizer's own tokens.

### Profile swipe

Establish the gesture architecture for switching to the next connected account where the Android interaction model supports it.

The gesture must be:

- discoverable where appropriate;
- bounded;
- accessible;
- interruptible;
- independent from message sender/company identity.

Do not implement real multi-account Gmail synchronization in this phase.

### Add account

The Add Account action must lead to the future official OAuth flow boundary.

Do not create a fake email/password/Google credential form.

A placeholder state may communicate that official Google sign-in will be used later.

---

## 10. Establish the Gmail → Mail Organizer recovery/sync visual system

Create the reusable visual component/state model for the initial recovery experience.

The production synchronization engine belongs to Phase 04, so Phase 01 must not invent a real sync progress source.

The component must be designed around real states:

- preparing;
- retrieving;
- organizing;
- indexing;
- completed;
- paused;
- retrying;
- authentication required;
- network unavailable;
- failed.

The visual direction is:

`Gmail → moving mail → Mail Organizer`

with truthful stage text.

When a trustworthy progress value is available later, the component may show determinate progress.

When it is not available, the component must use an indeterminate state.

Never fabricate percentages.

The visual concept defined in `design.md` should be treated as the reference.

### Adaptive behavior

The animation must be controlled by the centralized motion/performance system.

On lower capability or reduced-motion profiles:

- simplify the moving mail element;
- reduce expensive effects;
- reduce spring complexity;
- reduce unnecessary transitions;
- preserve the communication of source → destination.

When retrieval is effectively instant, the architecture must permit the blocking animation to be skipped.

Do not claim real Gmail retrieval is occurring during fixture previews.

---

## 11. Build the adaptive PerformanceProfile foundation

Create a lightweight, capability-based performance model.

It should be represented by a stable abstraction that UI/background systems can consume without directly querying hardware everywhere.

Potential inputs include:

- Android/API level;
- memory class;
- display modes/refresh capabilities;
- CPU/GPU capability signals where reliably available;
- thermal/power state where supported;
- battery saver;
- reduced-motion/accessibility preferences;
- relevant app performance history.

Do not collect unnecessary personal/device data.

### Capability tiers

Use conservative tiers such as:

- lower capability;
- standard capability;
- higher capability.

Do not hardcode lists of phone models.

### Performance policy outputs

The profile should be able to influence:

- animation complexity;
- spring/transition behavior;
- expensive visual effects;
- blur;
- list prefetching;
- image decode/cache budgets;
- background concurrency;
- future sync/index batch sizes;
- optional haptics.

### Refresh-rate rule

A 120 Hz display is not a command to render at 120 FPS.

Respect Android scheduling and adaptive refresh behavior.

Prioritize:

1. sustained smoothness;
2. interaction responsiveness;
3. thermal stability;
4. battery efficiency;
5. visual richness.

Do not implement a continuous hardware benchmark.

Do not continuously poll performance sensors.

Re-evaluate only at startup and meaningful state changes.

---

## 12. Establish the centralized motion system

Create a reusable motion policy with four conceptual tiers:

### Essential
Low-cost state changes required for comprehension.

### Standard
Normal navigation/list interactions.

### Expressive
High-value transitions such as the Gmail → Mail Organizer recovery experience.

### Reduced motion
Minimal movement/fades/position changes for accessibility or performance constraints.

Use current stable Material motion APIs/schemes where appropriate.

Every animation must have a purpose.

Do not animate:

- every button unnecessarily;
- background content continuously;
- large surfaces merely for decoration;
- anything that interferes with typing/scrolling;
- synchronization progress dishonestly.

---

## 13. Accessibility foundation

Every foundation component must establish:

- meaningful content descriptions;
- correct roles;
- selected/checked semantics;
- minimum touch targets;
- sufficient contrast;
- scalable text;
- support for large font sizes;
- reduced-motion behavior;
- predictable focus order.

Validate with Android accessibility tooling/TalkBack where available.

Do not rely on color alone to communicate:

- unread;
- spam;
- selected;
- starred;
- error;
- account identity.

The source-account circle in All Inbox must have an accessible alternative to visual color/initials alone.

---

## 14. Responsive layout foundation

Support the Android form factors that the current project targets without prematurely building tablet-specific product features.

At minimum, ensure:

- portrait works;
- supported landscape/window widths do not cause clipping;
- drawer behavior is appropriate to window size;
- search/account controls remain usable;
- message rows do not collapse incorrectly;
- large text does not overlap controls.

Use Compose/window-size guidance rather than hardcoded device names.

---

## 15. Light/dark and dynamic color

Implement:

- light theme;
- dark theme;
- Android 12+ dynamic color where appropriate;
- Mail Organizer fallback palette for devices/configurations without dynamic color.

Dynamic color must not destroy semantic meaning.

For example:

- spam attention;
- error;
- unread;
- success;
- account identity

must remain distinguishable and accessible.

Do not hardcode a Pixel-only color system.

---

## 16. Loading, empty, error and offline foundation states

Every foundation-level screen/state must have deliberate states for:

- loading;
- empty;
- error;
- offline/unavailable;
- normal content.

Avoid generic infinite spinners where a meaningful state explanation is possible.

Do not show a fake “syncing” state unless the fixture/state explicitly represents development data.

The app must remain usable when there is no authenticated account and no network.

---

## 17. Official authentication boundary

Phase 01 may establish the authentication entry UI and interface boundary, but production Google account access belongs to Phase 03.

Rules:

- never collect Google passwords;
- never ask the user to paste a Google password;
- never emulate Google's credential page;
- never embed Gmail web pages as a workaround;
- never use scraping;
- never use AccessibilityService for Gmail access.

The UI should make the future authentication method clear without pretending authentication has already occurred.

---

## 18. Component architecture

Create reusable components where repetition is expected:

- top app bar/search shell;
- navigation drawer;
- account/profile control;
- account switcher;
- message row;
- Star control;
- source-account indicator;
- company filter;
- company pin control;
- sync/recovery state;
- loading/empty/error states;
- motion/performance policy access.

Do not over-componentize trivial one-off layout.

Keep state ownership explicit.

Prefer unidirectional data flow.

Do not allow composables to own networking, Gmail API clients, database transactions or authentication tokens.

---

## 19. Performance and recomposition discipline

During implementation:

- use stable models where appropriate;
- avoid unnecessary recomposition;
- provide keys for dynamic lazy lists;
- avoid expensive work inside composition;
- avoid synchronous disk/network operations on the UI thread;
- avoid large image decoding during composition;
- keep animations bounded;
- avoid unnecessary allocations in frequently recomposed content.

Use profiling/inspection when practical.

Do not prematurely optimize with unreadable abstractions.

---

## 20. Mandatory validation

The phase is not complete because the code compiles.

### Build

Use the repository Gradle wrapper.

Verify:

- Android debug build;
- relevant shared/KMP compilation;
- relevant unit tests;
- UI tests if established and applicable.

### Installation/runtime

With an available emulator or physical Android device:

1. build;
2. install with ADB;
3. launch;
4. verify the shell;
5. force-stop;
6. relaunch;
7. inspect logcat;
8. inspect package/activity state with dumpsys where useful;
9. capture screenshots;
10. use screen recording when animation/runtime diagnosis benefits from it.

Use `adb reverse` only when genuinely required for this project.

### Visual QA

Inspect:

- top app bar;
- drawer;
- mailbox row;
- Star;
- source-account indicator;
- company filter;
- account switcher;
- recovery/sync component;
- loading/empty/error states.

Check:

- light mode;
- dark mode;
- dynamic color;
- large font;
- accessibility;
- portrait;
- supported wider windows;
- reduced motion.

### Performance QA

Validate at least:

- lower capability profile;
- standard/higher capability profile;
- reduced-motion profile.

Check for:

- visible jank;
- excessive animation work;
- blocked scrolling;
- slow drawer/search interactions;
- unnecessary recomposition;
- thermal/performance regressions where practical.

Do not claim frame-rate numbers unless actually measured.

---

## 21. Explicit non-goals

Do not implement in Phase 01:

- real Google OAuth;
- Gmail API access;
- Gmail message synchronization;
- Gmail history sync;
- Gmail label synchronization;
- production local database schema;
- production classification;
- company detection;
- local search/indexing;
- priority engine;
- Action Required engine;
- Calendar;
- Tasks;
- background sync;
- cleanup automation;
- conversation intelligence;
- Gmail write operations;
- remote AI;
- production analytics;
- advanced automation;
- Play Store release configuration.

Fixtures and interfaces are permitted only where necessary to establish this phase's UI architecture.

---

## 22. Documentation and permanent rules

After implementation and verification:

### editor-rules.md

Add only genuinely permanent rules discovered during this phase.

Do not delete or weaken existing rules.

Potential permanent rules may cover:

- Android UI component/token conventions;
- PerformanceProfile access conventions;
- centralized motion policy;
- accessibility requirements;
- fixture-vs-real-data labeling;
- state ownership boundaries.

### spec.md

Update Phase 01 status only after actual verification.

Use:

- `[ ]` not started;
- `[-]` in progress;
- `[x]` verified complete;
- `[!]` blocked.

Never mark complete because the implementation “looks finished.”

### Documentation truthfulness

Do not document a device test, accessibility result, performance result, build result or visual QA result that was not actually performed.

---

## 23. Final acceptance criteria

Phase 01 is complete only when:

1. Android has a stable product-quality foundation.
2. Material 3/theme/tokens are centralized.
3. The Gmail-familiar top shell exists.
4. The required navigation destinations exist at the foundation level.
5. All Inbox rows can represent receiving/source account identity separately from sender/company identity.
6. Individual email Star state has a dedicated UI contract.
7. Company filtering/pinning has a category-scoped UI boundary.
8. Account switching/add-account UI boundaries exist without fake authentication.
9. Gmail → Mail Organizer recovery/sync visualization exists as an honest, fixture-ready state system.
10. Motion is centralized and adaptive.
11. PerformanceProfile is centralized and capability-based.
12. Accessibility and responsive foundations are implemented.
13. Light/dark/dynamic color behavior is verified.
14. Loading/empty/error/offline states are deliberate.
15. Real build/test/runtime/device verification has been performed where the environment permits it.
16. No later-phase feature has been implemented prematurely.
17. Documentation/status matches actual verification.
18. Final diff is limited to intentional Phase 01 work.

---

## 24. Completion protocol

Before stopping:

1. inspect the final diff;
2. remove accidental/generated files;
3. build again;
4. rerun relevant automated tests;
5. reinstall the Android debug build;
6. relaunch and repeat runtime checks;
7. inspect logcat/errors;
8. repeat visual/accessibility checks after final fixes;
9. update permanent rules only where justified;
10. update phase status accurately;
11. commit only the Phase 01 work;
12. stop.

**Do not continue to Phase 02 in the same session.**

Phase 01 establishes the Android UI foundation. It does not establish the Gmail data system.
