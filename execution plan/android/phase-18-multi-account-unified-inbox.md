# Phase 18 — Multi-Account & Unified Inbox

## Mission

Implement the **Multi-Account & Unified Inbox** architecture for Mail Organizer.

Mail Organizer must support multiple Gmail accounts while maintaining strict account isolation.

The user should be able to:

- connect multiple Gmail accounts
- switch between accounts
- view one account independently
- optionally view a safe unified inbox
- search across accounts where explicitly supported
- see which account an email belongs to
- maintain account-specific rules and intelligence
- use Calendar/Tasks integrations against the correct account
- disconnect one account without damaging another

The central principle is:

```text
One Mail Organizer
        ↓
Multiple isolated accounts
        ↓
Each account owns its Gmail data + intelligence + integrations
        ↓
Optional unified presentation layer
```

Unified presentation must **never mean shared ownership**.

The system must prevent:

```text
Account A data
      ↓
Account B classification
      ↓
Account B action
```

and:

```text
Account A email
      ↓
Account B Calendar/Tasks
```

unless the user explicitly chooses a different destination account during a confirmed external action.

---

# 1. Mandatory Instruction-Folder Discovery

Before making changes:

1. Identify the **Mail Organizer project root**.
2. Locate the instruction/documentation folder.
3. Read:
    - `requirements.md`
    - `spec.md`
    - `design.md`
    - `editor-rules.md`
    - this Phase 18 prompt
    - `docs/development-status.md` if available
4. Verify Phase 17 is genuinely complete.
5. Inspect the existing:
    - account model
    - Gmail OAuth
    - Gmail sync
    - database
    - sender/company intelligence
    - classification
    - priority
    - Action Required
    - rules/corrections
    - temporal extraction
    - Action Engine
    - Calendar integration
    - Tasks integration
    - Integration Manager
    - navigation
    - Home/Dashboard
    - Search
6. Identify whether account isolation is already present and where it needs strengthening.

Do not assume multi-account support can be added by simply adding an `accountId` field.

---

# 2. Strict Sequential Execution

This session is:

> **Phase 18 only.**

Do not implement:

- Phase 19 Background Sync & Offline Behavior redesign
- Phase 20 Noise/Newsletter/Cleanup system
- Phase 21 Conversation Intelligence
- Phase 22 Gmail modification
- Phase 23 Privacy Center
- Phase 24 Performance Optimization
- Phase 25 Analytics
- Phase 26 AI
- Phase 27 Advanced Automation
- production release work

Future requirements may be documented but must not be implemented.

At completion:

```text
Implement
↓
Build
↓
Test
↓
Install
↓
Run
↓
Account isolation validation
↓
Multi-account validation
↓
Unified inbox validation
↓
External action routing validation
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
Documentation
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
- Gradle
- dependencies
- manifests
- resources
- assets
- tests
- SDK settings
- signing configuration
- OAuth configuration
- Google Cloud configuration
- Git state
- generated files

Never build, clean, install, uninstall, or test another project.

Do not change global Android/JDK/Gradle configuration merely to support Mail Organizer.

---

# 4. Android Tooling Requirement

Use:

- project Gradle wrapper
- ADB
- device/emulator inspection
- APK installation
- launch
- force-stop
- logcat
- screenshots
- screen recording where useful
- dumpsys
- package inspection
- database inspection

All ADB operations must target only Mail Organizer.

Use `adb reverse` only when genuinely required.

---

# 5. Account as a First-Class Domain Boundary

Account identity must be a first-class concept throughout the application.

Every account-owned object must be traceable to an account.

At minimum inspect:

```text
Account
Message
Thread
Sender
Company relationship
Classification
Priority
Action Required
User correction
Rule
Temporal extraction
Action candidate
Calendar relationship
Task relationship
Sync state
Search index
```

Do not assume account identity only belongs on the Gmail connection.

---

# 6. Account Model

The account model should contain enough information to distinguish accounts safely.

Potential fields:

```text
accountId
provider
emailAddress
displayName
status
createdAt
lastUsedAt
```

Use the existing schema where possible.

Do not store OAuth tokens directly in ordinary account rows.

---

# 7. Stable Account Identifier

Use a stable internal account identifier.

Do not use the email address as the sole database primary key unless the architecture has a compelling reason.

Email addresses can change.

Provider account identity should be handled appropriately.

The internal ID should remain stable across:

- sync
- classification
- rules
- actions
- integrations
- account switching

---

# 8. Account-Scoped Data

Every account-owned record must be scoped correctly.

Examples:

```text
Message.accountId
Thread.accountId
Sender.accountId
Rule.accountId
Action.accountId
SyncState.accountId
```

For entities that may legitimately be shared globally, document the reason.

Never make an entity globally shared accidentally.

---

# 9. Database Isolation

Database queries must include account scope where required.

Avoid unsafe queries such as:

```text
SELECT * FROM messages
```

when the caller intends to operate on one account.

Prefer:

```text
SELECT * FROM messages
WHERE accountId = :accountId
```

Use repository-level account scoping where appropriate.

Do not rely solely on UI filtering.

---

# 10. Defense in Depth

Account isolation must exist at multiple layers:

```text
UI
 ↓
ViewModel / Presentation
 ↓
Use Case
 ↓
Repository
 ↓
Database
```

A UI bug must not be able to expose another account's data.

Repository/domain operations should validate account ownership.

---

# 11. Cross-Account Object Validation

Before operating on an object, verify:

```text
requestedAccountId
        =
object.accountId
```

If not:

```text
reject operation
```

Do not silently switch accounts.

Do not "helpfully" find the object under another account.

---

# 12. Gmail Account Connections

Allow multiple Gmail accounts to coexist.

Example:

```text
Personal
personal@gmail.com

College
college@gmail.com

Work
work@gmail.com
```

The exact naming is user-controlled.

Do not infer account purpose from the email address.

---

# 13. Account Connection UI

The Accounts screen should allow:

- view connected accounts
- add account
- select active account
- switch account
- disconnect account
- view connection state
- identify integration status

Follow `design.md`.

Do not create a separate account management system outside the existing information architecture.

---

# 14. Account Switcher

Provide a clear account switcher.

It should communicate:

```text
Current account:
personal@gmail.com
```

and allow selection of another account.

Do not hide account context.

The account switcher should be available where account confusion could cause harm.

---

# 15. Active Account

Define an explicit active-account concept.

The active account controls:

- current Gmail view
- account-scoped search
- rules
- corrections
- sender/company views
- action views
- integrations where applicable

Do not store active account state in a way that can become inconsistent with the actual account registry.

---

# 16. Account Switching Safety

When switching:

```text
Account A
↓
switch
↓
Account B
```

ensure all relevant UI state is refreshed.

Do not retain:

- Account A message
- Account A rule
- Account A action
- Account A company
- Account A Calendar target

inside Account B context.

Clear or revalidate stale state.

---

# 17. Navigation State

Navigation must remain account-aware.

Example:

```text
Account A
→ Mail
→ Thread 123
```

Switching to Account B should not continue displaying:

as though it belongs to Account B.

Deep links and restored navigation state must include sufficient account context.

---

# 18. Unified Inbox

Implement an optional unified view.

The unified inbox is a **presentation layer**, not a new data store.

Conceptually:

```text
Account A messages
+
Account B messages
+
Account C messages
        ↓
Unified query
        ↓
Unified presentation
```

Do not duplicate messages into a separate unified database.

---

# 19. Unified Inbox Account Identity

Every message in the unified inbox must clearly communicate its source account.

Example:

```text
Acme Recruiting
Interview confirmation
10:30 AM

personal@gmail.com
```

or an equivalent compact account badge.

Do not rely only on sender name.

Two accounts may receive messages from the same sender.

---

# 20. Unified Inbox Sorting

Use a deterministic sort strategy.

Default should likely be:

```text
newest relevant email first
```

while preserving existing Mail Organizer priority/action signals where appropriate.

Document the ranking strategy.

Do not create an entirely separate classification engine for unified mode.

---

# 21. Unified vs Account View

Clearly distinguish:

```text
All Accounts
```

from:

```text
Personal
```

The user should never wonder whether they are seeing:

- one account
- all accounts
- a filtered subset

Display context clearly.

---

# 22. Unified Categories

Categories may be displayed across accounts.

For example:

```text
Career
24
```

may represent:

```text
college@gmail.com → 16
```

However, category definitions remain globally consistent while the underlying results remain account-owned.

Do not merge underlying account records.

---

# 23. Unified Companies

Company views may aggregate sender/company information across accounts.

But preserve account context.

Example:

```text
Acme

Personal account:
3 messages

College account:
8 messages
```

Do not accidentally treat two account-specific sender corrections as one global correction.

---

# 24. Sender Identity Across Accounts

The same email address may appear in multiple accounts.

Do not automatically assume that:

```text
sender@example.com
```

has one universal user-defined identity.

Account-specific intelligence must remain separate unless the architecture explicitly defines a safe global identity layer.

---

# 25. Company Identity

Company information may have a shared conceptual representation, but account-specific observations/corrections must remain scoped.

For example:

```text
Company:
Example Corp

Account A:
user correction → Career

Account B:
user correction → Important
```

must remain possible.

Do not flatten account-specific corrections into one global rule.

---

# 26. Rules and Corrections

Phase 12 rules remain account-scoped.

Example:

```text
Account A:
example.com → Career

Account B:
example.com → Promotions
```

must remain possible.

The unified inbox must display the effective result for each message according to its own account.

---

# 27. Classification

Classification itself may use the same deterministic engine across accounts.

But:

```text
classification input
+
account-specific rules/corrections
```

must produce the account-specific effective result.

Do not allow Account A's correction to alter Account B.

---

# 28. Priority and Action Required

Priority and Action Required must remain account-scoped.

The same sender may be:

```text
High priority in Account A
Low priority in Account B
```

if the account-specific context warrants it.

Unified views should display each message's effective result independently.

---

# 29. Temporal Intelligence

Meeting/deadline extraction must remain linked to the source account.

Example:

```text
Account A
Interview
Monday

Account B
Interview
Tuesday
```

Do not merge them merely because the sender or title matches.

---

# 30. Action Candidates

Every Action Candidate must retain:

- account ID
- source message ID
- source thread ID
- integration context where applicable

Unified Actions must never lose account identity.

Example:

```text
Submit application
personal@gmail.com
```

must not be confused with:

```text
Submit application
college@gmail.com
```

---

# 31. Calendar and Tasks Routing

External action routing must remain account-safe.

If an Action Card belongs to:

```text
personal@gmail.com
```

and the user chooses:

the system must determine the intended Calendar account explicitly.

Do not silently use:

or another account.

---

# 32. Cross-Account External Actions

Do not assume cross-account actions are allowed.

If the source email belongs to Account A but the user selects Account B's Calendar, the UI must clearly communicate:

```text
Source:
personal@gmail.com

Destination:
work@gmail.com

You are creating this Calendar event in Work.
```

Require explicit confirmation.

If the architecture does not safely support cross-account destinations, reject the operation.

---

# 33. Integration Manager Compatibility

Phase 17's Integration Manager must be updated to expose account associations correctly.

The manager should support:

```text
integration
+
accountId
```

rather than one global:

state.

---

# 34. OAuth Account Management

Adding another Google account must not overwrite existing credentials.

Each account needs separate secure authentication state.

Never:

```text
connect Account B
↓
replace Account A token
```

The credential architecture must support concurrent accounts safely.

---

# 35. Token Isolation

OAuth credentials must be isolated per account.

Never store:

```text
one global Google token
```

for multiple accounts.

Use the existing secure credential architecture appropriately.

Never expose tokens to UI/domain/database logs.

---

# 36. Gmail Sync Isolation

Each account must have independent sync state.

For example:

```text
Account A
history cursor A

Account B
history cursor B
```

Do not reuse:

- history IDs
- page tokens
- sync timestamps
- retry state
- failure state

across accounts.

---

# 37. Concurrent Sync

Multiple accounts may sync independently.

Do not create a global lock that unnecessarily prevents:

```text
Account A sync
+
Account B sync
```

where the architecture supports safe concurrency.

But prevent concurrent syncs for the same account where required.

---

# 38. Database Indexes

Ensure indexes exist for common account-scoped queries.

Examples:

```text
(accountId, timestamp)
(accountId, threadId)
(accountId, sender)
(accountId, category)
(accountId, priority)
(accountId, actionRequired)
```

Choose indexes based on actual query patterns.

Do not create dozens of unnecessary indexes.

---

# 39. Search Isolation

Phase 10 local search must become account-aware.

For account-specific search:

```text
search(accountId, query)
```

must return only that account.

For unified search:

```text
search(allAccounts, query)
```

must explicitly operate across all authorized accounts.

Never let unified search become the default accidentally if the user expects account-specific search.

---

# 40. Search Result Account Context

Unified search results must show the source account.

Example:

```text
"Interview"

Acme Recruiting
personal@gmail.com
```

and:

```text
"Interview"

Acme Recruiting
college@gmail.com
```

must remain distinguishable.

---

# 41. Search Ranking

Use the existing ranking model.

Account identity may be used as a filter/context, but do not create a completely different ranking engine.

Do not favor one account simply because it is active.

---

# 42. Dashboard

Home must clearly indicate account scope.

For example:

```text
All Accounts
```