# PHASE 8 — COMPANY & SENDER INTELLIGENCE

You are now executing:

**Phase 8 — Company & Sender Intelligence**

This phase may begin only after Phase 7 — Deterministic Classification Engine has been completed and verified.

Do not execute Phase 9 or any later phase automatically.

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
Phase 8 ← YOU ARE HERE
  ↓
Phase 9
  ↓
...
```

Complete only Phase 8.

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
    - Phase 8 instructions
3. Inspect the actual repository.
4. Verify Phase 7 is genuinely complete.
5. Identify:
    - Mail Organizer project root
    - Git root
    - Gradle root
    - Android package ID
6. Confirm all operations are scoped to Mail Organizer.

Do not assume the current working directory is correct.

---

# 2. STRICT MULTI-PROJECT ISOLATION

The workspace may contain multiple Android projects.

Only Mail Organizer may be modified.

Never modify sibling:

- source
- Gradle files
- dependencies
- manifests
- resources
- tests
- generated artifacts
- SDK configuration
- JDK configuration
- signing configuration
- Git repositories

ADB operations must target only the Mail Organizer package.

Do not install, uninstall, clear, launch, force-stop, or inspect unrelated applications.

---

# 3. PHASE OBJECTIVE

Build the **Company & Sender Intelligence** layer.

The system should be able to take information such as:

```text
Sender name
Sender email
Sender domain
Reply-to
Recipients
Email frequency
Classification results
Known local patterns
```

and derive useful sender/company information.

The goal is to move from:

```text
john@company.com
```

toward:

```text
John
Company
company.com
Career
```

without pretending that uncertain information is certain.

---

# 4. CORE PRODUCT PRINCIPLE

Sender intelligence must improve organization without compromising privacy.

The architecture should be:

```text
Normalized Email
       ↓
Sender Identity
       ↓
Domain Intelligence
       ↓
Company Resolution
       ↓
Confidence
       ↓
Local Persistence
       ↓
Classification / UI
```

Keep sender/company intelligence independent from UI.

---

# 5. DO NOT USE REMOTE AI

Do not send email data to:

- OpenAI
- Anthropic
- Gemini
- OpenRouter
- third-party enrichment APIs
- contact enrichment services
- external company databases

Do not use an LLM to identify companies in this phase.

The system must work offline using locally available information.

---

# 6. DO NOT BUILD A GIANT COMPANY DATABASE

Do not hardcode thousands of companies.

Do not attempt to create an internet-scale company directory.

Instead, build an extensible architecture capable of supporting:

- domain-based matching
- sender-based matching
- known-domain mappings
- user corrections
- local learned patterns
- future optional enrichment

Keep the initial built-in knowledge set small and meaningful.

---

# 7. SENDER IDENTITY MODEL

Create or refine a sender entity/model.

It should conceptually support:

```text
Sender
 ├── accountId
 ├── normalizedEmail
 ├── displayName
 ├── normalizedName
 ├── domain
 ├── replyTo
 ├── companyId
 ├── confidence
 ├── firstSeen
 ├── lastSeen
 ├── messageCount
 └── metadata
```

Adapt this to the existing architecture.

Do not blindly duplicate fields already represented elsewhere.

---

# 8. EMAIL NORMALIZATION

Sender emails must be normalized consistently.

Handle:

- casing
- whitespace
- display-name formatting
- malformed addresses
- Unicode where supported

For example:

```text
"John Doe <JOHN@Example.COM>"
```

should resolve to a normalized address such as:

```text
john@example.com
```

while preserving the original display name separately where useful.

Do not destroy the original source information.

---

# 9. DOMAIN EXTRACTION

Extract the domain safely.

Example:

```text
john@example.com
        ↓
example.com
```

Handle malformed addresses safely.

Do not crash if the domain cannot be extracted.

---

# 10. DOMAIN NORMALIZATION

Normalize domains consistently.

For example:

```text
EXAMPLE.COM
Example.com
example.com
```

should resolve to the same normalized domain.

Do not blindly strip meaningful subdomains.

For example:

```text
mail.example.com
accounts.example.com
```

may carry useful information.

The architecture should distinguish:

- full sender domain
- organizational/base domain where safely determinable

---

# 11. PUBLIC EMAIL PROVIDERS

Recognize common consumer email providers where useful.

Examples:

- gmail.com
- outlook.com
- hotmail.com
- yahoo.com
- icloud.com

Do not interpret:

```text
gmail.com
```

as a company.

For personal providers, the sender identity may simply remain:

```text
John Doe
gmail.com
```

unless other local information provides a reliable company association.

---

# 12. COMPANY RESOLUTION

Company resolution should use progressively stronger signals.

For example:

```text
Strong:
known domain mapping

Medium:
sender domain + repeated local evidence

Weak:
sender display name only
```

Do not claim a company from weak evidence.

---

# 13. KNOWN DOMAIN MAPPINGS

Create a small initial mapping system.

Conceptually:

```text
github.com       → GitHub
linkedin.com     → LinkedIn
amazon.in        → Amazon
google.com       → Google
microsoft.com   → Microsoft
```

The actual initial mapping should be reasonable and minimal.

Do not create hundreds of arbitrary mappings.

---

# 14. DOMAIN SUBDOMAIN HANDLING

Company domains may use subdomains.

For example:

```text
mail.example.com
notifications.example.com
careers.example.com
```

may all belong to:

```text
Example
```

Implement a safe organizational-domain strategy.

Do not rely on naïve string splitting such as simply taking the last two labels for every country/domain.

Examples such as:

```text
example.co.uk
example.com.au
```

must not be incorrectly resolved.

Use an appropriate existing library if already available or create a conservative strategy.

Do not introduce a large dependency without justification.

---

# 15. SENDER VS COMPANY

Do not conflate:

```text
Sender
```

with:

```text
Company
```

For example:

```text
Jane Doe <jane@company.com>
```

should preserve:

```text
Sender:
Jane Doe

Company:
Company
```

as separate concepts.

Multiple senders may belong to the same company.

---

# 16. COMPANY ENTITY

Create a company representation if needed.

Conceptually:

```text
Company
 ├── id
 ├── account scope
 ├── normalizedName
 ├── displayName
 ├── primaryDomain
 ├── knownDomains
 ├── confidence
 └── source
```

Decide whether company identity is global or account-scoped.

Any user-derived association must remain account-safe.

---

# 17. COMPANY SOURCE

Track where company information came from.

Examples:

```text
KNOWN_DOMAIN
USER_CORRECTION
LOCAL_PATTERN
IMPLICIT_DOMAIN
UNKNOWN
```

Do not represent inferred data as verified fact.

---

# 18. CONFIDENCE

Company resolution must expose confidence.

Possible levels:

```text
HIGH
MEDIUM
LOW
UNKNOWN
```

or a documented numerical scale.

Do not call deterministic confidence a statistical probability.

---

# 19. EXPLAINABILITY

The system should be able to answer:

> Why did you associate this sender with this company?

Examples:

```text
Company: GitHub

Reason:
Sender domain matches github.com.
```

or:

```text
Company: Example Corp

Reason:
User previously associated this sender domain with Example Corp.
```

Preserve structured evidence.

---

# 20. USER CORRECTIONS

Design the system so users can eventually correct:

```text
Sender → Company
Domain → Company
Sender name
Company name
```

Do not implement the full correction UI yet.

But architecture must support authoritative user overrides.

---

# 21. USER CORRECTION PRECEDENCE

When a user correction exists:

```text
User correction
      ↓
Known explicit mapping
      ↓
Strong local inference
      ↓
Weak inference
      ↓
Unknown
```

User corrections must win.

Do not overwrite user corrections during synchronization.

---

# 22. ACCOUNT ISOLATION

This is mandatory.

Consider:

```text
Account A:
john@company.com → Company A

Account B:
john@company.com → Company B
```

The system must not automatically merge these account-specific associations.

Global domain mappings may be shared only when they are genuinely global product knowledge.

User-created mappings should remain account-scoped unless explicitly designed otherwise.

---

# 23. RECURRING SENDERS

Track useful local sender patterns.

Possible information:

- first seen
- last seen
- message count
- thread count
- frequency
- categories
- account

Do not create invasive behavioral profiling.

Keep only data useful for mail organization.

---

# 24. SENDER FREQUENCY

Frequency can help distinguish:

- recurring newsletters
- service notifications
- frequent professional contacts
- occasional senders

However, frequency alone must not determine company or category.

It is supporting evidence only.

---

# 25. SENDER STATISTICS

If stored, keep them minimal.

Potential statistics:

```text
messageCount
threadCount
firstSeen
lastSeen
categoryDistribution
```

Do not store unnecessary behavioral analytics.

---

# 26. CATEGORY RELATIONSHIP

Company intelligence may connect with Phase 7 classification.

For example:

```text
GitHub
 ↓
Notifications
```

or:

```text
University
 ↓
Education
```

However:

**Do not let company identity automatically determine classification.**

Company is a signal, not an absolute category.

---

# 27. MULTIPLE DOMAINS

A company may use multiple domains.

Support:

```text
example.com
example.org
careers.example.com
```

where justified.

Do not automatically merge unrelated domains.

---

# 28. MULTIPLE COMPANIES / BRAND STRUCTURES

Avoid overcomplicated corporate hierarchies in this phase.

Do not build:

- parent company graphs
- subsidiaries
- acquisitions
- brand hierarchies

unless the existing requirements specifically require them.

Keep the company model simple.

---

# 29. DISPLAY NAME HANDLING

Sender names can be:

```text
Google
Google Security
Google Workspace
John from Google
```

Do not assume the display name is authoritative.

Use it as supporting evidence.

The email/domain should generally have stronger identity value than arbitrary display text.

---

# 30. SPOOFING / LOOKALIKE SENDERS

Be careful with sender names.

For example:

```text
"Google Security" <random@gmail.com>
```

must not automatically become:

```text
Google
```

Domain evidence should be stronger than display-name claims.

Never imply that a sender is officially associated with a company merely because the display name says so.

---

# 31. SECURITY SIGNALS

Company intelligence must not weaken security classification.

If a suspicious email claims to be from a company:

```text
Display name → known company
Domain → unrelated
```

preserve the mismatch.

Do not "correct" the sender into the claimed company merely to make classification look cleaner.

---

# 32. PHISHING-RESISTANT DESIGN

At minimum preserve:

```text
display name
actual email
actual domain
resolved company
confidence/source
```

Do not hide the actual sender address merely because a company name was inferred.

---

# 33. URL / DOMAIN CROSS-CHECKING

Email URLs may provide supporting evidence.

For example:

```text
sender → example.com
links → example.com
```

may increase confidence.

But:

```text
link → malicious-example.com
```

must not be silently treated as proof of company identity.

Do not visit links.

---

# 34. LOCAL-ONLY PROCESSING

Company resolution must work without internet.

Do not:

- DNS lookup
- WHOIS lookup
- web search
- URL fetch
- company API call
- favicon service
- remote logo service

during classification/sender processing.

---

# 35. COMPANY LOGOS

Do not automatically fetch company logos from the internet.

If the application already has bundled assets, they may be used.

Otherwise use:

- generated initials
- neutral icon
- local known asset

Do not leak sender information through third-party image services.

---

# 36. AVATARS

Sender avatars must be privacy-safe.

Do not automatically retrieve avatars from external services.

Use:

- local avatar if already available
- generated initials
- neutral fallback

---

# 37. UI INTEGRATION

Integrate sender/company information into the existing mail UI where useful.

Possible display:

```text
Jane Doe
Example Corp
jane@example.com
```

Keep the interface clean.

Do not overload every email row with excessive metadata.

---

# 38. SENDER DETAIL VIEW

If appropriate within the existing architecture, create a basic sender/company detail experience.

Potential information:

```text
Company
Domain
Sender count
Message count
Recent categories
```

Do not build the complete Companies dashboard.

Phase 11 handles broader information architecture.

---

# 39. COMPANY GROUPING

The system should be able to group messages by resolved company at the data layer.

Do not build the full company browsing experience yet.

Ensure future queries can efficiently retrieve:

---

# 40. SEARCH PREPARATION

Do not implement advanced search.

However, sender/company fields should be indexed appropriately so Phase 10 can build search without rewriting the data model.

---

# 41. DATABASE DESIGN

Use the existing local data architecture.

Potential relationships:

```text
Account
  ↓
Sender
  ↓
Company

Account
  ↓
Message
  ↓
Sender
```

Avoid unnecessary duplication.

Use foreign keys or equivalent relationships where appropriate.

---

# 42. INDEXING

Add indexes based on actual access patterns.

Potential candidates:

- account ID
- normalized sender email
- normalized domain
- company ID
- last seen
- message count

Do not create every possible index.

---

# 43. MIGRATIONS

If schema changes are required:

- create proper migrations
- preserve existing user data
- test migration from previous schema
- do not wipe the database

Do not use destructive migration simply because development is easier.

---

# 44. SYNCHRONIZATION INTEGRATION

When Phase 4 sync receives new messages:

sender/company intelligence should update naturally.

Do not create a second synchronization pipeline.

Prefer:

```text
Sync
 ↓
Normalize
 ↓
Persist
 ↓
Sender intelligence
 ↓
Classification
```

or another clean architecture appropriate to the existing project.
---

# 45. REPROCESSING

Existing messages must be able to receive newly introduced sender intelligence.

Do not require Gmail resynchronization.

Use locally stored normalized data.

---

# 46. IDEMPOTENCY

Processing the same sender repeatedly must not create duplicate sender entities.

For example:

```text
John <john@example.com>
john@example.com
JOHN@EXAMPLE.COM
```

must resolve to the same normalized sender identity within the appropriate account scope.

---

# 47. THREAD RELATIONSHIP

Sender intelligence should work across threads.

Do not duplicate a sender entity for every thread.

---

# 48. BCC / RECIPIENT CONSIDERATIONS

Sender intelligence primarily concerns the sender.

Recipients should not accidentally become senders.

Preserve correct semantics for:

- From
- Reply-To
- To
- CC
- BCC where available

---

# 49. REPLY-TO

A message may have:

```text
company@example.com

Reply-To:
support@vendor.com
```

Do not blindly replace the sender identity with Reply-To.

Preserve both.

Use Reply-To as supporting context only.

---

# 50. MALFORMED ADDRESS HANDLING

Handle:

- missing `@`
- malformed domains
- unusual but valid addresses
- display-name-only sender
- encoded names
- Unicode names

Do not crash.

Keep original source information where useful.

---

# 51. INTERNATIONAL DOMAINS

Support internationalized domain names where the platform/library safely supports them.

Do not corrupt Unicode domain information.

Avoid unsafe assumptions about ASCII-only addresses.

---

# 52. COMPANY NAME NORMALIZATION

Normalize company names for matching while preserving display names.

For example:

```text
Example, Inc.
Example Inc
EXAMPLE INC.
```

may normalize to the same identity where appropriate.

Do not remove meaningful legal distinctions if they matter.

---

# 53. KNOWN COMPANY DATA

If built-in mappings are introduced, keep them:

- versioned
- documented
- deterministic
- local
- easy to update

Do not scatter mappings across random UI files.

---

# 54. TEST DATA

Create synthetic sender/company fixtures.

Examples:

```text
John Doe <john@github.com>
Jane Doe <jane@university.edu>
Support <support@amazon.in>
Marketing <offers@example.com>
Fake Google <google-security@gmail.com>
```

Do not use real personal contacts.

---

# 55. TEST CASES

At minimum test:

### Exact domain

```text
user@github.com
→ GitHub
```

### Subdomain

```text
user@notifications.github.com
→ GitHub
```

### Consumer provider

```text
user@gmail.com
→ no company
```

### Display-name spoof

```text
Google Security <user@gmail.com>
→ must not blindly resolve to Google
```

### Case normalization

```text
USER@EXAMPLE.COM
user@example.com
→ same sender identity
```

### Multiple senders

```text
john@example.com
jane@example.com
→ same company
```

### Multiple accounts

Same sender in two accounts must remain account-safe.

---

# 56. CONFLICT TESTING

Test:

```text
display name says Company A
domain maps to Company B
URL suggests Company C
```

The system must follow documented precedence.

Do not invent certainty.

---

# 57. USER OVERRIDE TESTING

Create a synthetic correction:

```text
sender → Company A
```

while automatic detection says:

Verify the user correction wins.

Verify automatic processing does not overwrite it.

---

# 58. PERSISTENCE TESTING

Verify after:

- app restart
- process death
- database reload
- offline launch

sender/company data remains correct.

---

# 59. PERFORMANCE TESTING

Test with:

- hundreds of senders
- thousands of messages
- repeated senders
- many domains

Avoid repeatedly recalculating company identity for every UI recomposition.

Cache/persist derived information appropriately.

---

# 60. MEMORY SAFETY

Do not load the entire mailbox simply to build sender statistics.

Use:

- database aggregation
- paging
- bounded queries
- incremental processing

where appropriate.

---

# 61. UI PERFORMANCE

Verify:

- Mail list remains smooth
- opening a message remains fast
- company information does not cause visible delays
- scrolling does not repeatedly perform expensive domain resolution

---

# 62. PRIVACY REVIEW

Ensure the system does not send sender information externally.

Do not use:

- analytics
- external logos
- external avatar APIs
- company enrichment APIs
- DNS lookups

for basic functionality.

---

# 63. LOGGING RULES

Never log:

- full sender email
- recipient email
- email body
- private company associations
- URLs containing sensitive information

Safe diagnostics may include:

- internal IDs
- rule IDs
- source type
- classifier version
- processing duration
- generic failure type

---

# 64. SECURITY REVIEW

Verify:

- sender display name cannot override domain evidence
- malformed addresses cannot crash processing
- account isolation is enforced
- user corrections cannot leak across accounts
- no network requests are required
- no external resource loading occurs
- no arbitrary code execution exists

---

# 65. REGRESSION WITH PHASE 7

After implementing sender intelligence, verify classification still works.

Test examples such as:

```text
Recruiter@company.com
```

still produce appropriate Career classification.

Also verify that company detection does not automatically force an incorrect category.

---

# 66. REGRESSION WITH PHASE 6

Verify:

- mail list
- thread view
- message detail
- HTML rendering
- offline viewing
- account switching
- dark mode
- accessibility
- back navigation

continue working.

---

# 67. DEVICE TOOLCHAIN

This phase requires full Android validation.

Use:

```text
./gradlew
ADB
device/emulator
logcat
screenshots
screen recording
```

Build → install → run → inspect → fix → rebuild → reinstall → retest.

Do not consider unit tests alone sufficient.

---

# 68. ADB ISOLATION

All ADB commands must target the Mail Organizer package.

Never:

- uninstall unrelated applications
- clear unrelated app data
- alter another project's reverse mapping
- launch another project's package
- inspect unrelated app private data

---

# 69. SCREENSHOT QA

Inspect sender/company presentation for:

- long company names
- long sender names
- Unicode
- missing company
- low confidence
- spoofed display names
- dark mode
- large font
- narrow screen

Fix clipping, overflow, and hierarchy problems.

---

# 70. SCREEN RECORDING

Use screen recording where useful to verify:

- opening sender information
- navigating message → sender/company
- list scrolling
- account switching
- loading states

Look for:

- flicker
- layout shifts
- delayed company resolution
- frame drops

---

# 71. BUILD VALIDATION

Run:

- Gradle build
- unit tests
- Android tests
- lint/static analysis where configured

Do not change unrelated project configuration simply to force success.

---

# 72. GIT REVIEW

From the Mail Organizer Git root:

```text
git status
git diff
```

Verify:

- only Mail Organizer changed
- no private sender data committed
- no real contact information in fixtures
- no secrets
- no generated APKs
- no sibling project changes

Do not commit unless explicitly instructed.

---

# 73. UPDATE `editor-rules.md`

Before completing Phase 8, update `editor-rules.md` with permanent rules discovered during implementation.

At minimum preserve:

- sender/company separation
- domain-first identity reasoning
- user correction precedence
- account-scoped associations
- no remote enrichment
- no automatic avatar/logo fetching
- phishing-resistant sender display
- explainable company inference
- privacy-safe sender statistics

Do this yourself.

---

# 74. UPDATE `spec.md`

Only after successful verification:

- mark Phase 8 tasks complete
- document decisions
- record known limitations
- record deferred work

Do not mark Phase 9 or later phases complete.

---

# 75. UPDATE DEVELOPMENT STATUS

Update `docs/development-status.md` if present.

Record:

- sender model
- company model
- domain resolution
- confidence model
- source tracking
- account isolation
- user correction readiness
- testing
- known limitations

Do not include real user data.

---

# 76. PHASE 8 ACCEPTANCE CRITERIA

Phase 8 is complete only when:

### Sender Identity

- [ ] sender normalization works
- [ ] display name preserved
- [ ] email preserved
- [ ] domain extraction works
- [ ] malformed addresses handled safely
- [ ] Reply-To handled separately
- [ ] sender identity is account-scoped

### Company Intelligence

- [ ] company model exists where needed
- [ ] domain mapping supported
- [ ] subdomains handled
- [ ] known domains supported
- [ ] company confidence supported
- [ ] company source tracked
- [ ] unknown companies handled safely
- [ ] no giant hardcoded database

### User Corrections

- [ ] architecture supports sender correction
- [ ] architecture supports company correction
- [ ] user corrections have highest precedence
- [ ] automatic processing does not overwrite corrections

### Security

- [ ] display-name spoofing does not create false company identity
- [ ] sender/domain evidence preserved
- [ ] no external enrichment
- [ ] no DNS/WHOIS dependency
- [ ] no remote avatar/logo dependency
- [ ] no sensitive sender logging

### Account Isolation

- [ ] sender data account-scoped
- [ ] company associations account-safe
- [ ] corrections account-safe
- [ ] same sender can have different account-specific associations

### Integration

- [ ] Phase 7 classification still works
- [ ] Phase 6 mail UI still works
- [ ] company information available to future phases
- [ ] no classification corruption

### Performance

- [ ] repeated sender processing is efficient
- [ ] large mailbox does not require loading everything
- [ ] UI remains responsive
- [ ] no expensive work during recomposition

### Testing

- [ ] domain tests
- [ ] subdomain tests
- [ ] consumer-provider tests
- [ ] spoofing tests
- [ ] malformed-address tests
- [ ] multiple-sender tests
- [ ] multi-account tests
- [ ] conflict tests
- [ ] user-correction tests
- [ ] persistence tests
- [ ] performance tests

### Android/device

- [ ] Gradle build succeeds
- [ ] APK installs
- [ ] app launches
- [ ] sender/company information works
- [ ] screenshots inspected
- [ ] screen recording used where useful
- [ ] logcat inspected
- [ ] offline behavior works
- [ ] Phase 6 regression passes
- [ ] Phase 7 regression passes

### Workspace safety

- [ ] sibling projects untouched
- [ ] sibling Gradle files untouched
- [ ] sibling SDK configuration untouched
- [ ] unrelated apps untouched
- [ ] unrelated Git repositories untouched

---

# 77. FINAL PHASE REPORT

Provide:

## Phase 8 Status

`COMPLETE` / `PARTIAL` / `BLOCKED`

## Sender Architecture

Explain sender normalization and identity handling.

## Company Architecture

Explain:

- domain resolution
- company entities
- mappings
- confidence
- source tracking

## Security

Explain how spoofed display names and misleading sender information are handled.

## Account Isolation

Explain how sender/company associations remain isolated.

## User Corrections

Explain how future corrections override automatic inference.

## Privacy

Confirm no external enrichment or sender-data network calls are required.

## Testing

Report test coverage and important edge cases.

## Device Validation

Report:

- device/emulator
- Android/API level
- build
- installation
- runtime validation
- screenshots
- screen recording where used
- logcat review
- regression testing

## Workspace Isolation

Explicitly confirm unrelated projects were not modified.

## Files Changed

List important files.

## Known Issues

List unresolved issues.

## Deferred Work

Especially:

- Priority
- Action Required engine
- user correction UI
- advanced search
- company dashboard
- Calendar
- Tasks
- Gmail write operations
- AI fallback
- automation

## Acceptance Criteria

Show every Phase 8 criterion and its status.

## Next Phase

The next incomplete phase is:

**Phase 9 — Categories, Priority & Action Required**

Do not execute it automatically.

---

# FINAL OPERATING MODEL

Continue following:

```text
DISCOVER INSTRUCTION FOLDER
        ↓
READ REQUIREMENTS
        ↓
READ SPEC
        ↓
READ DESIGN
        ↓
READ EDITOR RULES
        ↓
IDENTIFY MAIL ORGANIZER ROOT
        ↓
VERIFY PREVIOUS PHASE
        ↓
READ ONLY CURRENT PHASE
        ↓
INSPECT ACTUAL PROJECT STATE
        ↓
IMPLEMENT PHASE 8 ONLY
        ↓
RUN UNIT TESTS
        ↓
RUN ANDROID TESTS
        ↓
GRADLE BUILD
        ↓
INSTALL ONLY MAIL ORGANIZER
        ↓
RUN ON DEVICE / EMULATOR
        ↓
VERIFY SENDER / COMPANY INTELLIGENCE
        ↓
VERIFY ACCOUNT ISOLATION
        ↓
VERIFY OFFLINE BEHAVIOR
        ↓
CAPTURE SCREENSHOTS
        ↓
SCREEN RECORD WHERE USEFUL
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
REGRESSION TEST PHASE 6
        ↓
REGRESSION TEST PHASE 7
        ↓
SECURITY REVIEW
        ↓
MULTI-PROJECT ISOLATION REVIEW
        ↓
UPDATE EDITOR-RULES.MD
        ↓
UPDATE SPEC.MD
        ↓
UPDATE DEVELOPMENT STATUS
        ↓
STOP
```

**Do not implement Phase 9.**

**Do not implement advanced priority.**

**Do not implement the full Action Required engine.**

**Do not implement the user correction UI.**

**Do not implement advanced search.**

**Do not implement Calendar or Tasks.**

**Do not implement AI.**

**Do not implement Gmail write operations.**

**Do not use external company enrichment.**

**Do not touch sibling Android projects.**

**Do not send sender or email data to external services.**

**Do not treat Gradle success alone as sufficient validation.**

Phase 8 is complete only when Mail Organizer can reliably identify and organize senders/companies from locally available email information, preserve uncertainty and evidence, respect account boundaries, remain privacy-first, and pass Android/device and regression validation.