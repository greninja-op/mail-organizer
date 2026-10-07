# Phase 14 — Action Cards & Action Engine

## Mission

Implement the **Action Cards & Action Engine** for Mail Organizer.

This phase transforms structured intelligence from earlier phases into **clear, actionable suggestions** while preserving strict user control.

The system should be able to recognize situations such as:

- reply needed
- follow-up needed
- application deadline approaching
- payment deadline approaching
- interview scheduled
- meeting scheduled
- registration deadline approaching
- information requiring user attention
- other supported action-oriented email situations

The Action Engine must determine:

> **What could the user reasonably do next?**

It must NOT automatically perform that action.

The core model is:

```text
Email
 ↓
Understanding
 ↓
Classification
 ↓
Priority
 ↓
Action Required
 ↓
Rules / Corrections
 ↓
Meeting / Deadline Intelligence
 ↓
Action Candidate
 ↓
Action Safety Validation
 ↓
Action Card
 ↓
User Review
 ↓
User Confirmation
 ↓
Future Action Executor
```

This phase establishes the action layer and safe execution architecture.

External integrations such as Google Calendar and Google Tasks are **not implemented in this phase**.

Gmail write operations are **not implemented in this phase**.

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
    - the Phase 14 prompt
    - `docs/development-status.md` if available
4. Verify that Phase 13 is genuinely complete.
5. Inspect the actual implementation of:
    - email model
    - classification
    - company/sender intelligence
    - priority
    - Action Required
    - user rules/corrections
    - temporal extraction
    - database
    - search
    - dashboard
    - navigation
6. Work with the existing architecture rather than creating duplicate systems.

If Phase 13 is incomplete, do not pretend it is complete.

---

# 2. Strict Sequential Execution

This session is for:

> **Phase 14 only.**

Do not implement:

- Google Calendar integration
- Google Tasks integration
- Gmail write operations
- automatic reply/send
- archive/delete
- automatic unsubscribe
- cleanup automation
- advanced automation
- AI
- analytics
- production release functionality

Those belong to later phases.

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
Inspect
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

Never touch sibling projects.

Do not modify another project's:

- source
- Gradle
- dependencies
- manifests
- resources
- tests
- assets
- SDK settings
- signing configuration
- Git repository
- generated files

Never build or clean another project.

Never install/uninstall another project's APK.

Never modify global Android/JDK/Gradle configuration merely to support Mail Organizer.

---

# 4. Android Tooling Requirement

Use the full Android development and validation workflow.

Use:

- project-local Gradle wrapper
- ADB
- emulator/device inspection
- APK installation
- launch
- force-stop
- logcat
- screenshots
- screen recording where useful
- dumpsys/package inspection
- database inspection where available

All ADB commands must target only Mail Organizer.

Use `adb reverse` only if actually required.

Do not disturb another application's ADB configuration.

---

# 5. Core Architectural Principle

The Action Engine must be a separate application/domain layer.

Do not embed action generation inside:

- classifier
- priority engine
- parser
- Gmail sync
- Calendar adapter
- Tasks adapter

Use a clear separation:

```text
Normalized Email
        ↓
Intelligence Results
        ↓
Action Candidate Generator
        ↓
Action Policy / Safety Rules
        ↓
Action Candidate
        ↓
Action Card Model
        ↓
User Confirmation
        ↓
Action Executor Interface
```

The executor interface may exist in this phase.

Actual external executors belong to later phases.

---

# 6. Intelligence vs Action

Maintain this distinction:

```text
"Deadline detected"
```

is intelligence.

```text
"Submit application"
```

is an action suggestion.

```text
"Create task"
```

is a proposed external action.

```text
"Task created"
```

is an executed external action.

Phase 14 must primarily establish the first three.

The fourth is not implemented for external systems.

---

# 7. Action Candidate Model

Create a structured action model.

A conceptual model may contain:

```text
ActionCandidate
 ├── id
 ├── accountId
 ├── messageId
 ├── threadId
 ├── actionType
 ├── title
 ├── description
 ├── urgency
 ├── source
 ├── confidence
 ├── explanation
 ├── status
 ├── createdAt
 ├── updatedAt
 └── version
```

Use the existing project's architecture and database conventions.

Do not blindly copy this model if a better equivalent already exists.

---

# 8. Action Types

Define a controlled initial action vocabulary.

Possible action types include:

```text
REPLY
FOLLOW_UP
REVIEW
COMPLETE_TASK
SUBMIT
REGISTER
PAY
ATTEND_MEETING
ATTEND_INTERVIEW
ADD_TO_CALENDAR
CREATE_TASK
REVIEW_DEADLINE
REVIEW_DOCUMENT
```

Only implement action types that can be safely inferred from current intelligence.

Do not create dozens of speculative actions.

---

# 9. External Action Boundary

The Action Engine must distinguish:

```text
Internal action suggestion
```

from:

```text
External side effect
```

Examples of external side effects:

- send Gmail message
- modify Gmail labels
- create Calendar event
- create Task
- open external service and submit information

No external side effect may occur automatically.

All external actions require explicit user confirmation.

---

# 10. Action Safety Model

Every candidate should pass through a safety layer.

Conceptually:

```text
Candidate
 ↓
Source validation
 ↓
Account validation
 ↓
Required information validation
 ↓
Confidence validation
 ↓
External side-effect classification
 ↓
User confirmation requirement
 ↓
Safe Action Card
```

If important information is missing, the system should not fabricate it.

Example:

```text
"Create a calendar event"

but:
date = unknown
time = unknown
```

should not produce an executable Calendar action.

It may instead produce:

```text
Review event details
```

---

# 11. Action Safety Principle

The user must always understand:

1. what will happen
2. which email caused it
3. what data will be used
4. what external service will be affected
5. whether the action is reversible
6. what confirmation is required

No hidden external side effects.

---

# 12. Action Cards

Create a reusable Action Card component following `design.md`.

An Action Card should communicate:

```text
[Action type]

Title

Why this matters

Relevant date/deadline

Source email

Suggested action

[Review] [Act]
```

The exact UI should fit the existing design system.

Avoid turning the Home screen into a wall of cards.

Use:

- strong hierarchy
- restrained surfaces
- clear status
- concise copy
- appropriate category/status colors
- accessible controls

---

# 13. Action Card Information Hierarchy

Recommended hierarchy:

```text
1. What needs attention?
2. Why?
3. When?
4. From whom?
5. What can I do?
6. What happens if I press the action button?
```

Do not force users to read long explanations.

Use progressive disclosure for details.

---

# 14. Source Email

Every action card must identify its source.

Example:

```text
Interview scheduled
Monday · 10:30 AM IST

From:
Acme Recruiting

Source:
"Interview confirmation"
```

The user should be able to open the source email.

Do not duplicate the entire email body inside the action card.

---

# 15. Explanation

Each action candidate must have a structured explanation.

Example:

```text
Suggested because:
• Email is classified as Career
• Action Required = Yes
• Interview detected
• Interview is scheduled for tomorrow
```

Do not use AI-generated explanations.

Use deterministic structured reasons.

---

# 16. Confidence

Action suggestions should have confidence.

Use a controlled representation:

```text
HIGH
MEDIUM
LOW
UNKNOWN
```

Low-confidence candidates should generally not be presented as urgent actions.

If the system cannot safely infer an action, do not invent one.

---

# 17. Action Priority

Do not confuse:

```text
Email priority
```

with:

```text
Action priority
```

Action priority may use existing email priority and temporal urgency as signals.

For example:

```text
Critical deadline tomorrow
```

may rank above:

```text
Low-priority newsletter review
```

Do not rewrite the existing Phase 9 priority engine unnecessarily.

The Action Engine should consume its output.

---

# 18. Deadline-Based Actions

Use Phase 13 temporal intelligence.

Examples:

```text
Application deadline approaching
```

```text
Payment due Friday
```

```text
Registration closes tomorrow
```

The action candidate should include the relevant deadline.

Do not automatically create reminders or Tasks.

---

# 19. Meeting-Based Actions

Use Phase 13 meeting intelligence.

Examples:

```text
Interview tomorrow
```

```text
Meeting scheduled for Monday
```

Possible suggestions:

```text
Review meeting details
```

or:

```text
Add to Calendar
```

However:

> The Calendar action must remain a proposed action in Phase 14.

Do not call Google Calendar.

---

# 20. Reply / Follow-Up Actions

Use existing Action Required and conversation data carefully.

A possible candidate:

```text
Reply needed
```

must not automatically send anything.

At this stage:

```text
[Review Email]
```

may be appropriate.

Do not implement an AI-generated reply system.

Do not generate or send message content unless that capability already exists and is explicitly within the current scope.

---

# 21. Action Candidate Generation

Create a deterministic action-generation layer.

Example:

```text
ActionCandidateGenerator
```

Possible rules:

```text
Action Required + explicit reply request
    → REPLY candidate

Action Required + deadline
    → REVIEW_DEADLINE / COMPLETE_TASK candidate

Interview detected
    → ATTEND_INTERVIEW candidate

Meeting detected
    → ATTEND_MEETING candidate

Registration deadline
    → REGISTER candidate

Payment deadline
    → PAY candidate
```

These are examples, not permission to create unsafe automatic actions.

Every candidate must pass safety validation.

---

# 22. Avoid Duplicate Actions

One email should not produce a dozen identical cards.

Implement deterministic deduplication.

For example:

```text
messageId
actionType
target/reference
```

can form an idempotent action key.

Repeated synchronization must not create:

```text
Action
Action
Action
Action
Action
```

for the same underlying action.

---

# 23. Thread-Level Deduplication

Where multiple messages in the same thread refer to the same action, avoid duplicate cards.

Example:

```text
Interview scheduled
Interview reminder
Interview confirmation
```

may represent one logical action.

Use thread/message chronology where practical.

Do not implement full conversation intelligence from Phase 21.

Build a safe foundation.

---

# 24. Action Lifecycle

Define action status.

Possible states:

```text
SUGGESTED
REVIEWED
CONFIRMED
EXECUTING
COMPLETED
DISMISSED
FAILED
EXPIRED
CANCELLED
```

Not all states need external execution in this phase.

The architecture should support them.

---

# 25. Suggested vs Confirmed

Never treat:

```text
SUGGESTED
```

as:

```text
CONFIRMED
```

A suggestion must remain non-destructive.

The user must explicitly confirm an external action.

---

# 26. Dismissal

Users should be able to dismiss an action suggestion.

Example:

```text
Dismiss
```

Dismissal should not delete the source email.

It should only affect the action candidate.

Do not modify Gmail.

---

# 27. Expiration

Some action candidates become stale.

Example:

```text
Interview scheduled for yesterday
```

should not remain indefinitely as a current urgent action.

Derive expiration/status from:

- temporal data
- action state
- source message state

Do not silently delete historical action records.

---

# 28. Action History

Where useful, retain local action state.

This supports:

- dismissal
- completion
- auditability
- duplicate prevention
- future external execution

Do not build a full analytics system.

Do not send action history to a remote server.

---

# 29. User Rules Compatibility

Phase 12 rules may affect action candidates.

For example:

```text
User marked sender as:
No Action Required
```

must be respected.

The action layer must consume the **effective Action Required result**, not merely the base classifier output.

Likewise, user priority/category corrections should be reflected where relevant.

---

# 30. Account Isolation

Every action candidate must be account-scoped.

Never allow:

```text
Account A email
        ↓
Account B action
```

Store and validate account identity throughout:

- generation
- persistence
- display
- confirmation
- future execution

---

# 31. Confirmation Architecture

Design an explicit confirmation boundary.

Conceptually:

```text
Action Card
   ↓
Review
   ↓
Confirmation UI
   ↓
Action Executor
```

For Phase 14, the executor may be a stub/interface.

Do not implement external adapters yet.

The confirmation UI should explain:

```text
You are about to:
Create a Calendar event

From:
Interview confirmation

Date:
Monday

Time:
10:30 AM IST

Account:
example@gmail.com
```

Then:

```text
Cancel
Confirm
```

But pressing Confirm must not call Calendar during this phase.

If no executor exists, record the confirmed intent locally or present a clearly scoped not-yet-connected state.

Do not fake successful external execution.

---

# 32. No Fake Success

Never display:

```text
Calendar event created
```

unless a real Calendar integration has executed successfully.

Phase 14 must not pretend that future integrations already work.

Correct states include:

```text
Calendar integration not connected
```

or:

```text
Action prepared
External integration will be available in a later phase
```

depending on the UI architecture.

---

# 33. Action Executor Interface

Create an abstraction for future external actions.

Conceptually:

```text
interface ActionExecutor {
    supports(actionType)
    validate(action)
    execute(action)
}
```

The exact implementation should follow the project's language and architecture.

Do not add fake Calendar/Gmail implementations.

Future phases should be able to plug in:

- Calendar