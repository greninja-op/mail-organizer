# Phase 16 — Google Tasks Integration

## Mission

Implement the **Google Tasks Integration** for Mail Organizer.

This phase connects Mail Organizer's existing:

- Gmail account identity
- local email data
- Action Required intelligence
- user rules/corrections
- deadline extraction
- Action Engine
- Calendar integration architecture

to Google Tasks through an official Google API integration.

The goal is to allow users to turn appropriate Mail Organizer action candidates into Google Tasks **only after explicit confirmation**.

The core flow is:

```text
Email
 ↓
Intelligence
 ↓
Action Candidate
 ↓
Task Proposal
 ↓
User Review
 ↓
Explicit Confirmation
 ↓
Google Tasks API
 ↓
Verified Result
 ↓
Local Action State
```

The most important rule:

> **A detected action is not permission to create a Task.**

No Task may be created automatically because:

- an email was synchronized
- a deadline was detected
- Action Required became true
- an Action Card appeared
- the dashboard opened
- a background worker ran
- a rule matched

Only an explicit user confirmation may initiate an external Task creation.

---

# 1. Mandatory Instruction-Folder Discovery

Before making any change:

1. Identify the **Mail Organizer project root**.
2. Locate the instruction/documentation folder.
3. Read:
    - `requirements.md`
    - `spec.md`
    - `design.md`
    - `editor-rules.md`
    - this Phase 16 prompt
    - `docs/development-status.md` if present
4. Verify Phase 15 is genuinely complete.
5. Inspect the existing implementations of:
    - Gmail OAuth
    - Google Calendar OAuth
    - account management
    - Action Engine
    - Action Cards
    - user rules
    - temporal extraction
    - database
    - integration abstractions
    - network layer
    - navigation
    - settings/integration UI
6. Reuse existing patterns where appropriate.

Do not rebuild existing authentication or integration infrastructure unnecessarily.

---

# 2. Strict Sequential Execution

This session is for:

> **Phase 16 only.**

Do not implement:

- Phase 17 Integration Manager
- multi-account redesign
- Gmail write features
- cleanup automation
- newsletter unsubscribe
- advanced automation
- AI
- analytics
- production release work

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
OAuth validation
↓
Task API validation
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

Multiple Android applications may exist in the same workspace.

Identify the Mail Organizer root before editing.

Never modify another project.

Do not touch sibling projects':

- source
- Gradle
- wrapper
- dependencies
- manifests
- resources
- assets
- tests
- SDK settings
- signing configuration
- OAuth configuration
- Google Cloud configuration
- Git repositories
- generated files

Never build, clean, install, uninstall, or test sibling applications.

Never modify another project's:

- Google Cloud project
- OAuth clients
- APIs
- consent screen
- test users
- credentials

Global Android/JDK/Gradle settings must not be changed merely to support Mail Organizer.

---

# 4. Android Tooling Requirement

Use the full Android development and validation workflow.

Use:

- project Gradle wrapper
- ADB
- device/emulator inspection
- APK installation
- launch
- force-stop
- logcat
- screenshots
- screen recording
- dumpsys
- package inspection
- database inspection where available

Every ADB operation must target only Mail Organizer.

Use `adb reverse` only if genuinely required.

Do not disturb another project's ADB/network mappings.

---

# 5. Official Google Tasks API

Use the official supported Google Tasks API.

Do not use:

- scraping
- browser automation
- cookies
- session tokens
- AccessibilityService
- unofficial endpoints
- embedded website automation

The integration must use the appropriate supported Google authentication and API architecture.

---

# 6. OAuth Scope

Determine the minimum Calendar/Tasks permissions required by the official API.

Request only the Tasks permission necessary for the implemented functionality.

Do not request broad Google permissions merely for convenience.

Do not add:

- Gmail send
- Gmail modify
- Gmail delete
- unrelated Drive permissions
- unrelated Google services

to support Tasks.

Maintain least privilege.

---

# 7. Google Cloud Configuration

Tasks API configuration must belong exclusively to Mail Organizer.

Verify:

- correct Google Cloud project
- Tasks API availability/enabling
- correct Android package
- correct signing certificate
- correct OAuth client
- consent screen
- test users where applicable

Never modify another project's Google Cloud resources.

Never commit:

- client secrets
- access tokens
- refresh tokens
- authorization codes
- service-account credentials
- private keys

---

# 8. Existing Google Authentication

Inspect the existing Google account architecture from Phases 3 and 15.

Determine whether Tasks authorization can be added to the existing Google account securely.

Do not assume that:

```text
Gmail permission
```

automatically provides:

```text
Tasks permission
```

or that Calendar authorization automatically provides Tasks access.

Request and manage the appropriate permission explicitly.

Avoid unnecessary account disconnect/reconnect cycles.

---

# 9. Account Identity

Every Tasks operation must be tied to the correct Mail Organizer account.

Example:

```text
Mail Organizer account:
personal@gmail.com

Task operation:
personal@gmail.com
```

must never accidentally execute through:

```text
work@gmail.com
```

Do not rely on whichever Google account happens to be active on the Android device.

---

# 10. Multi-Account Safety

Validate:

```text
Mail account
↓
Google account
↓
OAuth credential
↓
Tasks account
↓
Task list
```

All must correspond to the intended account.

Never allow Account A's Action Card to create a Task in Account B.

---

# 11. Task Lists

Google Tasks supports task lists.

Do not automatically assume the first list is correct.

Retrieve the available task lists needed for the feature.

Represent:

- task list ID
- task list name
- selected/default state
- account identity

where necessary.

Provide an appropriate mechanism for the user to select the destination task list.

---

# 12. Task List Selection

A user may have multiple task lists.

The UI should allow an appropriate selection such as:

```text
Google Tasks

Personal
Work
College
```

Do not expose raw API IDs as the primary UI.

Do not automatically select a list merely because it appears first in the API response.

If the product has a sensible default list, document the selection policy.

---

# 13. Tasks Integration Architecture

Keep Tasks integration modular.

Recommended conceptual architecture:

```text
Presentation
    ↓
Application / Use Case
    ↓
Action Engine
    ↓
Tasks Action Adapter
    ↓
Tasks Repository / Client
    ↓
Google Tasks API
```

Keep Google API models separate from domain models.

Do not allow Google-specific generated classes to spread through the UI/domain layer.

---

# 14. Tasks Adapter

Create a dedicated integration boundary.

Conceptually:

```text
TasksIntegration
 ├── authenticate()
 ├── getTaskLists()
 ├── validateTask()
 ├── createTask()
 └── optionally update/delete where explicitly supported
```

Follow the project's existing architecture and naming conventions.

Do not blindly copy this interface.

---

# 15. Action Engine Integration

Connect suitable Phase 14 actions to Tasks.

Examples:

```text
COMPLETE_TASK
REVIEW_DEADLINE
SUBMIT
REGISTER
PAY
FOLLOW_UP
```

may potentially become:

```text
CREATE_TASK
```

But do not convert every Action Card automatically.

Only actions with enough information and a sensible task interpretation should offer:

```text
Add to Google Tasks
```

---

# 16. Task Proposal

Create a structured task proposal before making an API request.

Conceptually:

```text
TaskProposal
 ├── accountId
 ├── taskListId
 ├── title
 ├── notes
 ├── dueDate
 ├── sourceMessageId
 ├── sourceThreadId
 └── sourceActionId
```

Use only information actually available.

Do not invent task details.

---

# 17. Task Title

Create a deterministic title.

Possible hierarchy:

```text
Explicit action
↓
Deadline/action description
↓
Email subject
↓
Safe sender + action type
```

Examples:

```text
Submit internship application

Pay college application fee

Review interview preparation

Follow up with recruiter
```

Avoid:

```text
Task
Task
Task
```

when better information exists.

---

# 18. Task Notes

Task notes should contain only useful context.

Potentially:

```text
Source:
Mail Organizer

From:
Acme Recruiting

Subject:
Interview confirmation

Deadline:
18 October 2026
```

Do not copy the entire email body.

Do not include:

- OAuth credentials
- internal database IDs
- sensitive technical metadata
- unnecessary personal email content

If a source link exists, only include it if the application actually supports a valid safe link.

Never fabricate one.

---

# 19. Task Due Date

Use Phase 13 deadline intelligence.

Example:

```text
Application deadline:
18 October 2026
```

may become:

```text
18 October 2026
```

Only when the deadline is sufficiently certain.

Do not invent a due date.

---

# 20. Ambiguous Deadlines

If the email says:

```text
"Please submit this soon."
```

do not create a Task with an arbitrary date.

If:

```text
"Submit by Friday"
```

is safely resolvable, use the normalized deadline from Phase 13.

If not resolvable:

```text
dueDate = unknown
```

The task may still be proposed without a due date if that is meaningful.

---

# 21. Task Due-Time Limitation

Do not assume Google Tasks supports the same date/time semantics as Calendar.

If the extracted intelligence includes:

```text
18 October 2026 at 10:30 AM
```

but Tasks only supports the relevant due-date representation:

- preserve the exact time in notes if appropriate
- do not falsely represent a time as a Task-native due time
- do not silently discard meaningful information if it matters

Follow the official API's actual capabilities.

---

# 22. Meeting vs Task

Do not automatically turn every meeting into a Task.

For example:

```text
Interview scheduled for Monday
```

is primarily a Calendar event.

The appropriate action may be:

rather than:

A Task could be appropriate for:

only if the email provides an actual preparatory action.

Do not create redundant Tasks.

---

# 23. Deadline vs Task

A deadline is not automatically a Task.

Use action semantics.

Examples:

```text
"Applications close October 18."
```

may reasonably produce:

if the action is clear.

But:

```text
"Your subscription expires October 18."
```

may simply be information unless the user has an actionable obligation.

Avoid over-automation.

---

# 24. User Confirmation

Task creation requires explicit user confirmation.

Example:

```text
Add to Google Tasks?

Submit internship application

Due:
18 October 2026

Task list:
College

Source:
Career email from Acme

[Cancel]
[Add Task]
```

The action button must explicitly communicate the external effect.

Do not use vague labels such as:

```text
Okay
Done
```

for an operation that creates a Google Task.

---

# 25. No Automatic Task Creation

Never create Tasks automatically during:

- Gmail sync
- classification
- priority calculation
- Action Required detection
- rule processing
- temporal extraction
- dashboard rendering
- action generation
- background workers

Only explicit confirmation can trigger creation.

---

# 26. Duplicate Prevention

Duplicate Google Tasks are undesirable.

Create a deterministic identity strategy using appropriate local identifiers such as:

```text
accountId
taskListId
sourceActionId
sourceMessageId
task fingerprint
```

Persist the resulting Google Task ID.

Repeated processing must not create duplicate Tasks.

---

# 27. Retry Safety

Network failure may happen after Google creates a Task but before the application receives the response.

Do not blindly retry the creation request.

Use a safe recovery strategy such as:

- stored execution state
- matching task lookup where supported
- deterministic fingerprint
- local source relationship
- API-supported mechanisms where available

Document the chosen strategy.

---

# 28. Task Lifecycle

Integrate with Phase 14 action lifecycle.

Possible flow:

```text
SUGGESTED
↓
REVIEWED
↓
CONFIRMED
↓
EXECUTING
↓
COMPLETED
```

On failure:

```text
FAILED
```

Never mark:

until Google Tasks actually confirms creation.

---

# 29. No Fake Success

Never show:

```text
Task created
```

unless the Google Tasks API confirms successful creation.

If the integration is unavailable:

```text
Google Tasks isn't connected.
```

If the request failed:

```text
Couldn't create the task.
```

Do not pretend.

---

# 30. Task Completion vs Creation

This phase primarily concerns **creating Tasks from Mail Organizer actions**.

Do not implement full two-way Task synchronization unless explicitly required.

Do not automatically mark a Google Task completed because an email was read.

Do not infer completion from:

- Gmail read state
- category
- priority
- action dismissal

Those concepts are different.

---

# 31. Future Task Update/Delete Preparation

Store enough local information to support future:

- update
- completion
- deletion
- reconciliation

without reconstructing the relationship.

For example:

```text
Mail Organizer Action
        ↓
Google Task ID
        ↓
Task List ID
```

Do not implement full synchronization yet.

---

# 32. Account Switching

Test:

```text
Account A
↓
Create Task proposal
↓
switch account
↓
Account B
```

The proposal must remain tied to Account A.

Do not execute it with Account B credentials.

Require explicit account validation before external execution.

---

# 33. Task List Switching

If a user changes the selected Task list:

```text
Personal → College
```

ensure future actions use the correct list.

Do not silently move existing Tasks unless the user explicitly requests it.

---

# 34. Offline Behavior

When offline:

- existing local action cards remain visible
- task proposals may still be generated
- task preview may still be displayed
- no Google API call occurs
- confirmed external actions remain pending
- the app must clearly communicate that external execution is unavailable

Do not repeatedly retry while offline.

---

# 35. Permission Revocation

Test:

```text
Tasks permission granted
↓
user revokes access
↓
Mail Organizer tries to create Task
↓
API returns authorization failure
↓
integration reports permission issue
↓
user can reconnect
```

Do not crash.

Do not endlessly prompt.

---

# 36. Token Expiration

Test expired credentials.

The app should:

1. detect expiration
2. attempt the supported refresh mechanism
3. retry safely if appropriate
4. otherwise ask the user to reconnect

Never log the token.

Never expose credentials in UI.

---

# 37. OAuth Scope Changes

If the existing Google account authorization lacks Tasks permission:

- request the appropriate additional authorization
- preserve existing Gmail/Calendar functionality
- do not revoke unrelated permissions unnecessarily

If the user denies Tasks:

```text
Gmail continues working.
Calendar continues working.
Tasks remains unavailable.
```

Integration failure must not break the core application.

---

# 38. Task API Error Handling