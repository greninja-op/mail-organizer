# PHASE 9 — CATEGORIES, PRIORITY & ACTION REQUIRED

You are now executing:

**Phase 9 — Categories, Priority & Action Required**

This phase may begin only after Phase 8 — Company & Sender Intelligence has been completed and verified.

Do not execute Phase 10 or any later phase automatically.

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
Phase 9 ← YOU ARE HERE
  ↓
Phase 10
  ↓
...
```

Complete only Phase 9.

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
    - Phase 9 instructions
3. Inspect the actual repository.
4. Verify Phase 8 is genuinely complete.
5. Identify:
    - Mail Organizer project root
    - Git root
    - Gradle root
    - Android package ID
6. Confirm all operations target only Mail Organizer.

Do not assume the current working directory is correct.

---

# 2. STRICT MULTI-PROJECT ISOLATION

The workspace may contain multiple Android applications.

Only Mail Organizer may be modified.

Never modify sibling:

- source code
- Gradle files
- settings
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

Do not install, uninstall, clear data, force-stop, launch, or inspect unrelated applications.

---

# 3. PHASE OBJECTIVE

Phase 7 created deterministic categories.

Phase 8 created sender/company intelligence.

Phase 9 now adds two important layers:

```text
CLASSIFICATION
      ↓
PRIORITY
      ↓
ACTION REQUIRED
```

The system should answer three different questions:

### Category

> What kind of email is this?

### Priority

> How important is this email?

### Action Required

> Does the user probably need to do something?

These are **not the same thing**.

---

# 4. DO NOT CONFLATE CATEGORY, PRIORITY, AND ACTION

Examples:

```text
Security email
→ Category: Security
→ Priority: High
→ Action Required: Maybe
```

```text
Newsletter
→ Category: Newsletters
→ Priority: Low
→ Action Required: No
```

```text
Job interview invitation
→ Category: Career
→ Priority: High
→ Action Required: Yes
```

```text
Order delivered
→ Category: Receipts & Orders
→ Priority: Medium/Low
→ Action Required: No
```

The system must keep these dimensions independent.

---

# 5. CATEGORY REMAINS FROM PHASE 7

Do not replace the existing classification architecture.

Continue using:

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

The category result remains the output of the deterministic classifier.

Phase 9 consumes that result.

---

# 6. DO NOT REWRITE THE CLASSIFIER

Do not unnecessarily rewrite Phase 7.

If improvements are required to support Phase 9:

- preserve existing behavior
- add narrowly scoped extensions
- update tests
- document the change

Do not replace a stable classifier with a new architecture simply because Phase 9 introduces priority.

---

# 7. PRIORITY MODEL

Create a deterministic priority model.

A reasonable initial model may contain:

```text
Critical
High
Medium
Low
```

Use the existing requirements/design if another representation is already defined.

Do not expose numerical scores as if they were scientific measurements.

---

# 8. PRIORITY IS NOT CATEGORY

Do not make:

automatically.

Security emails can vary greatly.

Likewise:

is not always correct.

Priority must consider multiple signals.

---

# 9. PRIORITY SIGNALS

Potential signals include:

- classification category
- sender/company
- sender history
- urgency language
- explicit deadline
- payment due
- account security event
- interview/request
- user importance
- message recency
- direct personal correspondence
- action-required state
- known critical sender/domain
- existing Gmail importance metadata where available

Use only information actually available locally.

---

# 10. PRIORITY SHOULD BE EXPLAINABLE

A priority result should be explainable.

Example:

```text
Priority: High

Reasons:
• Interview invitation
• Explicit response requested
• Deadline detected
```

Another:

```text
Priority: Low

Reasons:
• Promotional message
• No action requested
• Recurring marketing sender
```

Do not present opaque scores without explanations.

---

# 11. PRIORITY RESULT MODEL

Create a structured result.

Conceptually:

```text
PriorityResult
 ├── level
 ├── score / weight if used
 ├── reasons
 ├── matchedSignals
 ├── version
 └── timestamp
```

Adapt this to the existing architecture.

---

# 12. PRIORITY VERSIONING

Like classification:

Priority logic must be versioned.

For example:

```text
priorityVersion = 1
```

This allows future changes without losing traceability.

---

# 13. DETERMINISM

The same:

```text
+
classification
+
sender/company data
+
priority rules
```

must produce the same priority result.

Do not use:

- randomness
- network state
- LLM output
- current time as an unexplained scoring factor

If recency is intentionally used, define it explicitly and make the behavior deterministic relative to a known reference timestamp.

---

# 14. PRIORITY LEVEL DEFINITIONS

Define clear semantics.

### Critical

Potentially time-sensitive or consequential information where missing it could cause serious harm, loss, security impact, or major opportunity loss.

Examples:

- critical security alert
- account compromise warning
- immediate payment/security issue

Use this sparingly.

---

### High

Important information likely requiring timely attention.

Examples:

- interview invitation
- application deadline
- important college deadline
- important financial/account request
- direct response requested

---

### Medium

Meaningful but not immediately urgent.

Examples:

- ordinary service notification
- non-urgent receipt
- routine professional message
- general education update

---

### Low

Minimal urgency or attention value.

Examples:

- routine newsletters
- promotions
- repetitive low-value notifications

Do not automatically make every unfamiliar message Low.

---

# 15. CRITICAL MUST BE RARE

Do not create a system where most emails become Critical or High.

A useful priority system must preserve attention.

Test the distribution across a realistic synthetic mailbox.

---

# 16. ACTION REQUIRED MODEL

Build a deterministic Action Required detector.

The question is:

> Does this message plausibly require the user to perform an action?

Possible states:

```text
YES
NO
UNKNOWN
```

or an equivalent confidence-aware representation.

Do not force uncertain messages into YES.

---

# 17. ACTION REQUIRED IS NOT A TASK

Do not create a task.

Do not call:

- Google Tasks
- Calendar
- external APIs

Action Required simply means:

> The message appears to require user attention/action.

Actual action execution belongs to later phases.

---

# 18. ACTION SIGNALS

Potential signals include:

### Explicit requests

- "Please reply"
- "Please confirm"
- "Please submit"
- "Please complete"
- "Action required"
- "Response needed"

### Deadlines

- "Due by..."
- "Deadline..."
- "Before..."
- "Expires..."
- "Submit by..."

### Scheduling

- interview invitation
- meeting confirmation requiring response
- appointment confirmation

### Transactions

- payment due
- invoice due
- verification required

### Security

- password reset
- suspicious login
- identity verification
- account confirmation

Use these carefully.

---

# 19. NEGATIVE ACTION SIGNALS

The absence of a request matters.

For example:

```text
"Your package was delivered."
```

should not automatically become Action Required.

Likewise:

```text
"Your monthly newsletter is available."
```

should normally not be Action Required.

---

# 20. ACTION REQUIRED CONFIDENCE

Store confidence separately.

For example:

```text
YES + HIGH
YES + MEDIUM
UNKNOWN
NO
```

Do not represent weak inference as definite user obligation.

---

# 21. EXPLANATION

Action-required results must be explainable.

Example:

```text
Action required: Yes

Reason:
The sender explicitly asks the recipient to submit a document before a stated deadline.
```

Do not simply display:

---

# 22. DEADLINES

Phase 9 may detect simple deadline signals only if needed to support Action Required.

Do not build the full deadline extraction system.

Phase 13 handles:

- robust date extraction
- deadline normalization
- timezone handling
- recurring dates
- date ambiguity

Phase 9 may preserve raw evidence such as:

but should not become the full deadline engine.

---

# 23. MEETING DETECTION

Do not implement the full meeting extraction engine.

Phase 13 handles meeting/deadline extraction.

Phase 9 may use obvious existing metadata as a signal if already available.

Do not create Calendar events.

---

# 24. GMAIL IMPORTANCE

Gmail's own importance/label signals may be used as supporting evidence.

Do not treat Gmail importance as the Mail Organizer priority.

The two systems are separate.

---

# 25. RECENCY

Recency can influence priority, but carefully.

Do not create a system where every new email becomes High.

If recency is used:

- define decay behavior
- make it predictable
- do not permanently inflate priority
- preserve the underlying reasons

---

# 26. SENDER IMPORTANCE

Phase 8 sender/company information may influence priority.

Examples:

```text
Known recruiter
Known professor
Known critical service
Known personal contact
```

But sender identity must never automatically guarantee High/Critical.

Use it as supporting evidence.

---

# 27. USER CORRECTION COMPATIBILITY

Future user corrections must be able to override:

- category
- priority
- action-required state

Do not implement the full correction UI yet.

Design the data model so:

```text
classifier result
priority result
action result
user override
effective result
```

can coexist.

Never destroy the original deterministic result.

---

# 28. EFFECTIVE STATE

The application should conceptually distinguish:

```text
Raw/derived result
       ↓
User override
       ↓
Effective category/priority/action
```

This prevents future corrections from destroying traceability.

---

# 29. PERSISTENCE

Persist:

- category result
- priority result
- action-required result
- explanations/reasons
- versions
- source signals where appropriate

All data must remain account-scoped.

---

# 30. IDEMPOTENCY

Reprocessing the same message must update the existing derived state.

Do not create duplicates.

For example:

```text
Message 123
Priority result
```

should remain one logical current result.

---

# 31. STALE RESULT HANDLING

If any source data changes:

```text
message content
sender
classification
rules
```

the dependent priority/action result may become stale.

Design invalidation/recalculation appropriately.

Do not silently display stale derived intelligence indefinitely.

---

# 32. PROCESSING ORDER

Prefer a pipeline similar to:

```text
Normalized Email
      ↓
Classification
      ↓
Sender / Company
      ↓
Priority
      ↓
Action Required
```

Avoid circular dependencies.

For example:

must not occur.

---

# 33. RULE ENGINE SEPARATION

Keep separate rule sets for:

```text
Classification
Priority
Action Required
```

Do not create one giant rule engine where every concern is inseparable.

Shared signal infrastructure is fine.

---

# 34. SIGNAL SHARING

Signals can be shared.

For example:

```text
"Please submit your application by Friday"
```

may generate:

```text
Action-required signal
Deadline signal
Priority signal
```

The same underlying evidence can be reused.

Avoid parsing the same content repeatedly.

---

# 35. SIGNAL TRACEABILITY

Every result should retain enough information to explain:

```text
What signal?
Which rule?
Which version?
What result?
```

Do not store unnecessary copies of the entire email body.

---

# 36. CATEGORY + PRIORITY EXAMPLES

Build tests around cases such as:

### Job interview

```text
Category: Career
Priority: High
Action Required: Yes
```

### Security verification

```text
Category: Security
Priority: High/Critical depending on evidence
Action Required: Yes
```

### Newsletter

```text
Category: Newsletters
Priority: Low
Action Required: No
```

### Promotional sale

```text
Category: Promotions
Priority: Low
Action Required: No
```

### College deadline

```text
Category: Education
Priority: High
Action Required: Yes
```

### Order delivered

```text
Category: Receipts & Orders
Priority: Low/Medium
Action Required: No
```

---

# 37. CONFLICT TESTS

Test combinations such as:

```text
Security + Promotion
Career + Newsletter
Education + Notification
Order + Promotion
Security + Notification
Career + Important
```

Verify category, priority, and action remain independent.

---

# 38. AMBIGUOUS CASES

Test messages such as:

```text
"Your account update is ready."
```

Do not automatically mark:

```text
High
Action Required
```

without sufficient evidence.

Uncertainty must remain possible.

---

# 39. FALSE POSITIVE TESTING

Explicitly test false positives.

Examples:

```text
Newsletter mentioning "urgent"
```

should not automatically become Critical.

```text
Promotion containing "action required"
```

should not automatically become Action Required.

```text
Security newsletter
```

should not automatically become a critical security alert.

---

# 40. FALSE NEGATIVE TESTING

Test obvious signals.

For example:

```text
"Please verify your account by Friday."
```

should strongly indicate Action Required.

---

# 41. SECURITY PRIORITY

Security-related messages require special care.

Strong evidence may include:

- password reset requested
- suspicious login
- account compromise
- verification required
- security alert

Weak evidence:

- generic marketing using "secure"
- promotional "security sale"
- newsletter discussing cybersecurity

Do not treat words alone as sufficient.

---

# 42. FINANCIAL PRIORITY

Financial messages may be important.

But:

does not automatically mean:

Distinguish:

- receipt
- payment confirmation
- payment due
- failed payment
- suspicious payment
- refund

Phase 13/20 may later expand this logic.

---

# 43. CAREER PRIORITY

Career signals may include:

- interview
- application deadline
- offer- recruiter request
- assessment deadline

Routine career newsletters should not automatically be High.

---

# 44. EDUCATION PRIORITY

Education signals may include:

- exam
- assignment deadline
- fee deadline
- admission deadline
- registration deadline

Routine newsletters should remain lower priority.

---

# 45. USER ATTENTION MODEL

Priority exists to help the user decide:

> What should I look at first?

It is not intended to judge the intrinsic value of a person or organization.

Keep the model practical and conservative.

---

# 46. UI INTEGRATION

Integrate the results into the existing mail experience.

Possible indicators:

```text
HIGH
Action Required
```

Use subtle visual hierarchy.

Do not turn every email into a collection of badges.

---

# 47. PRIORITY VISUAL DESIGN

Follow `design.md`.

Use:

- typography
- iconography
- subtle status colors
- spacing
- clear labels

Do not rely exclusively on red/yellow/green colors.

---

# 48. ACTION REQUIRED UI

Where appropriate, display:

```text
Action required
```

with a concise reason.

Do not create an actionable button such as:

```text
Pay now
Submit now
```

yet.

Those belong to the Action Engine.

---

# 49. CATEGORY UI REGRESSION

Verify all Phase 7 category UI still works.

Do not accidentally change category semantics while adding priority.

---

# 50. COMPANY UI REGRESSION

Verify Phase 8 company/sender information remains correct.

For example:

```text
company
category
priority
```

must not conflict.

---

# 51. ADVANCED DASHBOARD IS NOT PHASE 9

Do not build the complete dashboard.

Do not build:

- analytics dashboard
- trends
- productivity metrics
- category charts
- company dashboard
- insights

Phase 11 and Phase 25 handle these areas.

---

# 52. ADVANCED SEARCH IS NOT PHASE 9

Do not build:

- search engine
- full-text index
- search filters
- saved searches

Phase 10 handles search.

---

# 53. RULE EDITOR IS NOT PHASE 9

Do not build the complete user rule editor.

Phase 12 handles:

- rules
- corrections
- user-defined organization

Phase 9 only establishes the engine foundation.

---

# 54. ACTION ENGINE IS NOT PHASE 9

Do not implement external actions.

No:

- Calendar event creation
- Task creation
- reply
- send
- archive
- delete
- Gmail modification

Phase 14 and later phases handle actions.

---

# 55. AI IS NOT PHASE 9

Do not add LLM classification.

Do not add cloud inference.

Do not add local AI models.

Phase 26 handles optional AI fallback.

---

# 56. PERFORMANCE

Classification, priority, and action detection must not block the UI.

For batches:

- process in background
- batch database writes
- avoid unnecessary recomputation
- avoid repeated parsing

---

# 57. LARGE MAILBOX

Test with:

- 100 emails
- 1,000 emails
- 10,000 emails where practical

Measure:

- processing time
- memory
- database writes
- UI responsiveness

Do not require all emails to be loaded into memory.

---

# 58. INCREMENTAL PROCESSING

When new email arrives:

only process:

- new messages
- changed messages
- invalidated derived state

where possible.

Do not classify the entire mailbox on every synchronization cycle.

---

# 59. BATCH PROCESSING

Use efficient batch processing.

Avoid:

```text
for every email:
    open transaction
    write
    close transaction
```

when a safer batched transaction can be used.

---

# 60. DATABASE INDEXING

Ensure efficient queries for:

- category
- priority
- action-required
- account
- message
- thread

Do not create indexes without actual access patterns.

---

# 61. ACCOUNT ISOLATION

Test:

```text
Account A:
High priority email

Account B:
Low priority email
```

Ensure querying Account A cannot return Account B results.

Test user overrides similarly.

---

# 62. OFFLINE OPERATION

Priority and Action Required should work from locally synchronized data.

After initial synchronization:

```text
Disable network
 ↓
Open Mail Organizer
 ↓
Browse emails
 ↓
View category
 ↓
View priority
 ↓
View action-required state
```

must work.

---

# 63. NO NETWORK DEPENDENCY

Do not require network access for:

- classification
- priority calculation
- action detection
- explanations

---

# 64. LOGGING

Never log:

- full email body
- sender email
- recipient
- full subject
- private URLs
- inferred sensitive information

Safe logs may include:

```text
classifier version
priority version
rule ID
generic error
processing duration
count
```

---

# 65. SECURITY REVIEW

Review:

- regex safety
- malformed input
- large content
- Unicode
- account isolation
- user override isolation
- no external processing
- no unsafe code execution
- no sensitive logging

---

# 66. TEST SUITE

At minimum create tests for:

### Priority

- Critical
- High
- Medium
- Low
- ambiguous
- conflicting signals
- recency if used
- sender/company influence

### Action Required

- explicit request
- deadline
- verification
- payment due
- interview
- no-action notification
- newsletter
- promotion
- ambiguous message

### Combined

- category + priority
- category + action
- priority + action
- all three together

---

# 67. ADVERSARIAL TESTS

Test:

```text
"URGENT SALE!!!"
```

should not automatically become Critical.

```text
"Action required: read our newsletter"
```

should not automatically become Action Required.

```text
"Security tips newsletter"
```

should not automatically become a security incident.

---

# 68. USER OVERRIDE TESTS

Prepare fixtures for:

```text
Classifier:
Promotions

User override:
Important

Effective:
Important
```

And:

```text
Action detector:
No

User override:
Yes

Effective:
Yes
```

Verify the original derived result remains available.

---

# 69. VERSIONING TESTS

Change the rule version in a test environment.

Verify:

- old results can be identified
- reprocessing produces the new version
- current result does not create duplicates
- history/traceability is preserved according to architecture

---

# 70. DEVICE VALIDATION

Use the full Android toolchain:

```text
./gradlew
ADB
device/emulator
logcat
screenshots
screen recording
```

Workflow:

```text
Build
 ↓
Install Mail Organizer
 ↓
Launch
 ↓
Open Mail
 ↓
Inspect categories
 ↓
Inspect priority
 ↓
Inspect action-required indicators
 ↓
Open message
 ↓
Inspect explanation
 ↓
Test offline
 ↓
Test account isolation
 ↓
Capture screenshots
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

---

# 71. SCREENSHOT QA

Inspect:

- category labels
- priority indicators
- action-required indicators
- hierarchy
- spacing
- dark mode
- accessibility
- long text
- small screens
- large fonts

Do not allow the mail list to become visually noisy.

---

# 72. SCREEN RECORDING

Use where useful to inspect:

- list scrolling
- opening messages
- category transitions
- priority display
- action-required display
- loading states

Look for:

- flicker
- layout shifts
- lag
- state inconsistencies

---

# 73. ACCESSIBILITY

Ensure:

- category is not represented only by color
- priority has accessible text
- Action Required is accessible to screen readers
- explanations are readable
- dynamic font sizes work
- touch targets remain accessible
- contrast remains sufficient

---

# 74. DARK MODE

Verify:

- category colors
- priority indicators
- action-required indicators
- explanation surfaces

in dark mode.

Follow centralized design tokens.

---

# 75. REGRESSION TEST PHASE 6

Verify:

- mail list
- thread view
- message detail
- HTML rendering
- links
- attachment metadata
- offline viewing
- navigation

still work.

---

# 76. REGRESSION TEST PHASE 7

Verify:

- classification categories
- classifier explanations
- classifier version
- category persistence
- account isolation

still work.

---

# 77. REGRESSION TEST PHASE 8

Verify:

- sender identity
- company identity
- domain resolution
- confidence
- source tracking
- account isolation

still work.

---

# 78. FINAL BUILD

Run the Mail Organizer project's:

- Gradle build
- unit tests
- Android tests
- lint/static checks where configured

Fix all issues introduced by Phase 9.

Do not alter unrelated projects to make the build pass.

---

# 79. GIT REVIEW

From the Mail Organizer Git root:

```text
git status
git diff
```

Verify:

- only Mail Organizer files changed
- no real email data
- no private email content
- no secrets
- no generated APKs
- no sibling project modifications

Do not commit unless explicitly instructed.

---

# 80. UPDATE `editor-rules.md`

Before completing Phase 9, update `editor-rules.md` with permanent rules discovered during implementation.

At minimum preserve:

- category/priority/action separation
- deterministic priority
- conservative Action Required detection
- explainability
- versioning
- user override compatibility
- account isolation
- local-only processing
- no external AI
- no destructive actions
- no automatic external actions

Do this yourself.

---

# 81. UPDATE `spec.md`

Only after verification:

- mark Phase 9 tasks complete
- record decisions
- record known limitations
- record deferred functionality

Do not mark Phase 10 or later complete.

---

# 82. UPDATE DEVELOPMENT STATUS

If `docs/development-status.md` exists, update it with:

- priority model
- Action Required model
- scoring/precedence
- explanation model
- persistence
- testing
- performance
- known limitations

Do not include private email data.

---

# 83. PHASE 9 ACCEPTANCE CRITERIA

Phase 9 is complete only when:

### Priority

- [ ] priority model exists
- [ ] Critical supported
- [ ] High supported
- [ ] Medium supported
- [ ] Low supported
- [ ] priority is deterministic
- [ ] priority is explainable
- [ ] priority is versioned
- [ ] priority does not simply mirror category
- [ ] Critical is conservative

### Action Required

- [ ] Action Required model exists
- [ ] Yes supported
- [ ] No supported
- [ ] Unknown/uncertain state supported where appropriate
- [ ] explicit requests detected
- [ ] deadlines can provide supporting evidence
- [ ] verification requests detected
- [ ] payment/action requests detected
- [ ] interview/application requests detected
- [ ] newsletters do not automatically become Action Required
- [ ] promotions do not automatically become Action Required
- [ ] false positives tested
- [ ] false negatives tested
- [ ] result is explainable

### Architecture

- [ ] category remains separate
- [ ] priority remains separate
- [ ] Action Required remains separate
- [ ] shared signals can be reused
- [ ] results are persisted
- [ ] results are account-scoped
- [ ] results are versioned
- [ ] processing is idempotent
- [ ] stale derived data can be recalculated
- [ ] user overrides can be supported later

### Privacy/Security

- [ ] all processing is local
- [ ] no external AI
- [ ] no sensitive logging
- [ ] malformed content handled
- [ ] large input handled
- [ ] regex safety reviewed
- [ ] account isolation verified

### Performance

- [ ] batch processing works
- [ ] incremental processing works
- [ ] large mailbox tested
- [ ] UI remains responsive
- [ ] database queries are efficient

### UI

- [ ] category remains visible
- [ ] priority is understandable
- [ ] Action Required is understandable
- [ ] explanation available where appropriate
- [ ] dark mode works
- [ ] accessibility works
- [ ] UI is not overloaded with badges

### Regression

- [ ] Phase 6 passes
- [ ] Phase 7 passes
- [ ] Phase 8 passes

### Device

- [ ] Gradle build succeeds
- [ ] APK installs
- [ ] application launches
- [ ] category works
- [ ] priority works
- [ ] Action Required works
- [ ] offline mode works
- [ ] screenshots inspected
- [ ] screen recording used where useful
- [ ] logcat inspected

### Workspace safety

- [ ] sibling projects untouched
- [ ] sibling Gradle files untouched
- [ ] sibling SDK configuration untouched
- [ ] unrelated apps untouched
- [ ] unrelated Git repositories untouched

---

# 84. FINAL PHASE REPORT

Provide:

## Phase 9 Status

`COMPLETE` / `PARTIAL` / `BLOCKED`

## Priority System

Explain:

- levels
- signals
- precedence/scoring
- versioning
- explanations

## Action Required System

Explain:

- signals
- confidence
- uncertainty
- false-positive handling
- explanations

## Category Relationship

Explain how category, priority, and Action Required remain independent.

## Persistence

Explain how results are stored and invalidated.

## User Override Readiness

Explain how future user corrections can override derived results without destroying source intelligence.

## Privacy

Confirm processing is local and no email data is sent externally.

## Testing

Report important test cases and results.

## Performance

Report large-mailbox and batch-processing validation.

## Device Validation

Report:

- device/emulator
- Android/API level
- build
- installation
- runtime validation
- offline test
- screenshots
- screen recording where used
- logcat
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

- Search
- Dashboard
- Rules/corrections
- Deadline extraction
- Meeting extraction
- Action engine
- Calendar
- Tasks
- Gmail write operations
- AI fallback
- advanced automation
- analytics

## Acceptance Criteria

Show every Phase 9 criterion and its status.

## Next Phase

The next incomplete phase is:

**Phase 10 — Search & Local Indexing**

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
IMPLEMENT PHASE 9 ONLY
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
VERIFY CATEGORY
        ↓
VERIFY PRIORITY
        ↓
VERIFY ACTION REQUIRED
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
REGRESSION TEST PHASES 6–8
        ↓