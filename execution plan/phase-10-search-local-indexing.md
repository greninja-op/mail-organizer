# PHASE 5 — EMAIL DATA MODEL & PARSING

You are now executing:

**Phase 5 — Email Data Model & Parsing**

This phase may begin only after Phase 4 — Gmail Synchronization Engine has been completed and verified.

Do not execute Phase 6 or any later phase automatically.

The project continues to follow the strict sequential execution model:

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
Phase 5 ← YOU ARE HERE
  ↓
Phase 6
  ↓
...
```

Complete this phase, verify it, update the relevant project documentation/rules, and STOP.

Do not implement classification, company intelligence, priority logic, action extraction, Calendar, Tasks, AI, advanced search, or Gmail write operations during this phase.

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
    - Phase 5 instructions
4. Inspect the actual repository.
5. Verify Phase 4 is genuinely complete.
6. Identify the Mail Organizer project root.
7. Confirm that all tooling will operate only against Mail Organizer.

Do not assume the current directory is the correct project.

---

# 2. MULTI-PROJECT ISOLATION

The workspace may contain multiple Android applications.

Before running any command:

- identify the Mail Organizer project root
- identify its Git root
- identify its Gradle root
- identify its package ID

All commands must be scoped to this project.

Do not touch:

- sibling source files
- sibling Gradle files
- sibling SDK settings
- sibling dependencies
- sibling manifests
- sibling build outputs
- sibling Git repositories
- sibling Android applications on the device

Do not run broad cleanup commands from the workspace parent.

Do not alter global Android SDK configuration to solve a Mail Organizer problem.

---

# 3. UPDATE `editor-rules.md`

Before implementation, update `editor-rules.md` with permanent rules discovered in this phase where necessary.

In particular, preserve or add rules covering:

- email content is untrusted input
- Gmail API models must not leak through the entire application
- parsing must be deterministic and testable
- HTML must be sanitized
- JavaScript must never execute
- external URLs must be treated as untrusted
- MIME parsing must be defensive
- malformed emails must not crash the application
- email body processing must remain local by default
- raw and normalized representations must be clearly separated
- sensitive email content must never enter logs
- attachment binaries must not be downloaded unnecessarily
- parser failures must degrade gracefully
- parser behavior must be covered by deterministic tests
- phase execution remains sequential
- sibling projects remain isolated

Do this yourself.

---

# 4. PHASE OBJECTIVE

Phase 4 successfully brought Gmail data into the local application.

Phase 5 now establishes the **clean, normalized internal representation of that email data**.

The pipeline should become:

```text
Gmail API Response
        ↓
Raw Gmail Representation
        ↓
Parser / Normalizer
        ↓
Normalized Email Model
        ↓
Local Database
        ↓
Application
```

The goal is to make later systems independent of Gmail's raw API/MIME representation.

---

# 5. RAW VS NORMALIZED DATA

Clearly separate:

### Raw Gmail/API data

The original remote representation.

### Parsed data

Information extracted from the raw representation.

### Normalized application data

Clean, predictable values used by Mail Organizer.

### Derived intelligence

Future:

- category
- company
- priority
- action-required
- meetings
- deadlines

Do not mix these layers.

---

# 6. DO NOT IMPLEMENT CLASSIFICATION

This is critical.

Do NOT implement:

- Career classification
- Promotions classification
- Newsletter classification
- Important classification
- Action Required classification
- priority scoring
- company detection
- sender intelligence
- AI classification

The parser only prepares reliable input for those future systems.

---

# 7. DO NOT IMPLEMENT ACTION EXTRACTION

Do not detect:

- meetings
- deadlines
- payments
- travel
- applications
- tasks
- appointments

Those belong to later phases.

The parser may expose the raw information needed by those systems, such as dates, URLs, and text.

It must not decide that an email represents a meeting or deadline.

---

# 8. NORMALIZED EMAIL MODEL

Establish a clean normalized email representation.

It should support appropriate fields such as:

```text
Email
 ├── accountId
 ├── messageId
 ├── threadId
 ├── sender
 ├── recipients
 ├── cc
 ├── bcc where appropriate
 ├── subject
 ├── timestamp
 ├── snippet
 ├── plainTextBody
 ├── sanitizedHtmlBody
 ├── labels
 ├── Gmail category
 ├── unread
 ├── important
 ├── starred where relevant
 ├── attachments
 ├── URLs
 ├── unsubscribe information
 └── parser metadata
```

Use the actual project data model where already established.

Do not blindly add every possible Gmail field.

---

# 9. SENDER PARSING

Implement reliable sender parsing.

Handle formats such as:

```text
John Doe <john@example.com>
```

and:

```text
john@example.com
```

Extract:

- display name
- email address
- normalized email address
- domain

Handle:

- quoted names
- encoded names
- unusual whitespace
- international characters where applicable
- malformed headers

Do not crash on malformed sender information.

---

# 10. RECIPIENT PARSING

Parse:

- To
- CC
- BCC where available/appropriate

Represent recipients as structured data rather than leaving everything as an opaque string.

Handle:

- multiple recipients
- display names
- encoded names
- malformed entries
- duplicate addresses

Do not unnecessarily expose BCC information in UI if Gmail does not make it available to the authorized account.

---

# 11. EMAIL HEADER PARSING

Parse relevant headers safely.

Potential useful headers include:

- From
- To
- Cc
- Bcc
- Subject
- Date
- Reply-To
- Message-ID
- In-Reply-To
- References
- List-Unsubscribe
- List-Unsubscribe-Post
- Content-Type
- MIME-related headers

Do not assume every header exists.

Do not crash when headers are malformed.

---

# 12. SUBJECT NORMALIZATION

Normalize subject values carefully.

Handle:

- encoded MIME headers
- Unicode
- whitespace
- empty subjects
- repeated prefixes such as:
    - `Re:`
    - `Fwd:`
    - `FW:`

Do not destroy the original user-visible subject.

If normalization is used for analysis/search later, keep the display subject separately.

---

# 13. DATE AND TIME PARSING

Parse email timestamps robustly.

Support:

- standard email date formats
- timezone offsets
- UTC
- unusual but valid timezone formats
- malformed timestamps

Store timestamps in a consistent internal representation.

Do not silently interpret an invalid timestamp as the current time.

If parsing fails:

- retain the raw value where appropriate
- mark parsing failure
- use a safe fallback only when explicitly justified

---

# 14. MIME PARSING

Implement defensive MIME parsing.

Emails may contain:

- text/plain
- text/html
- multipart/alternative
- multipart/mixed
- nested multipart content
- attachments
- inline content

The parser must correctly navigate common MIME structures.

Do not assume a message contains only one body part.

---

# 15. PLAIN-TEXT EXTRACTION

When a valid `text/plain` body exists:

Extract it.

Normalize:

- line endings
- excessive whitespace where appropriate
- malformed encoding
- quoted content where appropriate

Do not aggressively destroy formatting that may be meaningful.

Preserve useful text structure.

---

# 16. HTML EMAIL EXTRACTION

When HTML content exists:

Parse it safely.

The parser must:

- remove unsafe scripts
- remove executable content
- sanitize unsafe elements
- handle malformed HTML
- preserve useful formatting
- preserve links where appropriate
- avoid executing JavaScript
- avoid loading arbitrary external resources automatically

The output must be safe for later rendering.

---

# 17. HTML SECURITY — CRITICAL

Email HTML is untrusted content.

Never:

- execute JavaScript from an email
- evaluate embedded code
- allow arbitrary HTML to become trusted application UI
- inject raw email HTML directly into privileged UI
- allow malicious HTML to manipulate application state

Treat all email content as hostile/untrusted input.

---

# 18. URL EXTRACTION

Extract URLs from:

- HTML anchors
- plain text
- relevant content

Normalize them carefully.

Handle:

- HTTPS
- HTTP
- malformed URLs
- tracking URLs
- encoded URLs
- duplicate URLs

Do not visit the URLs.

Do not fetch the URLs.

Do not make network requests merely because a URL appears inside an email.

---

# 19. URL SAFETY

A URL inside an email is untrusted.

Do not:

- automatically open it
- automatically download from it
- automatically execute it
- automatically authenticate to it
- automatically send data to it

Later UI may allow the user to open links explicitly.

The parser should only extract and normalize them.

---

# 20. UNSUBSCRIBE INFORMATION

Extract newsletter/unsubscribe information where present.

Relevant headers may include:

- `List-Unsubscribe`
- `List-Unsubscribe-Post`

Support common formats such as:

- mailto links
- HTTPS unsubscribe links

Do not automatically unsubscribe the user.

Do not call unsubscribe URLs.

Do not send unsubscribe emails.

Only store the information for future UI/features.

---

# 21. ATTACHMENT METADATA

Parse attachment metadata when present.

Capture only appropriate metadata such as:

- filename
- MIME type
- size where available
- attachment ID/reference
- inline/regular status

Do NOT download the attachment binary during this phase.

Do not store large files merely because metadata exists.

---

# 22. INLINE IMAGES

Handle inline image references safely.

Do not automatically fetch external image resources.

Do not allow remote images to become a hidden tracking mechanism.

If the email references an embedded image that is locally available through Gmail data, preserve metadata safely.

Actual image rendering policy belongs to the email viewer phase.

---

# 23. SIGNATURES

Do not aggressively remove signatures in Phase 5.

Signature detection can be useful later, but it should not corrupt email content.

If signature detection is introduced:

- make it deterministic
- keep original content available
- never permanently destroy source content

Prefer preserving the complete normalized body.

---

# 24. QUOTED REPLIES

Do not remove quoted replies blindly.

Preserve enough structure for future thread/conversation intelligence.

Later phases may distinguish:

```text
new content
quoted content
signature
```

but Phase 5 must not destroy information needed for that work.

---

# 25. ENCODING

Handle common email encodings correctly.

Pay particular attention to:

- UTF-8
- quoted-printable
- Base64
- MIME encoded headers
- Unicode display names
- international subjects

Malformed encoding must not crash synchronization.

---

# 26. MALFORMED EMAILS

Email data in the real world is messy.

Test and handle:

- missing subject
- missing sender
- malformed sender
- malformed MIME
- invalid date
- missing body
- HTML without plain text
- plain text without HTML
- broken encoding
- malformed URLs
- malformed unsubscribe headers
- unexpected MIME types

The application should gracefully degrade.

One malformed email must NOT stop the entire synchronization pipeline.

---

# 27. PARSER FAILURE ISOLATION

If one email fails parsing:

Bad behavior:

```text
Email #172 parsing fails
↓
Entire synchronization fails
```

Preferred behavior:

```text
Email #172 parsing fails
↓
Record safe parser error
↓
Preserve raw/sync data
↓
Continue processing other messages
```

The exact recovery mechanism should match the architecture.

---

# 28. PARSER RESULT MODEL

Create a predictable parser result.

For example conceptually:

```text
ParseResult
 ├── success
 │     └── normalized email
 │
 └── failure
       ├── safe error category
       └── recoverable source data
```

Do not expose raw parser exceptions throughout the application.

---

# 29. PARSER VERSIONING

Consider parser versioning.

If normalization behavior changes later, existing data may need to be reprocessed.

Support a mechanism such as:

```text
parserVersion
```

where appropriate.

This allows future migrations/reprocessing without ambiguity.

Do not overengineer the system.

---

# 30. SOURCE TRACEABILITY

Where appropriate, retain enough information to understand where normalized values originated.

For example:

```text
Subject
← MIME header

Sender
← From header

URLs
← HTML/plain text

Unsubscribe
← List-Unsubscribe
```

This will help future explainability/debugging.

Do not expose internal parsing metadata unnecessarily to users.

---

# 31. NORMALIZATION RULES

Create deterministic normalization rules for:

### Email addresses

- trim
- lowercase where appropriate
- preserve display form separately

### Domains

- lowercase
- normalize whitespace
- preserve original where necessary

### Names

- normalize whitespace
- decode MIME encoding

### Subject

- decode
- preserve display form
- optionally create normalized search/analysis form

### Body

- normalize encoding
- preserve meaningful formatting

Document important normalization decisions.

---

# 32. DATABASE INTEGRATION

Integrate the parser with the existing synchronization/database pipeline.

The desired flow becomes:

```text
Gmail API
   ↓
Raw Gmail Data
   ↓
Parser
   ↓
Normalized Model
   ↓
Repository
   ↓
Room
```

Do not bypass repositories.

Do not let Gmail API models become database entities directly.

---

# 33. PRESERVE GMAIL METADATA

Do not lose important remote information during normalization.

Preserve where required:

- Gmail message ID
- Gmail thread ID
- Gmail labels
- Gmail category
- unread state
- important state
- timestamps
- attachment metadata

Do not reinterpret Gmail's metadata as Mail Organizer intelligence.

---

# 34. KEEP DERIVED INTELLIGENCE SEPARATE

The parser must NOT decide:

```text
"This is Career."
"This is Promotions."
"This is Important."
"This is Action Required."
"This is Amazon."
"This is a meeting."
```

Those are later systems.

The parser only produces trustworthy normalized input.

---

# 35. TESTING STRATEGY

Create a comprehensive deterministic parser test suite.

Use synthetic email fixtures.

Do not rely on real personal emails.

Test:

- plain text
- HTML
- multipart
- encoded headers
- Unicode
- malformed MIME
- attachments
- links
- unsubscribe
- missing fields
- malformed dates
- malformed addresses
- duplicate headers
- unusual whitespace

---

# 36. FIXTURE LIBRARY

Create a structured fixture library if useful.

For example:

```text
test-fixtures/
    plain-text.eml
    html.eml
    multipart.eml
    attachment.eml
    encoded-subject.eml
    unicode.eml
    malformed-mime.eml
    unsubscribe.eml
    missing-fields.eml
```

Use the repository's existing test conventions if available.

Do not add enormous fixtures unnecessarily.

---

# 37. PARSER TEST — SENDER

Test:

```text
John Doe <john@example.com>
```

Expected:

```text
displayName = John Doe
email = john@example.com
domain = example.com
```

Also test:

```text
john@example.com
```

and malformed values.

---

# 38. PARSER TEST — SUBJECT

Test:

- normal subject
- encoded subject
- Unicode subject
- empty subject
- `Re:`
- `Fwd:`

Verify display subject remains correct.

---

# 39. PARSER TEST — BODY

Test:

### Plain

```text
Hello world.
```

### HTML

```html
<p>Hello <b>world</b>.</p>
```

### Multipart

Verify the correct representation is extracted.

Do not execute HTML.

---

# 40. SECURITY TEST — HTML

Include malicious synthetic HTML fixtures.

Examples conceptually:

```html
<script>...</script>
```

and dangerous attributes/URLs.

Verify:

- scripts removed/neutralized
- executable content cannot run
- safe content remains usable

Do not use real malicious payloads beyond what is necessary for the security test.

---

# 41. URL TESTING

Test:

- HTTPS
- HTTP
- duplicate URL
- encoded URL
- malformed URL
- URL in HTML
- URL in plain text

Verify extraction without network requests.

---

# 42. UNSUBSCRIBE TESTING

Test:

```text
List-Unsubscribe: <mailto:unsubscribe@example.com>
```

and:

```text
List-Unsubscribe: <https://example.com/unsubscribe>
```

Verify the information is extracted.

Verify no request is sent.

---
# 43. ATTACHMENT TESTING

Test synthetic attachment metadata.

Verify:

- filename
- MIME type
- size
- attachment ID/reference

Verify binary data is not downloaded.

---

# 44. MALFORMED EMAIL TESTING

Create fixtures that intentionally contain:

- invalid headers
- broken MIME
- missing boundaries
- malformed dates
- malformed addresses
- broken encoding

Expected behavior:

```text
No application crash
+
Safe parser failure/recovery
+
Synchronization continues
```

---

# 45. PERFORMANCE TESTING

Parser performance matters because mailboxes can contain many messages.

Test with a batch of synthetic messages.

Measure:

- parsing throughput
- memory usage
- large HTML handling
- MIME nesting
- database write performance

Do not perform parsing on the Android main thread.

---

# 46. LARGE EMAIL SAFETY

Test an unusually large synthetic email.

Verify:

- no UI freeze
- no excessive memory growth
- no crash
- no uncontrolled string duplication
- parser remains cancellable where appropriate

Avoid retaining unnecessary duplicate representations in memory.

---

# 47. PRIVACY

All email parsing must happen locally unless an explicit future product requirement says otherwise.

Do not send email content to:

- external AI services
- third-party parsers
- analytics services
- external logging systems
- custom backend

No network request should occur simply because parsing an email requires understanding it.

---

# 48. LOGGING

Never log:

- full email bodies
- complete HTML
- OAuth tokens
- full authorization headers
- attachment content
- unnecessary personal email addresses

If parser debugging requires logging, log only safe metadata or synthetic fixture identifiers.

---

# 49. DEVICE VALIDATION

After implementation:

1. Build using the Mail Organizer project's own Gradle wrapper.
2. Install only the Mail Organizer APK.
3. Launch the app.
4. Connect the test Gmail account.
5. Synchronize controlled test data.
6. Inspect the resulting local representation.
7. Navigate through any affected UI.
8. Capture screenshots.
9. Inspect logcat.
10. Fix issues.
11. Rebuild.
12. Reinstall.
13. Retest.

Do not touch unrelated applications on the device.

---

# 50. ADB REVERSE

Use ADB reverse only if required by the development environment.

Before modifying reverse mappings:

- inspect existing mappings where possible
- identify Mail Organizer's development port
- avoid disturbing another application's development environment

Do not create a local backend just to use ADB reverse.

---

# 51. SCREENSHOT VALIDATION

If Phase 5 changes any email preview/data display, inspect screenshots for:

- sender rendering
- subject rendering
- Unicode
- long text
- HTML rendering preparation
- missing fields
- attachment indicators
- dark mode
- overflow
- line wrapping
- spacing
- accessibility

Do not expose private mailbox data in screenshots stored in the repository.

Use synthetic test data whenever possible.

---

# 52. SCREEN RECORDING

Use screen recording when it helps validate:

- parsing-related loading
- synchronization-to-display flow
- long email rendering
- scrolling
- UI stability

Do not record real sensitive email content unnecessarily.

---

# 53. LOGCAT VALIDATION

Inspect runtime logs.

Verify:

- no crashes
- no parser stack traces escaping unexpectedly
- no email-body logging
- no HTML content logging
- no token logging
- no repeated parsing loop
- no ANR

---

# 54. NO CLASSIFICATION YET

Even though the normalized data will make classification possible, do not implement it.

Phase 7 will handle the classification engine.

Do not add hidden classification logic into the parser.

The parser must remain deterministic and semantically neutral.

---

# 55. NO COMPANY DETECTION YET

Do not infer:

```text
amazon.com → Amazon
google.com → Google
```

inside the parser.

Company intelligence belongs to Phase 8.

The parser should only reliably provide:

```text
sender
email
domain
```

for that later system.

---

# 56. NO ACTION EXTRACTION YET

Do not infer:

```text
"Meeting tomorrow at 3 PM"
```

as a meeting object.

Phase 13 will handle that.

The parser can preserve:

- body text
- URLs
- timestamps
- headers

for later extraction.

---

# 57. DATABASE MIGRATION

If the normalized model requires schema changes:

Create proper Room migrations.

Do not use destructive migration shortcuts for production data.

Test the migration.

Verify existing synchronized data remains intact.

---

# 58. BACKWARD COMPATIBILITY

If the parser model changes:

- preserve existing user data
- preserve account identity
- preserve synchronization state
- preserve local overrides
- preserve Gmail IDs

Do not wipe the database merely because the model changed.

---

# 59. FINAL MULTI-PROJECT SAFETY CHECK

Before completion, verify:

- Mail Organizer project root only
- Mail Organizer Git root only
- Mail Organizer Gradle wrapper only
- no sibling project files changed
- no sibling project built
- no sibling SDK configuration changed
- no sibling APK installed/uninstalled
- no unrelated application data cleared
- no unrelated Google Cloud configuration modified

---

# 60. FINAL SECURITY REVIEW

Verify:

- [ ] email treated as untrusted
- [ ] HTML sanitized
- [ ] JavaScript never executed
- [ ] URLs never automatically fetched
- [ ] unsubscribe links never automatically invoked
- [ ] attachment binaries not unnecessarily downloaded
- [ ] malformed messages cannot crash sync
- [ ] email content not logged
- [ ] OAuth tokens not logged
- [ ] email content remains local
- [ ] parser has deterministic behavior

---

# 61. FINAL BUILD

Run the Mail Organizer project's own Gradle build.

Verify:

- compilation
- unit tests
- parser tests
- database tests
- relevant Android tests
- lint/static checks if configured
- debug APK generation

Fix all issues introduced during this phase.

---

# 62. FINAL DEVICE VALIDATION

Perform:

```text
Build
 ↓
Install
 ↓
Launch
 ↓
Connect Gmail
 ↓
Sync controlled test messages
 ↓
Parse
 ↓
Display affected UI
 ↓
Inspect
 ↓
Screenshot
 ↓
Logcat
 ↓
Fix
 ↓
Rebuild
 ↓
Reinstall
 ↓
Retest
```

Do not stop at compilation success.

---

# 63. FINAL GIT REVIEW

From the Mail Organizer Git root:

Inspect:

```text
git status
git diff
```

Verify:

- only intended Mail Organizer files changed
- no test credentials
- no real email content committed
- no private screenshots committed
- no generated APKs accidentally committed
- no sibling project modifications

Do not commit unless explicitly instructed.

---

# 64. UPDATE `editor-rules.md`

Before completing Phase 5, update `editor-rules.md` with any permanent parser/security rules discovered during implementation.

Do not ask the user.

Preserve existing rules.

Do not replace the entire file unnecessarily.

---

# 65. UPDATE `spec.md`

Only after successful verification:

- update Phase 5 checkboxes
- mark only completed tasks
- record deferred work
- record known limitations
- update phase status

Do not mark Phase 6 or later phases.

---

# 66. UPDATE DEVELOPMENT STATUS

Update `docs/development-status.md` if present.

Record:

- parser architecture
- normalized data model
- MIME handling
- HTML safety
- URL extraction
- attachment metadata behavior
- test coverage
- known limitations

Do not include private email content.

---

# 67. PHASE 5 ACCEPTANCE CRITERIA

Phase 5 is complete only when:

### Parsing

- [ ] sender parsing works
- [ ] recipient parsing works
- [ ] subject parsing works
- [ ] date/time parsing works
- [ ] header parsing works
- [ ] MIME parsing works
- [ ] plain-text extraction works
- [ ] HTML extraction works
- [ ] HTML sanitization works
- [ ] URL extraction works
- [ ] unsubscribe extraction works
- [ ] attachment metadata parsing works

### Robustness

- [ ] malformed emails handled safely
- [ ] malformed headers handled
- [ ] malformed MIME handled
- [ ] invalid dates handled
- [ ] malformed addresses handled
- [ ] parser failure does not stop entire synchronization

### Architecture

- [ ] Gmail API models separated from application models
- [ ] parser/normalizer boundary established
- [ ] normalized data model established
- [ ] parser versioning considered/implemented where appropriate
- [ ] repository boundary preserved

### Security

- [ ] email HTML treated as untrusted
- [ ] JavaScript cannot execute
- [ ] URLs not automatically fetched
- [ ] unsubscribe URLs not automatically invoked
- [ ] attachment binaries not unnecessarily downloaded
- [ ] no sensitive email logging
- [ ] no external email processing

### Performance

- [ ] parsing off main thread
- [ ] large emails handled safely
- [ ] memory usage bounded
- [ ] large message batches tested

### Database

- [ ] normalized data persists correctly
- [ ] migrations work
- [ ] existing synchronized data preserved
- [ ] Gmail IDs preserved
- [ ] account isolation preserved

### Device

- [ ] build passes
- [ ] APK installs
- [ ] real synchronization tested
- [ ] parser behavior validated
- [ ] screenshots inspected
- [ ] logcat inspected
- [ ] runtime issues fixed

### Workspace

- [ ] sibling projects untouched
- [ ] sibling Gradle configurations untouched
- [ ] sibling SDK configuration untouched
- [ ] unrelated device applications untouched
- [ ] unrelated Git repositories untouched

---

# 68. FINAL PHASE REPORT

Provide:

## Phase 5 Status

`COMPLETE` / `PARTIAL` / `BLOCKED`

## Parsing Architecture

Explain:

```text
Gmail API
 ↓
Raw Representation
 ↓
Parser
 ↓
Normalized Model
 ↓
Repository
 ↓
Database
```

## Supported Email Data

List what is now reliably extracted.

## MIME / HTML Security

Explain the safety model.

## URL / Unsubscribe Handling

Explain what is extracted and what is intentionally NOT executed.

## Attachment Handling

Explain metadata behavior and why binary downloads are deferred.

## Robustness

Explain malformed-email handling.

## Testing

Report:

- parser fixtures
- MIME tests
- HTML security tests
- URL tests
- malformed email tests
- performance tests
- database tests

## Device Validation

Report:

- device/emulator
- build
- install
- sync
- parsing
- UI inspection
- screenshots
- logcat

## Security

Report the security validation.

## Workspace Isolation

Explicitly confirm unrelated projects were not touched.

## Files Changed

List important files.

## Known Issues

List unresolved issues.

## Deferred Work

Especially:

- inbox UI
- classification
- company detection
- priority
- action-required detection
- search
- Calendar
- Tasks
- AI
- Gmail modifications

## Acceptance Criteria

Show every Phase 5 criterion and its status.

## Next Phase

The next incomplete phase is:

**Phase 6 — Core Inbox & Email Viewer**

Do not execute it automatically.

---

# FINAL OPERATING MODEL

Continue using:

```text
DISCOVER INSTRUCTION FOLDER
        ↓
READ PROJECT CONTRACT
        ↓
IDENTIFY CURRENT PHASE
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
RUN ON DEVICE
        ↓
SYNC CONTROLLED DATA
        ↓
TEST REAL PARSING
        ↓
SCREENSHOT / RECORD WHERE USEFUL
        ↓
INSPECT LOGCAT
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

**Do not implement Phase 6.**

**Do not implement classification.**

**Do not implement company detection.**

**Do not implement priority.**

**Do not implement action extraction.**

**Do not implement AI.**

**Do not implement Calendar or Tasks.**

**Do not implement Gmail write operations.**

**Do not touch sibling projects.**

**Do not modify unrelated Android SDK, Gradle, or build configuration.**

**Do not treat a successful build as sufficient validation.**

Phase 5 ends when synchronized Gmail data has been transformed into a robust, normalized, secure, deterministic internal representation that later phases can reliably consume.