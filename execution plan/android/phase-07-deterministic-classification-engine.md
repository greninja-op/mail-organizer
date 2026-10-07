# PHASE 7 — DETERMINISTIC CLASSIFICATION ENGINE

You are now executing:

**Phase 7 — Deterministic Classification Engine**

This phase may begin only after Phase 6 — Core Inbox & Email Viewer has been completed and verified.

Do not execute Phase 8 or any later phase automatically.

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
Phase 7 ← YOU ARE HERE
  ↓
Phase 8
  ↓
...
```

Complete only Phase 7.

Verify it.

Update persistent project rules and documentation.

Then STOP.

---

# 1. DISCOVER THE PROJECT INSTRUCTION FOLDER

Before changing anything:

1. Locate the Mail Organizer instruction folder.
2. Read:
    - `requirements.md`
    - `spec.md`
    - `design.md`
    - `editor-rules.md`
    - development-status documentation
    - architecture documentation
    - Phase 7 instructions
3. Inspect the actual repository.
4. Verify Phase 6 is genuinely complete.
5. Identify the Mail Organizer project root.
6. Identify its Git root.
7. Identify its Android package ID.
8. Confirm all build/device/Git operations target only Mail Organizer.

Do not assume the current directory is correct.

---

# 2. STRICT MULTI-PROJECT ISOLATION

The workspace may contain multiple Android projects.

Mail Organizer is the only project permitted to change.

Never modify sibling:

- source code
- Gradle files
- settings
- dependencies
- manifests
- resources
- assets
- tests
- generated files
- SDK configuration
- JDK configuration
- Git repositories

Do not run broad commands such as workspace-wide:

```text
clean
build
format
lint
dependency updates
```

unless they are explicitly scoped to Mail Organizer.

ADB operations must target only the Mail Organizer package.

Do not uninstall, clear data, force-stop, launch, or inspect unrelated applications.

---

# 3. READ THE PRODUCT PRINCIPLE

The classification engine must support the product pipeline:

```text
Gmail
  ↓
Synchronization
  ↓
Parsing / Normalization
  ↓
Classification
  ↓
Company / Sender Intelligence
  ↓
Priority
  ↓
Structured Information
  ↓
Actions
```

Phase 7 is specifically the **classification** stage.

Do not prematurely implement later intelligence layers.

---

# 4. PHASE OBJECTIVE

Build a deterministic, explainable, testable classification engine that assigns normalized emails to the Mail Organizer categories defined in `requirements.md`.

The initial categories are:

```text
Action Required
Important
Career
Education
Receipts & Orders
Security
Notifications
Newsletters
Promotions
Low Value
```

Classification must be:

- deterministic
- explainable
- local-first
- testable
- account-safe
- versioned
- user-correctable later
- independent of the UI
- independent of Gmail write operations
- independent of external AI services

---

# 5. CORE RULE

Do not use an AI/LLM to perform primary classification in this phase.

Do not send email content to:

- OpenAI
- Anthropic
- Gemini
- OpenRouter
- third-party classification APIs
- analytics services
- remote ML APIs

The classification engine must work without internet once the email data is locally available.

---

# 6. CLASSIFICATION ARCHITECTURE

Use a clean architecture similar to:

```text
Normalized Email
       ↓
Classification Input
       ↓
Signal Extraction
       ↓
Rule Evaluation
       ↓
Candidate Categories
       ↓
Scoring / Resolution
       ↓
Classification Result
       ↓
Local Persistence
       ↓
UI
```

Keep the classifier independent from Android UI.

The classifier should be callable from unit tests without launching the application.

---

# 7. DO NOT PUT CLASSIFICATION LOGIC IN UI

Do not implement rules directly inside:

- Composables
- Activities
- Fragments
- ViewModels

Prefer:

```text
UI
 ↓
ViewModel
 ↓
Use Case
 ↓
Classification Engine
```

The classification engine must remain independently testable.

---

# 8. CLASSIFICATION INPUT

Use the normalized data produced by Phase 5.

Potential signals include:

- sender name
- sender email
- sender domain
- recipient
- CC
- subject
- normalized plain text
- sanitized body-derived text
- Gmail labels
- Gmail category
- URLs
- unsubscribe presence
- attachment metadata
- recurring sender information
- message/thread metadata

Do not duplicate parsing logic from Phase 5.

---

# 9. RAW EMAIL IS NOT THE CLASSIFIER'S PRIMARY INPUT

Do not make the classification engine depend directly on raw Gmail API responses.

Use the normalized application/domain model.

Architecture should remain:

```text
Gmail API
 ↓
Raw Gmail model
 ↓
Parser
 ↓
Normalized Email
 ↓
Classification Engine
```

---

# 10. SIGNAL EXTRACTION

Create a controlled signal extraction layer.

For example:

```text
SenderSignal
DomainSignal
SubjectSignal
BodyKeywordSignal
GmailCategorySignal
LabelSignal
UrlSignal
UnsubscribeSignal
AttachmentSignal
RecurringPatternSignal
```

Do not blindly search the entire raw email for random words.

Signals must be meaningful and documented.

---

# 11. NORMALIZATION BEFORE MATCHING

Classification should operate on normalized text.

Handle:

- case differences
- whitespace
- Unicode normalization where appropriate
- punctuation
- common formatting noise

For example:

```text
"Your   ORDER Confirmation!"
```

should be comparable to:

```text
"your order confirmation"
```

Do not aggressively stem or rewrite text unless justified.

Avoid damaging names, product identifiers, URLs, or company names.

---

# 12. CATEGORY DEFINITIONS

Create explicit definitions for every category.

### Action Required

Messages that plausibly require a meaningful user action.

Examples:

- application deadline
- payment due
- document request
- interview scheduling
- account verification
- required response
- important request

Do not make every notification Action Required.

---

### Important

Messages that appear materially important but do not clearly fit a more specific category.

Examples:

- high-value personal correspondence
- critical service communication
- significant account communication

---

### Career

Messages related to:

- jobs
- internships
- recruiters
- interviews
- employment
- career opportunities
- professional networking
- hiring processes

---

### Education

Messages related to:

- college
- university
- classes
- assignments
- exams
- courses
- learning platforms
- academic administration
- educational events

---

### Receipts & Orders

Messages related to:

- purchases
- order confirmation
- shipping
- delivery
- invoices
- receipts
- refunds
- subscriptions with transaction evidence

---

### Security

Messages related to:

- login alerts
- password changes
- verification codes
- suspicious activity
- account security
- two-factor authentication
- security warnings
- identity verification

Security should receive strong treatment.

---

### Notifications

Routine service/application notifications.

Examples:

- GitHub notifications
- application alerts
- account activity notifications
- system notifications
- status notifications

---

### Newsletters

Recurring informational emails intentionally distributed to subscribers.

Signals may include:

- unsubscribe information
- newsletter indicators
- recurring sender patterns
- known newsletter formats
- marketing-style content

Do not classify every promotional email as Newsletter.

---

### Promotions

Marketing and promotional communication.

Examples:

- sales
- discounts
- product promotions
- offers
- marketing campaigns

---

### Low Value

Low-priority/noisy messages that do not fit a more meaningful category.

This should be conservative.

Do not dump unfamiliar emails into Low Value simply because the classifier is uncertain.

---

# 13. CATEGORY PRIORITY / CONFLICT RESOLUTION

Emails may match multiple categories.

For example:

```text
"Your Amazon order has shipped"
```

could match:

- Receipts & Orders
- Notifications
- Promotions

The classifier must resolve conflicts deterministically.

Create an explicit precedence strategy.

For example, security-sensitive messages should normally outrank generic notification/promotional matches.

Do not rely on random rule order.

Document the precedence.

---

# 14. CLASSIFICATION RESULT MODEL

Create a structured classification result.

Conceptually:

```text
ClassificationResult
 ├── category
 ├── confidence
 ├── matchedSignals
 ├── ruleId
 ├── classifierVersion
 └── timestamp
```

Adapt naming to the existing project conventions.

Do not blindly copy this structure if the existing architecture has a better equivalent.

---

# 15. CONFIDENCE

Classification should expose confidence.

Confidence does not need to be machine-learning probability.

It can represent deterministic rule strength.

For example:

```text
HIGH
MEDIUM
LOW
```

or a bounded numeric representation.

The meaning must be documented.

Do not present deterministic confidence as statistical certainty.

---

# 16. EXPLAINABILITY

Every non-trivial classification should be explainable.

For example:

```text
Category:
Career

Why:
Sender domain matches a known recruiting domain.
Subject contains "interview".
```

Or:

```text
Category:
Receipts & Orders

Why:
Detected order confirmation language and transaction-related sender.
```

The exact UI explanation can be implemented later.

The classification result must contain enough structured information to support it.

---

# 17. RULE IDENTIFIERS

Give meaningful rules identifiers.

Examples:

```text
SECURITY_OTP
SECURITY_LOGIN_ALERT
CAREER_JOB_APPLICATION
CAREER_RECRUITER
EDUCATION_COLLEGE
ORDER_CONFIRMATION
SHIPPING_NOTIFICATION
NEWSLETTER_UNSUBSCRIBE
PROMOTION_DISCOUNT
```

Use a consistent naming convention.

Avoid anonymous rules such as:

```text
RULE_1
RULE_2
RULE_3
```

---

# 18. RULE VERSIONING

The classifier must have a version.

For example:

```text
classifierVersion = 1
```

or an equivalent project-specific mechanism.

When classification logic changes materially, future phases should be able to determine which classifier version produced a result.

Do not silently overwrite classification history without version awareness.

---

# 19. DETERMINISM

The same normalized email and same rule configuration must produce the same classification result.

For:

```text
same input
+
same rules
+
same classifier version
```

the output must be deterministic.

Do not use:

- randomness
- network state
- current time
- model sampling
- device state

to influence classification.

---

# 20. SENDER / DOMAIN SIGNALS

Sender/domain matching can be highly useful.

Support controlled matching for known domains and sender patterns.

Examples:

```text
linkedin.com
github.com
coursera.org
university domain
bank domain
```

Do not hardcode an enormous arbitrary database.

Create an architecture that can grow.

---

# 21. SUBJECT SIGNALS

Use subject signals carefully.

Examples:

```text
"verification code"
"password reset"
"interview"
"application"
"order confirmed"
"invoice"
"shipped"
"delivery"
"exam"
"assignment"
"sale"
"discount"
"newsletter"
```

Avoid classifying solely from one weak keyword.

For example:

```text
"sale"
```

inside an unrelated sentence should not automatically mean Promotion.

---

# 22. BODY SIGNALS

Body signals may be used when subject/sender information is insufficient.

Use normalized text.

Do not scan enormous raw HTML.

Do not render the body merely to classify it.

Classification must remain local and computationally controlled.

---

# 23. GMAIL CATEGORY SIGNALS

Use Gmail's existing categories when available as a signal.

For example:

- PRIMARY
- SOCIAL
- PROMOTIONS
- UPDATES
- FORUMS

However:

**Gmail category must not automatically become the Mail Organizer category.**

Mail Organizer has its own classification model.

Use Gmail metadata as one signal among several.

---

# 24. LABEL SIGNALS

Existing Gmail labels can provide useful context.

Do not assume every label is trustworthy or semantically identical across accounts.

Account-specific labels must remain account-scoped.

---

# 25. UNSUBSCRIBE SIGNAL

Presence of unsubscribe information may strongly suggest:

- Newsletter
- Promotion

But unsubscribe alone must not determine the final category.

For example, legitimate account/service emails may contain unsubscribe mechanisms.

Use it as a supporting signal.

---

# 26. URL SIGNALS

URLs can provide contextual information.

For example:

- job platforms
- educational platforms
- shopping platforms
- security/account domains

Do not visit URLs.

Do not make network requests to classify an email.

Analyze only normalized URL information already present locally.

---

# 27. ATTACHMENT SIGNALS

Attachment metadata can support classification.

Examples:

- invoice PDF
- resume document
- academic document

Do not download attachments for classification.

Do not inspect arbitrary attachment binaries in this phase unless the existing normalized model already safely exposes relevant metadata.

---

# 28. RECURRING SENDER SIGNAL

If the local data model already contains recurring sender information, it may be used.

Do not build the complete sender/company intelligence system yet.

That belongs to Phase 8.

Keep this signal minimal and architecture-friendly.

---

# 29. ACTION REQUIRED IS A CATEGORY, NOT A SECOND ENGINE

Phase 9 will implement the richer Action Required and priority system.

For Phase 7, only establish enough category semantics to avoid architecture conflicts.

Do not build deadline extraction.

Do not build meeting detection.

Do not build action cards.

Do not build task creation.

---

# 30. USER CORRECTIONS

The classification engine must be designed so that later user corrections can override deterministic classification.

Do not implement the complete correction UI yet.

However, the architecture must support:

```text
Classifier result
        ↓
User correction
        ↓
Override
        ↓
Final effective category
```

Do not make classifier output permanently immutable.

---

# 31. CLASSIFIER VS USER OVERRIDE

Never confuse:

```text
Predicted category
```

with:

```text
Effective category
```

Future architecture should be capable of preserving both.

For example:

```text
classifierCategory = Promotions
userOverride = Career
effectiveCategory = Career
```

Do not destroy the original classifier result when implementing future corrections.

---

# 32. PERSISTENCE

Store classification results locally.

Use the data architecture from Phase 2.

Classification must remain account-scoped.

Do not store classification in a global table without account ownership.

---

# 33. RECLASSIFICATION

When classifier rules change:

The architecture should support reclassification.

Do not require a complete Gmail resynchronization.

Classification is a local derived operation.

Conceptually:

```text
Existing normalized email
        ↓
New classifier version
        ↓
New classification
```

---

# 34. IDEMPOTENCY

Running classification twice on the same unchanged input must not create duplicate records.

Use deterministic updates/upserts.

Do not create:

```text
classification #1
classification #2
classification #3
```

for the same email unless explicit history is intentionally designed.

---

# 35. THREAD VS MESSAGE CLASSIFICATION

Decide explicitly whether classification is:

- message-level
- thread-level
- or both

The preferred architecture should preserve message-level evidence while allowing thread-level presentation later.

Do not destroy individual message classification context.

Document the decision.

---

# 36. ACCOUNT ISOLATION

The same sender or domain may behave differently across accounts.

Classification data must remain account-aware.

Never allow:

```text
Account A correction
```

to silently affect:

```text
Account B
```

unless the product explicitly defines a global rule later.

---

# 37. RULE CONFIGURATION

Keep classification rules structured.
Avoid a giant function such as:

```text
if (...) {
...
} else if (...) {
...
} else if (...) {
...
}
```

Prefer a maintainable rule architecture.

The exact implementation is up to the existing codebase.

The architecture should support:

- rule registration
- rule IDs
- signals
- scores/weights
- precedence
- explanations
- versioning
- future user rules

---

# 38. SCORING

A scoring model is acceptable if it remains deterministic.

For example:

```text
Security signal      +100
Career strong signal  +80
Education strong      +80
Order confirmation    +80
Newsletter            +50
Promotion             +40
Generic notification  +30
```

These numbers are examples only.

Do not blindly use them.

Choose values based on the product requirements and document them.

---

# 39. STRONG VS WEAK SIGNALS

Distinguish:

### Strong signals

- known security pattern
- OTP/verification code
- explicit order confirmation
- explicit interview invitation
- explicit university communication

### Weak signals

- generic words
- generic unsubscribe
- generic marketing language
- isolated keywords

A strong signal should generally outweigh multiple weak unrelated signals.

---

# 40. NEGATIVE SIGNALS

Where necessary, rules may contain negative signals.

Example:

A generic "sale" keyword should not override a strong educational sender/domain signal.

Use negative evidence carefully.

Do not make the system unnecessarily complex.

---

# 41. UNKNOWN / UNCERTAIN EMAILS

When confidence is low:

Do not force an arbitrary category simply to avoid uncertainty.

If the product contract allows an "unclassified" internal state, use it.

If the visible category set requires one of the defined categories, choose the safest fallback and preserve low confidence.

Document the behavior.

Do not use Low Value as a universal unknown bucket.

---

# 42. NO REMOTE AI FALLBACK

Do not add:

```text
Classifier fails
 ↓
send email to AI
```

Not in Phase 7.

Optional AI fallback is Phase 26.

---

# 43. NO USER TRACKING

Do not add classification telemetry.

Do not send:

- subjects
- senders
- body text
- categories
- URLs

to external analytics.

---

# 44. TEST-FIRST CLASSIFICATION

Build comprehensive unit tests.

At minimum include:

### Security

- OTP
- password reset
- login alert
- suspicious login
- account verification

### Career

- job opening
- recruiter email
- interview invitation
- application status

### Education

- college notice
- assignment
- exam
- course update

### Orders

- order confirmation
- shipping
- delivery
- invoice
- refund

### Newsletter

- recurring newsletter
- unsubscribe footer

### Promotion

- discount
- sale
- promotional campaign

### Notification

- routine service notification

### Low Value

- clearly low-value/noisy content

---

# 45. CONFLICT TESTS

Explicitly test overlapping signals.

Examples:

```text
Promotion + Order
Newsletter + Security
Notification + Career
Promotion + Career
Education + Notification
Security + Notification
```

Verify the precedence strategy.

---

# 46. ADVERSARIAL TESTS

Test misleading messages.

For example:

```text
Subject:
"Your account security sale is here!"
```

Do not blindly classify from one word.

Also test:

```text
"Interview tips newsletter"
```

which may contain both Career and Newsletter signals.

The result must follow documented rules.

---

# 47. MALICIOUS CONTENT TESTING

Classification must safely handle:

- HTML
- script-like text
- JavaScript URLs
- huge strings
- Unicode
- unusual punctuation
- encoded text

Classification must never execute content.

---

# 48. PERFORMANCE TESTING

Test classification with:

- 100 emails
- 1,000 emails
- 10,000 emails where practical

Measure whether classification remains reasonable.

Do not block the main UI thread.

If batch classification is required, use appropriate background execution.

---

# 49. INCREMENTAL CLASSIFICATION

When new email arrives:

Only classify newly changed/new normalized messages when possible.

Do not reclassify the entire mailbox on every sync.

However, design the system so a full reclassification can be requested later.

---

# 50. CLASSIFIER CACHE / DERIVED DATA

Treat classification as derived data.

If normalized email content changes:

the classification may become stale.

Design appropriate invalidation/reclassification behavior.

Do not allow stale results to silently persist forever.

---

# 51. DATABASE INDEXING

If classification queries become frequent, add appropriate indexes.

Possible fields:

- account ID
- message ID
- thread ID
- category
- classifier version

Do not create indexes without a query/use case.

---

# 52. UI INTEGRATION

Connect classification to the existing mail UI only enough to make the result visible/useful.

For example:

- category label
- category filter entry if already appropriate
- category shown on message detail

Do not redesign the entire dashboard.

Phase 11 will handle the broader information architecture.

---

# 53. CATEGORY VISUALS

Use `design.md`.

Category colors:

- Action Required → red
- Important → primary
- Career → blue
- Education → purple
- Receipts & Orders → green
- Security → amber
- Notifications → blue/neutral
- Newsletters → teal
- Promotions → orange
- Low Value → gray

Do not use color as the only category indicator.

Include text/icon/accessible semantics.

---

# 54. DARK MODE

Verify category presentation in:

- light mode
- dark mode

Ensure category colors remain readable and do not become overly saturated.

Follow the design tokens.

---

# 55. EXPLANATION UI

If the current UI architecture makes it straightforward, provide a basic way to inspect why an email received its category.

For example:

```text
Career

Why this category?
• Sender matches a recruiting domain
• Subject indicates an interview
```

If full explanation UI would exceed Phase 7 scope, expose the data through the domain/application layer and defer polished presentation.

Do not build a complete intelligence dashboard.

---

# 56. CLASSIFICATION FAILURE

If classification fails for one message:

- do not crash synchronization
- preserve normalized email
- record a safe diagnostic
- continue processing other messages

Do not lose the mailbox because one email is malformed.

---

# 57. LOGGING

Never log:

- email body
- full subject
- sender email
- recipient email
- URLs
- authentication tokens

Diagnostics should use safe identifiers such as:

```text
classification rule ID
classifier version
internal message ID/hash where safe
error category
```

---

# 58. PRIVACY

Classification must run locally.

The default architecture should allow:

```text
email data
 ↓
local parser
 ↓
local classifier
 ↓
local database
```

No external service should receive email content.

---

# 59. SECURITY REVIEW

Verify:

- no arbitrary code execution
- no network calls from classifier
- no email-content logging
- no account mixing
- no unsafe regex behavior
- no catastrophic regex backtracking on attacker-controlled text
- bounded processing of very large messages
- safe Unicode handling

---

# 60. REGEX SAFETY

If regular expressions are used:

- keep them simple
- avoid catastrophic patterns
- test worst-case strings
- do not compile dynamic attacker-controlled regexes

Do not allow user-provided content to become executable regex configuration.

---

# 61. RULE DOCUMENTATION

Document each production rule with:

```text
Rule ID
Purpose
Signals
Strength
Category
Precedence
Explanation
Known limitations
```

This documentation may live in code comments, structured rule definitions, or project documentation depending on architecture.

Avoid duplicating the same rule definition in multiple places.

---

# 62. SOURCE TRACEABILITY

For each classification result, retain enough information to determine:

```text
Which email?
Which account?
Which classifier version?
Which rule?
Which signals?
When classified?
```

Do not require raw email content to explain a classification.

---

# 63. TEST FIXTURES

Create safe synthetic fixtures.

Do not commit:

- real user emails
- private addresses
- real tokens
- private attachments
- production mailbox dumps

Use fictional test data.

---

# 64. DEVICE VALIDATION

This is still an Android development phase.

Use the complete toolchain.

Perform:

```text
Gradle build
↓
Install Mail Organizer APK
↓
Launch
↓
Use synchronized/local test data
↓
Verify category presentation
↓
Open message
↓
Verify explanation where implemented
↓
Test account isolation
↓
Test offline behavior
↓
Capture screenshots
↓
Use screen recording where useful
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

Do not consider unit tests alone sufficient.

---

# 65. ADB ISOLATION

All ADB operations must target the Mail Organizer package.

Do not:

- uninstall another app
- clear another app's data
- inspect another app's private storage
- alter another project's runtime
- overwrite another project's reverse proxy

---

# 66. SCREENSHOT QA

Inspect:

- category labels
- typography
- category colors
- spacing
- unread state
- dark mode
- long category names
- small screen sizes
- large text

Fix visual issues.

---

# 67. ACCESSIBILITY QA

Verify:

- category is not communicated by color alone
- screen reader can identify category
- explanations are understandable
- touch targets remain usable
- text scaling works
- contrast remains acceptable

---

# 68. REGRESSION TESTING

After classification is integrated, verify Phase 6 still works.

Test:

- Mail list
- Thread view
- Message detail
- Offline viewing
- Account switching
- HTML rendering
- Back navigation

Classification must not break the core mail experience.

---

# 69. BUILD AND TEST

Run the Mail Organizer project's own:

- Gradle build
- unit tests
- Android tests
- lint/static analysis where configured

Fix errors caused by this phase.

Do not modify unrelated project configurations to make the build pass.

---

# 70. GIT REVIEW

From the Mail Organizer Git root:

```text
git status
git diff
```

Verify:

- only Mail Organizer files changed
- no private email data added
- no secrets added
- no unrelated project files changed
- no generated artifacts accidentally committed

Do not commit unless explicitly instructed.

---

# 71. UPDATE `editor-rules.md`

Before completing the phase, update `editor-rules.md` with any permanent classification rules discovered.

At minimum preserve:

- deterministic-first classification
- local-only classification
- explainability
- versioning
- account isolation
- user override compatibility
- no remote AI
- safe signal extraction
- bounded processing
- no sensitive logging

Do this yourself.

---

# 72. UPDATE `spec.md`

Only after verification:

- mark Phase 7 tasks complete
- record implementation decisions
- record deferred work
- record known limitations

Do not mark Phase 8 or later complete.

---

# 73. UPDATE DEVELOPMENT STATUS

If `docs/development-status.md` exists, update it with:

- classifier architecture
- category definitions
- precedence strategy
- classifier version
- persistence model
- test coverage
- known limitations
- deferred improvements

Do not include private email content.

---

# 74. PHASE 7 ACCEPTANCE CRITERIA

Phase 7 is complete only when:

### Architecture

- [ ] classification engine is independent of UI
- [ ] normalized email model is the classifier input
- [ ] deterministic rules are structured
- [ ] rule IDs exist
- [ ] classifier version exists
- [ ] result model exists
- [ ] explanation data exists
- [ ] account scope is preserved

### Categories

- [ ] Action Required
- [ ] Important
- [ ] Career
- [ ] Education
- [ ] Receipts & Orders
- [ ] Security
- [ ] Notifications
- [ ] Newsletters
- [ ] Promotions
- [ ] Low Value

are supported according to the product contract.

### Determinism

- [ ] same input produces same result
- [ ] no randomness
- [ ] no network dependency
- [ ] no remote AI

### Signals

- [ ] sender
- [ ] domain
- [ ] subject
- [ ] normalized body
- [ ] Gmail category
- [ ] labels
- [ ] URLs
- [ ] unsubscribe
- [ ] attachment metadata where applicable

are supported appropriately.

### Safety

- [ ] no email content sent externally
- [ ] no unsafe code execution
- [ ] regex safety reviewed
- [ ] large inputs handled safely
- [ ] malformed messages do not crash classifier
- [ ] sensitive email content not logged

### Persistence

- [ ] classification stored locally
- [ ] idempotent updates
- [ ] classifier version retained
- [ ] account isolation verified
- [ ] reclassification architecture supported

### Testing

- [ ] security tests
- [ ] career tests
- [ ] education tests
- [ ] order tests
- [ ] newsletter tests
- [ ] promotion tests
- [ ] notification tests
- [ ] low-value tests
- [ ] conflict tests
- [ ] adversarial tests
- [ ] Unicode tests
- [ ] malformed-content tests
- [ ] performance tests

### Android/device

- [ ] Gradle build succeeds
- [ ] APK installs
- [ ] app launches
- [ ] classifications appear correctly
- [ ] screenshots inspected
- [ ] screen recording used where useful
- [ ] logcat inspected
- [ ] Phase 6 functionality still works
- [ ] offline behavior still works

### Workspace safety

- [ ] sibling projects untouched
- [ ] sibling Gradle files untouched
- [ ] sibling SDK configuration untouched
- [ ] unrelated apps untouched
- [ ] unrelated Git repositories untouched

---

# 75. FINAL PHASE REPORT

Provide:

## Phase 7 Status

`COMPLETE` / `PARTIAL` / `BLOCKED`

## Classification Architecture

Explain the implemented pipeline.

## Categories

List the supported categories and their behavior.

## Rule System

Explain:

- rule IDs
- signal extraction
- precedence
- scoring if used
- classifier version

## Explainability

Explain how the engine records why a category was selected.

## Persistence

Explain how classification is stored and associated with accounts/messages.

## User Override Readiness

Explain how later corrections can override classifier output without destroying the original result.

## Privacy

Confirm that classification is local and no email content is sent to external services.

## Testing

Report classification test coverage and important edge cases.

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
- regression results

## Security

Report the security review.

## Workspace Isolation

Explicitly confirm unrelated projects were not modified.

## Files Changed

List important files.

## Known Issues

List unresolved issues.

## Deferred Work

Especially:

- Company & Sender Intelligence
- advanced priority
- Action Required engine
- user corrections UI
- rules UI
- advanced search
- Calendar
- Tasks
- AI fallback
- automation
- Gmail write operations

## Acceptance Criteria

Show every Phase 7 criterion and its status.

## Next Phase

The next incomplete phase is:

**Phase 8 — Company & Sender Intelligence**

Do not execute it automatically.

---

# FINAL OPERATING MODEL

Continue following this exact workflow:

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
IMPLEMENT PHASE 7 ONLY
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
VERIFY CLASSIFICATION
        ↓
VERIFY OFFLINE BEHAVIOR
        ↓
VERIFY ACCOUNT ISOLATION
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
**Do not implement Phase 8.**

**Do not implement company intelligence.**

**Do not implement advanced priority.**

**Do not implement the full Action Required engine.**

**Do not implement user correction UI.**

**Do not implement Calendar or Tasks.**

**Do not implement AI classification.**

**Do not implement Gmail write operations.**

**Do not touch sibling Android projects.**

**Do not send email content to external services.**

**Do not consider Gradle success alone sufficient.**

Phase 7 is complete only when deterministic classification works reliably on the actual Mail Organizer application, is explainable, locally processed, persisted safely, tested against conflicting/malformed inputs, and verified on an Android device/emulator.