# PHASE 2 — LOCAL DATA ARCHITECTURE

You are now executing:

**Phase 2 — Local Data Architecture**

This phase must be executed **only after Phase 1 has been completed and verified**.

Do not execute Phase 3 or any later phase automatically.

The project uses a persistent, sequential phase-development model.

You must complete:

```text
Phase 0
    ↓
Phase 1
    ↓
Phase 2  ← YOU ARE HERE
    ↓
Phase 3
    ↓
...
    ↓
Final Release
```

Each phase is an independent implementation/verification boundary.

Do not analyze all future phases and then implement them in one shot.

Do not create a giant implementation plan covering the entire product and execute it automatically.

Instead:

1. Determine the current phase.
2. Read the requirements relevant to that phase.
3. Execute that phase.
4. Verify it completely.
5. Update the project status.
6. Stop.
7. On the next execution/session, inspect the phase library again and continue with the next incomplete phase.

---

# 1. FIRST: DISCOVER THE PROJECT INSTRUCTION FOLDER

Before touching the codebase, locate the folder containing the Mail Organizer project `.md` files.

The folder may contain documents such as:

```text
requirements.md
spec.md
design.md
editor-rules.md
phase-0.md
phase-1.md
phase-2.md
...
```

The exact filenames may differ.

Do not assume the filenames.

Inspect the folder.

Read all relevant Mail Organizer instruction files before implementation.

The editor must treat these documents as the persistent project contract.

If the folder contains future phase prompts, you may READ them for context when necessary, but **do not execute future phases**.

The existence of Phase 3, Phase 4, etc. does not authorize their implementation.

---

# 2. UPDATE THE PERSISTENT EDITOR RULES

Before continuing, update `editor-rules.md` to include the following permanent rules if they are not already present.

Do not ask the user to manually modify the file.

You are responsible for maintaining the project rules.

## Sequential Phase Execution

The editor must work strictly one phase at a time.

The editor must:

1. discover the project instruction folder
2. read the master documents
3. determine the current phase
4. execute only that phase
5. verify that phase
6. update the phase status
7. stop at the phase boundary

The editor must NOT:

- implement multiple phases in one session unless explicitly instructed
- silently skip phases
- execute future phases because their requirements are visible
- combine several phase prompts into one implementation
- mark future phases complete
- create the entire application in one pass

When the current phase is complete, the editor must stop and wait for the next explicit continuation.

On the next continuation, it must inspect the project documentation again and identify the next incomplete phase.

---

# 3. MULTI-PROJECT WORKSPACE ISOLATION — PERMANENT RULE

This is a critical project rule.

The development directory may contain multiple unrelated Android applications.

For example:

```text
workspace/
├── MailOrganizer/
├── AnotherAndroidApp/
├── CollegeProject/
├── ExperimentApp/
├── ClientProject/
└── ...
```

The editor MUST NOT assume that the entire workspace belongs to Mail Organizer.

Before performing any operation, identify the **Mail Organizer project root**.

Once identified, establish a strict project boundary.

Only modify files belonging to the Mail Organizer project.

---

# 4. NEVER TOUCH SIBLING PROJECTS

If multiple projects exist in the same parent directory, do not modify them.

Do not:

- edit their source files
- edit their Gradle files
- edit their `settings.gradle`
- edit their `build.gradle`
- edit their `gradle.properties`
- modify their SDK configuration
- upgrade their dependencies
- modify their AndroidManifest
- modify their signing configuration
- modify their local properties
- modify their environment variables
- modify their `.gitignore`
- modify their IDE configuration
- modify their tests
- modify their resources
- modify their assets
- modify their generated files
- modify their Git configuration
- install their APKs
- uninstall their APKs
- rebuild their projects
- run destructive Gradle operations against them

The existence of another Android project in the workspace is irrelevant to Mail Organizer.

---

# 5. GRADLE PROJECT ISOLATION

This is especially important.

Never execute a Gradle command from a parent directory that could accidentally operate on multiple projects.

Always identify the Mail Organizer Gradle root first.

For example:

```text
cd <MAIL_ORGANIZER_PROJECT_ROOT>
./gradlew ...
```

Use the Mail Organizer project's own:

```text
gradlew
gradlew.bat
gradle/
settings.gradle
settings.gradle.kts
```

Do not use another project's Gradle wrapper.

Do not modify global Gradle configuration unless absolutely necessary and explicitly justified.

Do not upgrade a globally installed Android SDK because another project happens to require a different version.

Do not change SDK versions globally just to make Mail Organizer build.

---

# 6. ANDROID SDK ISOLATION

Multiple Android projects may use different:

- compileSdk
- targetSdk
- build tools
- Kotlin versions
- AGP versions
- NDK versions
- Gradle versions
- Java/JDK versions

Respect the Mail Organizer project's configuration.

Do not change another project's configuration to accommodate Mail Organizer.

If Mail Organizer requires an SDK component that is not installed:

1. identify exactly what is missing
2. determine whether installation is safe
3. avoid modifying other projects
4. install only the required component if the environment permits it
5. verify that existing projects remain untouched

Never blindly upgrade or remove SDK components.

---

# 7. DEVICE INSTALLATION ISOLATION

The connected Android phone may be used by multiple projects.

Do not assume that uninstalling or reinstalling an APK is harmless.

When interacting with the device:

- identify the Mail Organizer package ID
- operate only on that package
- do not uninstall unrelated applications
- do not clear unrelated application data
- do not modify unrelated application permissions
- do not change global device settings unless necessary
- do not overwrite another application's data

For example, operations must target:

```text
<MAIL_ORGANIZER_PACKAGE_ID>
```

and not generic package operations.

---

# 8. ADB ISOLATION

ADB commands must be scoped carefully.

Allowed when required:

```text
adb install <MailOrganizer.apk>
adb shell am start <MailOrganizer.package/activity>
adb shell am force-stop <MailOrganizer.package>
adb logcat
adb exec-out screencap
adb shell screenrecord
```

Do not use broad destructive commands such as:

```text
adb uninstall ...
adb shell pm clear ...
adb shell settings reset ...
```

against unrelated applications or the entire device.

Never clear the entire device's app state merely to test Mail Organizer.

---

# 9. ADB REVERSE ISOLATION

If `adb reverse` is needed:

Use a clearly identified development port.

Do not disturb another application's existing development networking unnecessarily.

Before changing a reverse port:

1. inspect existing reverse configuration where possible
2. determine whether the port is already in use
3. select an appropriate Mail Organizer development port
4. configure only what Mail Organizer requires

Do not remove another project's reverse mapping unless you have established that it belongs to Mail Organizer.

If no local development server is needed, do not create an ADB reverse mapping.

---

# 10. GIT ISOLATION

The Mail Organizer project may have its own Git repository.

Determine:

```text
git rev-parse --show-toplevel
```

or an equivalent safe method.

The Git root must be identified before modifications.

Do not run Git commands from a parent directory containing multiple repositories when the command could affect the wrong repository.

Do not:

- reset another repository
- checkout another project's branch
- clean another repository
- modify another repository's remotes
- commit another project's files
- merge another project's changes

All Git operations must target the Mail Organizer repository.

---

# 11. VERIFY PHASE 1 BEFORE STARTING PHASE 2

Before modifying the local data architecture, verify that Phase 1 actually exists and is functional.

Check:

- application builds
- application installs
- application launches
- package ID
- UI foundation
- navigation foundation
- theme
- design tokens
- device testing status
- current `spec.md` phase status

If Phase 1 is not actually complete:

Do NOT pretend it is complete.

Determine whether Phase 2 can safely proceed.

If Phase 1 has blocking failures, report them and resolve only what is necessary to restore the Phase 1 acceptance criteria.

Do not silently skip the missing foundation.

---

# 12. PHASE 2 OBJECTIVE

The objective of Phase 2 is to establish the application's **local persistence architecture**.

Mail Organizer is local-first.

Therefore, the local data layer is one of the most important foundations in the entire application.

The database must eventually support:

- multiple Gmail accounts
- email messages
- threads
- sender information
- company information
- categories
- priority
- action-required state
- extracted structured information
- synchronization state
- user corrections
- rules
- search
- future analytics
- future integrations

Phase 2 establishes this foundation without implementing Gmail synchronization yet.

---

# 13. DO NOT IMPLEMENT GMAIL SYNC

This phase must NOT implement:

- Gmail OAuth
- Gmail API calls
- mailbox synchronization
- Gmail pagination
- Gmail push notifications
- Gmail incremental history
- email downloading
- Gmail classification from live data

Those belong to later phases.

The database must be designed to receive data from Gmail later, but Gmail itself is not connected during this phase.

---

# 14. CHOOSE THE LOCAL DATABASE TECHNOLOGY

Use the database architecture established during Phase 0.

For a modern Android application, Room is preferred unless the existing project has a documented reason to use another technology.

If Room is used, establish:

- Room database
- entities
- DAOs
- repositories
- migrations
- converters where necessary
- database versioning
- test database strategy

Do not add multiple competing database technologies.

Do not introduce a remote database.

---

# 15. DATABASE ARCHITECTURE

Establish a clean separation:

```text
UI
 ↓
Use Case
 ↓
Repository
 ↓
DAO
 ↓
Room
```

The UI must not directly query the database.

The Gmail API must not directly write to UI state.

The synchronization engine will later communicate with repositories.

---

# 16. ACCOUNT ENTITY

Create the foundation for Gmail account identity.

The account model should be capable of representing information such as:

- internal local account ID
- Google account identifier where appropriate
- email address
- display name if available
- provider
- connection state
- last successful synchronization
- created timestamp
- updated timestamp
- enabled/disabled state

Do not store unnecessary OAuth secrets in the ordinary database.

Do not store passwords.

Do not store refresh tokens as plain database fields without an appropriate secure-token architecture.

The account entity must become the parent boundary for account-specific email data.

---

# 17. ACCOUNT ISOLATION

All future email-related records must be associated with the correct account.

Establish relationships that make accidental cross-account mixing difficult.

Conceptually:

```text
Account
   │
   ├── Threads
   │      └── Messages
   │
   ├── Senders
   │
   ├── Companies
   │
   ├── Rules
   │
   └── Sync State
```

Do not rely solely on UI filtering to separate accounts.

The data layer itself must preserve account identity.

---

# 18. EMAIL MESSAGE ENTITY FOUNDATION

Create the database representation needed for an email message.

It should be capable of representing information such as:

- local ID
- Gmail message ID
- Gmail thread ID
- account ID
- sender
- recipients
- CC
- BCC where appropriate
- subject
- timestamp
- snippet
- plain-text body where required
- sanitized body representation where required
- labels
- Gmail category
- unread state
- important state
- attachment metadata
- message size if available
- synchronization metadata

Do not store more data than the requirements justify.

---

# 19. THREAD ENTITY

Create a thread/conversation representation.

It should support:

- local thread ID
- Gmail thread ID
- account ID
- subject
- participant information
- latest message timestamp
- message count
- unread count
- latest message reference
- conversation state where appropriate

Threads must be account-scoped.

Two Gmail accounts may theoretically contain identifiers that appear similar.

Never assume a Gmail ID alone is globally unique across the entire application.

---

# 20. SENDER ENTITY

Establish sender normalization.

The sender model should support information such as:

- email address
- normalized email address
- display name
- domain
- account scope where required
- known/unknown status
- user override
- first seen
- last seen
- message count

Do not build the complete sender-intelligence engine yet.

This phase establishes storage.

---

# 21. COMPANY ENTITY

Establish the foundation for company grouping.

The company model may eventually contain:

- company ID
- canonical company name
- normalized domain
- known domains
- logo reference if eventually supported
- category associations where appropriate
- user override
- created/updated timestamps

Do not implement company detection yet.

Do not call external company-enrichment APIs.

Do not download company logos.

---

# 22. CATEGORY / CLASSIFICATION STORAGE

Prepare storage for the future deterministic classification system.

The system will eventually support:

- Action Required
- Important
- Career
- Education
- Receipts & Orders
- Security
- Notifications
- Newsletters
- Promotions
- Low Value

The database should be capable of storing:

- category
- confidence
- classification source
- classification version
- explanation/reason
- manually overridden state
- classified timestamp

Do not implement the classifier in Phase 2.

---

# 23. PRIORITY STORAGE

Priority is independent from category.

Prepare the model for:

```text
Low
Normal
High
Critical
```

or the exact priority system chosen by the product specification.

Store:

- computed priority
- manual override
- reason/source
- classification version

Do not implement priority calculation yet.

---

# 24. ACTION-REQUIRED STORAGE

Prepare a clean representation for future action-required detection.

Potential fields:

- requires action
- action type
- confidence
- explanation
- due date
- detected timestamp
- completed state
- dismissed state
- source message/thread

Do not build the action engine yet.

---

# 25. STRUCTURED EXTRACTION STORAGE

Future phases will extract structured information from email.

Prepare the architecture for objects such as:

```text
Meeting
Deadline
Payment
Appointment
Travel
Application
Reminder
ReplyRequired
```

Do not create an unnecessarily complicated universal JSON blob as the only storage mechanism.

Prefer typed, queryable structures where appropriate.

The design should allow future migration if the exact extraction model evolves.

---

# 26. SYNC STATE ENTITY

Create a synchronization-state foundation.

The future Gmail sync engine will need to know things such as:

- account
- last successful sync
- sync status
- cursor/history information where applicable
- last attempted sync
- error state
- retry information
- synchronization version

Do not implement the sync engine yet.

Do not invent Gmail history tokens without understanding the future Gmail API implementation.

The schema should be flexible enough for Phase 4.

---

# 27. USER RULES FOUNDATION

Prepare storage for future user rules.

Examples:

```text
Sender → Category
Domain → Category
Sender → Priority
Company → Category
Keyword → Category
Sender → Action Required
```

The exact rule engine belongs to a later phase.

Phase 2 only establishes the persistence boundary.

---

# 28. USER CORRECTIONS FOUNDATION

The user's corrections are authoritative.

The database must eventually support corrections such as:

```text
"Always classify this sender as Career."
"Never mark this company as Promotions."
"These emails should be Important."
```

Prepare a durable representation for such overrides.

Do not implement the correction UI yet.

---

# 29. SEARCH FOUNDATION

The application will eventually need local search.

Do not build the complete search engine now.

However, identify which database fields will eventually need efficient indexing.

Potential fields include:

- account ID
- message ID
- thread ID
- sender
- domain
- company
- subject
- timestamp
- category
- priority
- action-required
- unread
- labels

Use indexes intentionally.

Do not create indexes for every column automatically.

---

# 30. DATABASE INDEXING

Think about the future query patterns.

Indexes should support likely queries such as:

```text
All messages for account
Messages by thread
Messages by sender
Messages by company
Messages by category
Messages by priority
Action-required messages
Unread messages
Recent messages
Messages matching structured search
```

Document important indexing decisions.

---

# 31. NORMALIZATION VS PERFORMANCE

Do not blindly normalize everything.

The application will eventually process potentially large mailboxes.

Balance:

- data integrity
- query performance
- storage size
- synchronization speed
- simplicity

Document important denormalization decisions if used.

Do not prematurely optimize obscure queries.

---

# 32. DATABASE MIGRATIONS

Database schema must be versioned.

Establish migration infrastructure.

Do not use destructive migrations as a shortcut.

For development-only databases, destructive recreation may be acceptable only when explicitly safe and documented.

Production migrations must preserve user data.

---

# 33. CONVERTERS

If Room requires converters for:

- enums
- lists
- structured objects
- timestamps
- other types

implement them consistently.

Do not serialize everything into arbitrary JSON simply because it is convenient.

Use typed representations where the data will be queried.

---

# 34. REPOSITORY INTERFACES

Create clean repository boundaries.

For example:

```text
AccountRepository
EmailRepository
ThreadRepository
SenderRepository
CompanyRepository
ClassificationRepository
RuleRepository
SyncStateRepository
```

Only create repositories that are genuinely useful.

Do not create empty interfaces with no architectural purpose.

---

# 35. FLOW / REACTIVE DATA

Where the architecture uses Kotlin Flow or an equivalent reactive mechanism, repositories should expose observable local data appropriately.

Examples:

```text
Flow<Account>
Flow<List<Email>>
Flow<Thread>
Flow<List<Action>>
```

Do not expose database implementation details directly to the UI.

---

# 36. DATABASE TESTING

Create meaningful tests for the local data layer.

At minimum test:

### Account isolation

Data belonging to Account A must not appear in Account B queries.

### Insert/read

Inserted entities can be retrieved.

### Update

Updated entities return the latest values.

### Delete

Deletion behaves as expected where deletion is supported.

### Relationships

Thread/message relationships remain correct.

### Index-backed queries

Important queries return correct results.

### Migration

Database migrations work where migrations are introduced.

---

# 37. ACCOUNT ISOLATION TEST — MANDATORY

Create an explicit test proving:

```text
Account A
  Email A1
  Email A2

Account B
  Email B1
  Email B2
```

A query for Account A must return:

```text
A1
A2
```

and never:

```text
B1
B2```

Likewise, Account B queries must never return Account A data.

This is a security requirement, not merely a correctness test.

---

# 38. PRIVACY AND DATA MINIMIZATION

Review every stored field.

For each field ask:

> Does Mail Organizer actually need to persist this?

If not, do not store it.

Avoid storing:

- passwords
- OAuth access tokens
- refresh tokens in ordinary tables
- unnecessary full MIME payloads
- unnecessary attachment binaries
- unnecessary duplicate content
- unnecessary tracking information

The database should store what the application needs, not everything Gmail can provide.

---

# 39. TOKEN STORAGE

OAuth tokens do NOT belong in the ordinary email database.

Establish a clear separation between:

```text
Application data
```

and:

```text
Authentication secrets
```

Use Android's appropriate secure credential/token storage strategy when OAuth is implemented later.

Phase 2 should document this boundary without implementing Gmail OAuth.

---

# 40. ATTACHMENTS

Do not download or store attachment files in Phase 2.

Only establish metadata support if required by the data model.

Future attachment handling must consider:

- storage size
- privacy
- offline availability
- deletion
- cleanup
- MIME type
- filename
- Gmail attachment ID

Actual attachment synchronization belongs to a later phase.

---

# 41. DATABASE PERFORMANCE

The database layer must avoid:

- main-thread database access
- unbounded queries
- loading an entire mailbox into memory
- unnecessary duplicate writes
- unnecessary database transactions
- N+1 query patterns

Prepare repository methods for pagination or bounded retrieval where appropriate.

---

# 42. TRANSACTION BOUNDARIES

Identify operations that will eventually require transactions.

Examples:

```text
Insert thread + messages
Update sync state + imported messages
Apply classification + related metadata
Apply user correction + classification override
```

Do not implement Gmail synchronization yet.

But establish transaction patterns that future synchronization can safely use.

---

# 43. DATABASE RESET / DEVELOPMENT TOOLS

If development tooling includes a database reset capability, it must be clearly development-only.

Never create a production UI button that silently destroys the user's mailbox-derived local data.

If a developer reset mechanism is created, make it difficult to trigger accidentally.

---

# 44. DEVICE VALIDATION

After implementing the database foundation:

Build the application.

Use:

```text
Gradle
+
ADB
+
Android device/emulator
```

where available.

Install the application.

Launch it.

Verify that:

- the database initializes
- the application starts
- no migration crash occurs
- no startup database error occurs
- no unexpected network call occurs
- no sensitive data is logged

Use logcat when appropriate.

---

# 45. DATABASE INSPECTION

If the development environment supports Android database inspection, use it.

Inspect the database schema where practical.

Verify:

- tables
- columns
- relationships
- indexes
- database version
- migration state

Do not expose real user data in screenshots or reports.

If no database inspector is available, use tests and controlled development data.

---

# 46. USE CONTROLLED TEST DATA

For database tests, use synthetic data.

Example:

```text
Account:
test.user.one@example.test

Sender:
notifications@example.test

Company:
Example Company

Subject:
Test Message
```

Never use real personal emails simply to test database relationships.

---

# 47. SCREENSHOT / VISUAL VALIDATION

Phase 2 is primarily a data architecture phase, but any affected UI must still be tested visually.

If a database-backed screen changes:

1. build
2. install
3. launch
4. navigate
5. capture screenshot
6. inspect
7. fix
8. retest

Do not skip device validation because the change is "only backend code."

---

# 48. DO NOT BUILD FUTURE FEATURES

Explicitly do NOT implement:

- Gmail OAuth
- Gmail synchronization
- classification algorithm
- company detection
- search UI
- Calendar
- Tasks
- AI
- automation
- Gmail modifications
- email sending
- production analytics

Phase 2 is about local data architecture.

---

# 49. FINAL SECURITY REVIEW

Before completion, verify:

- account isolation
- no tokens in DB
- no passwords
- no unnecessary sensitive fields
- no sensitive logging
- no real user test data
- no cross-project modifications
- no cross-application device modifications
- no destructive ADB operations
- no global SDK changes
- no unrelated Gradle changes

---

# 50. FINAL GIT REVIEW

From the Mail Organizer Git root only:

Inspect:

```text
git status
git diff
```

Verify that only Mail Organizer files were changed.

If the parent workspace contains other repositories, explicitly verify that they were not modified.

Do not commit unless instructed by the project's workflow.

---

# 51. PHASE 2 ACCEPTANCE CRITERIA

Phase 2 is complete only when:

### Database

- [ ] local database established
- [ ] database versioning established
- [ ] migration framework established
- [ ] Account entity implemented
- [ ] Message entity implemented
- [ ] Thread entity implemented
- [ ] Sender foundation implemented
- [ ] Company foundation implemented
- [ ] classification storage foundation implemented
- [ ] priority storage foundation implemented
- [ ] action-required storage foundation implemented
- [ ] sync-state foundation implemented
- [ ] rule/correction storage foundation implemented where appropriate

### Architecture

- [ ] repository layer established
- [ ] DAO layer established
- [ ] domain/data boundaries preserved
- [ ] account isolation preserved
- [ ] authentication secrets separated from application data

### Performance

- [ ] important indexes established
- [ ] no main-thread database access
- [ ] bounded queries used where appropriate
- [ ] transaction strategy established

### Security

- [ ] account isolation test passes
- [ ] no tokens stored in ordinary DB
- [ ] no passwords
- [ ] no unnecessary sensitive data
- [ ] no sensitive logs

### Testing

- [ ] CRUD tests
- [ ] relationship tests
- [ ] account isolation tests
- [ ] migration tests where applicable
- [ ] repository tests where applicable

### Device

- [ ] application builds
- [ ] application installs
- [ ] application launches
- [ ] database initializes successfully
- [ ] no startup database crash
- [ ] runtime logs inspected

### Workspace Safety

- [ ] Mail Organizer project root verified
- [ ] no sibling project files modified
- [ ] no sibling Gradle configuration modified
- [ ] no sibling SDK/build configuration modified
- [ ] no unrelated APK installed/uninstalled
- [ ] no unrelated application data cleared
- [ ] no global destructive SDK changes
- [ ] Git operations restricted to Mail Organizer repository

---

# 52. UPDATE `editor-rules.md`

Before completing Phase 2, update `editor-rules.md` with any newly discovered permanent rules from this phase, especially:

- sequential phase execution
- instruction-folder discovery
- multi-project workspace isolation
- Gradle project isolation
- Android SDK isolation
- ADB package isolation
- ADB reverse isolation
- Git repository isolation
- database/account isolation
- local-first data architecture
- no future-phase execution

Do this yourself.

Do not ask the user to copy rules manually.

Do not regenerate the entire editor-rules document unnecessarily.

Only add/update the relevant rules while preserving the existing contract.

---

# 53. UPDATE PROJECT STATUS

Update:

```text
spec.md
```

only after the Phase 2 implementation has been verified.

Mark only the tasks actually completed.

Do not mark Phase 3 or any future phase.

Update:

```text
docs/development-status.md
```

with the Phase 2 outcome if that document exists.

---

# 54. FINAL PHASE REPORT

At completion, provide:

## Phase 2 Status

`COMPLETE` / `PARTIAL` / `BLOCKED`

## Database Technology

State what was selected and why.

## Data Model

List the implemented entities.

## Account Isolation

Explain how account-scoped data is enforced.

## Repository Architecture

Explain:

```text
UI
↓
Use Case
↓
Repository
↓
DAO
↓
Database
```

where applicable.

## Security

Explain how authentication secrets are separated from application data.

## Testing

Report:

- database tests
- isolation tests
- migration tests
- repository tests

## Device Validation

Report:

- build
- install
- launch
- database initialization
- runtime inspection

## Workspace Safety

Explicitly confirm that:

- only the Mail Organizer project was modified
- sibling Android projects were not modified
- sibling Gradle projects were not built or reconfigured unnecessarily
- unrelated APKs were not installed/uninstalled
- unrelated device data was not cleared

## Files Changed

List important files.

## Known Issues

List unresolved issues.

## Deferred Work

List work intentionally left for later phases.

## Acceptance Criteria

Show all Phase 2 criteria and their status.

## Next Phase

The next incomplete phase is:

**Phase 3 — Google OAuth & Gmail Connection**

Do not execute it automatically.

---

# FINAL OPERATING MODEL

This project must now follow this exact development model:

```text
PROJECT INSTRUCTION FOLDER
        ↓
READ MASTER RULES
        ↓
IDENTIFY CURRENT PHASE
        ↓
READ ONLY THAT PHASE'S EXECUTION CONTRACT
        ↓
INSPECT CURRENT PROJECT STATE
        ↓
IMPLEMENT CURRENT PHASE
        ↓
BUILD WITH THE PROJECT'S GRADLE
        ↓
INSTALL ONLY MAIL ORGANIZER
        ↓
TEST ON DEVICE/EMULATOR
        ↓
SCREENSHOT / SCREEN RECORD WHEN RELEVANT
        ↓
INSPECT LOGCAT / RUNTIME
        ↓
FIX
        ↓
REBUILD
        ↓
REINSTALL
        ↓
RETEST
        ↓
VERIFY ACCEPTANCE CRITERIA
        ↓
UPDATE RULES / STATUS
        ↓
STOP
```

On the next session/continuation, do not require the user to paste another phase prompt manually.

Instead:

1. Find the Mail Organizer instruction folder.
2. Read the project documents.
3. Inspect `spec.md`.
4. Determine the next incomplete phase.
5. Read that phase's prompt.
6. Execute only that phase.
7. Follow its instructions exactly.
8. Stop when that phase is complete.

The user should be able to keep all phase `.md` files in one dedicated instruction folder and simply tell you to continue.

**Never execute the entire roadmap in one shot.**

**Never touch sibling Android projects.**

**Never modify another project's Gradle/SDK/build configuration.**

**Never confuse the workspace root with the Mail Organizer project root.**

**Never consider a phase complete without actual implementation, build validation, and appropriate real-device verification.**

Stop strictly at the Phase 2 boundary.