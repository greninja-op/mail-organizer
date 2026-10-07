# Phase 22 — Gmail Modification & Optional Write Features

## Mission

Implement Mail Organizer's **optional Gmail write/modification capabilities** using the official Gmail API.

This is the first phase where Mail Organizer may intentionally modify Gmail data.

The system must support safe, explicit, account-scoped Gmail operations while preserving the product's core principles:

- user control
- least privilege
- explicit authorization
- clear consequences
- account isolation
- idempotency
- explainability
- reversible operations where possible
- destructive-action protection
- no silent automation

The central principle is:

> **Reading email can be background intelligence; modifying Gmail is an explicit user-authorized action.**

The architecture must never turn:

```text
classification
↓
recommendation
```

into:

```text
automatic Gmail mutation
```

without the required user authorization and confirmation.

---

# 1. Mandatory Instruction-Folder Discovery

Before making any changes:

1. Identify the **Mail Organizer project root**.
2. Locate the instruction/documentation folder.
3. Read:
    - `requirements.md`
    - `spec.md`
    - `design.md`
    - `editor-rules.md`
    - this Phase 22 prompt
    - `docs/development-status.md` if present
4. Verify Phase 21 is genuinely complete.
5. Inspect the existing implementation of:
    - Gmail OAuth
    - Gmail API client
    - account management
    - sync
    - local database
    - parser
    - classification
    - sender/company intelligence
    - priority
    - Action Required
    - rules
    - temporal extraction
    - Action Engine
    - Calendar
    - Tasks
    - Integration Manager
    - multi-account
    - background sync
    - offline behavior
    - cleanup/newsletter/noise
    - conversation intelligence
6. Determine exactly which Gmail write capabilities already have architectural placeholders.

Do not replace working architecture unnecessarily.

---

# 2. Strict Sequential Execution

This session is **Phase 22 only**.

Do not implement:

- Phase 23 Privacy Center & Security Hardening
- Phase 24 Performance & Battery Optimization
- Phase 25 Analytics & Insights
- Phase 26 Optional AI
- Phase 27 Advanced Automation
- Phase 28 Full Testing & QA as the dedicated final QA phase
- Phase 29 Production OAuth / Play Store Preparation
- Phase 30 Final Production Hardening

Phase 22 may create interfaces required by future phases, but must not implement future automation systems.

Execution:

```text
Read docs
↓
Verify Phase 21
↓
Audit existing Gmail permissions
↓
Design write authorization boundary
↓
Implement Gmail write adapter
↓
Implement safe action execution
↓
Build
↓
Test with controlled Gmail test account
↓
Install
↓
Execute safe operations
↓
Verify actual Gmail state
↓
Test failures/duplicates/account isolation
↓
Fix
↓
Rebuild
↓
Reinstall
↓
Retest
↓
Security review
↓
Update rules/spec/status
↓
STOP
```

---

# 3. Permanent Multi-Project Isolation

Multiple Android projects may exist in the same workspace.

Identify the Mail Organizer root before editing.

Never modify sibling projects.

Do not touch another project's:

- source
- Gradle files
- dependencies
- manifests
- resources
- tests
- SDK/JDK configuration
- signing configuration
- OAuth configuration
- Google Cloud configuration
- Git repository
- generated files

Never build, clean, install, uninstall, or test another project.

All ADB operations must target only Mail Organizer.

---

# 4. Android Tooling Requirement

Use the complete Android workflow:

- project `./gradlew`
- ADB
- install
- launch
- force-stop
- logcat
- screenshots
- screen recording
- dumpsys
- database inspection
- network-state inspection

Use `adb reverse` only if needed.

---

# 5. Gmail API Requirement

Use the official Gmail API.

Do not use:

- Gmail scraping
- browser automation
- AccessibilityService
- unofficial endpoints
- IMAP as a workaround
- cookie/session extraction
- password-based authentication

The Gmail API remains the authoritative interface.

---

# 6. Write Scope

Implement only the Gmail write capabilities justified by existing requirements and Action Engine architecture.

Potential operations include:

```text
MARK_READ
MARK_UNREAD
STAR
UNSTAR
ADD_LABEL
REMOVE_LABEL
ARCHIVE
MOVE_TO_TRASH
```

Potentially later:

```text
SEND
REPLY
FORWARD
```

Only implement send/reply/forward if the actual product requirements and existing architecture explicitly require them in this phase.

Do not add capabilities simply because the API supports them.

---

# 7. Permission Scope

Review current Gmail OAuth scopes.

If write operations require broader Gmail permission:

```text
https://www.googleapis.com/auth/gmail.modify
```

request it only when necessary.

Do not request:

```text
gmail.send
```

unless send/reply/forward functionality is actually being implemented.

Do not request unrelated Google scopes.

---

# 8. Least Privilege

Use the smallest permission set capable of implementing the selected write operations.

Document:

- current Gmail scopes
- why each is required
- which features depend on each scope

Do not request broad access merely for future possibilities.

---

# 9. OAuth Upgrade Flow

Users who previously connected with read-only Gmail access may need to grant broader permission.

Implement a clear upgrade flow.

Example:

```text
Gmail connected

Write actions require additional permission.

[Allow Gmail changes]
```

Do not silently request additional permissions.

---

# 10. Permission Explanation

Before requesting write access, explain:

- what Mail Organizer can do
- why permission is needed
- which Gmail data/actions are affected
- that the user controls execution

Avoid vague:

> "We need more permissions."

---

# 11. Account-Specific Authorization

OAuth permission state is account-specific.

If:

```text
Account A
write access granted

Account B
read-only
```

then:

- Account A may execute supported writes
- Account B must not

Never treat write authorization as global.

---

# 12. Integration Manager

Use the Phase 17 Integration Manager.

Gmail should expose capabilities such as:

```text
READ_EMAIL
MODIFY_EMAIL
```

and, only if implemented:

```text
SEND_EMAIL
```

Capabilities must reflect actual authorization.

Do not claim:

when OAuth permission is missing.

---

# 13. Gmail Adapter

Keep Gmail-specific API calls inside the Gmail integration layer.

Do not place Gmail API calls directly inside:

- UI
- ViewModel
- Compose screens
- generic Action Engine
- database repositories

Architecture:

```text
UI
↓
Action / Use Case
↓
Integration Manager
↓
Gmail Adapter
↓
Gmail API
```

---

# 14. Action Engine Boundary

The Action Engine generates candidate actions.

It does not directly execute Gmail mutations.

Example:

```text
Action Candidate
↓
User Confirmation
↓
Executor
↓
Integration Manager
↓
Gmail Adapter
↓
Gmail API
```

Preserve this separation.

---

# 15. Explicit Confirmation

Every Gmail write must require explicit user confirmation unless it is an operation that the product specification explicitly defines as safe and user-authorized through an established persistent setting.

For this phase, default to:

> **Require explicit confirmation.**

Do not automatically modify Gmail because:

- a message is low value
- a newsletter was detected
- Action Required is false
- a rule matched
- a conversation became stale
- background sync ran

---

# 16. Destructive vs Non-Destructive

Classify operations.

### Lower-risk

- mark read
- mark unread
- star
- unstar
- add label
- remove label

### Higher-risk

- archive
- move to trash
- permanent deletion where applicable
- send
- reply
- forward

Higher-risk actions require especially clear confirmation.

---

# 17. Trash vs Permanent Delete

Do not implement permanent Gmail deletion unless explicitly required.

Prefer:

```text
MOVE_TO_TRASH
```

where cleanup functionality requires removal.

Trash is safer and more recoverable than permanent deletion.

---

# 18. Archive Semantics

Clearly communicate that archive:

> removes the Inbox label while retaining the email in Gmail.

Do not describe archive as delete.

---

# 19. Mark Read/Unread

Mark-read operations must target the correct Gmail message/thread.

Do not mark an entire thread read if the user selected only one message unless that behavior is explicitly intended and clearly communicated.

Verify Gmail API semantics before implementation.

---

# 20. Star/Unstar

Star operations must preserve account and message identity.

Do not accidentally star/unstar another account's message.

---

# 21. Labels

Support Gmail labels only where required.

Before adding/removing a label:

- resolve the correct Gmail label ID
- account-scope the lookup
- distinguish system labels from user labels
- handle missing labels safely

Never assume label IDs are globally shared between accounts.

---

# 22. Label Creation

Do not automatically create large numbers of Gmail labels.

If label creation is implemented:

- require explicit user intent
- use deterministic naming
- avoid duplicate labels
- handle naming conflicts
- account-scope label creation

Do not create labels simply because Mail Organizer has categories.

---

# 23. Mail Organizer Categories vs Gmail Labels

Keep these concepts separate.

Mail Organizer:

```text
Career
Security
Action Required
```

are local intelligence categories.

Gmail labels are Gmail-side metadata.

Do not automatically mirror every Mail Organizer category into Gmail labels.

---

# 24. Local State After Write

After a successful Gmail mutation:

```text
Gmail API success
↓
update local representation
```

Do not update local state before confirmed remote success unless the operation uses a clearly implemented optimistic-state architecture with rollback.

For this phase, conservative synchronization is preferred.

---

# 25. Source of Truth

Gmail remains authoritative for Gmail state.

Example:

```text
Mark read
↓
Gmail confirms success
↓
local database reflects read
```

If the local update fails after Gmail succeeds, mark the local state as needing reconciliation rather than claiming failure of the Gmail operation.

---

# 26. Idempotency

Write operations must be safe against retries.

Example:

```text
Mark read
```

repeated should not create harmful duplicate effects.

For higher-risk actions, stronger duplicate prevention is required.

---

# 27. Action Identity

Create a stable execution identity where needed.

Potential:

```text
accountId
messageId/threadId
actionType
target
```

Do not execute the same high-risk action twice because of:

- screen rotation
- app restart
- network retry
- worker retry
- duplicate tap

---

# 28. Double-Tap Protection

UI must prevent accidental repeated execution.

After confirmation:

```text
Executing…
```

Disable the action appropriately until a result is known.

Do not allow rapid repeated taps to create duplicate operations.

---

# 29. Retry Safety

Not every failed write should be blindly retried.

Distinguish:

- network timeout
- server failure
- authentication failure
- permission failure
- invalid message
- already-completed state

For ambiguous results, reconcile with Gmail before retrying a high-risk operation.

---

# 30. Ambiguous Network Failure

Example:

```text
Gmail request sent
↓
network timeout
↓
unknown whether operation succeeded
```

Do not immediately repeat a dangerous action.

Use safe reconciliation where possible.

---

# 31. Reconciliation

If write result is uncertain:

1. fetch current Gmail state
2. compare with desired state
3. determine whether the operation already succeeded
4. only retry if necessary
5. update local state

Do not assume timeout means failure.

---

# 32. Offline Writes

Do not queue destructive Gmail actions for automatic background execution in Phase 22.

If offline:

```text
Gmail write unavailable
```

The UI may let the user review the action but should not pretend it succeeded.

Do not introduce an offline action queue unless explicitly required and safely designed.

---

# 33. Authentication Failure

If OAuth expires or is revoked:

```text
write
↓
AUTH_REQUIRED
```

Do not repeatedly retry.

Prompt the user to reconnect/reauthorize.

---

# 34. Permission Failure

If the user has read-only Gmail access:

```text
Modify email
↓
PERMISSION_REQUIRED
```

Provide an appropriate upgrade flow.

Do not crash.

---

# 35. Account Isolation

Every write must verify:

```text
action.accountId
=
target.accountId
```

and:

```text
credential.accountId
=
target.accountId
```

If not:

```text
reject
```

Never silently use another account.

---

# 36. Active Account Must Not Determine Write Target

Do not implement:

```text
action
↓
use current active account
```

The action itself must carry its source account.

The active account is UI state, not authorization.

---

# 37. Unified Inbox Write Safety

A unified inbox may show:

```text
personal@gmail.com
```

and:

```text
college@gmail.com
```

The write operation must target the selected item's account.

Never target whichever account happens to be active.

---

# 38. Cross-Account Actions

If the user intentionally wants:

```text
Account A email
→
Account B operation
```

do not allow it implicitly.

For Gmail modifications, default to source-account-only.

A cross-account write should be rejected unless the product explicitly defines a safe use case.

---

# 39. Cleanup Integration

Phase 20 cleanup candidates may now be connected to supported Gmail writes.

However:

```text
cleanup candidate
↓
user review
↓
explicit confirmation
↓
Gmail write
```

is required.

Never:

```text
↓
automatic deletion
```

---

# 40. Newsletter Integration

If the user selects a newsletter group:

```text
Review newsletters
```

they may choose supported operations such as:

- mark read
- archive
- move to trash

only after clear confirmation.

Do not automatically unsubscribe.

---

# 41. Conversation Integration

Phase 21 may generate:

- mark read
- archive
- other supported actions

but conversation intelligence must not execute them automatically.

---

# 42. Action Required Protection

Do not allow bulk cleanup operations to silently include:

```text
Action Required = YES
```

or:

without explicit warning and selection.

---

# 43. Security Email Protection

For:

- password resets
- security alerts
- account recovery
- suspicious login alerts
- verification emails

require stronger confirmation before destructive actions.

Consider blocking certain automatic/bulk operations entirely if the product requirements warrant it.

---

# 44. Receipts and Orders

Receipts/order messages may be valuable for future reference.

Bulk cleanup should clearly identify them.

Do not automatically trash them because they are old or automated.

---

# 45. Career and Education

Likewise protect:

- job applications
- recruiter communication
- interview emails
- admission
- coursework
- education notifications

from careless bulk mutation.

---

# 46. Bulk Operations

Support bulk Gmail modifications only where the operation is sufficiently safe and well-defined.

Potential:

```text
Select 20
↓
Mark read
```

or:

```text
Select 12 newsletters
↓
Archive
```

The confirmation must state:

- number of messages
- number of threads
- account
- exact operation
- whether reversible
- important exceptions

---

# 47. Bulk Account Scope

Never allow a bulk action to silently cross accounts.

If unified selection contains multiple accounts:

```text
Account A: 5
Account B: 7
```

the confirmation must show that clearly.

Prefer separate account-scoped executions.

---

# 48. Selection Safety

Selections must be invalidated or revalidated when:

- account changes
- sync changes the underlying message
- the message is no longer available
- the user leaves the screen
- the account disconnects

Never execute stale selection blindly.