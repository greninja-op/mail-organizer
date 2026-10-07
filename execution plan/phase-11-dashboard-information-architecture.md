# PHASE 11 — DASHBOARD & INFORMATION ARCHITECTURE

You are now executing:

**Phase 11 — Dashboard & Information Architecture**

This phase may begin only after Phase 10 — Search & Local Indexing has been completed and verified.

Do not execute Phase 12 or any later phase automatically.

The project continues to use strict sequential phase execution.

```text
Phase 0
  ↓
Phase 1
  ↓
Phase 2
  ↓
Phase 3
  ↓
Phase 4
  ↓
Phase 5
  ↓
Phase 6
  ↓
Phase 7
  ↓
Phase 8
  ↓
Phase 9
  ↓
Phase 10
  ↓
Phase 11 ← YOU ARE HERE
  ↓
Phase 12
  ↓
...
```

Complete only Phase 11.

Verify it.

Update persistent project rules and documentation.

Then STOP.

---

# 1. DISCOVER THE PROJECT INSTRUCTION FOLDER

Before modifying anything:

1. Locate the Mail Organizer instruction folder.
2. Read:
    - `requirements.md`
    - `spec.md`
    - `design.md`
    - `editor-rules.md`
    - development-status documentation
    - architecture documentation
    - Phase 11 instructions
3. Inspect the actual repository.
4. Verify Phase 10 is genuinely complete.
5. Identify:
    - Mail Organizer project root
    - Git root
    - Gradle root
    - Android package ID
6. Confirm all operations target only Mail Organizer.

Do not assume the current directory is the project root.

---

# 2. STRICT MULTI-PROJECT ISOLATION

The workspace may contain multiple Android projects.

Only Mail Organizer may be modified.

Never modify sibling:

- source code
- Gradle files
- dependencies
- manifests
- resources
- tests
- generated artifacts
- SDK/JDK configuration
- signing configuration
- Git repositories

ADB operations must target only the Mail Organizer package.

Do not install, uninstall, clear, force-stop, launch, or inspect unrelated applications.

---

# 3. PHASE OBJECTIVE

This phase consolidates the functionality built in Phases 1–10 into a coherent product information architecture.

The application should stop feeling like a collection of independently built screens.

It should now communicate a clear product model:

```text
MAIL
 ↓
UNDERSTAND
 ↓
ORGANIZE
 ↓
PRIORITIZE
 ↓
ACT
```

The interface should make the most important information accessible quickly while preserving deeper functionality through progressive disclosure.

---

# 4. PRODUCT IDENTITY

Mail Organizer is not intended to be:

- a Gmail clone
- a generic email client
- a spreadsheet of messages
- an analytics dashboard
- an AI chatbot

It is a:

> Personal email organization and action system.

The UI should emphasize:

- clarity
- organization
- priority
- privacy
- control
- useful intelligence
- speed

---

# 5. DESIGN SOURCE OF TRUTH

Use `design.md` as the visual source of truth.

Preserve:

### Primary

`#5B5CE2`

### Primary Container

`#E8E8FF`

### Light Background

`#F8F9FC`

### Dark Background

`#101114`

### Light Surface

`#FFFFFF`

### Dark Surface

`#1A1B20`

### Dark Elevated Surface

`#222329`

### Success

`#16A34A`

### Warning

`#D97706`

### Error

`#DC2626`

### Info

`#2563EB`

Do not introduce a competing color system.

---

# 6. DESIGN LANGUAGE

The product should feel:

- modern
- calm
- intelligent
- trustworthy
- privacy-conscious
- structured

Avoid:

- excessive gradients
- excessive glassmorphism
- neon effects
- unnecessary animations
- visual clutter
- Gmail-style imitation
- excessive cards
- excessive badges

---

# 7. CORE NAVIGATION

Implement the primary information architecture described in `design.md`.

Primary destinations:

```text
Home
Mail
Categories
Companies
Actions
```

Secondary destinations:

```text
Search
Integrations
Settings
Privacy
Accounts
```

Use the existing navigation architecture rather than replacing it unnecessarily.

---

# 8. HOME

Home is the primary landing experience.

It should answer:

> What should I pay attention to right now?

It should not simply show:

```text
Inbox
Inbox
Inbox
Inbox
```

The Home screen should prioritize useful information.

---

# 9. HOME INFORMATION HIERARCHY

A reasonable structure:

```text
Account context
        ↓
Attention / Action Required
        ↓
Important / High Priority
        ↓
Recent mail
        ↓
Category overview
        ↓
Company / sender context
```

Adapt to the actual available data.

Do not invent information that the backend does not yet provide.

---

# 10. HOME SHOULD USE REAL DATA

Do not use fake:

- counts
- messages
- companies
- categories
- action items
- priority scores

All displayed data must come from the local data layer.

If there is no data, show a proper empty state.

---

# 11. HOME EMPTY STATE

For a new account with no synchronized email:

Explain:

- account connected
- synchronization status
- what the user can do next

Do not display fabricated statistics.

---

# 12. HOME LOADING STATE

Initial loading should distinguish between:

```text
App loading
Syncing
Local data loading
```

Do not display a meaningless spinner indefinitely.

---

# 13. HOME OFFLINE STATE

If offline:

Show locally available information.

If synchronization is unavailable, communicate it subtly.

Do not disable the entire application.

---

# 14. ATTENTION SECTION

If Action Required data exists:

Display a concise attention section.

For example:

```text
Needs your attention
3 emails
```

The exact UI is up to the design system.

Do not build external action execution here.

---

# 15. PRIORITY SECTION

Show meaningful high-priority mail where appropriate.

Possible:

```text
High priority
Interview invitation
Payment due
Security alert
```

Do not show every High item if it creates overwhelming density.

Use sensible limits and allow navigation to the complete filtered list.

---

# 16. RECENT MAIL

Provide a concise recent-mail section.

It should allow the user to quickly access:

- latest messages
- unread messages
- important messages

Do not duplicate the entire Mail screen.

---

# 17. CATEGORY OVERVIEW

Show the core Mail Organizer categories.

Possible display:

```text
Career
Education
Security
Receipts & Orders
Newsletters
Promotions
...
```

Use actual local counts.

Do not create fake counts.

---

# 18. COMPANY OVERVIEW

If company intelligence is available:

Show a concise company section.

For example:

```text
GitHub
University
Amazon
```

Only show companies supported by actual data.

Do not create a giant company dashboard.

---

# 19. HOME PERSONALIZATION

Do not implement a complex personalization engine.

Use deterministic product data already available.

Do not use AI to decide Home layout.

---

# 20. MAIL DESTINATION

The Mail destination should be the complete mail-browsing experience from Phase 6.

It should provide access to:

- inbox/list
- threads
- messages
- account context
- filters where already implemented
- search entry point

Do not rebuild the mail viewer.

---

# 21. CATEGORIES DESTINATION

Build a category-oriented browsing experience.

The user should be able to select:

```text
Career
Education
Security
Receipts & Orders
Notifications
Newsletters
Promotions
Low Value
...
```

and see relevant locally stored messages.

Use Phase 7 classification.

---

# 22. CATEGORY DETAIL

A category detail screen should show:

- category name
- concise description where useful
- relevant messages
- count if available
- filtering/sorting where already supported

Do not implement the Phase 12 custom rules system.

---

# 23. CATEGORY COUNTS

Counts must be derived from local data.

Be explicit about what a count means.

For example:

```text
12 messages
```

must not silently mean:

unless the UI explicitly says so.

---

# 24. CATEGORY EMPTY STATES

For:

```text
Career
```

with no results:

Show:

> No career emails yet.

Do not show fake examples.

---

# 25. COMPANIES DESTINATION

Build a company-oriented browsing screen using Phase 8 data.

It should allow the user to understand:

```text
Company
 ↓
Senders
 ↓
Messages
```

---

# 26. COMPANY LIST

Display useful information such as:

- company name
- domain
- message count
- most recent message
- category distribution where useful

Do not overload the list.

---

# 27. COMPANY DETAIL

A company detail screen may show:

- company name
- domains
- known senders
- recent messages
- categories
- priority distribution where useful

Do not implement full company analytics.

---

# 28. UNKNOWN SENDERS

Do not hide senders simply because company resolution failed.

Display:

```text
Unknown company
```

or simply the sender identity.

Unknown is valid data.

---

# 29. ACTIONS DESTINATION

Phase 9 introduced Action Required.

Phase 11 can create the **Actions destination UI** as an organizational view.

However:

**Do not implement actual external action execution yet.**

The Actions screen can show:

```text
Needs attention
Interview invitation
Payment due
Verify account
```

but not automatically perform:

- reply
- send
- Calendar creation
- Tasks creation
- Gmail modification

---

# 30. ACTIONS vs ACTION ENGINE

This distinction is mandatory.

Phase 11:

```text
→ view things that need attention
```

Later Phase 14:

```text
→ safely execute confirmed actions
```

Do not merge them.

---

# 31. ACTION ITEM DETAILS

Where appropriate, an action item can show:

- email subject
- sender
- category
- priority
- reason
- extracted evidence
- deadline evidence if available

Do not create fake deadlines.

---

# 32. ACTIONS EMPTY STATE

If nothing requires attention:

Show a positive but calm empty state.

For example:

> You're all caught up.

Do not use excessive celebration animations.

---

# 33. SEARCH INTEGRATION

Search from Phase 10 must remain accessible.

Search should be reachable without navigating through multiple unrelated screens.

Use the existing search implementation.

Do not create a second search engine.

---

# 34. GLOBAL SEARCH ENTRY

If design permits, provide a prominent search entry point.

Do not make search permanently consume excessive screen space.

---

# 35. ACCOUNT SWITCHER

The current account must be understandable.

The account switcher should support the multi-account architecture already created.

If only one account is connected:

Do not make the interface unnecessarily complex.

---

# 36. ACCOUNT IDENTITY

When viewing account-specific data, make it clear which account is active.

This is especially important for:

- Mail
- Categories
- Companies
- Actions
- Search

---

# 37. UNIFIED VIEW

Do not implement a complex unified inbox unless the existing architecture already supports it safely.

If unified mode is available:

Every item must retain account identity.

Never hide the account context when ambiguity could matter.

---

# 38. PRIVACY DESTINATION

The navigation may expose a Privacy screen.

Phase 11 can create the information architecture and basic presentation.

Do not claim privacy capabilities that are not implemented.

Possible information:

- local-first architecture
- Gmail permissions
- data storage explanation
- external processing status
- account connections

Be truthful.

---

# 39. SETTINGS DESTINATION

Create the basic Settings structure.

Possible sections:

```text
Accounts
Privacy
Appearance
Notifications
Sync
About
```

Only expose settings supported by implemented functionality.

Do not build every future setting.

---

# 40. INTEGRATIONS DESTINATION

A basic Integration entry can exist.

However:

Do not implement Calendar or Tasks yet.

The screen may communicate:

> Integrations will be available as they are connected.

Do not present unavailable integrations as active.

---

# 41. NAVIGATION STATES

Test:

```text
Home
 ↓
Mail
 ↓
Message
 ↓
Back
 ↓
Home
```

and:

```text
Home
 ↓
Categories
 ↓
Career
 ↓
Message
 ↓
Back
 ↓
Career
 ↓
Back
 ↓
Categories
```

Navigation should preserve sensible state.

---

# 42. DEEP STATE RESTORATION

Where practical, preserve:

- selected account
- selected category
- search query
- list scroll position
- selected thread

Do not introduce complex state persistence unnecessarily.

---

# 43. RESPONSIVE DESIGN

Test different screen sizes.

At minimum:

- small Android phone
- normal phone
- large phone
- emulator sizes available

If tablet/foldable support exists, adapt appropriately.

Do not design only for one screen width.

---

# 44. DESIGN SYSTEM CONSISTENCY

All screens must use centralized:

- colors
- typography
- spacing
- radii
- elevation
- icons
- components

Do not create one-off styles for each screen.

---

# 45. TYPOGRAPHY

Follow `design.md`.

Preferred:

**Inter**

with system fallback.

Use the established hierarchy:

```text
36 / 32 Display
28 Large Title
22 Title
18 Section
16 Body
13–14 Secondary
12 Caption
```

Adapt where necessary for Android UI conventions.

---

# 46. SPACING

Use the 4dp grid:

```text
4
8
12
16
20
24
32
40
48
64
```

Avoid arbitrary spacing values unless technically justified.

---

# 47. RADII

Use:

```text
8
12
16
20
pill
```

Avoid mixing many unrelated corner-radius values.

---

# 48. SURFACE HIERARCHY

Use subtle elevation and surfaces.

Do not turn every section into a floating card.

Prefer:

```text
 ↓
section
 ↓
content
```

with clear hierarchy.

---

# 49. MOTION

Follow:

```text
Micro: 100–150ms
Standard: 200–300ms
Complex: 300–400ms
```

Use motion to clarify:

- navigation
- expansion
- filtering
- state changes

Do not animate every component.

---