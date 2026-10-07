# Phase 06 — Core Inbox & Email Viewer

## Execution contract

Read all source-of-truth documents and confirm Phase 06 is the first incomplete phase.

Implement the **core local-data mailbox and email/thread viewer**.

Use synchronized local data and the normalized models from earlier phases.

Do not add new Gmail synchronization logic, classification intelligence, Calendar, Tasks, search engine, automation, AI or Gmail write behavior.

---

## 1. Product objective

Make Mail Organizer's core mail experience usable with real synchronized local data.

Users must be able to:

- enter the mailbox;
- see message/thread lists;
- understand sender, subject, preview and time;
- distinguish unread/read;
- identify the receiving Gmail account in All Inbox;
- open a thread/message;
- safely read normalized email content;
- see attachment metadata;
- use Starred state;
- navigate category context;
- handle loading/empty/error/offline states.

The UI must use the Android shell established in Phase 01.

---

## 2. Mailbox data source

Use local repositories/database as the primary UI source.

Do not call Gmail APIs directly from composables.

Use a state-driven architecture:

`UI → ViewModel/use case → repository → local database`

Gmail synchronization remains responsible for populating local state.

The viewer should remain useful offline when local data exists.

---

## 3. Inbox list

Implement a reusable mailbox list.

Each row should support:

- sender;
- subject;
- preview;
- timestamp;
- unread emphasis;
- Star;
- thread/message indication where appropriate;
- safe account/source identity.

Do not make rows unnecessarily dense.

Preserve Gmail familiarity while maintaining Mail Organizer's visual identity.

Use lazy lists with stable keys.

---

## 4. All Inbox

All Inbox is a unified cross-account presentation.

Every message row must expose the receiving/source Gmail account using a compact circular account indicator or equivalent.

The indicator represents:

**receiving Gmail account ≠ sender ≠ company**

The UI must make these identities distinguishable.

A user should be able to determine the receiving account without opening the message.

Unified presentation must not bypass account-scoped repository boundaries.

---

## 5. Categories

Respect the navigation shell from Phase 01:

- All Inbox;
- Primary;
- Promotional;
- Social;
- Spam;
- Starred.

Use the actual local state/labels available from synchronization.

Do not invent classification categories beyond what previous phases provide.

Do not implement the full deterministic classifier here.

---

## 6. Starred

Implement the user-facing Star state using local state.

Requirements:

- Star control is available from message rows;
- starred messages remain in their original category;
- Starred is a global view;
- starring does not mean company pinning;
- account ownership remains preserved.

If Gmail write access does not exist yet, the local Star action must remain explicitly local/pending rather than pretending Gmail has been modified.

Do not implement Gmail write APIs in this phase.

---

## 7. Company/category context

The company intelligence engine is a later phase, but the UI must preserve category context.

If a company-filter state is already represented by fixture/domain state:

- show the current category;
- show the selected company;
- keep filtering scoped to that category.

Do not build company detection here.

Do not turn companies into top-level drawer destinations.

---

## 8. Thread viewer

Implement thread-oriented reading using local normalized data.

Show:

- participants;
- message order;
- sender identity;
- timestamp;
- normalized body;
- read/unread state where available;
- attachments metadata;
- safe links.

Handle long threads efficiently.

Avoid loading the entire thread into memory unnecessarily when data is large.

---

## 9. Safe HTML rendering

Use the sanitized output from Phase 05.

The viewer must never execute email JavaScript.

Unsafe links/content must remain blocked or require deliberate safe interaction.

Do not inject unsanitized HTML into a WebView or equivalent renderer.

If a WebView is used for rendering, harden it appropriately and keep navigation under explicit application control.

---

## 10. Links

Links are user actions.

Provide a deliberate interaction before leaving the app where appropriate.

Do not automatically open links on message load.

Do not execute javascript URLs.

Do not allow an email to invoke application actions simply by containing a crafted URL.

---

## 11. Attachments

Display attachment metadata such as:

- filename;
- type;
- size;
- inline/attachment state.

Do not automatically download attachment contents.

A later user-driven download/open flow must be architected separately.

Do not make opening an attachment an automatic side effect of viewing a message.

---

## 12. Read/unread state

Display unread state clearly.

Where local state permits a local read-state mutation, keep it account-scoped.

Do not claim Gmail state has changed unless Gmail write capability exists and has actually succeeded.

Do not add Gmail write calls in this phase.

---

## 13. Loading/empty/error/offline states

Implement deliberate states for:

- initial loading;
- empty mailbox;
- empty category;
- thread loading;
- message parse failure;
- offline local-data availability;
- no local data;
- repository/database error.

The UI should distinguish:

- “there is no local mail”;
- “the account has not synchronized yet”;
- “the local database is unavailable”;
- “the network is unavailable.”

Do not use a generic infinite spinner for all failures.

---

## 14. Pagination and large mailboxes

Use local pagination/windowing.

Do not load an entire mailbox into memory.

Use stable lazy-list keys.

Avoid unnecessary recomputation during scrolling.

Ensure the UI remains responsive with large synthetic datasets.

---

## 15. Accessibility

Validate:

- message sender/subject/preview semantics;
- Star state;
- source-account identity;
- unread state;
- navigation drawer;
- thread controls;
- links;
- attachment controls.

Do not rely solely on color for:

- account identity;
- unread;
- Starred;
- Spam;
- errors.

Support large text and reduced motion.

---

## 16. Visual consistency

Review against `design.md`:

- top app bar/search/account shell;
- drawer;
- message-row density;
- typography;
- spacing;
- surfaces;
- icons;
- account identity treatment;
- Star treatment;
- thread layout;
- dark mode;
- dynamic color;
- responsive windows.

Fix inconsistencies instead of adding one-off styling.

---

## 17. Performance

Profile/inspect:

- scrolling;
- opening a thread;
- large thread rendering;
- HTML rendering;
- account switching context;
- Star interaction.

Avoid:

- synchronous database calls on UI thread;
- expensive parsing during composition;
- unnecessary recomposition;
- unbounded WebView/resource loading;
- large image decoding.

Respect the existing PerformanceProfile.

---

## 18. Testing

Create tests for:

- mailbox list state;
- account identity rendering;
- account isolation;
- Star state;
- category preservation after starring;
- Starred query;
- empty/error/offline states;
- thread ordering;
- malformed/sanitized content rendering;
- attachment metadata;
- safe link behavior.

Use synthetic local data.

Do not require Gmail access for ordinary UI/unit tests.

---

## 19. Mandatory runtime/device verification

Use Gradle wrapper.

Run all relevant unit/database/UI tests.

On emulator/real device:

1. install;
2. launch;
3. open All Inbox;
4. open each applicable category;
5. verify source-account indicators;
6. star/unstar messages;
7. open Starred;
8. open a thread;
9. inspect HTML safety;
10. inspect attachments metadata;
11. test empty/error/offline states;
12. force-stop/relaunch;
13. inspect logcat;
14. capture screenshots;
15. test light/dark and large font;
16. verify no sensitive content leaks into logs.

Fix defects, rebuild, reinstall and retest.

---

## 20. Explicit non-goals

Do not implement:

- new Gmail sync engine;
- deterministic classification;
- company intelligence;
- search;
- Calendar;
- Tasks;
- Gmail writes;
- AI;
- automation;
- background sync.

---

## 21. Completion

Update permanent editor rules only when genuinely justified.

Update `spec.md` only after real verification.

Document any local-viewer limitations and real verification results.

Final sequence:

`diff review → build → tests → runtime/visual/security QA → fix → rebuild/reinstall/retest → docs/status → commit → stop`

Do not continue to Phase 07.