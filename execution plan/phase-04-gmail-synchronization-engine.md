# PHASE 4 — GMAIL SYNCHRONIZATION ENGINE

You are now executing:

**Phase 4 — Gmail Synchronization Engine**

This phase begins only after Phase 3 — Google OAuth & Gmail Connection has been successfully completed and verified.

Do not execute Phase 5 or any later phase automatically.

The application must continue following the strict sequential development model:

```text
Phase 0
  ↓
Phase 1
  ↓
Phase 2
  ↓
Phase 3
  ↓
Phase 4 ← YOU ARE HERE
  ↓
Phase 5
  ↓
...
```

Complete Phase 4, verify it thoroughly, update the project documentation and persistent rules where necessary, and STOP.

Do not implement classification, company intelligence, advanced search, Calendar, Tasks, AI, automation, or Gmail write operations during this phase.

---

# 1. DISCOVER THE INSTRUCTION FOLDER FIRST

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
    - Phase 4 instructions
4. Inspect the actual repository.
5. Determine the current implementation state.
6. Confirm Phase 3 is actually complete.

Do not assume the previous phase is complete merely because `spec.md` says so.

Verify the implementation.

---

# 2. STRICT MULTI-PROJECT ISOLATION

The development workspace may contain multiple Android applications.

Identify the Mail Organizer project root before running any:

- Gradle command
- Git command
- Android build command
- ADB command
- test command
- file modification
- SDK-related command

Never operate on sibling projects.

Do not:

- edit sibling projects
- build sibling projects
- clean sibling projects
- modify sibling Gradle files
- upgrade sibling dependencies
- alter sibling SDK configuration
- change sibling signing configuration
- install sibling APKs
- uninstall sibling APKs
- clear sibling application data
- modify sibling Git repositories

All operations must be scoped to Mail Organizer.

---

# 3. UPDATE `editor-rules.md`

Before implementation, update `editor-rules.md` with any permanent rules discovered in this phase.

Especially preserve:

- synchronization must be account-scoped
- Gmail is the source of truth
- local database is the application's local representation
- synchronization must be incremental where supported
- synchronization must be resumable
- synchronization must be idempotent
- synchronization must not duplicate messages
- synchronization must not lose local user state
- API pagination must be handled correctly
- rate limits must be respected
- retries must use controlled backoff
- cancellation must be supported
- background work must be battery-conscious
- email content must not be logged
- sync failures must not corrupt existing local data
- one account's sync must never affect another account
- Gmail write operations remain disabled
- sibling projects must never be touched
- phase execution remains sequential

Do this yourself.

Do not ask the user to edit the rules manually.

---

# 4. PHASE OBJECTIVE

The objective of Phase 4 is to build the first real Gmail data pipeline:

```text
Google OAuth
      ↓
Authenticated Gmail API
      ↓
Synchronization Engine
      ↓
Pagination
      ↓
Normalization
      ↓
Local Database
      ↓
Local Mail Data
```

At the end of this phase, Mail Organizer should be capable of synchronizing authorized Gmail mailbox data into its local database safely.

The synchronization system must be:

- account-aware
- incremental where possible
- resumable
- cancellable
- retryable
- idempotent
- pagination-safe
- rate-limit-aware
- battery-conscious
- observable
- privacy-safe

---

# 5. GMAIL REMAINS THE SOURCE OF TRUTH

The local database is NOT the authoritative source of the mailbox.

Gmail is.

The synchronization engine must therefore treat local data as a synchronized representation.

Conceptually:

```text
Gmail
  ↓
Remote state
  ↓
Sync engine
  ↓
Local state
```

Do not invent local-only mailbox state that contradicts Gmail.

User-created application metadata such as classification, priority overrides, rules, or corrections may remain local.

Clearly distinguish:

### Gmail-derived data

from:

### Mail Organizer-derived data

and:

### User-created overrides

---

# 6. DO NOT MODIFY GMAIL

Phase 4 is read-only synchronization.

Do not:

- archive
- delete
- trash
- mark read
- mark unread
- modify labels
- send
- reply
- forward
- star/unstar
- modify Gmail categories

Do not request `gmail.modify` merely to simplify synchronization.

The existing least-privilege Gmail read-only authorization must remain sufficient.

---

# 7. DETERMINE THE GMAIL API DATA STRATEGY

Before implementation, inspect the current Gmail API client.

Determine the correct API strategy for:

- mailbox discovery
- message listing
- message retrieval
- thread retrieval where useful
- labels
- history/incremental synchronization where appropriate

Use official Gmail API behavior and current documentation.

Do not invent endpoints or fields.

If current API behavior has changed from assumptions in the project documents, document the discrepancy and adapt the implementation safely.

---

# 8. SYNCHRONIZATION ARCHITECTURE

Establish a dedicated synchronization layer.

Conceptually:

```text
SyncCoordinator
      ↓
AccountSyncWorker
      ↓
GmailSyncClient
      ↓
Gmail API
      ↓
Normalizer
      ↓
Repository
      ↓
Room Database
```

The exact class names may differ.

The responsibilities must remain separated.

The UI must not directly control Gmail API requests.

---

# 9. ACCOUNT-SCOPED SYNCHRONIZATION

Every synchronization operation must be associated with exactly one account.

For example:

```text
Sync Account A
```

must never accidentally:

```text
read Account B credentials
write Account B email records
update Account B sync state
```

Account ID must flow through the synchronization pipeline.

Conceptually:

```text
Account
 ↓
SyncState
 ↓
GmailClient
 ↓
Messages
 ↓
Repository
 ↓
Database
```

---

# 10. SYNCHRONIZATION STATE

Use the synchronization state established in Phase 2.

It should track appropriate information such as:

- account ID
- current sync state
- last successful synchronization
- last attempted synchronization
- synchronization progress where useful
- remote cursor/history state where applicable
- retry state
- last error category
- synchronization version
- initial-sync completion state

Do not store arbitrary temporary data indefinitely.

---

# 11. INITIAL SYNCHRONIZATION

Implement a controlled initial synchronization.

Do not blindly download an unlimited mailbox.

The initial sync strategy must account for:

- mailbox size
- API pagination
- local storage
- battery
- network usage
- user experience
- cancellation

The implementation should have a clearly defined initial synchronization policy.

If the product specification requires a bounded initial sync, respect that.

If the user has a very large mailbox, the application must not appear frozen while attempting to download everything.

---

# 12. MESSAGE LISTING

Use the Gmail API's supported message-listing mechanism.

Handle:

- page tokens
- empty pages
- end of pagination
- API errors
- cancellation

Never assume one API response contains the entire mailbox.

---

# 13. PAGINATION

Pagination is mandatory.

Implement proper pagination.

Conceptually:

```text
Request page 1
   ↓
Process
   ↓
nextPageToken?
   ├── yes → request next page
   └── no  → finished
```

Do not:

- ignore page tokens
- request the same page repeatedly
- create duplicate records
- accidentally skip pages

Test pagination using controlled test data.

---

# 14. MESSAGE RETRIEVAL

Determine the appropriate Gmail API format for message retrieval.

Do not request unnecessarily large representations when a smaller representation is sufficient.

Use the minimum message data required by the current phase and downstream parser.

Phase 5 will perform deeper parsing.

The synchronization layer should focus on reliably obtaining and storing the source data required by the application.

---

# 15. THREAD HANDLING

Where Gmail provides thread identifiers:

Persist them.

Do not assume every message is an independent conversation.

The local database must preserve:

```text
Gmail Thread
   ↓
Message 1
Message 2
Message 3
...
```

The synchronization engine should avoid creating duplicate thread records.

---

# 16. MESSAGE IDENTITY

Gmail message IDs are critical.

Use them as the remote identity.

The local schema must prevent accidental duplicate insertion of the same Gmail message for the same account.

Conceptually:

```text
Account ID + Gmail Message ID
```

should identify a unique synchronized message.

Do not assume Gmail message IDs are sufficient across every account boundary without considering account scoping.

---

# 17. THREAD IDENTITY

Similarly:

```text
Account ID + Gmail Thread ID
```

must be treated as the thread identity boundary.

Do not accidentally merge threads from different accounts.

---

# 18. UPSERT / IDEMPOTENCY

Synchronization must be idempotent.

Running the same synchronization twice must NOT produce:

```text
duplicate message
duplicate thread
duplicate sender
duplicate account
```

The desired behavior is:

```text
First sync
→ insert

Second sync
→ update/no-op
```

not:

```text
First sync
→ insert

Second sync
→ duplicate
```

---

# 19. HANDLE MESSAGE UPDATES

If a synchronized message already exists:

Determine whether the remote representation has changed.

Update only what should be updated.

Do not blindly overwrite Mail Organizer-derived metadata such as:

- manual category override
- manual priority override
- user correction
- local action state

Remote Gmail data and local application metadata must remain conceptually separate.

---

# 20. PRESERVE USER OVERRIDES

This is critical.

Suppose the user later says:

> Always classify emails from this sender as Career.

A future synchronization must not erase that user preference simply because Gmail sent the message again.

Likewise, synchronization must not destroy local:

- classification overrides
- priority overrides
- rules
- action state
- user notes
- extracted application metadata

Where necessary, separate remote fields from local fields.

---

# 21. TRANSACTIONAL WRITES

Synchronizing a batch of messages should use safe database transactions where appropriate.

A partially processed page must not leave the database in a corrupt or contradictory state.

However, do not create a gigantic transaction containing the entire mailbox.

Use sensible transaction boundaries.

For example:

```text
Fetch page
   ↓
Validate
   ↓
Normalize
   ↓
Transaction
   ↓
Persist page
   ↓
Commit
   ↓
Advance sync state
```

Only advance the relevant synchronization cursor/state after the corresponding data has been successfully persisted.

---

# 22. SYNC STATE SAFETY

Never mark synchronization as successful before the corresponding local data is safely committed.

Bad:

```text
Fetch page
↓
Mark sync successful
↓
Database write fails
```

Correct:

```text
Fetch page
↓
Validate
↓
Persist
↓
Commit
↓
Advance sync state
```

This prevents data loss and inconsistent incremental synchronization.

---

# 23. INCREMENTAL SYNCHRONIZATION

Implement incremental synchronization where supported by Gmail's API.

Do not repeatedly download the entire mailbox after the initial sync.

Use the official Gmail synchronization/history mechanisms appropriate to the API.

The design should support:

```text
Initial sync
      ↓
Save synchronization state
      ↓
Later sync
      ↓
Fetch changes
      ↓
Apply changes
      ↓
Update synchronization state
```

Do not implement an unofficial shortcut.

---

# 24. HISTORY / CURSOR INVALIDATION

The incremental synchronization mechanism may become invalid or unavailable.

Handle this safely.

If the remote synchronization cursor/history state can no longer be used:

```text
Invalid incremental state
        ↓
Controlled recovery
        ↓
Re-establish synchronization baseline
```

Do not crash.

Do not silently assume the local mailbox is still synchronized.

Document the recovery strategy.

---

# 25. RETRIES

Implement controlled retries.

Retry appropriate transient failures such as:

- temporary network failure
- server errors
- rate limiting where appropriate

Do NOT blindly retry:

- invalid credentials
- permission denial
- malformed requests
- permanent authorization errors

Use exponential backoff where appropriate.

Do not create infinite retry loops.

---

# 26. RATE LIMITING

Respect Gmail API limits.

Do not:

- issue unnecessary duplicate requests
- hammer the API
- parallelize requests without justification
- continuously poll
- fetch the same page repeatedly

Where batching or concurrency can improve performance safely, use it only after correctness is established.

Correctness comes before speed.

---

# 27. CANCELLATION

Synchronization must support cancellation.

For example:

```text
User leaves sync screen
```

or:

```text
Application is stopped
```

must not leave uncontrolled work running.

Use structured concurrency and cancellation-aware APIs.

Do not swallow cancellation exceptions and continue synchronizing.

---

# 28. BACKGROUND SYNCHRONIZATION FOUNDATION

Use the Android background-work mechanism selected by the architecture, such as WorkManager where appropriate.

Establish the foundation for:

- network constraints
- retry
- cancellation
- battery-aware scheduling
- account-specific work

Do not create aggressive periodic polling.

The detailed background-sync strategy can be expanded in Phase 19.

---

# 29. FOREGROUND / USER-INITIATED SYNC

Support an explicit user-initiated sync where appropriate.

The user should be able to trigger:

```text
Sync now
```

when the UI supports it.

Provide a clear state:

```text
Syncing...
```

and then:

```text
Synced just now
```

or an appropriate error state.

Do not allow repeated rapid sync taps to create duplicate concurrent sync jobs.

---

# 30. CONCURRENT SYNC PROTECTION

Prevent two synchronization operations for the same account from corrupting each other.

For example:

```text
Sync Account A
+
Sync Account A
```

should not result in uncontrolled concurrent database writes.

Use appropriate synchronization/coordinator mechanisms.

Different accounts may potentially synchronize independently if the architecture safely supports it.

---

# 31. SYNC PROGRESS

Where practical, expose useful progress information.

Examples:

```text
Connecting...
Fetching messages...
Saving messages...
Finishing...
Synced
```

Do not fabricate percentages such as:

```text
73%
```

unless the system actually knows what 73% means.

If exact progress cannot be known, use indeterminate progress.

---

# 32. ERROR STATES

Create clear synchronization errors.

Examples:

### Authentication expired

> Gmail authorization has expired. Reconnect your account.

### Network unavailable

> Couldn't sync right now. Check your connection and try again.

### Rate limited

> Gmail temporarily limited requests. We'll retry later.

### Unknown failure

> We couldn't finish syncing. Your existing mail data is still available.

Never erase existing synchronized data because a new sync failed.

---

# 33. OFFLINE BEHAVIOR

If the device is offline:

- local data must remain available
- synchronization should fail gracefully
- the app should not crash
- the user should understand that synchronization is unavailable

Do not delete local data because Gmail cannot currently be reached.

---

# 34. DATA INTEGRITY

A failed sync must not destroy the last known good local mailbox.

If:

```text
Previous sync = successful
Current sync = failed
```

the application should retain the previous synchronized data.

The user should still be able to use the local mailbox.

---

# 35. MESSAGE CONTENT SAFETY

Email content is untrusted external input.

Do not execute:

- JavaScript
- HTML scripts
- embedded code
- arbitrary content

during synchronization.

Do not interpret email HTML as application code.

Phase 5 will handle detailed parsing/sanitization.

---

# 36. ATTACHMENT HANDLING

Do not download attachment binaries during Phase 4 unless explicitly required by the current requirements.

Store only necessary attachment metadata if the synchronization representation contains it.

Avoid filling local storage with large attachments.

Future attachment behavior can be implemented separately.

---

# 37. LABELS

Synchronize relevant Gmail labels/categories needed by the application.

Do not modify them.

Preserve their relationship to the message.

Do not confuse Gmail's native categories with Mail Organizer's own classification system.

For example:

```text
Gmail category:
PROMOTIONS
```

is not the same thing as:

```text
Mail Organizer category:
Promotions
```

They may inform future classification, but they are separate concepts.

---

# 38. GMAIL CATEGORY DATA

Where available, preserve Gmail's category information.

Potential categories include:

- PRIMARY
- SOCIAL
- PROMOTIONS
- UPDATES
- FORUMS

Do not replace the Gmail category with the application's own category.

Store them as remote Gmail metadata.

---

# 39. MESSAGE STATE

Where useful, synchronize remote message state such as:

- unread
- important
- starred
- labels

Do not modify those states.

Do not assume Gmail's "important" state equals Mail Organizer's priority.

---

# 40. SYNC OBSERVABILITY

Create safe internal diagnostics.

Track:

- sync start
- sync end
- account
- number of messages processed
- number inserted
- number updated
- number skipped
- duration
- error category
- retry count

Do NOT log:

- email bodies
- full subjects unnecessarily
- tokens
- authorization headers
- attachment contents
- sensitive personal data
Diagnostics should be privacy-safe.

---

# 41. SYNC METRICS

Where useful, capture local development metrics such as:

```text
messages fetched
messages persisted
messages updated
messages skipped
API calls
duration
database write duration
```

These are for engineering diagnostics, not remote analytics.

Do not introduce a third-party analytics service.

---

# 42. MEMORY MANAGEMENT

A mailbox can be large.

Do not load the entire mailbox into memory.

Avoid:

```text
List<EveryEmailInMailbox>
```

when processing potentially large datasets.

Prefer:

```text
page
→ process
→ persist
→ release
→ next page
```

Use bounded memory.

---

# 43. DATABASE BATCHING

Use efficient database operations.

Where safe:

- batch inserts
- batch updates
- transactions

Avoid one database transaction per message if that causes unacceptable performance.

But do not make transactions so large that memory/storage pressure becomes a problem.

Measure before aggressively optimizing.

---

# 44. NORMALIZATION BOUNDARY

Keep raw Gmail API models separate from local database models.

Prefer:

```text
Gmail API Model
       ↓
Normalizer / Mapper
       ↓
Domain Model
       ↓
Database Entity
```

Do not expose Gmail API model classes throughout the entire application.

This protects the architecture from API changes.

---

# 45. TEST WITH SYNTHETIC MAIL DATA

Create deterministic test data.

Include cases such as:

- one message
- multiple messages
- same thread
- different threads
- duplicate message
- repeated sync
- multiple accounts
- unread/read state
- Gmail labels
- empty page
- multiple pages
- API failure
- retry
- cancellation

Do not use real private email data in automated tests.

---

# 46. PAGINATION TEST

Create a test that simulates:

```text
Page 1
→ token A

Page 2
→ token B

Page 3
→ no token
```

Verify that all records are imported exactly once.

Also test:

```text
same page repeated
```

and ensure idempotency prevents duplicates.

---

# 47. DUPLICATE TEST

Run synchronization twice against the same synthetic Gmail dataset.

Expected:

```text
First sync:
10 messages inserted

Second sync:
0 duplicate messages
10 existing records updated/no-op
```

The exact numbers can differ, but no duplicate records may be created.

---

# 48. ACCOUNT ISOLATION TEST

Use at least two synthetic accounts.

Example:

```text
Account A:
A-message-1
A-message-2

Account B:
B-message-1
B-message-2
```

Synchronize both.

Verify:

```text
Account A query
→ A-message-1
→ A-message-2

Account B query
→ B-message-1
→ B-message-2
```

No cross-account contamination is allowed.

---

# 49. FAILURE RECOVERY TEST

Simulate:

```text
Fetch page
↓
Database failure
```

Verify:

- sync state does not falsely advance
- existing data remains intact
- retry is possible
- application does not crash

Also test:

```text
Network failure
```

and:

```text
Authentication failure
```

---

# 50. CANCELLATION TEST

Start a controlled synchronization.

Cancel it during processing.

Verify:

- work stops
- cancellation is respected
- no corrupt database state
- application remains usable
- future sync can resume safely

---

# 51. DEVICE TESTING

This phase must be tested on an Android device/emulator whenever available.

Use the actual development toolchain:

```text
Gradle
+
ADB
+
Android device/emulator
+
logcat
+
screenshots
+
screen recording where useful
```

Build using the Mail Organizer project's own Gradle wrapper.

Install only the Mail Organizer APK.

---

# 52. SCREENSHOT SYNC STATES

Capture and inspect relevant UI states:

### Before sync

```text
Not synced
```

### Syncing

```text
Syncing...
```

### Success

```text
Synced
```

### Failure

```text
Sync failed
```

### Offline

```text
Offline
```

### Retry

```text
Retry available
```

Check:

- spacing
- typography
- loading indicators
- buttons
- error messages
- dark mode
- accessibility
- responsive behavior

---

# 53. SCREEN RECORDING

Where useful, record:

```text
Open app
→ Start sync
→ Sync progress
→ Completion
```

Use recording to inspect:

- animation
- loading transitions
- duplicate taps
- UI freezes
- unexpected navigation
- crashes

Do not record private mailbox content unnecessarily.

---

# 54. LOGCAT

Inspect logs during:

- initial synchronization
- repeated synchronization
- failed synchronization
- cancellation
- account switching if available

Verify:

- no crash
- no token leakage
- no email-body logging
- no uncontrolled retry loop
- no ANR
- no database corruption

---

# 55. PERFORMANCE TEST

Measure or inspect:

- initial sync duration
- memory usage
- database write behavior
- UI responsiveness
- battery behavior
- API request count

Do not optimize blindly.

First ensure correctness.

Then fix obvious bottlenecks.

---

# 56. LARGE MAILBOX SAFETY

Test with a sufficiently large synthetic dataset where possible.

The application must not:

- freeze the UI
- run out of memory
- create duplicate messages
- endlessly retry
- lose synchronization state
- corrupt local data

If a real test mailbox is used, avoid using a personal mailbox containing sensitive data unless absolutely necessary.

---

# 57. NO AI

Do not introduce AI in Phase 4.

The sync engine must remain deterministic.

Do not send email content to:

- OpenAI
- Anthropic
- Gemini
- OpenRouter
- any other AI provider

for synchronization.

AI classification belongs to Phase 26 and is optional.

---

# 58. NO THIRD-PARTY EMAIL PROCESSING

Do not send Gmail messages through an external email-processing service.

The intended architecture remains:

```text
Gmail
 ↓
Mail Organizer
 ↓
Local database
```

not:

```text
Gmail
 ↓
Third-party processing server
 ↓
Mail Organizer
```

---

# 59. BATTERY SAFETY

Do not create aggressive sync schedules.

Avoid:

- minute-level polling
- permanent foreground services
- unnecessary wake locks
- repeated background API requests

The detailed battery optimization belongs to later phases.

Phase 4 should simply establish safe synchronization behavior.

---

# 60. FINAL SECURITY REVIEW

Before marking Phase 4 complete, verify:

- [ ] Gmail remains read-only
- [ ] no Gmail write operations
- [ ] no token logging
- [ ] no email-body logging
- [ ] no third-party email processing
- [ ] no AI processing
- [ ] account isolation
- [ ] idempotency
- [ ] safe sync state
- [ ] safe retries
- [ ] safe cancellation
- [ ] no destructive recovery
- [ ] no unrelated device operations
- [ ] no sibling project modifications

---

# 61. FINAL BUILD AND DEVICE VALIDATION

Perform the complete real-world validation loop:

```text
Inspect
 ↓
Build with Mail Organizer Gradle
 ↓
Install Mail Organizer APK
 ↓
Launch
 ↓
Connect Gmail
 ↓
Start initial sync
 ↓
Observe
 ↓
Capture screenshots
 ↓
Inspect logcat
 ↓
Verify local database
 ↓
Repeat sync
 ↓
Verify no duplicates
 ↓
Simulate failure
 ↓
Recover
 ↓
Restart application
 ↓
Verify synchronized state
```

Fix all problems discovered.

Then rebuild and repeat.

Do not stop at the first successful build.

---

# 62. MULTI-PROJECT FINAL CHECK

Before finalizing the phase, verify again:

### Files

Only Mail Organizer files changed.

### Gradle

Only Mail Organizer Gradle configuration was touched.

### SDK

No unrelated project SDK configuration was changed.

### Device

Only Mail Organizer package/data was affected.

### Git

Only Mail Organizer repository state was affected.

### Google Cloud

Only Mail Organizer's Google Cloud/Gmail configuration was used.

If any accidental cross-project modification occurred, investigate and correct it safely before completion.

---

# 63. UPDATE `editor-rules.md`

Before completion, add any new permanent synchronization rules discovered during implementation.

Do not ask the user.

Do not duplicate the entire file unnecessarily.

Preserve the existing rules and extend them.

---

# 64. UPDATE `spec.md`

Only after the implementation and verification are complete:

- update Phase 4 checkboxes
- mark only genuinely completed tasks
- record deferred work
- record known limitations
- update phase status

Do not mark Phase 5 or later phases.

---

# 65. UPDATE DEVELOPMENT STATUS

Update:

```text
docs/development-status.md
```

if present.

Record:

- synchronization architecture
- initial sync behavior
- incremental sync behavior
- retry strategy
- account isolation
- known API limitations
- known performance limitations
- test results

Do not include credentials or private email content.

---

# 66. PHASE 4 ACCEPTANCE CRITERIA

Phase 4 is complete only when all applicable criteria pass.

### Gmail API

- [ ] authenticated Gmail API client works
- [ ] message listing works
- [ ] message retrieval works
- [ ] pagination works
- [ ] Gmail metadata is preserved
- [ ] thread IDs preserved
- [ ] labels/categories preserved
- [ ] message state preserved

### Synchronization

- [ ] initial synchronization works
- [ ] incremental synchronization architecture works
- [ ] synchronization state persisted
- [ ] synchronization is idempotent
- [ ] duplicate prevention works
- [ ] updates do not destroy local metadata
- [ ] account isolation works
- [ ] synchronization is cancellable
- [ ] retries are controlled
- [ ] rate limits are respected
- [ ] failures do not corrupt existing data

### Database

- [ ] messages persisted
- [ ] threads persisted
- [ ] account association correct
- [ ] batch writes work
- [ ] transaction boundaries correct
- [ ] queries remain bounded
- [ ] database remains responsive

### Background

- [ ] background synchronization foundation established
- [ ] network constraints considered
- [ ] battery constraints considered
- [ ] duplicate concurrent sync prevented

### Privacy

- [ ] no email body logging
- [ ] no token logging
- [ ] no external email processing
- [ ] no AI processing
- [ ] no unnecessary data transmission

### Device

- [ ] Gradle build passes
- [ ] APK installed
- [ ] Gmail connection works
- [ ] initial sync works
- [ ] repeated sync works
- [ ] failure recovery tested
- [ ] cancellation tested
- [ ] screenshots inspected
- [ ] logcat inspected
- [ ] app remains responsive

### Workspace isolation

- [ ] sibling projects untouched
- [ ] sibling Gradle configuration untouched
- [ ] unrelated SDK configuration untouched
- [ ] unrelated applications untouched
- [ ] unrelated Git repositories untouched
- [ ] unrelated Google Cloud projects untouched

---

# 67. FINAL PHASE REPORT

Provide:

## Phase 4 Status

`COMPLETE` / `PARTIAL` / `BLOCKED`

## Synchronization Architecture

Explain the implemented flow.

## Initial Sync

Explain how initial synchronization works.

## Incremental Sync

Explain how later synchronization works.

## Pagination

Explain pagination handling.

## Idempotency

Explain duplicate prevention.

## Account Isolation

Explain how account-specific synchronization is enforced.

## Error Handling

Explain retries, cancellation, authentication errors, network failures, and recovery.

## Database

Explain how Gmail data reaches Room/local persistence.

## Performance

Report important observations.

## Device Validation

Report:

- device/emulator
- Android/API level
- build
- installation
- sync test
- repeated sync
- failure recovery
- cancellation
- screenshots
- logcat

## Security

Report privacy/security validation.

## Workspace Isolation

Explicitly confirm that unrelated Android projects were not touched.

## Files Changed

List important files.

## Known Issues

List unresolved issues.

## Deferred Work

Especially:

- detailed parsing
- classification
- company detection
- search
- Calendar
- Tasks
- AI
- advanced automation
- Gmail modification

## Acceptance Criteria

Show every Phase 4 criterion and its final status.

## Next Phase

The next incomplete phase is:

**Phase 5 — Email Data Model & Parsing**

Do not execute it automatically.

---

# FINAL OPERATING MODEL

Always follow:

```text
DISCOVER PROJECT INSTRUCTION FOLDER
        ↓
READ MASTER RULES
        ↓
IDENTIFY MAIL ORGANIZER ROOT
        ↓
VERIFY PREVIOUS PHASE
        ↓
READ ONLY CURRENT PHASE
        ↓
IMPLEMENT
        ↓
BUILD WITH MAIL ORGANIZER GRADLE
        ↓
INSTALL ONLY MAIL ORGANIZER
        ↓
RUN ON DEVICE/EMULATOR
        ↓
TEST REAL BEHAVIOR
        ↓
SCREENSHOT / RECORD WHERE USEFUL
        ↓
INSPECT LOGCAT
        ↓
INSPECT DATABASE
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

**Do not implement Phase 5.**

**Do not implement classification.**

**Do not implement AI.**

**Do not implement Calendar or Tasks.**

**Do not implement Gmail write operations.**

**Do not touch sibling Android projects.**

**Do not modify unrelated SDK/Gradle configurations.**

**Do not treat a successful Gradle build as sufficient validation.**

Phase 4 ends only when the Gmail synchronization engine has been implemented, tested, installed, exercised against controlled Gmail data, and verified for correctness, privacy, account isolation, idempotency, and recovery.