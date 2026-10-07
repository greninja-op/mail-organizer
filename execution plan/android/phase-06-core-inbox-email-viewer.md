# PHASE 6 — CORE INBOX & EMAIL VIEWER

You are now executing:

**Phase 6 — Core Inbox & Email Viewer**

This phase may begin only after Phase 5 — Email Data Model & Parsing has been completed and verified.

Do not execute Phase 7 or any later phase automatically.

The project continues to use strict sequential phase execution:

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
Phase 6 ← YOU ARE HERE
  ↓
Phase 7
  ↓
...
```

Complete Phase 6, verify it on a real Android device/emulator where available, update the project documentation and persistent rules, and STOP.

---

# 1. DISCOVER THE PROJECT INSTRUCTION FOLDER

Before modifying anything:

1. Locate the Mail Organizer instruction/phase folder.
2. Read the master project documents.
3. Read:
    - `requirements.md`
    - `spec.md`
    - `design.md`
    - `editor-rules.md`
    - architecture documentation
    - development-status documentation
    - Phase 6 instructions
4. Inspect the actual repository.
5. Verify Phase 5 is genuinely complete.
6. Identify the Mail Organizer project root.
7. Confirm all commands and modifications will remain inside this project.

Do not assume the current directory is correct.

---

# 2. MULTI-PROJECT WORKSPACE ISOLATION

The workspace may contain several Android applications.

Before running any build, test, ADB, Git, or file operation:

- identify the Mail Organizer root
- identify its Git root
- identify its Gradle root
- identify its package ID

Only operate on Mail Organizer.

Never modify:

- sibling projects
- sibling Gradle files
- sibling SDK settings
- sibling dependencies
- sibling manifests
- sibling build configurations
- sibling signing configurations
- sibling Git repositories
- unrelated Android applications on the test device

Never use broad workspace-level cleanup operations.

---

# 3. UPDATE `editor-rules.md`

If Phase 6 reveals permanent rules, update `editor-rules.md` yourself.

Especially establish/preserve rules for:

- Gmail remains the source of truth
- local data must be treated as synchronized data
- email HTML is untrusted
- email content must be safely rendered
- no JavaScript execution
- no automatic external URL requests
- email UI must handle malformed content
- large emails must not freeze the UI
- offline data must remain usable
- account identity must remain visible/traceable where necessary
- UI states must be explicit
- accessibility is mandatory
- screenshots/device validation are required for UI phases
- sibling projects remain isolated
- phase execution remains sequential

Do not ask the user to manually update the file.

---

# 4. PHASE OBJECTIVE

The objective of Phase 6 is to turn the synchronized local email data into a functional, reliable, privacy-safe core mail experience.

The user should be able to:

```text
Open Mail Organizer
      ↓
View synchronized email
      ↓
Browse email list
      ↓
See sender / subject / timestamp
      ↓
Open an email
      ↓
Read its content safely
      ↓
Navigate back
      ↓
Open a thread
      ↓
Read conversation content
```

The application should feel like a focused mail workspace, but it is **not** a Gmail clone.

Do not implement the advanced Mail Organizer intelligence yet.

---

# 5. DO NOT IMPLEMENT CLASSIFICATION

Do not implement:

- categories
- classification
- company grouping
- priority scoring
- action-required detection
- sender intelligence
- AI classification

Phase 7 begins classification.

The email viewer must simply display the currently synchronized/normalized information.

---

# 6. DO NOT IMPLEMENT GMAIL WRITE OPERATIONS

Do not implement:

- archive
- delete
- trash
- mark read/unread
- labels
- star
- send
- reply
- forward
- move

This phase is read-only.

---

# 7. CORE INFORMATION ARCHITECTURE

Establish the basic mail experience.

At minimum:

```text
Mail
 ├── Account context
 ├── Email list
 ├── Thread list/detail
 └── Email detail
```

Use the navigation foundation created in Phase 1.

Do not redesign the entire application navigation.

---

# 8. ACCOUNT CONTEXT

The current Gmail account must remain clear.

Where appropriate, display:

- account email
- account avatar/initial
- account switcher if already supported

The user must not accidentally believe that emails from another account belong to the currently selected account.

If unified multi-account views are not yet implemented, clearly show the active account.

---

# 9. EMAIL LIST

Build the core email list.

Each item should communicate the most important information quickly.

At minimum consider:

- sender
- subject
- preview/snippet
- timestamp
- unread state
- attachment indicator where applicable
- thread/message count where applicable

Do not display every available field.

Prioritize scanability.

---

# 10. EMAIL LIST VISUAL HIERARCHY

Follow `design.md`.

The hierarchy should make it easy to distinguish:

```text
Sender
Subject
Preview
Timestamp
```

Unread messages should have a clear visual distinction.

Avoid excessive decoration.

Do not turn every email row into a card with unnecessary shadows.

---

# 11. LONG CONTENT

Handle:

- long sender names
- long subjects
- long snippets
- very long email addresses

Use:

- ellipsis
- line limits
- appropriate wrapping

Do not allow one unusually long email to destroy list layout.

---

# 12. EMPTY STATE

If there are no synchronized messages:

Display a useful empty state.

For example:

> No synchronized email yet.

Explain what the user should do next.

Do not fabricate sample emails.

---

# 13. LOADING STATE

When loading local data:

Show an appropriate loading state.

Avoid unnecessary spinners for extremely fast local operations.

Use skeleton/loading placeholders where they improve the experience.

Do not block the UI unnecessarily.

---

# 14. ERROR STATE

If local database access fails:

Display a user-friendly error state.

Do not show:

- stack traces
- database SQL
- internal class names
- raw exceptions

Provide an appropriate retry action where possible.

---

# 15. OFFLINE STATE

The local mail list must remain useful when offline.

If synchronized data exists:

```text
Offline
+
Local data available
```

should still allow browsing.

Do not require an internet connection merely to read locally synchronized email.

---

# 16. SYNC STATUS

Provide a subtle indication of synchronization state where appropriate.

Possible states:

- Synced
- Syncing
- Last synced
- Sync failed
- Offline

Do not overwhelm the mail list with technical information.

The user should be able to understand whether displayed mail is current.

---

# 17. PAGINATION / LARGE MAILBOX UI

The list must handle large mailboxes.

Do not load every email into the UI simultaneously.

Use:

- paging
- lazy loading
- bounded queries
- efficient database access

The exact implementation should fit the existing architecture.

Do not load thousands of emails into memory simply because Room can return them.

---

# 18. THREAD VIEW

Implement a thread/conversation view.

A Gmail thread may contain:

```text
Message 1
Message 2
Message 3
Message 4
```

The UI should make the conversation relationship clear.

At minimum show:

- sender
- timestamp
- subject
- message content
- collapsed/expanded state where useful

---

# 19. THREAD EXPANSION

For multi-message conversations:

Consider showing:

```text
Older message
Older message
Latest message
```

with the newest message easy to access.

Do not make users repeatedly navigate through separate screens just to read a thread.

---

# 20. MESSAGE DETAIL

Build a dedicated email-detail experience.

Display:

- sender
- sender email
- recipients where appropriate
- timestamp
- subject
- body
- attachments metadata
- links
- relevant Gmail metadata where appropriate

Do not display technical MIME headers to ordinary users.

---

# 21. EMAIL HEADER DETAILS

Where useful, provide a "show details" interaction for information such as:

- from
- to
- cc
- date

Do not expose raw MIME data.

Use progressive disclosure.

---

# 22. SAFE HTML RENDERING

This is a critical security requirement.

Email HTML is untrusted.

The viewer must only render the sanitized representation produced by the parsing layer.

Never directly inject raw Gmail HTML into the UI.

Never execute JavaScript from an email.

Never allow email HTML to manipulate application UI.

---

# 23. JAVASCRIPT MUST NEVER EXECUTE

Ensure the email viewer cannot execute:

- `<script>`
- JavaScript URLs
- inline JavaScript
- arbitrary embedded code
- event handlers such as `onclick`

If the rendering technology has JavaScript support, disable it.

Do not make exceptions for "trusted" senders.

All email is untrusted.

---

# 24. EXTERNAL RESOURCES

Do not automatically load:

- remote images
- remote scripts
- tracking pixels
- external stylesheets
- arbitrary remote resources

Remote content can reveal that the user opened an email.

The default should be privacy-preserving.

If images are eventually allowed, they should use an explicit, controlled policy.

Do not introduce automatic remote loading during Phase 6.

---

# 25. URL INTERACTION

Links may be displayed.

When the user explicitly taps a link:

- use a safe external browser mechanism
- do not inject it into an internal privileged WebView without a clear security reason
- do not silently send user data
- do not automatically authenticate

Show the destination appropriately where possible.

Do not automatically open links.

---

# 26. LINK SAFETY

Handle:

- malformed URLs
- dangerous schemes
- JavaScript URLs
- unsupported schemes

Do not allow:

```text
javascript:
```

or other executable schemes to become active.

Allow only appropriate schemes such as HTTPS/HTTP where justified.

---

# 27. ATTACHMENT PRESENTATION

Display attachment metadata when available.

For example:

```text
📎 invoice.pdf
PDF · 1.2 MB
```

Do not download the attachment automatically.

Do not fetch attachment contents just because the email is opened.

Actual attachment download/open behavior can be implemented later.

---

# 28. MISSING DATA

Real emails can be incomplete.

Handle:

- missing sender
- missing subject
- missing body
- missing timestamp
- malformed recipient
- missing attachment metadata

Do not crash.

Use safe fallback text.

Examples:

```text
Unknown sender
No subject
No content available
```

Use user-friendly wording.

---

# 29. HTML / PLAIN-TEXT FALLBACK

If sanitized HTML exists:

Display it safely.

If not:

Display plain text.

If neither exists:

Show a safe empty-content state.

Do not treat empty HTML as an application error.

---

# 30. BODY FORMATTING

Preserve useful formatting:

- paragraphs
- lists
- basic emphasis
- links
- line breaks

Do not preserve dangerous executable behavior.

Avoid excessive custom rendering logic.

---

# 31. LARGE EMAIL PERFORMANCE

A single email may contain:

- huge HTML
- long quoted history
- many links
- large tables
- extensive formatting

The viewer must remain responsive.

Do not parse/render enormous content on the main thread.

Avoid unnecessary copies of large strings.

Use appropriate lazy/deferred processing where needed.

---

# 32. EMAIL SEARCH IS NOT PHASE 6

Do not implement the complete search system here.

Basic list filtering required for the core screen may be acceptable if already supported by the architecture.

Advanced structured search belongs to Phase 10.

Do not create a second search architecture.

---

# 33. MULTI-ACCOUNT BEHAVIOR

The viewer must respect account scope.

For:

```text
Account A
```

the list must only show Account A's synchronized messages unless an explicitly implemented unified view exists.

Do not mix accounts accidentally.

Test this.

---

# 34. DATABASE QUERIES

Use repository/database boundaries.

Do not do:

```text
UI → Room DAO
```

Prefer:

```text
UI
 ↓
ViewModel / Use Case
 ↓
Repository
 ↓
DAO
```

Preserve the architecture established in earlier phases.

---

# 35. UI STATE ARCHITECTURE

Use explicit UI state.

Conceptually:

```text
Loading
Content
Empty
Error
Offline
```

Do not create scattered booleans such as:

```text
isLoading
isError
hasData
isOffline
showRetry
...
```

when a sealed/state model would make the state clearer.

Follow the project's existing architecture.

---

# 36. NAVIGATION

Implement navigation between:

```text
Mail list
   ↓
Thread
   ↓
Message detail
```

Ensure:

- back works
- deep navigation does not lose account context
- returning from message detail preserves list position where practical
- rotation/process recreation does not cause obvious navigation corruption

---

# 37. DEEP LINKS / FUTURE SUPPORT

Do not implement external email deep links unless required.

However, structure navigation so future features can open:

```text
specific account
specific thread
specific message
```

without rewriting navigation.

---

# 38. REFRESH / SYNC ACTION

If the existing sync engine supports user-initiated synchronization, expose an appropriate refresh action.

It should:

- start sync
- prevent duplicate concurrent sync
- show state
- update the list when data changes
- handle errors

Do not create a separate sync implementation.

Use Phase 4's synchronization engine.

---

# 39. REAL-TIME LOCAL UPDATES

When new synchronized messages arrive:

The UI should update through the local data layer.

Prefer reactive local data observation.

Avoid manually rebuilding the entire screen through tightly coupled callbacks.

---

# 40. UI PERFORMANCE

The email list should remain responsive while:

- database emits updates
- sync runs
- large datasets are loaded
- user scrolls
- email details are opened

Do not perform database/network work on the main thread.

---

# 41. ACCESSIBILITY

The core mail UI must support:

- screen readers
- content descriptions
- accessible sender/subject hierarchy
- accessible timestamps
- touch target sizes
- keyboard/focus behavior where applicable
- dynamic font sizes
- sufficient contrast

For an email row, a screen reader should communicate useful information rather than meaningless component fragments.

---

# 42. TOUCH TARGETS

Interactive controls should have appropriate touch targets.

Avoid tiny:

- icons
- buttons
- close controls
- navigation targets

Accessibility must be considered at normal device size.

---

# 43. DARK MODE

Test:

- email list in light mode
- email list in dark mode
- email detail in light mode
- email detail in dark mode
- HTML rendering in dark mode

Do not allow email HTML to force an inappropriate application-wide theme.

---

# 44. EMAIL HTML AND DARK MODE

Be careful with HTML email styles.

A message may contain:

- white backgrounds
- black text
- inline colors

Do not allow poorly controlled email styles to make content unreadable in dark mode.

Use a safe rendering strategy that balances email fidelity and readability.

Do not modify the raw email data merely to achieve dark mode.

---

# 45. SCREENSHOT-BASED UI QA

This phase requires visual validation.

For every major screen:

1. build
2. install
3. launch
4. navigate
5. capture screenshot
6. inspect
7. compare against `design.md`
8. identify issues
9. fix
10. rebuild
11. capture again

Inspect:

- alignment
- spacing
- typography
- contrast
- hierarchy
- clipping
- long text
- dark mode
- system bars
- scrolling
- loading
- empty
- error
- offline
- thread expansion

---

# 46. SCREEN RECORDING

Use screen recording where useful to validate:

- list scrolling
- opening an email
- thread expansion
- back navigation
- loading transitions
- sync refresh
- animations

Look for:

- dropped frames
- layout jumps
- unexpected flicker
- incorrect state transitions
- UI freezes

---

# 47. ADB DEVICE WORKFLOW

Use the available Android device tooling.

The workflow should include:

```text
Gradle build
↓
APK discovery
↓
ADB install
↓
Launch Mail Organizer package
↓
ADB/logcat inspection
↓
Screenshot
↓
Screen recording where useful
↓
Retest
```

Only operate on the Mail Organizer package.

---

# 48. ADB REVERSE

Use ADB reverse only if required.

Do not modify unrelated reverse mappings.

If no local server is needed, do not create one.

---

# 49. REAL TEST DATA

For visual testing, prefer controlled/synthetic emails when possible.

Create test cases such as:

### Normal

```text
Google
Your account information
```

### Long
A very long subject.

### HTML

Formatted email with headings and links.

### Attachment

Email with attachment metadata.

### Unicode

Malayalam and other Unicode content.

### Missing fields

No subject/sender/body.

### Malformed

Controlled malformed content.

Do not commit private real email content to the repository.

---

# 50. MALAYALAM / UNICODE TESTING

Because the application may display international email content, verify Unicode handling.

Test:

- Malayalam
- emoji
- accented Latin
- non-Latin sender names
- non-Latin subjects

Verify:

- correct rendering
- no broken characters
- no clipping
- correct line wrapping

---

# 51. THREAD TESTING

Create a synthetic thread containing several messages.

Verify:

- ordering
- sender
- timestamps
- message count
- expansion/collapse
- scrolling
- newest message visibility

Do not incorrectly reverse conversation order.

Document the chosen ordering.

---

# 52. ACCOUNT ISOLATION TESTING

Use at least two controlled accounts if available.

Verify:

```text
Account A
→ only A emails

Account B
→ only B emails
```

Switching accounts must not leak data.

---

# 53. OFFLINE TESTING

After synchronization:

1. disable network
2. open Mail Organizer
3. browse synchronized messages
4. open a thread
5. open a message
6. verify local data remains available

The core read experience should continue working.

---

# 54. SYNC FAILURE TESTING

Trigger a controlled sync failure.

Verify:

- existing mail remains visible
- error is understandable
- retry is available
- application does not crash
- list does not disappear unnecessarily

---

# 55. PROCESS RESTART TEST

Test:

```text
Open app
→ browse mail
→ force stop
→ relaunch
```

Verify:

- database survives
- selected account state remains correct
- mail remains available
- no corruption
- navigation recovers gracefully

---

# 56. ROTATION / CONFIGURATION TEST

Where applicable, test:

- rotation
- background/foreground
- system theme change
- font scale

Verify that:

- list state does not corrupt
- message content remains accessible
- account context remains correct
- no unnecessary reload destroys the user experience

---

# 57. PRIVACY REVIEW

Verify that the UI does not accidentally expose sensitive email information through:

- logs
- debug overlays
- analytics
- screenshots committed to the repository
- crash reporting
- clipboard operations
- unnecessary system notifications

Do not introduce analytics in this phase.

---

# 58. SECURITY REVIEW

Verify:

- raw HTML never reaches unsafe rendering
- JavaScript disabled
- dangerous URL schemes blocked
- remote resources not automatically loaded
- attachment downloads not automatic
- malformed email cannot crash the app
- email content is not logged
- account isolation works

---

# 59. NO CLASSIFICATION

Even though the email list could visually show future categories, do not add them yet.

Do not create placeholder category chips pretending classification exists.

The next phase will introduce the actual classification engine.

---

# 60. NO COMPANY INTELLIGENCE

Do not display fake company names or inferred companies.

The sender/domain information can be shown.

Company intelligence belongs to Phase 8.

---

# 61. NO PRIORITY

Do not add priority indicators unless they already represent actual data from an earlier implemented system.

Do not invent priority.

---

# 62. NO ACTION CARDS

Do not create fake:

- meeting cards
- deadline cards
- task cards
- payment cards
- travel cards

Those belong to later phases.

---

# 63. FINAL BUILD

Use the Mail Organizer project's own Gradle wrapper.

Run appropriate:

- build
- unit tests
- Android tests
- lint/static checks

Fix all errors introduced by this phase.

---

# 64. FINAL DEVICE VALIDATION

Perform:

```text
Build
 ↓
Install
 ↓
Launch
 ↓
Verify account
 ↓
Open Mail
 ↓
Scroll
 ↓
Open thread
 ↓
Open message
 ↓
Read content
 ↓
Open safe link if applicable
 ↓
Back
 ↓
Switch theme
 ↓
Test offline
 ↓
Restart app
 ↓
Inspect screenshots
 ↓
Inspect logcat
 ↓
Fix
 ↓
Rebuild
 ↓
Reinstall
 ↓
Retest
```

Do not consider Phase 6 complete until the core read experience works on the available device/emulator.

---

# 65. FINAL GIT REVIEW

From the Mail Organizer Git root:

Inspect:

```text
git status
git diff
```

Verify:

- only Mail Organizer files changed
- no private email content committed
- no screenshots containing personal data committed
- no generated APKs committed
- no sibling project modifications

Do not commit unless explicitly instructed.

---

# 66. UPDATE `editor-rules.md`

Before completing Phase 6, update `editor-rules.md` with any permanent UI/email-viewer rules discovered during implementation.

Do not ask the user to do this.

---

# 67. UPDATE `spec.md`

Only after successful verification:

- update Phase 6 checkboxes
- mark only verified tasks
- record deferred work
- record known issues
- update phase status

Do not mark Phase 7 or later phases.

---

# 68. UPDATE DEVELOPMENT STATUS

Update `docs/development-status.md` if present.

Record:

- mail list architecture
- thread architecture
- email viewer architecture
- HTML rendering strategy
- offline behavior
- accessibility state
- device validation
- known limitations

Do not include private email content.

---

# 69. PHASE 6 ACCEPTANCE CRITERIA

Phase 6 is complete only when:

### Mail List

- [ ] synchronized emails displayed
- [ ] sender displayed
- [ ] subject displayed
- [ ] preview/snippet displayed
- [ ] timestamp displayed
- [ ] unread state represented
- [ ] attachment indication supported where appropriate
- [ ] large lists handled efficiently
- [ ] empty state works
- [ ] loading state works
- [ ] error state works
- [ ] offline state works

### Thread

- [ ] threads represented correctly
- [ ] messages ordered correctly
- [ ] multiple messages supported
- [ ] thread expansion works
- [ ] account scope preserved

### Message Viewer

- [ ] sender details
- [ ] recipients
- [ ] subject
- [ ] timestamp
- [ ] body
- [ ] attachment metadata
- [ ] safe links
- [ ] missing-data handling
- [ ] HTML rendering
- [ ] plain-text fallback

### Security

- [ ] raw HTML never rendered unsafely
- [ ] JavaScript disabled
- [ ] unsafe URL schemes blocked
- [ ] remote resources not automatically loaded
- [ ] attachment binaries not automatically downloaded
- [ ] email content not logged

### UX

- [ ] navigation works
- [ ] back behavior works
- [ ] light theme works
- [ ] dark theme works
- [ ] typography follows design
- [ ] spacing follows design
- [ ] accessibility works
- [ ] large text works reasonably
- [ ] Unicode works

### Offline

- [ ] synchronized mail readable offline
- [ ] no unnecessary network dependency for local viewing

### Device

- [ ] Gradle build passes
- [ ] APK installed
- [ ] real email data displayed
- [ ] thread tested
- [ ] message detail tested
- [ ] screenshots captured
- [ ] screen recording used where useful
- [ ] logcat inspected
- [ ] runtime issues fixed

### Workspace

- [ ] sibling projects untouched
- [ ] sibling Gradle files untouched
- [ ] sibling SDK configuration untouched
- [ ] unrelated APKs untouched
- [ ] unrelated device data untouched
- [ ] unrelated Git repositories untouched

---

# 70. FINAL PHASE REPORT

Provide:

## Phase 6 Status

`COMPLETE` / `PARTIAL` / `BLOCKED`

## Mail List

Explain the implementation.

## Thread View

Explain conversation handling.

## Email Viewer

Explain rendering and interaction.

## HTML Security

Explain how untrusted email content is safely rendered.

## Offline Behavior

Explain how local mail remains accessible without network.

## Accessibility

Report accessibility validation.

## Device Validation

Report:

- device/emulator
- Android/API level
- build
- installation
- mail browsing
- thread test
- message test
- offline test
- restart test
- screenshots
- screen recordings where used
- logcat

## Security

Report security validation.

## Workspace Isolation

Explicitly confirm unrelated projects were not modified.

## Files Changed

List important files.

## Known Issues

List unresolved issues.

## Deferred Work

Especially:

- classification
- company intelligence
- priority
- action-required
- search
- Calendar
- Tasks
- AI
- advanced automation
- Gmail write operations

## Acceptance Criteria

Show every Phase 6 criterion and its status.

## Next Phase

The next incomplete phase is:

**Phase 7 — Deterministic Classification Engine**

Do not execute it automatically.

---

# FINAL OPERATING MODEL

Continue following:

```text
DISCOVER INSTRUCTION FOLDER
        ↓
READ PROJECT CONTRACT
        ↓
IDENTIFY MAIL ORGANIZER ROOT
        ↓
VERIFY PREVIOUS PHASE
        ↓
READ CURRENT PHASE
        ↓
IMPLEMENT ONLY CURRENT PHASE
        ↓
GRADLE BUILD
        ↓
INSTALL ONLY MAIL ORGANIZER
        ↓
RUN ON REAL DEVICE / EMULATOR
        ↓
INTERACT WITH THE APPLICATION
        ↓
SCREENSHOT / SCREEN RECORD
        ↓
INSPECT LOGCAT
        ↓
TEST OFFLINE / RESTART / THEME / ACCESSIBILITY
        ↓
FIX
        ↓
REBUILD
        ↓
REINSTALL
        ↓
RETEST
        ↓
SECURITY REVIEW
        ↓
MULTI-PROJECT SAFETY REVIEW
        ↓
UPDATE RULES
        ↓
UPDATE SPEC
        ↓
STOP
```

**Do not implement Phase 7.**

**Do not implement classification.**

**Do not implement company intelligence.**

**Do not implement priority.**

**Do not implement action extraction.**

**Do not implement Calendar or Tasks.**

**Do not implement AI.**

**Do not implement Gmail write operations.**

**Do not touch sibling Android projects.**

**Do not modify unrelated SDK, Gradle, or build configuration.**

**Do not treat source-code completion or Gradle success as sufficient validation.**

Phase 6 ends only when the user can reliably browse synchronized Gmail data, open threads, read messages safely, use the interface offline, and the complete experience has been validated on an Android device/emulator.