# Phase 28 — Full Testing & QA

## Mission

Perform a comprehensive **end-to-end testing and quality-assurance pass** over the entire Mail Organizer application.

This is not a phase for adding major new product functionality.

The objective is to determine whether everything implemented through Phase 27 works together as a reliable, secure, performant, privacy-preserving Android application.

The application must be treated as a real product, not merely a successful Gradle build.

The central principle is:

> **Nothing is considered complete because it compiles. It is complete only after the implemented behavior has been exercised, verified, and shown to remain stable under realistic and adversarial conditions.**

***

# 1. Mandatory Instruction-Folder Discovery

Before testing:

1. Identify the **Mail Organizer project root**.
2. Locate the instruction/documentation folder.
3. Read: 
   - `requirements.md`
   - `spec.md`
   - `design.md`
   - `editor-rules.md`
   - this Phase 28 prompt
   - `docs/development-status.md` if available
4. Inspect every phase status from Phase 0 through Phase 27.
5. Verify which phases are actually complete.
6. Inspect the real implementation and test suite.

Do not assume previous phases are correct simply because they were marked `[x]`.

If a previous phase is falsely marked complete, correct the status and document the issue.

***

# 2. Strict Sequential Execution

This session is **Phase 28 only**.

Do not implement:

- Phase 29 Production OAuth / Play Store Preparation
- Phase 30 Final Production Hardening & Release

Bug fixes are allowed and expected.

Small implementation changes required to correct verified defects are part of QA.

Do not use QA as an excuse for unrelated feature development or architectural rewrites.

Execution:

```text
Read requirements
↓
Read implementation contract
↓
Inspect phase history
↓
Build baseline
↓
Run automated tests
↓
Static analysis
↓
Database tests
↓
Integration tests
↓
Security tests
↓
Account isolation tests
↓
Offline tests
↓
Background tests
↓
UI tests
↓
Accessibility tests
↓
Performance tests
↓
Real-device tests
↓
Gmail/Calendar/Tasks tests
↓
Automation tests
↓
AI fallback tests
↓
Regression
↓
Fix defects
↓
Rebuild
↓
Reinstall
↓
Retest
↓
Final QA review
↓
Update rules/spec/status
↓
STOP
```

***

# 3. Permanent Multi-Project Isolation

Multiple Android projects may exist in the same parent directory.

First identify the Mail Organizer project root.

Never touch sibling projects.

Do not modify another project's:

- source
- Gradle files
- SDK/JDK configuration
- dependencies
- manifests
- resources
- tests
- assets
- signing
- Git repository
- generated files
- OAuth
- Google Cloud configuration

Never build, clean, install, or test another project.

***

# 4. Android Tooling Requirement

Use the full Android development/testing toolchain:

- `./gradlew`
- Gradle tests
- lint/static analysis
- ADB
- device/emulator
- install
- launch
- force-stop
- logcat
- dumpsys
- screencap
- screenrecord
- package manager
- database inspection
- WorkManager inspection
- network diagnostics
- performance/profiling tools where available

ADB commands must target only Mail Organizer's package.

***

# 5. QA Mindset

Do not optimize for:

> "All tests pass."

Optimize for:

> "A real user can safely use Mail Organizer without data corruption, account leakage, unsafe automation, broken synchronization, or misleading UI."

***

# 6. Quality Gates

Evaluate the application against:

1. Functional correctness
2. Integration correctness
3. Data integrity
4. Account isolation
5. Privacy
6. Security
7. Reliability
8. Offline behavior
9. Background behavior
10. Performance
11. Battery
12. Accessibility
13. UI/UX
14. Error handling
15. Automation safety
16. AI safety
17. Gmail safety
18. Calendar/Tasks safety
19. Upgrade/migration behavior
20. Release readiness

***

# 7. Defect Classification

Classify discovered defects.

### P0 — Critical

Examples:

- cross-account data leak
- credential/token exposure
- unauthorized Gmail mutation
- automatic email sending
- permanent data deletion
- security bypass
- catastrophic database corruption

Must be fixed before Phase 28 completion.

### P1 — High

Examples:

- sync corruption
- repeated destructive action
- broken account isolation
- major crash
- incorrect external action
- serious data loss

Must be fixed before completion.

### P2 — Medium

Examples:

- major broken workflow
- incorrect classification behavior
- significant UI issue
- repeated background failure

Should be fixed before completion where practical.

### P3 — Low

Examples:

- minor visual issue
- small wording problem
- non-critical UX polish

Document and fix where practical.

***

# 8. No Fake Test Results

Never:

- claim a test passed without running it
- claim device testing without a device
- claim Gmail testing without Gmail verification
- claim Calendar testing without Calendar verification
- claim Tasks testing without Tasks verification
- claim AI testing without actual implementation/testing
- mark phases complete based on assumptions

If something cannot be tested:

```text
NOT VERIFIED
```

with the reason.

***

# 9. Baseline Build

Before modifications:

```text
./gradlew ...
```

Record:

- build result
- test result
- lint result
- warnings
- build time
- generated APK/AAB size where useful

***

# 10. Clean Baseline

Only within Mail Organizer:

- clean where appropriate
- rebuild
- verify reproducibility

Do not clean sibling projects.

***

# 11. Static Analysis

Run applicable:

- lint
- compiler warnings
- static analysis
- dependency checks
- formatting checks
- Kotlin/Java inspections

Review meaningful warnings.

Do not blindly suppress warnings.

***

# 12. Dependency Audit

Inspect:

- dependencies
- versions
- transitive dependencies
- unnecessary libraries
- unused AI SDKs
- unnecessary network libraries
- analytics SDKs
- libraries with excessive permissions

Do not update dependencies simply for the sake of updates.

***

# 13. Secret Scan

Search Mail Organizer for:

- API keys
- OAuth secrets
- private keys
- tokens
- passwords
- test credentials
- hard-coded endpoints containing secrets

Also inspect:

- Git diff
- resources
- BuildConfig
- local configuration
- logs
- test fixtures

***

# 14. Test Environment

Define controlled environments for:

- unit testing
- integration testing
- synthetic Gmail data
- controlled Gmail account
- Calendar
- Tasks
- AI provider if applicable
- real device

Do not use real private mailbox data in committed fixtures.

***

# 15. Test Data

Build a comprehensive synthetic dataset.

Include:

- normal personal email
- career
- education
- receipts
- orders
- security
- notifications
- newsletters
- promotions
- low-value mail
- action-required mail
- urgent mail
- deadlines
- meetings
- interviews
- waiting-for-reply conversations
- malformed email
- HTML email
- multipart email
- Unicode
- attachments
- unsubscribe metadata
- malicious HTML
- prompt injection
- spoofed sender names
- multiple accounts

***

# 16. Database Integrity

Test:

- migrations
- constraints
- indexes
- foreign keys
- account scoping
- transactions
- duplicate prevention
- deletion
- corruption recovery
- rebuild operations

***

# 17. Database Migration Tests

Test upgrades from realistic prior schema versions.

Verify:

- no data loss
- no cross-account contamination
- indexes remain valid
- new fields have correct defaults

***

# 18. Fresh Install

Test:

```text
install
↓
first launch
↓
initial state
```

Verify:

- no crashes
- correct onboarding
- no fake data
- correct permissions
- correct empty states

***

# 19. Upgrade Install

Test upgrading from the previous build.

Verify:

- database preserved
- OAuth state preserved appropriately
- settings preserved
- automations preserved
- AI configuration preserved safely
- no duplicate records

***

# 20. App Restart

Test:

```text
launch
↓
force-stop
↓
launch
```

Verify state restoration.

***

# 21. Process Death

Kill the application process while:

- syncing
- indexing
- parsing
- running automation
- processing AI
- loading UI

Verify safe recovery.

***

# 22. Device Reboot

Reboot the test device.

Verify:

- scheduled work
- background sync
- automation
- notifications
- account state

recover correctly.

***

# 23. Account Authentication

Test each account:

- connect
- cancel
- deny
- grant
- reconnect
- disconnect
- token refresh
- permission revocation
- account removal

***

# 24. Multi-Account Testing

Use at least two controlled accounts.

Verify:

- account A data remains A
- account B data remains B
- unified inbox is intentional
- account switching is safe
- search is safe
- analytics are safe
- automations are safe
- Gmail writes are safe
- Calendar/Tasks routing is safe

***

# 25. Cross-Account Attack Test

Intentionally create conditions where:

- Account A message appears in unified view
- active UI account is B
- action is initiated

Verify target remains Account A.

***

# 26. OAuth Scope Review

Inspect actual requested scopes.

Verify only required scopes are requested.

Ensure:

- Gmail read-only behavior remains where applicable
- Gmail modify scope is requested only after the write feature requires it
- send scope is not requested unnecessarily
- Calendar scope is isolated
- Tasks scope is isolated

***

# 27. OAuth Security

Verify:

- tokens not logged
- tokens not in UI
- tokens not stored in ordinary plaintext storage
- token refresh safe
- logout/disconnect behavior safe

***

# 28. Gmail API Testing

Using a controlled Gmail account, test:

- authentication
- profile/account identity
- message retrieval
- thread retrieval
- pagination
- labels
- Gmail categories
- incremental sync
- history cursor
- cursor invalidation
- retry
- rate limiting
- cancellation

***

# 29. Gmail Sync Integrity

Verify:

```text
remote Gmail
↓
local database
```

matches expected state.

Test:

- duplicate messages
- changed messages
- new labels
- deleted/removed messages
- history gaps
- interrupted sync

***

# 30. Sync Recovery

Interrupt synchronization:

- network off
- process kill
- app force-stop
- device reboot

Verify it resumes safely.

***

# 31. Sync Concurrency

Attempt multiple syncs for the same account.

Verify only safe concurrency occurs.

No duplicate writes.

***

# 32. Sync Multi-Account

Sync Account A and B.

Verify:

- no account mixing
- independent state
- independent cursors
- independent failures

***

# 33. Email Parsing

Test:

- plain text
- HTML
- multipart
- nested multipart
- Unicode
- malformed MIME
- missing headers
- malformed addresses
- malformed dates
- quoted replies
- signatures
- attachments
- unsubscribe
- malicious HTML
- embedded scripts

***

# 34. HTML Security

Verify:

- JavaScript never executes
- unsafe HTML is sanitized
- external resources are controlled
- links require user interaction
- content cannot execute application commands

***

# 35. URL Safety

Test malicious URLs.

Verify:

- URLs are not automatically opened
- unsafe schemes are blocked where appropriate
- redirects do not bypass safety
- unsubscribe URLs are never automatically invoked

***

# 36. Classification Testing

Test all categories.

Verify:

- deterministic result
- confidence
- explanation
- rule ID/version
- account scope

***

# 37. Classification Adversarial Tests

Include emails where:

- subject contradicts body
- sender name is misleading
- malicious content attempts to change classification
- category signals conflict
- Gmail category conflicts with content

Verify deterministic precedence remains correct.

***

# 38. Company Intelligence

Test:

- exact domains
- subdomains
- consumer providers
- spoofed display names
- multiple senders
- user corrections
- multiple accounts

***

# 39. Priority

Test:

- Critical
- High
- Medium
- Low
- conflicting signals
- deadlines
- security
- user rules

Verify priority remains separate from category.

***

# 40. Action Required

Test:

- explicit request
- question
- deadline
- security issue
- transaction
- informational email
- newsletter
- automated notification

Verify conservative behavior.

***

# 41. Rules & Corrections

Test:

- create
- edit
- disable
- enable
- delete
- reorder
- preview
- conflict
- correction
- account-specific rules
- global rules if supported
- reprocessing

***

# 42. Temporal Intelligence

Test:

- dates
- times
- relative dates
- timezone
- IST
- historical dates
- future dates
- date ranges
- deadlines
- meetings
- cancellations
- rescheduling
- multiple temporal candidates

***

# 43. Action Engine

Test:

- candidate generation
- deduplication
- ranking
- lifecycle
- source traceability
- confidence
- account scope

Verify no action executes without required confirmation.

***

# 44. Calendar

Using a controlled account:

- connect
- disconnect
- select calendar
- create event proposal
- confirm
- reject
- duplicate attempt
- permission revoke
- network failure

Verify actual Calendar state.

***

# 45. Tasks

Using a controlled account:

- connect
- select task list
- create proposal
- confirm
- reject
- duplicate attempt
- permission revoke
- network failure

Verify actual Tasks state.

***

# 46. Integration Manager

Test:

- Gmail connected
- Calendar disconnected
- Tasks connected
- permission revoked
- offline
- unavailable
- authentication required

One integration failure must not break others.

***

# 47. Multi-Account Integrations

Verify Calendar/Tasks destination belongs to the intended account.

Never use active UI account as implicit destination.

***

# 48. Background Sync

Test:

- app closed
- device idle
- Wi-Fi
- mobile data
- no network
- reboot
- battery restrictions
- connectivity restored

***

# 49. Offline Mode

Disable network.

Verify:

- mail browsing
- search
- categories
- companies
- rules
- analytics
- conversation intelligence
- local actions

remain usable where expected.

***

# 50. Offline External Actions

Verify Gmail/Calendar/Tasks actions do not falsely report success offline.

***

# 51. Background Work

Inspect WorkManager/system state.

Verify:

- unique work
- correct constraints
- account scope
- retry policy
- cancellation
- no runaway work

***

# 52. Cleanup/Noise

Test:

- newsletter detection
- promotions
- notifications
- low-value
- cleanup candidates
- protected categories
- unsubscribe handling

Verify no automatic unsafe cleanup.

***

# 53. Conversation Intelligence

Test:

- awaiting user
- awaiting other party
- recently replied
- stale
- resolved
- unknown

Verify quoted text/signatures do not create false states.

***

# 54. Gmail Writes

Test every implemented write operation.

Examples:

- mark read
- mark unread
- star
- unstar
- label
- archive
- trash

Verify:

- confirmation
- account scope
- remote success
- local reconciliation
- retry safety
- duplicate prevention

***

# 55. Destructive Action Testing

Verify:

- trash requires appropriate confirmation
- permanent delete is unavailable unless explicitly required
- protected categories are handled conservatively
- bulk actions expose scope

***

# 56. Ambiguous Network Result

Simulate:

```text
request sent
↓
connection lost
↓
remote result unknown
```

Verify state becomes:

```text
UNKNOWN
```

and the application does not blindly retry a destructive operation.

***

# 57. Bulk Gmail Operations

Test:

- small batch
- large batch
- partial failure
- duplicate execution
- unexpected scope expansion

***

# 58. Automation Testing

Test:

- creation
- validation
- preview
- dry-run
- enable/disable
- new-email trigger
- scheduled trigger
- conditions
- actions
- account scope
- duplicate events
- recursion
- conflicts
- retry
- cancellation
- history

***

# 59. Automation Safety

Test that email content cannot:

- create automation
- enable automation
- authorize Gmail writes
- bypass confirmation
- modify account scope

***

# 60. Automation Failure

Force:

- permission failure
- network failure
- provider failure
- invalid configuration
- stale message
- account removal

Verify safe behavior.

***

# 61. AI Testing

If AI is actually implemented:

Test:

- disabled
- enabled
- provider unavailable
- invalid credentials
- rate limit
- malformed response
- prompt injection
- account isolation
- data minimization
- fallback
- cancellation
- cache invalidation

If AI is architecture-only:

Record:

> AI provider behavior not implemented; architecture reviewed only.

Do not fabricate AI tests.

***

# 62. AI Privacy

If remote AI exists, inspect network requests.

Verify no:

- OAuth tokens
- unrelated account data
- unnecessary email content
- application secrets

are transmitted.

***

# 63. Analytics Testing

Verify Phase 25 metrics:

- account scope
- unified scope
- date ranges
- category distribution
- priority
- Action Required
- sender/company
- newsletter/noise
- conversation
- deadline/meeting
- trends
- empty states

***

# 64. Analytics Integrity

Verify analytics do not:

- fabricate values
- mix accounts
- show stale deleted data
- treat unknown as false
- claim real-time data when offline

***

# 65. Search Testing

Test:

- sender
- domain
- company
- subject
- body
- category
- priority
- Action Required
- labels
- date
- multiple words
- Unicode
- phrase search if supported
- no results

***

# 66. Search Security

Test:

- SQL injection-like input
- pathological queries
- very long queries
- special characters
- Unicode
- empty query

Verify safe behavior.

***

# 67. Search Account Isolation

Test account-specific and unified search.

Verify every result carries source account.

***

# 68. Dashboard Testing

Verify Home uses real data.

Test:

- Action Required
- High Priority
- Recent Mail
- categories
- companies
- insights
- empty state
- loading
- error
- offline

***

# 69. Navigation Testing

Test every primary destination:

- Home
- Mail
- Categories
- Companies
- Actions

Secondary:

- Search
- Integrations
- Settings
- Privacy
- Accounts

Verify:

- back navigation
- state restoration
- deep links where implemented
- account context

***

# 70. UI State Matrix

Every major screen should have:

- loading
- populated
- empty
- error
- offline
- permission-required
- disconnected where applicable

***

# 71. Visual QA

Capture screenshots for:

- Home
- Mail
- thread
- email detail
- Categories
- category detail
- Companies
- company detail
- Actions
- Search
- results
- Insights
- Integrations
- Settings
- Privacy
- Accounts
- automation list
- automation builder
- AI settings if implemented

***

# 72. Design Compliance

Compare screenshots against `design.md`.

Verify:

- colors
- typography
- spacing
- radii
- elevation
- surfaces
- motion
- category colors
- dark mode

***

# 73. Dark Mode

Test every major screen.

Look for:

- unreadable text
- incorrect contrast
- white backgrounds
- incorrect chart colors
- broken icons
- clipped controls

***

# 74. Accessibility

Test:

- TalkBack
- large font
- touch target size
- focus order
- semantic labels
- content descriptions
- contrast
- keyboard navigation where applicable

***

# 75. Accessibility on Data Visualizations

Every chart must have a textual equivalent.

***

# 76. Localization Robustness

Even if only English is supported, test:

- long strings
- Unicode
- Malayalam names/text
- emoji
- RTL-sensitive content
- unusual sender names

Do not assume ASCII-only email data.

***

# 77. Responsive Layout

Test multiple screen sizes where available:

- small phone
- standard phone
- large phone
- tablet/emulator where practical

Verify no clipping or overflow.

***

# 78. Rotation

If orientation changes are supported:

- preserve state
- do not duplicate work
- do not lose forms
- do not restart destructive operations

If portrait-only by design, verify the restriction is intentional.

***

# 79. Keyboard

Test:

- search
- rule builder
- automation builder
- settings fields

Verify keyboard does not obscure important controls.

***

# 80. Performance Baseline

Measure:

- cold startup
- warm startup
- first meaningful UI
- inbox load
- thread load
- search
- dashboard
- analytics
- account switching

***

# 81. Large Dataset

Use synthetic datasets such as:

- 100 messages
- 1,000
- 10,000
- larger if practical

Measure:

- database queries
- memory
- UI responsiveness
- search
- classification
- analytics
- automation evaluation

***

# 82. Main Thread

Verify expensive operations do not run on the UI thread:

- parsing
- classification
- indexing
- analytics
- AI
- sync
- automation evaluation

***

# 83. Memory

Monitor:

- startup
- sync
- large inbox
- search
- analytics
- automation
- AI if implemented

Look for:

- leaks
- unbounded caches
- excessive image memory
- retained Activity/context references

***

# 84. Battery

Measure or inspect:

- background sync
- WorkManager
- automation
- indexing
- AI
- repeated database processing

No continuous loops.

***

# 85. Network Usage

Inspect network behavior.

Verify:

- Gmail API only when appropriate
- Calendar only when appropriate
- Tasks only when appropriate
- AI only when enabled/allowed
- no unexpected analytics telemetry
- no arbitrary external requests

***

# 86. Privacy Network Audit

Review outbound requests.

Classify:

- Gmail
- Calendar
- Tasks
- AI
- other

Any unexpected endpoint is a QA finding.

***

# 87. Logging Audit

Inspect logcat throughout testing.

Ensure logs do not contain:

- email bodies
- full subjects
- sender addresses unnecessarily
- OAuth tokens
- API keys
- raw AI prompts
- raw AI responses
- private Calendar/Tasks data

***

# 88. Crash Testing

Intentionally trigger safe failure conditions:

- network failure
- permission denial
- malformed data
- process death
- invalid database state
- provider failure
- malformed AI response

Verify graceful recovery.

***

# 89. Error Message Quality

Errors should explain:

- what happened
- what the user can do
- whether data is safe
- whether retry is possible

Avoid technical stack traces.

***

# 90. Recovery Testing

For each major failure:

```text
failure
↓
error
↓
retry/recovery
↓
successful operation
```

Verify the application returns to a correct state.

***

# 91. Data Consistency

After recovery, verify:

- local database
- Gmail state
- Calendar state
- Tasks state
- Action state
- automation state

remain consistent.

***

# 92. Duplicate Prevention

Across all systems test:

- sync
- classification
- indexing
- actions
- Calendar
- Tasks
- automation
- AI cache

for duplicate records/results.

***

# 93. Race Conditions

Test concurrent:

- sync + UI
- sync + search
- sync + automation
- account switch + sync
- account removal + background work
- Gmail write + sync
- rule change + processing

***

# 94. Account Removal Race

Remove an account while:

- sync running
- automation queued
- Calendar action pending
- Tasks action pending
- AI processing

Verify all account-scoped work is safely cancelled/invalidated.

***

# 95. Permission Revocation Race

Revoke permission while an operation is running.

Verify:

- no unauthorized retry
- correct error state
- account remains safe

***

# 96. Connectivity Race

Toggle network during:

- sync
- Gmail write
- Calendar action
- Tasks action
- AI request
- automation

Verify correct state transitions.

***

# 97. Time-Based Tests

Test:

- midnight boundary
- timezone
- daylight-saving-aware code where relevant
- scheduled automation
- deadline transitions
- stale conversation thresholds

***

# 98. Clock Changes

If practical, test device clock changes.

Verify scheduled tasks and temporal intelligence do not produce catastrophic behavior.

***

# 99. Database Stress

Test:

- many messages
- many senders
- many companies
- many rules
- many automations
- many execution-history entries

***

# 100. Rule Stress

Test dozens/hundreds of structured rules if architecture permits.

Verify:

- deterministic ordering
- acceptable performance
- no accidental loops
- no account leakage

***

# 101. Automation Stress

Test many enabled automations.

Verify:

- event filtering
- bounded evaluation
- no runaway execution
- no recursion
- no excessive battery use

***

# 102. AI Stress

Only if AI exists.

Test:

- repeated requests
- cancellation
- large context
- provider rate limit
- network loss
- model unavailable

***

# 103. Search Stress

Test large queries and datasets.

Verify:

- no crashes
- no SQL injection
- bounded memory
- acceptable latency

***

# 104. Background Stress

Leave device idle and inspect:

- WorkManager
- alarms
- services
- jobs
- network activity

There must be no unexpected persistent work.

***

# 105. Battery Saver

Test under battery-saving restrictions where practical.

Verify core mail remains functional.

***

# 106. Doze / Background Restrictions

Test background sync/automation under Android background restrictions.

Verify graceful delay rather than incorrect success.

***

# 107. App Standby

Where practical, verify deferred work resumes safely.

***

# 108. Notification Testing

If notifications exist:

- permission denied
- permission granted
- lock screen
- dark mode
- multiple notifications
- notification actions
- account scope

***

# 109. Notification Privacy

Verify sensitive email content is not exposed unnecessarily.

***

# 110. Deep-Link Testing

Test all supported deep links.

Verify:

- authentication
- account context
- target message
- no unauthorized action
- safe fallback when source is missing

***

# 111. Security Threat Model Review

Review threats involving:

- malicious emails
- prompt injection
- spoofed senders
- account mixing
- token theft
- deep links
- exported components
- notifications
- local database
- backups
- logs
- network
- AI providers
- automation

***

# 112. Backup Review

Inspect Android backup behavior.

Determine whether sensitive local data should be excluded or protected.

Do not accidentally expose:

- tokens
- database contents
- automation secrets
- AI credentials

through backups.

***

# 113. App Data Reset

Test Android:

> Clear storage

Verify clean recovery.

***

# 114. Reinstall

Test uninstall/reinstall behavior.

Verify expected:

- local data loss/preservation semantics
- OAuth reconnection
- settings reset
- no stale state

***

# 115. Notification/Work Cleanup

After uninstall/reinstall, verify stale scheduled work does not affect the new installation.

***

# 116. Permissions Reset

Reset permissions and verify correct recovery.

***

# 117. Storage Pressure

Where practical, test low-storage behavior.

Verify:

- database failure handled
- downloads not required for core mail
- clear error
- no corruption

***

# 118. Network Security

Verify:

- HTTPS
- certificate validation
- no insecure HTTP for sensitive communication
- no unexpected cleartext traffic

***

# 119. WebView Security

If WebView is used:

- JavaScript disabled unless strictly required
- no unsafe bridges
- no arbitrary file access
- safe URL handling

***

# 120. External Links

Verify external links require user interaction.

***

# 121. Attachment Safety

Verify attachments:

- are metadata-first
- are not automatically executed
- are not automatically downloaded unless explicitly designed
- cannot escape storage sandbox

***

# 122. Email Content Safety

Verify email HTML/content cannot:

- execute code
- invoke internal actions
- manipulate app navigation
- trigger automation

***

# 123. Gmail Write Authorization

Verify only explicit application actions can reach Gmail write adapters.

Email content cannot call them.

***

# 124. Calendar Authorization

Verify email content cannot directly create Calendar events.

***

# 125. Tasks Authorization

Verify email content cannot directly create Tasks.

***

# 126. Automation Authorization

Verify email content cannot:

- create
- enable
- modify
- execute

automations.

***

# 127. AI Authorization

Verify AI cannot directly execute application actions.

***

# 128. Analytics Privacy

Verify analytics remain local unless explicitly designed otherwise.

***

# 129. Search Privacy

Verify search history is not unexpectedly transmitted.

***

# 130. Privacy Center Accuracy

Read every privacy/security claim in the UI.

Verify each claim matches actual behavior.

Do not allow:

> "Your data never leaves the device"

if remote Gmail/AI/API calls are intentionally made.

Use precise wording such as:

> "Mail Organizer processes organization data locally by default. Data is transmitted to Google when using Gmail services and to an AI provider only when AI assistance is enabled."

Only use wording that matches actual implementation.

***

# 131. Performance Regression

Compare against Phase 24 baseline.

Flag regressions in:

- startup
- sync
- inbox rendering
- search
- account switching
- background work
- memory
- battery

***

# 132. UI Regression

Compare Phase 11 design expectations.

Look for:

- spacing regressions
- typography regressions
- color regressions
- navigation regressions
- dark-mode regressions
- accessibility regressions

***

# 133. Functional Regression

Run workflows from all prior phases.

At minimum:

```text
Install
↓
OAuth
↓
Sync
↓
Parse
↓
Classify
↓
Company
↓
Priority
↓
Action Required
↓
Search
↓
Dashboard
↓
Rules
↓
Temporal extraction
↓
Action Engine
↓
Calendar
↓
Tasks
↓
Integrations
↓
Multi-account
↓
Background sync
↓
Cleanup
↓
Conversation intelligence
↓
Gmail writes
↓
Privacy
↓
Analytics
↓
AI fallback if implemented
↓
Automation
```

***

# 134. End-to-End User Scenario 1

Simulate:

> User connects Gmail → syncs → sees Action Required → opens email → creates Calendar event → returns to dashboard.

Verify entire workflow.

***

# 135. End-to-End User Scenario 2

Simulate:

> User receives a career email → classifier identifies Career → priority becomes High → deadline extracted → Action Engine proposes follow-up → user confirms Calendar/Task action.

Verify source traceability.

***

# 136. End-to-End User Scenario 3

Simulate:

> User receives newsletter → classified as Newsletter → cleanup candidate appears → user reviews → explicitly confirms Gmail mutation.

Verify no automatic deletion/unsubscribe.

***

# 137. End-to-End User Scenario 4

Simulate:

> Two Gmail accounts are connected → unified inbox displayed → user opens Account A message while Account B is active → performs Gmail action.

Verify action targets Account A.

***

# 138. End-to-End User Scenario 5

Simulate:

> User enables an automation for Career mail → new Career email arrives → automation triggers → safe local action executes → execution history records it.

Verify no duplicate execution.

***

# 139. End-to-End User Scenario 6

Simulate:

> Automation attempts external action while Calendar permission is revoked.

Verify safe failure and no false success.

***

# 140. End-to-End User Scenario 7

If AI is implemented:

> Deterministic classifier is uncertain → user has opted into AI → selected email context is sent → structured result returned → validated → user sees AI-assisted result.

Verify privacy boundary.

***

# 141. End-to-End Offline Scenario

Simulate:

> Device loses network → user opens Mail Organizer → searches → reviews categories → checks analytics → edits local rules → network returns → sync resumes.

Verify no unnecessary blocking.

***

# 142. End-to-End Failure Scenario

Simulate:

> Sync starts → network disappears → process dies → device restarts → network returns.

Verify:

- no corruption
- no duplicate messages
- safe recovery
- correct sync cursor

***

# 143. End-to-End Account Removal

Simulate:

> Account has messages, rules, analytics, automations, and pending work → user removes account.

Verify complete account-scoped cleanup.

***

# 144. End-to-End Upgrade

Simulate:

> Existing user upgrades app version.

Verify:

- data
- accounts
- rules
- automations
- analytics
- AI settings
- migrations

remain correct.

***

# 145. Release-Like Build

Create a release-like build.

Verify:

- no debug-only behavior leaks
- no debug logging
- no test providers enabled
- no fake data
- no development endpoints
- no secrets

***

# 146. ProGuard/R8

If enabled:

- build release
- launch
- run critical workflows
- verify serialization/reflection
- verify OAuth
- verify Room
- verify WorkManager
- verify integrations

***

# 147. Minification

Check:

- startup
- navigation
- database
- API clients
- background workers
- automation
- AI provider if applicable

***

# 148. APK/AAB Review

Inspect:

- package name
- version
- permissions
- embedded secrets
- unnecessary assets
- unnecessary SDKs
- debug components

***

# 149. Manifest Review

Inspect:

- exported activities
- services
- receivers
- providers
- permissions
- deep links
- backup configuration
- network security

***

# 150. Permission Review

Every permission must have a documented reason.

Avoid:

- unnecessary storage permission
- contacts
- location
- microphone
- camera
- accessibility
- notification access
- broad device permissions

unless requirements explicitly need them.

***

# 151. No AccessibilityService

Do not introduce AccessibilityService as a Gmail mechanism.

***

# 152. No Scraping

Verify Gmail functionality uses official APIs.

***

# 153. No Credential Scraping

Verify no browser-cookie/password authentication exists.

***

# 154. No Hidden Backend

Verify no unexpected Mail Organizer backend exists.

***

# 155. Third-Party SDK Review

Inspect SDKs for:

- analytics
- advertising
- tracking
- unnecessary network access

Do not allow unrelated tracking in a privacy-first app.

***

# 156. Crash/ANR Review

Inspect logs for:

- crashes
- ANRs
- StrictMode issues
- database exceptions
- network exceptions
- WorkManager failures

***

# 157. UI Jank

Inspect:

- scrolling
- navigation
- search
- charts
- account switching
- thread rendering

***

# 158. Startup

Cold-start the application repeatedly.

Verify startup remains reasonable after:

- large database
- multiple accounts
- many rules
- many automations
- AI configuration

***

# 159. Database Size

Inspect database growth.

Ensure:

- indexes are appropriate
- derived data is bounded
- analytics history is bounded
- automation history is bounded
- AI cache is bounded

***

# 160. Storage Cleanup

Verify stale caches/derived data can be removed or rebuilt.

***

# 161. Rebuildability

Test rebuild/recompute mechanisms for:

- search index
- classification
- analytics
- AI derived data where applicable

***

# 162. Data Recovery

Where practical, simulate corrupted derived state.

Verify core source data remains usable.

***

# 163. Regression After Bug Fixes

Every bug fix must be followed by:

```text
fix
↓
targeted test
↓
related regression
↓
full relevant workflow
```

Do not fix one workflow while breaking another.

***

# 164. Test Documentation

Create/update:

```text
docs/qa/
```

or the existing QA documentation location.

Record:

- test environment
- build version
- device
- Android version
- tests executed
- pass/fail
- known limitations
- unresolved defects

Use the project's existing documentation structure if one exists.

***

# 165. Test Matrix

Create a test matrix covering:

|Area|Unit|Integration|UI|Device|External|
|---|---|---|---|---|---|
|OAuth|✓|✓|✓|✓|Google|
|Gmail sync|✓|✓|✓|✓|Gmail|
|Parsing|✓|✓|✓|✓|—|
|Classification|✓|✓|✓|✓|—|
|Search|✓|✓|✓|✓|—|
|Calendar|✓|✓|✓|✓|Google|
|Tasks|✓|✓|✓|✓|Google|
|Multi-account|✓|✓|✓|✓|Google|
|Automation|✓|✓|✓|✓|provider|
|AI|✓|✓|✓|✓|provider|
|Analytics|✓|✓|✓|✓|—|
|Privacy|✓|✓|✓|✓|network|
|Performance|—|✓|✓|✓|—|

Adapt the matrix to actual implemented functionality.

***

# 166. Test Evidence

Where practical retain:

- screenshots
- screen recordings
- test output
- logs
- performance measurements

Do not commit private user data.

***

# 167. Screenshot Privacy

Ensure test screenshots contain only:

- synthetic data
- controlled test data
- non-sensitive content

Do not commit personal mailbox screenshots.

***

# 168. Screen Recording Privacy

Same rule:

- controlled account
- synthetic/private test content
- no credentials
- no personal data

***

# 169. Test Credential Safety

Never commit:

- test passwords
- API keys
- OAuth refresh tokens
- Calendar credentials
- Tasks credentials

***

# 170. Test Account Cleanup

After external integration testing:

- remove test Calendar events
- remove test Tasks
- clean test Gmail mutations where appropriate
- revoke temporary permissions if appropriate

Do not delete unrelated user data.

***

# 171. Defect Tracking

For every unresolved defect record:

- ID
- severity
- component
- reproduction
- expected
- actual
- impact
- workaround
- status

***

# 172. P0/P1 Policy

No P0 or P1 defect may remain silently hidden.

Phase 28 cannot be marked complete while critical/high defects remain unresolved unless a genuine external blocker is documented.

***

# 173. P2/P3 Policy

Document remaining P2/P3 issues.

Fix those that materially affect product reliability.

***

# 174. Test Flakiness

Investigate flaky tests.

Do not simply rerun until they pass.

***

# 175. Deterministic Tests

Prefer deterministic test data and controlled clocks/network conditions.

***

# 176. Network Mocking

For unit/integration tests, use controlled network mocks where practical.

Do not depend on live APIs for every test.

***

# 177. External API Smoke Tests

Use a small number of controlled live tests for:

- Gmail
- Calendar
- Tasks
- AI if applicable

***

# 178. External API Failure Tests

Simulate:

- 401
- 403
- 404
- rate limit
- timeout
- malformed response
- server failure

Verify safe behavior.

***

# 179. OAuth Failure Tests

Simulate:

- user cancellation
- denial
- revoked permission
- expired credential
- account removed

***

# 180. Background Failure Tests

Simulate:

- worker cancellation
- process death
- network unavailable
- constraints unmet
- app force-stop

***

# 181. Data Race Tests

Where practical use concurrent test execution.

Look for:

- lost updates
- stale state
- duplicate records
- transaction errors

***

# 182. Security Regression

Re-run all major security controls from Phases 3, 5, 22, 23, 26, and 27.

***

# 183. Privacy Regression

Verify:

- local-first behavior
- remote data boundaries
- logging
- deletion
- account isolation
- AI opt-in
- analytics privacy

***

# 184. Automation Regression

Verify automation cannot become more permissive due to unrelated changes.

***

# 185. AI Regression

Verify AI cannot become mandatory due to a classification/parser failure.

***

# 186. Gmail Regression

Verify Gmail remains source of truth.

***

# 187. Integration Regression

Verify Calendar/Tasks failures do not break Gmail.

***

# 188. Performance Regression

Verify Phase 24 optimizations remain intact.

***

# 189. Design Regression

Verify `design.md` remains the source of truth.

***

# 190. Requirements Traceability

Map each major requirement from `requirements.md` to:

- implementation
- test
- verification status

Anything without verification should be identified.

***

# 191. Specification Traceability

Map each Phase 0–27 acceptance area to test coverage.

***

# 192. Coverage

Review test coverage.

Do not chase a meaningless percentage.

Focus on:

- security-critical code
- account isolation
- synchronization
- data parsing
- action execution
- automation
- external integrations
- privacy boundaries

***

# 193. Mutation Testing

If practical, introduce small controlled mutations to critical logic and verify tests detect them.

Do not require this if tooling is unavailable.

***

# 194. Static Security Review

Inspect for:

- unsafe WebView
- exported components
- insecure storage
- hardcoded secrets
- SQL injection
- path traversal
- unsafe intents
- unsafe deserialization
- insecure networking

***

# 195. Database Security

Verify account-scoped queries cannot return another account's data.

***

# 196. Search Security

Verify user query cannot escape parameterized query boundaries.

***

# 197. File Security

Verify attachment/file handling cannot escape app storage.

***

# 198. Intent Security

Verify external intents cannot directly execute privileged actions.

***

# 199. Notification Security

Verify notification actions cannot bypass authorization.

***

# 200. Automation Security

Verify external events cannot execute arbitrary automations.

***

# 201. AI Security

Verify model output cannot directly execute application actions.

***

# 202. Gmail Write Security

Verify only authorized code paths can modify Gmail.

***

# 203. Calendar Security

Verify only authorized code paths can create/modify Calendar events.

***

# 204. Tasks Security

Verify only authorized code paths can create/modify Tasks.

***

# 205. Privacy Deletion

Test account deletion comprehensively.

Verify removal of:

- messages
- sender/company associations
- classifications
- rules
- corrections
- action state
- temporal state
- conversation state
- analytics
- AI state
- automation state
- integration state
- cached search/index data

Do not delete data belonging to other accounts.

***

# 206. Logout

Verify logout/disconnect behavior.

Do not leave credentials accessible.

***

# 207. Reauthentication

Verify reauthentication restores correct account state without duplicate accounts.

***

# 208. Account Identity

Verify stable internal account IDs are not confused with email addresses.

***

# 209. Unified View

Verify unified view never becomes an unintended data source.

***

# 210. Source Traceability

For every important derived result verify:

```text
source message/thread
↓
derived intelligence
↓
action/automation/insight
```

can be traced.

***

# 211. Explainability

Test user-facing explanations:

- category
- priority
- Action Required
- company
- temporal extraction
- action candidate
- automation execution
- AI suggestion

***

# 212. No False Claims

Audit UI for claims such as:

- "synced"
- "saved"
- "sent"
- "created"
- "deleted"
- "processed"

Verify they correspond to real state.

***

# 213. Unknown State

Where remote outcome is unknown, UI must not say:

> Success

***

# 214. Partial Success

Where bulk operations partially succeed:

show:

- success count
- failure count
- retry/review path

***

# 215. User Confirmation

Verify every high-risk action requires the intended confirmation.

***

# 216. Confirmation Bypass

Try bypassing confirmation through:

- back navigation
- deep link
- notification
- automation
- AI
- background work
- account switch

All must remain protected.

***

# 217. UI Race Testing

Rapidly tap:

- confirm
- delete
- archive
- connect
- disconnect
- run automation

Verify double execution does not occur.

***

# 218. Double-Tap Protection

Especially test:

- Gmail writes
- Calendar creation
- Tasks creation
- automation manual run

***

# 219. Navigation Race

Rapidly switch accounts/screens during:

- loading
- sync
- action execution

Verify stale state does not appear.

***

# 220. Memory Pressure

Use Android memory pressure where practical.

Verify graceful behavior.

***

# 221. Large Attachment Metadata

Test emails with large attachment metadata/counts.

Do not automatically download large files.

***

# 222. Large Email Body

Test very large emails.

Verify:

- parser remains responsive
- database remains stable
- viewer handles content safely
- search/index does not explode memory

***

# 223. Malformed Data

Inject malformed local records where practical.

Verify defensive handling.

***

# 224. Clock/Date Edge Cases

Test:

- leap year
- month boundaries
- year boundaries
- timezone offsets
- midnight
- relative dates
- invalid dates

***

# 225. Unicode

Test:

- Malayalam
- emoji
- accented names
- CJK
- unusual punctuation
- right-to-left text where practical

***

# 226. Accessibility Content

Verify email content itself cannot create inaccessible or unsafe UI.

***

# 227. Final Device Matrix

Test on available combinations.

At minimum document:

- physical Android device
- emulator if available
- Android version
- screen size
- API level

If only one environment is available, document that limitation.

***

# 228. Final QA Run

After all fixes:

1. Clean/build where appropriate.
2. Run complete automated suite.
3. Install fresh build.
4. Launch.
5. Connect controlled account.
6. Sync.
7. Verify primary flows.
8. Test offline.
9. Test background.
10. Test multi-account.
11. Test Gmail writes.
12. Test Calendar/Tasks.
13. Test automation.
14. Test AI if implemented.
15. Test analytics.
16. Test privacy/security.
17. Capture final screenshots.
18. Review logs.
19. Review Git diff.

***

# 229. Final Regression

Run the full user journey again after the final fix.

Do not assume targeted fixes are sufficient.

***

# 230. Final Build

Produce the most representative release-like build available at this phase.

Verify:

- package
- version
- signing configuration
- permissions
- startup
- critical workflows

Do not perform Play Store submission yet.

***

# 231. QA Report

Create/update a QA report containing:

```text
Build
Date
Device
Android version

Automated tests
Integration tests
UI tests
Security tests
Performance tests
Accessibility tests
External API tests

Passed
Failed
Not verified

P0
P1
P2
P3

Known limitations
Recommended follow-up
```

Use the existing documentation structure where available.

***

# 232. Development Status

Update:

```text
spec.md
```

and:

```text
docs/development-status.md
```

after actual verification.

Phase 28 must not be marked complete if critical unresolved issues remain.

***

# 233. Editor Rules Update

Update `editor-rules.md` only with permanent QA lessons discovered during this phase.

Potential additions:

- never treat build success as completion
- critical account/security paths require explicit tests
- external actions require live controlled verification
- unknown remote outcomes must remain UNKNOWN
- regression testing is required after bug fixes
- screenshots/device testing are required for major UI changes
- sensitive test data must never be committed
- no test may depend unnecessarily on production/private data
- sibling projects must remain untouched during QA

Do not regenerate the entire file.

***

# 234. Git Review

Before completion:

```text
git status
git diff
```

Verify:

- only Mail Organizer changed
- no sibling project changes
- no generated junk
- no test credentials
- no private data
- no screenshots containing private email
- no secrets
- no unrelated refactors

***

# 235. Final Acceptance Criteria

Phase 28 is complete only if:

- Full automated test suite passes.
- Relevant integration tests pass.
- UI tests pass.
- Device testing passes.
- Fresh-install testing passes.
- Upgrade testing passes.
- Database migration testing passes.
- Database integrity passes.
- Multi-account isolation passes.
- OAuth testing passes.
- Gmail sync testing passes.
- Parsing tests pass.
- Classification tests pass.
- Company intelligence tests pass.
- Priority tests pass.
- Action Required tests pass.
- Search tests pass.
- Dashboard tests pass.
- Rules/corrections tests pass.
- Temporal intelligence tests pass.
- Action Engine tests pass.
- Calendar tests pass where implemented.
- Tasks tests pass where implemented.
- Integration Manager tests pass.
- Background sync tests pass.
- Offline tests pass.
- Cleanup/noise tests pass.
- Conversation intelligence tests pass.
- Gmail write tests pass.
- Analytics tests pass.
- Automation tests pass.
- AI tests pass if AI is implemented.
- Security regression passes.
- Privacy regression passes.
- Network audit passes.
- Secret audit passes.
- Logging audit passes.
- Accessibility tests pass.
- Dark mode tests pass.
- Responsive UI tests pass.
- Performance regression passes.
- Battery/background behavior is acceptable.
- Large dataset testing passes.
- Memory testing passes.
- Race-condition testing passes where practical.
- Error recovery passes.
- Duplicate prevention passes.
- Account removal passes.
- Permission revocation passes.
- Process death recovery passes.
- Device reboot recovery passes.
- Notification testing passes where applicable.
- Deep-link testing passes where applicable.
- Backup/data-protection behavior is reviewed.
- Release-like build works.
- Manifest reviewed.
- Permissions reviewed.
- Dependencies reviewed.
- No AccessibilityService is being used as a Gmail mechanism.
- No Gmail scraping exists.
- No credential scraping exists.
- No hidden backend exists unless explicitly required.
- No unauthorized telemetry exists.
- QA documentation exists.
- All P0 defects are resolved.
- All P1 defects are resolved.
- Remaining P2/P3 issues are documented.
- Requirements traceability reviewed.
- Phase traceability reviewed.
- `editor-rules.md` updated.
- `spec.md` updated.
- Development status updated.
- Git diff reviewed.
- Multi-project isolation verified.

***

# 236. Completion Protocol

When finished:

1. Review all Phase 0–27 requirements.
2. Build the project with the Gradle wrapper.
3. Run the full automated test suite.
4. Run lint/static checks.
5. Run database/migration tests.
6. Run integration tests.
7. Run UI tests.
8. Install on the Android test device/emulator.
9. Run the full primary user journey.
10. Test OAuth.
11. Test Gmail synchronization.
12. Test parsing.
13. Test classification.
14. Test rules/corrections.
15. Test search.
16. Test dashboard.
17. Test temporal intelligence.
18. Test actions.
19. Test Calendar.
20. Test Tasks.
21. Test multi-account.
22. Test offline behavior.
23. Test background work.
24. Test Gmail writes.
25. Test cleanup.
26. Test conversation intelligence.
27. Test analytics.
28. Test automation.
29. Test AI if implemented.
30. Test privacy/security.
31. Test accessibility.
32. Test performance.
33. Test large datasets.
34. Test process death.
35. Test reboot.
36. Test account removal.
37. Test permission revocation.
38. Test upgrade.
39. Inspect network.
40. Inspect logcat.
41. Inspect database.
42. Capture screenshots.
43. Review screen recordings where useful.
44. Fix all discovered P0/P1 defects.
45. Fix important P2 defects.
46. Rebuild.
47. Reinstall.
48. Repeat the relevant tests.
49. Run final end-to-end regression.
50. Review Git changes.
51. Verify sibling projects were untouched.
52. Update QA documentation.
53. Update `editor-rules.md`.
54. Update `spec.md`.
55. Update development status.
56. Mark Phase 28 complete only after genuine verification.
57. Report:

- test environment
- tests executed
- pass/fail status
- fixed defects
- remaining defects
- performance findings
- security findings
- privacy findings
- known limitations

58. **STOP.**

Do not automatically continue to Phase 29.

***

# 237. Next Phase

The next phase after successful completion is:

**Phase 29 — Production OAuth / Play Store Preparation**

Do not execute it during this session.