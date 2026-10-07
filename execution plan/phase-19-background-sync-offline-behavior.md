# Phase 19 — Background Sync & Offline Behavior

## Mission

Implement a reliable, battery-conscious, account-safe **Background Sync & Offline Behavior** system for Mail Organizer.

The goal is to make Mail Organizer remain useful when:

- the app is not open
- the device is temporarily offline
- connectivity changes
- multiple Gmail accounts are connected
- synchronization fails
- Android restricts background execution
- the application is restarted
- the device reboots

The system must preserve the core principle:

> **Gmail remains the remote source of truth, while the local database remains the source for the application's offline UI and intelligence.**

The architecture must be:

```text
Gmail
  ↓
Account-scoped sync
  ↓
Local database
  ↓
Local parsing / classification / intelligence
  ↓
Offline-capable UI
```

Background synchronization must never become an excuse to move processing to an unnecessary remote backend.

---

# 1. Mandatory Instruction-Folder Discovery

Before changing anything:

1. Identify the **Mail Organizer project root**.
2. Locate the instruction/documentation folder.
3. Read:
    - `requirements.md`
    - `spec.md`
    - `design.md`
    - `editor-rules.md`
    - this Phase 19 prompt
    - `docs/development-status.md` if present
4. Verify that Phase 18 is genuinely complete.
5. Inspect the actual implementation of:
    - Gmail OAuth
    - Gmail synchronization
    - local database
    - parsing
    - classification
    - rules/corrections
    - temporal extraction
    - Action Engine
    - Integration Manager
    - multi-account support
    - account switching
    - unified inbox
    - current offline behavior
    - existing WorkManager/background infrastructure
6. Do not assume Phase 18's description accurately represents the current code. Verify it.

---

# 2. Strict Sequential Execution

This session is **Phase 19 only**.

Do not implement:

- Phase 20 Noise, Newsletter & Cleanup
- Phase 21 Waiting-for-Reply & Conversation Intelligence
- Phase 22 Gmail Modification & Optional Write Features
- Phase 23 Privacy Center & Security Hardening
- Phase 24 Performance & Battery Optimization as a separate optimization phase
- Phase 25 Analytics & Insights
- Phase 26 AI
- Phase 27 Advanced Automation
- Phase 28 Full Testing & QA as the dedicated final QA phase
- Phase 29/30 release work

Phase 19 may include the minimum performance/battery decisions necessary to make background sync safe, but do not turn this into a broad optimization project.

Execution:

```text
Read docs
↓
Verify Phase 18
↓
Inspect actual architecture
↓
Implement Phase 19 only
↓
Build
↓
Test
↓
Install
↓
Run
↓
Background execution tests
↓
Offline tests
↓
Multi-account tests
↓
Network transition tests
↓
Inspect logs
↓
Fix
↓
Rebuild
↓
Reinstall
↓
Retest
↓
Security/isolation review
↓
Update docs/rules
↓
STOP
```

---

# 3. Permanent Multi-Project Isolation

Multiple Android applications may exist in the same workspace.

First identify the Mail Organizer project root.

Never modify sibling projects.

Do not touch another project's:

- source
- Gradle files
- dependencies
- manifests
- resources
- tests
- assets
- SDK configuration
- JDK configuration
- signing configuration
- Git state
- OAuth configuration
- Google Cloud configuration
- generated files

Never build or clean another project.

Never change global Android/JDK/Gradle configuration merely to support Mail Organizer.

All ADB operations must target only Mail Organizer's package.

---

# 4. Android Tooling Requirement

Use the actual Android development environment.

Required where applicable:

- project `./gradlew`
- ADB
- emulator or physical Android device
- install
- launch
- force-stop
- logcat
- dumpsys
- package inspection
- screenshots
- screen recording
- network-state inspection
- battery/background restriction inspection
- database inspection

Use `adb reverse` only if genuinely required by Mail Organizer.

Do not disturb other applications' ADB configuration.

---

# 5. Core Background-Sync Principle

Background sync must be:

- account-aware
- idempotent
- resumable
- cancellable
- network-aware
- battery-conscious
- rate-limit-aware
- failure-tolerant
- observable
- safe after process death
- safe after device reboot

Do not create an always-running service merely to keep Mail Organizer synchronized.

Prefer Android-supported background execution mechanisms.

Use WorkManager or the existing appropriate architecture where possible.

---

# 6. Sync Architecture

Establish a clear separation:

```text
Background scheduler
        ↓
Sync coordinator
        ↓
Account sync worker
        ↓
Gmail synchronization client
        ↓
Normalization
        ↓
Local database
        ↓
Local processing pipeline
```

The scheduler must not contain Gmail business logic.

The Gmail client must not decide Android scheduling policy.

The database must not know about WorkManager.

Maintain clean boundaries.

---

# 7. WorkManager / Background Execution

If WorkManager is already used, inspect and improve it rather than creating a duplicate scheduler.

If it is not used and is appropriate, introduce it.

Background work must survive:

- app process death
- application restart
- reasonable device lifecycle changes
- reboot where supported/configured

Do not promise background execution that Android cannot guarantee.

---

# 8. Account-Scoped Background Work

Every sync job must know which account it belongs to.

Example:

```text
Account A
↓
Sync Work A

Account B
↓
Sync Work B
```

Do not create one ambiguous global job that can accidentally use the wrong credentials.

---

# 9. Unique Work

Prevent duplicate sync jobs for the same account.

Use account-specific unique work identifiers.

Conceptually:

```text
mail-organizer-sync-{accountId}
```

Do not hard-code a single:

```text
mail-organizer-sync
```

if doing so causes multiple accounts to collide.

---

# 10. Multi-Account Concurrency

Multiple accounts may synchronize independently when safe.

Example:

```text
Account A → syncing
Account B → syncing
```

should not inherently block each other.

However, prevent:

```text
Account A → sync
Account A → duplicate sync
```

from executing concurrently when unsafe.

---

# 11. Sync Coordination

The SyncCoordinator should determine:

- which accounts need synchronization
- whether an account is connected
- whether credentials are valid
- whether network requirements are met
- whether another sync is running
- whether retry is appropriate
- whether the account is paused
- whether the app/device is offline

It must not blindly sync every account on every trigger.

---

# 12. Sync Triggers

Define supported triggers clearly.

Possible triggers:

- periodic background sync
- app launch
- app resume
- manual refresh
- connectivity recovery
- successful OAuth connection
- account reauthentication
- retry after transient failure
- device reboot where appropriate

Do not create redundant syncs for every event.

---

# 13. Periodic Sync

Use Android-supported periodic work.

Respect platform minimum intervals and scheduling constraints.

Do not attempt to bypass Android background execution restrictions.

Do not implement:

```text
every 10 seconds
every minute
```

background polling.

The system should tolerate the fact that Android controls the exact execution time of periodic work.

---

# 14. Manual Refresh

Manual refresh must:

- trigger account-scoped sync
- provide visible progress/state
- prevent duplicate concurrent sync
- support cancellation where appropriate
- return to a consistent UI state
- report errors honestly

Do not show "Synced successfully" before the operation actually succeeds.

---

# 15. Network Constraints

Background Gmail synchronization should require an appropriate network state.

At minimum distinguish:

```text
Connected
Disconnected
Possibly metered
```

Do not automatically consume excessive mobile data.

Where appropriate, make the sync policy configurable.

---

# 16. Offline Detection

The app must distinguish:

```text
Offline
```

from:

```text
Gmail account disconnected
```

and:

```text
Gmail authentication expired
```

and:

```text
Gmail API temporarily unavailable
```

These are different states.

Do not display:

> "Reconnect Gmail"

when the phone is simply offline.

---

# 17. Offline-First UI

When offline, the app should continue showing locally cached information.

The following should remain usable where data exists:

- Home
- Mail
- Threads
- Email detail
- Categories
- Companies
- Actions
- Search
- Rules
- account switching
- unified inbox

Do not replace the entire application with an offline error screen.

---

# 18. Offline Banner / Status

Communicate offline state clearly but calmly.

For example:

```text
Offline
Showing saved mail
```

or equivalent design-compliant UI.

Follow `design.md`.

Do not use alarming language for ordinary connectivity loss.

---

# 19. Last Sync Information

Where useful, display:

```text
Last synced:
10 minutes ago
```

or:

```text
Last synced:
Today at 10:42 AM
```

The timestamp must come from actual sync state.

Do not fabricate it.

For unified views, account-specific sync information should remain available where relevant.

---

# 20. Per-Account Sync Status

Each account must maintain its own sync state.

Possible states:

```text
NEVER_SYNCED
SYNCING
SYNCED
PARTIAL
OFFLINE
AUTH_REQUIRED
PERMISSION_REQUIRED
RATE_LIMITED
TRANSIENT_ERROR
PERMANENT_ERROR
PAUSED
```

Use the existing architecture where possible.

Do not create redundant competing status models.

---

# 21. Unified Sync Status

If multiple accounts exist, the unified UI should communicate aggregate state appropriately.

Example:

```text
2 accounts connected
1 up to date
1 needs attention
```

Do not label the entire system "synced" if one account failed.

---

# 22. Partial Sync

A sync can succeed partially.

Examples:

- some pages succeeded
- some messages failed
- API quota interrupted processing
- network dropped mid-sync
- process died during sync

Do not discard all successful progress unnecessarily.

Persist safe progress.

---

# 23. Resumability

Sync must resume safely after interruption.

Persist appropriate state such as:

- history cursor
- page token where safe
- sync checkpoint
- last successful operation
- sync version
- timestamps

Do not persist transient secrets.

Do not assume a process will remain alive for the entire sync.

---

# 24. Idempotency

Repeated background sync must not create:

- duplicate messages
- duplicate threads
- duplicate senders
- duplicate classification records
- duplicate action candidates
- duplicate temporal records

Use stable Gmail IDs and existing database uniqueness constraints.

---

# 25. Sync Transactions

Use appropriate transactions.

A partially completed database update must not leave impossible state such as:

```text
message exists
but required account relationship does not
```

or:

```text
thread belongs to Account A
message belongs to Account B
```

unless explicitly valid by the data model.

---

# 26. Sync Cursor Safety

History/cursor state must be account-specific.

Example:

```text
Account A → history cursor A
Account B → history cursor B
```

Never share cursor state.

If a cursor becomes invalid:

1. detect it
2. mark the state appropriately
3. perform safe recovery
4. rebuild required local state if necessary
5. do not silently claim the mailbox is synchronized

---

# 27. Recovery from Invalid History

If Gmail history cannot be continued safely:

```text
history cursor invalid
↓
safe recovery path
↓
resynchronize required mailbox state
↓
persist new cursor
```

Do not blindly advance the cursor after an error.

Do not lose messages because recovery was implemented incorrectly.

---

# 28. Retry Policy

Define retry categories.

### Retryable

Examples:

- temporary network failure
- timeout
- temporary server error
- transient API availability issue

### Non-retryable

Examples:

- revoked authorization
- invalid credentials
- unsupported request
- permanent permission failure

Do not retry authentication failures indefinitely.

---

# 29. Exponential Backoff

Use controlled backoff for transient failures.

Avoid:

```text
retry immediately forever
```

Use bounded retries.

Respect provider/platform constraints.

---

# 30. Rate Limits

Respect Gmail API quotas and rate limits.

Do not implement aggressive parallel requests merely to make sync appear faster.

Use:

- bounded concurrency
- pagination
- batching where appropriate
- backoff
- cancellation

Do not make unnecessary Gmail API requests.

---

# 31. Battery Awareness

Background synchronization must be conservative.

Avoid:

- wake locks unless absolutely necessary
- persistent foreground services without a valid need
- continuous polling
- excessive retries
- unnecessary network requests
- repeated database rescans

Do not optimize prematurely at the cost of reliability.

---

# 32. Sync Scheduling Policy

Document:

- periodic interval
- network constraint
- retry policy
- backoff
- account selection
- manual refresh behavior
- connectivity recovery
- reboot behavior
- authentication failure behavior

The policy must be understandable to future maintainers.

---

# 33. Manual Sync vs Background Sync

Manual sync may have stricter immediacy expectations than background sync.

Example:

```text
Manual refresh
→ execute as soon as platform permits
```

while:

```text
Background refresh
→ platform-controlled scheduling
```

Do not pretend WorkManager periodic execution is exact.

---

# 34. App Launch Sync

On app launch:

- show cached data immediately
- determine whether sync is needed
- schedule/trigger appropriate synchronization
- do not block the entire UI waiting for Gmail

The user should be able to interact with local data while synchronization occurs.

---

# 35. App Resume

Do not sync on every lifecycle callback.

Use a sensible freshness check.

Example:

```text
resume
↓
is data stale enough?
↓
yes → schedule/trigger sync
no → continue
```

Avoid excessive requests.

---

# 36. Connectivity Recovery

When network connectivity returns:

- identify accounts needing sync
- avoid duplicate workers
- schedule appropriate work
- respect authentication state
- respect battery/network constraints

Do not immediately hammer the Gmail API for every account.

---

# 37. Device Reboot

Verify background scheduling behavior after device reboot where supported.

Do not assume every scheduled job survives reboot automatically.

Use platform-supported mechanisms.

---

# 38. Background Processing Pipeline

After synchronization, local processing may include:

```text
Gmail data
↓
normalize
↓
parse
↓
sender/company
↓
classification
↓
priority
↓
Action Required
↓
rules/corrections
↓
temporal extraction
↓
action candidate refresh
↓
search index update
```

Respect existing phase boundaries.

Do not reimplement these engines.

---

# 39. Processing Idempotency

Background processing must be safe to run repeatedly.

A message being processed twice must not create duplicate:

- classifications
- companies
- rules
- action candidates
- temporal events
- search index entries

---

# 40. Processing Failures

One malformed email must not stop the entire background pipeline.

For example:

```text
1000 messages
↓
message 431 fails parsing
↓
continue processing other messages
```

Record safe diagnostic information.

Do not log the full email body.

---

# 41. Local-Only Processing

Background processing should remain local.

Do not send:

- email bodies
- sender data
- attachments
- user rules
- classification data

to an external service merely because background processing is occurring.

No AI service should be introduced in Phase 19.

---

# 42. Database Access

Background workers must use the same repositories/use cases as appropriate.

Do not create a second database access layer only for background execution.

Avoid bypassing domain rules.

---

# 43. Account Isolation in Workers

Every worker must explicitly carry account context.

Verify:

```text
worker accountId
=
credential accountId
=
sync state accountId
=
database query accountId
```

A worker must never "pick whichever account is active."

Background work runs independently of UI state.

---

# 44. Critical Rule: Active Account Must Not Control Background Sync

Never do:

```text
background worker
↓
read current UI account
↓
sync that account
```

The background worker must use its own persisted account identifier.

The active UI account may change while the worker runs.

---

# 45. Multi-Account Scheduling

Test scenarios: