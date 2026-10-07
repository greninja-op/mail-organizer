# Phase 17 — Integration Manager

## Mission

Implement the **Integration Manager** for Mail Organizer.

Phases 3, 15, and 16 established separate integrations for:

- Google Gmail
- Google Calendar
- Google Tasks

Phase 17 now provides a coherent application-level system for managing those integrations.

The Integration Manager must answer:

- Which integrations are available?
- Which Google account is each integration connected to?
- Which permissions are granted?
- Which integrations are disconnected or unavailable?
- Which integrations need reauthorization?
- Which account is associated with an integration?
- What capabilities does each integration provide?
- How can the user connect, reconnect, configure, or disconnect an integration safely?

The architecture must remain modular.

The Integration Manager is **not** a replacement for the individual Gmail, Calendar, or Tasks integrations.

The intended architecture is:

```text
Integration Manager
        │
        ├── Gmail Integration
        │
        ├── Calendar Integration
        │
        └── Tasks Integration
```

The Integration Manager coordinates them.

It must not absorb their API-specific logic.

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
    - this Phase 17 prompt
    - `docs/development-status.md` if available
4. Verify Phase 16 is genuinely complete.
5. Inspect the actual implementations of:
    - Gmail OAuth
    - Gmail integration
    - Calendar integration
    - Tasks integration
    - account management
    - Action Engine
    - database
    - Settings
    - existing integration UI
6. Identify duplicated connection/account/permission logic that should be consolidated without breaking existing integrations.

Do not rewrite working integrations unnecessarily.

---

# 2. Strict Sequential Execution

This session is:

> **Phase 17 only.**

Do not implement:

- Phase 18 multi-account redesign
- Phase 19 background sync redesign
- Phase 20 cleanup system
- Phase 21 conversation intelligence
- Phase 22 Gmail write features
- Phase 23 privacy center
- Phase 26 AI
- Phase 27 advanced automation

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
Connect/Disconnect testing
↓
Account isolation testing
↓
Permission testing
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

Multiple Android projects may exist in the workspace.

Identify the Mail Organizer project root before editing.

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
- Google Cloud project
- Git repository
- generated files

Never build or clean another project.

Never install/uninstall another project's application.

Never modify global Android/JDK/Gradle configuration merely to support Mail Organizer.

---

# 4. Android Tooling Requirement

Use the complete Android validation workflow.

Use:

- project Gradle wrapper
- ADB
- package inspection
- device/emulator inspection
- install
- launch
- force-stop
- logcat
- screenshots
- screen recording where useful
- dumpsys
- database inspection where available

All ADB operations must target only Mail Organizer.

Use `adb reverse` only if required.

Do not disturb mappings belonging to other projects.

---

# 5. Core Architecture

The Integration Manager must sit above individual integrations.

Use a structure conceptually similar to:

```text
IntegrationManager
        ↓
IntegrationRegistry
        ↓
IntegrationAdapter
        ├── GmailAdapter
        ├── CalendarAdapter
        └── TasksAdapter
```

The exact implementation should follow the existing architecture.

Do not force all integrations into identical APIs when their capabilities genuinely differ.

---

# 6. Integration Contract

Define a common integration contract where useful.

Possible capabilities:

```text
integrationId
displayName
provider
accountId
status
capabilities
permissions
connect()
disconnect()
refreshStatus()
```

Do not expose Google API classes to the presentation layer.

The UI should interact with application/domain-level integration models.

---

# 7. Integration Status Model

Create a deterministic state model.

Possible states:

```text
AVAILABLE
CONNECTED
CONNECTING
DISCONNECTED
AUTH_REQUIRED
PERMISSION_REQUIRED
PERMISSION_REVOKED
ERROR
OFFLINE
UNAVAILABLE
```

Use only states that are meaningful for the actual architecture.

Do not report:

unless the integration is genuinely usable.

---

# 8. Capability Model

The Integration Manager should understand what each integration can do.

Examples:

### Gmail

```text
READ_EMAIL
SYNC_EMAIL
```

Later capabilities may include:

```text
SEND_EMAIL
```

but those must not be enabled in Phase 17 unless already legitimately implemented.

### Calendar

```text
READ_CALENDAR_METADATA
CREATE_EVENT
```

### Tasks

```text
READ_TASK_LISTS
CREATE_TASK
```

Do not advertise unsupported capabilities.

---

# 9. Capability vs Permission

Keep these concepts separate:

```text
Capability
=
what Mail Organizer can do
```

and:

```text
Permission
=
what Google has authorized
```

Example:

```text
Capability = CREATE_EVENT

Permission = not granted
```

The UI should communicate:

```text
but permission required
```

rather than falsely saying the feature is unavailable.

---

# 10. Integration Registry

Create a central registry if appropriate.

Conceptually:

```text
Gmail
Calendar
Tasks
```

The registry should make it possible to:

- discover integrations
- query status
- query capabilities
- connect
- disconnect
- refresh state

Do not hard-code integration logic throughout the Settings screens.

---

# 11. Gmail Integration Preservation

Gmail is the core data source.

Do not break existing:

- OAuth
- Gmail synchronization
- local data
- account state
- read-only permissions

The Integration Manager must treat Gmail as foundational.

Do not introduce Gmail write permissions during this phase.

---

# 12. Calendar Integration Preservation

The existing Calendar integration must continue to support:

- authorization
- account identification
- calendar selection
- event creation
- error handling
- duplicate prevention

Do not rewrite its API layer merely to introduce the Integration Manager.

Wrap or adapt it where necessary.

---

# 13. Tasks Integration Preservation

The existing Tasks integration must continue to support:

- authorization
- account identification
- task-list selection
- Task creation
- duplicate prevention
- error handling

Again, use adapters/interfaces rather than rewriting working API code.

---

# 14. Account Association

Every integration must expose its associated Google account safely.

Example:

```text
Gmail
personal@gmail.com

Calendar
personal@gmail.com

Tasks
personal@gmail.com
```

A different configuration is also valid:

```text
Gmail
personal@gmail.com

Calendar
work@gmail.com
```

if the product supports it.

Do not assume all integrations must always use the same account.

The association must be explicit.

---

# 15. Account Isolation

Never allow:

```text
Account A Gmail
        ↓
Account B Calendar
```

unless the user explicitly selected Account B as the Calendar destination and the action is clearly associated with it.

Integration state must always carry account identity.

---

# 16. Integration Connection Flow

Create a coherent connection experience.

Example:

```text
Google Calendar
Not connected

[Connect]
```

After tapping:

```text
Authorization
↓
Account selection
↓
Permission
↓
Calendar selection
↓
Validation
↓
Connected
```

Do not hide authorization failures.

---

# 17. Reconnection

Support safe reconnection.

Example:

```text
Calendar
Permission expired

[Reconnect]
```

Reconnection must not:

- delete local email
- delete action cards
- reset Gmail
- reset Tasks
- modify unrelated integrations

---

# 18. Disconnect

Provide safe disconnect behavior.

When disconnecting:

```text
Calendar
```

remove or invalidate the local integration credential/state as appropriate.

Do not delete:

- Gmail messages
- local classifications
- user rules
- action history
- unrelated integration data

unless explicitly required.

Do not automatically delete Calendar events created previously.

---

# 19. Disconnect Confirmation

If disconnecting has meaningful consequences, communicate them clearly.

Example:

```text
Disconnect Google Calendar?

Mail Organizer will no longer be able to:
• add events to your Calendar

Your existing Calendar events will not be deleted.

[Cancel]
[Disconnect]
```

Use precise language.

---

# 20. Integration Settings UI

Create a coherent Integration section.

Possible layout:

```text
Integrations

Google
────────────────

Gmail
Connected
personal@gmail.com

Google Calendar
Connected
personal@gmail.com
Calendar: Personal

Google Tasks
Not connected
[Connect]
```

Follow `design.md`.

Do not create a generic settings wall.

---

# 21. Integration Detail Screen

Each integration should provide:

- provider
- status
- account
- capabilities
- selected calendar/task list where applicable
- connection state
- reconnect
- disconnect
- relevant configuration

Do not expose raw API IDs.

---

# 22. Permission Explanation

Users should understand why permissions are requested.

Example:

```text
Google Calendar

Mail Organizer uses Calendar access only when you choose
to add an email-based event to your Calendar.

It does not automatically create events.
```

Similarly:

```text
Google Tasks

Mail Organizer uses Tasks access when you explicitly
choose to create a Task from an email action.
```

Do not make false privacy/security claims.

---

# 23. Least Privilege

The Integration Manager must not request additional permissions simply because an integration exists.

Each integration should expose only the permissions required by its actual capabilities.

Do not request:

- Gmail modify
- Gmail send
- Drive
- Contacts
- Location
- unrelated Google APIs

unless a later phase explicitly requires them.

---

# 24. Permission State

Maintain enough state to distinguish:

```text
Not connected
```

from:

```text
Connected but permission missing
```

from:

```text
Connected and operational
```

Do not rely solely on a locally stored boolean.

Where necessary, validate the integration.

---

# 25. Integration Health Check

Create a lightweight status validation mechanism.

For example:

```text
Refresh integration status
```

may validate:

- credentials
- permission
- account
- required API availability

Do not perform expensive network checks continuously.

Do not poll APIs unnecessarily.

---

# 26. Offline Integration State

When offline:

```text
Calendar
Connected previously
Offline
```

is different from:

```text
Calendar
Disconnected
```

Do not incorrectly mark a healthy integration as disconnected merely because the device currently lacks internet access.

---

# 27. Integration Availability

An integration can be unavailable because:

- device offline
- Google service unavailable
- permission revoked
- OAuth expired
- configuration missing
- API unavailable
- account removed

Represent these conditions accurately.

Do not collapse every failure into:

if the user can reasonably recover differently.

---

# 28. Action Engine Integration

The Integration Manager should provide the Action Engine with capability information.

Example:

```text
Action:
ADD_TO_CALENDAR

Integration Manager:
Calendar connected
CREATE_EVENT available

→ Show action
```

If disconnected:

```text
Calendar not connected

→ Show:
Connect Calendar
```

Do not allow the Action Engine to directly inspect OAuth credentials.

---

# 29. Confirmation Boundary Preservation

The Integration Manager must not bypass Phase 14's confirmation boundary.

This remains mandatory:

```text
Action suggestion
↓
User review
↓
Explicit confirmation
↓
Integration execution
```

The Integration Manager cannot autonomously execute external actions.

---

# 30. Integration Executor Routing

Where appropriate, centralize routing:

```text
Action
 ↓
Integration Manager
 ↓
Correct integration
 ↓
Executor
```

Example:

```text
CREATE_TASK
 ↓
Google Tasks
```

and:

```text
CREATE_EVENT
 ↓
Google Calendar
```

Do not allow action cards to directly instantiate Google API clients.

---

# 31. Error Normalization

Individual integrations may return different errors.

The Integration Manager may normalize them into application-level categories:

```text
AUTH_REQUIRED
PERMISSION_DENIED
ACCOUNT_MISMATCH
OFFLINE
RATE_LIMITED
SERVICE_UNAVAILABLE
INVALID_REQUEST
NOT_FOUND
UNKNOWN
```

Do not erase useful underlying information.

Keep provider-specific details available for diagnostics.

---

# 32. Retry Policy

Do not let each screen implement its own retry logic.

Define sensible application-level retry semantics.

For example:

```text
OFFLINE
→ wait/retry later

AUTH_REQUIRED
→ ask user to reconnect

INVALID_REQUEST
→ do not retry automatically

RATE_LIMITED
→ backoff
```

Do not retry external side effects blindly.

---

# 33. Integration Events

If useful, define internal events such as:

```text
IntegrationConnected
IntegrationDisconnected
PermissionChanged
AccountChanged
IntegrationError
```

These should be internal application events.

Do not introduce a heavyweight event bus unless the existing architecture needs it.

---

# 34. State Persistence

Persist integration configuration/state safely.

Possible local data:

- integration ID
- account ID
- connected state
- selected calendar ID
- selected task-list ID
- capability state
- timestamps
- configuration version

Do not store sensitive OAuth secrets in ordinary application database tables unless the existing secure credential architecture explicitly requires it.

---

# 35. Token Separation

Integration metadata and credentials must remain separate.

For example:

```text
Integration metadata
        ↓
Database

OAuth credentials
        ↓
Secure credential/token storage
```

Never put access tokens into:

- Room email entities
- Action Cards
- logs
- ordinary settings preferences
- Git

---

# 36. Account Removal

If a Google account is removed from Mail Organizer, integration state associated with that account must be handled safely.

Do not delete another account's integrations.

Do not delete unrelated local data.

This phase should establish the correct cleanup boundary for future multi-account work.

---

# 37. Integration Configuration Validation

Before showing an integration as fully connected, verify required configuration.

Example Calendar:

```text
OAuth valid
+
Calendar API available
+
account valid
+
selected calendar valid
=
Connected
```

Example Tasks:

```text
OAuth valid
+
Tasks permission
+
task list available
=
Connected
```

---

# 38. Calendar and Tasks Independence