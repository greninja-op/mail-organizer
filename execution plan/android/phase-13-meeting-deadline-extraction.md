# Phase 13 — Meeting & Deadline Extraction

## Mission

Implement the **Meeting & Deadline Extraction system** for Mail Organizer.

This phase adds deterministic, explainable extraction of structured temporal and scheduling information from already-normalized local email data.

The system should identify information such as:

- deadlines
- due dates
- meeting dates
- meeting times
- event dates
- appointment times
- application deadlines
- payment deadlines
- registration deadlines
- interview schedules
- submission deadlines
- time ranges
- timezone information
- location information
- meeting links
- cancellation/rescheduling language

The output of this phase is **structured intelligence**, not external action.

The system must NOT automatically:

- create Calendar events
- create Tasks
- send emails
- modify Gmail
- accept invitations
- reject invitations
- unsubscribe
- contact external services

Those actions belong to later phases.

The conceptual pipeline becomes:

```text
Gmail
   ↓
Synchronization
   ↓
Parsing / Normalization
   ↓
Sender / Company Intelligence
   ↓
Classification
   ↓
Priority
   ↓
Action Required
   ↓
Rules / User Corrections
   ↓
Meeting & Deadline Extraction
   ↓
Structured Temporal Intelligence
   ↓
Future Action / Calendar / Tasks layers
```

---

# 1. Mandatory Instruction-Folder Discovery

Before changing anything:

1. Identify the **Mail Organizer project root**.
2. Locate the project's instruction/documentation folder.
3. Read:
    - `requirements.md`
    - `spec.md`
    - `design.md`
    - `editor-rules.md`
    - the Phase 13 prompt
    - `docs/development-status.md` if present
4. Verify that Phase 12 has actually been completed.
5. Inspect the real implementation rather than assuming the architecture from the prompt.
6. Identify:
    - normalized email model
    - parsing pipeline
    - database
    - classification pipeline
    - rule engine
    - sender/company intelligence
    - priority engine
    - Action Required engine
    - search/index architecture
    - existing UI architecture

Do not proceed if Phase 12 is incomplete or falsely marked complete.

---

# 2. Strict Sequential Execution

This session is for:

> **Phase 13 only.**

Do not implement Phase 14 or later.

Do not prematurely implement:

- Action Engine
- Calendar integration
- Tasks integration
- Gmail writes
- cleanup automation
- advanced automation
- AI
- analytics
- production release functionality

Future requirements may be documented, but must not be implemented.

At the end:

```text
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

Never modify another project.

Do not touch sibling projects':

- source
- Kotlin/Java
- Compose/XML
- Gradle files
- wrapper
- settings
- manifests
- resources
- assets
- dependencies
- tests
- signing configuration
- SDK configuration
- Git state
- generated output

Do not build, clean, install, uninstall, or test sibling projects.

Do not change global Android/JDK/Gradle configuration just to support Mail Organizer.

---

# 4. ADB and Android Tooling

Android tooling remains mandatory.

Use the project-local Gradle wrapper.

When appropriate use:

- Gradle builds
- unit tests
- instrumentation tests
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
- database inspection

ADB commands must target only the Mail Organizer package.

Do not manipulate another application's package.

Only use `adb reverse` when actually required by Mail Organizer.

Never disturb another project's reverse mappings.

---

# 5. Core Architectural Principle

Meeting/deadline extraction is an **intelligence layer**, not an action layer.

Maintain the separation:

```text
Raw Gmail message
        ↓
Normalized email
        ↓
Temporal signal extraction
        ↓
Candidate temporal entities
        ↓
Validation / normalization
        ↓
Confidence evaluation
        ↓
Conflict resolution
        ↓
Structured temporal result
        ↓
Local persistence
        ↓
UI / Search / Future action layers
```

Do not let extraction directly trigger external side effects.

---

# 6. Local-First Requirement

All extraction must operate on data already available locally.

Do not send email content to:

- remote AI
- third-party NLP services
- external parsing APIs
- remote enrichment services

Do not introduce a network dependency for date/time extraction.

The system must continue functioning offline.

---

# 7. Deterministic First

The initial implementation must be deterministic and explainable.

Use structured parsing techniques such as:

- known date formats
- known time formats
- date ranges
- relative date expressions
- deadline phrases
- meeting phrases
- structured email metadata
- known calendar/event patterns
- normalized sender/context signals
- nearby textual context

Do not introduce an LLM merely because natural-language date extraction is difficult.

If some language is ambiguous, return:

```text
UNKNOWN
```

rather than inventing a date.

---

# 8. Temporal Entity Types

Define a controlled set of temporal entity types.

At minimum consider:

```text
DEADLINE
MEETING
APPOINTMENT
EVENT
INTERVIEW
SUBMISSION_DEADLINE
APPLICATION_DEADLINE
PAYMENT_DEADLINE
REGISTRATION_DEADLINE
REMINDER_DATE
DATE_ONLY
TIME_ONLY
DATE_TIME
DATE_RANGE
```

Do not create dozens of overlapping types without a clear purpose.

The model should be extensible.

---

# 9. Structured Temporal Model

Create an appropriate domain representation.

A temporal item should be capable of representing:

```text
type
startDateTime
endDateTime
dateOnly
timeOnly
timezone
timezoneSource
location
meetingUrl
title
sourceMessageId
sourceThreadId
confidence
explanation
extractionVersion
status
```

Only include fields that are actually needed by the architecture.

Do not store duplicated raw email bodies inside temporal entities.

---

# 10. Source Traceability

Every extracted temporal item must be traceable to its source.

At minimum retain:

- message ID
- thread ID where available
- extraction version
- relevant source context identifier

Where practical, retain the source text span or a safe reference to it.

Example:

```text
"Application closes on 18 October"
        ↓
SUBMISSION_DEADLINE
2026-10-18
```

The user should be able to understand where the information came from.

Do not expose sensitive internal parsing metadata unnecessarily in the normal UI.

---

# 11. Date Parsing

Support common date formats.

Examples include:

```text
18 October 2026
October 18, 2026
18/10/2026
18-10-2026
2026-10-18
Oct 18
18 Oct
```

Support formats appropriate to the project's target audience and locale.

Be careful with ambiguous formats such as:

```text
03/04/2026
```

Do not arbitrarily assume a locale when the meaning is genuinely ambiguous.

Use:

- email locale
- application locale
- explicit month names
- surrounding context

when available.

If ambiguity cannot be safely resolved:

```text
confidence = LOW
```

or retain the unresolved candidate.

---

# 12. Relative Dates

Support common relative expressions where safe.

Examples:

```text
tomorrow
next Monday
this Friday
next week
in 3 days
by tomorrow
within 48 hours
two weeks from now
```

Relative dates must be resolved against a clearly defined reference time.

Do not use the device's current time inconsistently.

Prefer:

```text
message received timestamp
```

or another explicitly defined contextual reference.

Document the selected reference-time policy.

---

# 13. Historical vs Future Dates

Do not assume every extracted date is an upcoming event.

Example:

```text
"The interview was held on June 4."
```

must not become an upcoming interview.

Use contextual language to determine whether the date refers to:

- past
- present
- future
- recurring
- unknown

Store the appropriate temporal status if useful.

---

# 14. Deadline Detection

Detect explicit deadline language.

Examples:

```text
deadline
due
due by
submit by
submission closes
applications close
last date
final date
must be completed by
complete before
register by
payment due
expires on
valid until
```

Do not treat every occurrence of a date as a deadline.

Example:

```text
"The course started on September 1."
```

is not a deadline.

Context must determine the semantic role.

---

# 15. Meeting Detection

Detect meeting/event language such as:

```text
meeting
call
conference
appointment
interview
session
webinar
discussion
sync
demo
orientation
appointment
```

Extract date/time only when the surrounding context supports the interpretation.

For example:

```text
"Interview scheduled for Monday at 10:30 AM"
```

should produce a structured interview event.

But:

```text
"We discussed our previous Monday meeting."
```

should not automatically become an upcoming meeting.

---

# 16. Time Parsing

Support common time formats:

```text
10:30 AM
10:30 a.m.
10 AM
22:30
10.30 AM
```

Normalize to a consistent internal representation.

Be careful with:

```text
12 AM
12 PM
```

Ensure midnight/noon are handled correctly.

Test them explicitly.

---

# 17. Timezone Handling

Timezone information is important.

Support explicit timezone expressions where available:

```text
IST
UTC
GMT
PST
EST
CET
Asia/Kolkata
```

Prefer explicit timezone information over device timezone assumptions.

Define a deterministic hierarchy such as:

```text
Explicit timezone in email
        ↓
Structured calendar metadata if available
        ↓
Known sender/event context if safely available
        ↓
User/application timezone
        ↓
Unknown
```

Do not silently invent a timezone when it is genuinely unknown.

Store the timezone source.

---

# 18. Indian Timezone Support

Because the product must support Indian users, explicitly test:

```text
IST
Asia/Kolkata
10:30 AM IST
10:30 AM India time
```

Do not hard-code India as the timezone for every user.

Use user/application/device timezone only as a fallback where appropriate.

---

# 19. Date Ranges

Support expressions such as:

```text
October 10–12
10 to 12 October
Monday to Wednesday
10/10/2026 - 12/10/2026
```

Represent:

```text
start
end
```

rather than creating three unrelated dates.

Validate that:

```text
end >= start
```

unless the semantics explicitly support something else.

---

# 20. Meeting Duration

When a duration is explicitly stated:

```text
10:00 AM–11:30 AM
```

represent the start and end.

If only:

```text
10:00 AM
```

is known, do not invent an end time.

Do not assume every meeting lasts:

```text
30 minutes
```

or:

```text
1 hour
```

unless the product explicitly defines that behavior later.

---

# 21. Natural-Language Ambiguity

Natural language is inherently ambiguous.

Examples:

```text
"by Friday"
"Friday"
"this weekend"
"next Friday"
"tomorrow morning"
"later this month"
```

The extractor must not manufacture precision.

For example:

```text
tomorrow morning
```

may represent:

```text
date = tomorrow
time = unknown
```

rather than arbitrarily choosing 9:00 AM.

---

# 22. Context Windows

Temporal extraction should consider nearby context.

For example:

```text
"Please submit your assignment.
The final deadline is 18 October."
```

The date's meaning comes from nearby text.

Use bounded context windows rather than analyzing unlimited unrelated content.

Avoid expensive full-document repeated parsing.

---

# 23. Multiple Temporal Candidates

An email may contain multiple dates.

Example:

```text
Applications open September 1.
Applications close October 15.
Interviews begin October 20.
```

Do not choose only one.

Extract all meaningful candidates:

```text
APPLICATION_OPENING
APPLICATION_DEADLINE
INTERVIEW_PERIOD
```

or the closest supported structured representation.

Do not discard useful information merely because another date exists.

---

# 24. Candidate Ranking

When multiple interpretations exist, rank candidates using deterministic signals.

Potential signals:

- explicit deadline vocabulary
- explicit meeting vocabulary
- proximity between date and semantic phrase
- structured calendar/event content
- sender context
- subject context
- future/past relation
- timezone information
- date format certainty

Store confidence such as:

```text
HIGH
MEDIUM
LOW
UNKNOWN
```

Do not expose numerical confidence unless the UI benefits from it.

---

# 25. Explainability

Each extracted result should be explainable.

Example:

```text
Deadline
18 October 2026

Detected because:
• "deadline" appears near the date
• date is explicitly stated
• message refers to submission
```

Another example:

```text
Interview
Monday, 10:30 AM IST

Detected because:
• "interview" appears near the date/time
• message states it is scheduled
```

Do not use AI-generated explanations.

Generate explanations from structured extraction metadata.

---

# 26. User Corrections

The extraction layer must be compatible with Phase 12.

A user must eventually be able to correct extracted information.

Do not make the extraction result immutable.

Support an architecture capable of:

```text
Base extracted temporal result
        ↓
User correction / override
        ↓
Effective temporal result
```

However, do not build a large temporal-correction UI if it is outside the necessary Phase 13 scope.

Prepare the model correctly.

---

# 27. Interaction With Rules

Do not duplicate the Phase 12 rule engine.

Where useful, allow future rules to consume temporal signals.

Examples:

```text
deadline exists
meeting exists
deadline within N days
meeting type = interview
```

But do not build a full temporal rule-language extension unless required for the current implementation.

Maintain clean separation.

---

# 28. Interaction With Priority

Phase 9 already established priority.

Phase 13 may expose temporal signals to the existing priority/action systems where architecturally appropriate.

For example:

```text
deadline approaching
interview scheduled
payment due
```

may eventually influence priority.

However:

> Do not rewrite the Phase 9 priority engine wholesale.

Only add the minimal integration required by the architecture.

Document any new signal.

---

# 29. Interaction With Action Required

Temporal information can support Action Required.

Example:

```text
"Submit the application by Friday."
```

could strengthen an existing Action Required result.

But do not turn every deadline into Action Required automatically.

For example:

```text
"Your subscription expires next year."
```

may be informational.

Keep the distinction between:

```text
temporal fact
```

and:

```text
action required
```

clear.

---

# 30. Meeting Links

Where emails contain obvious meeting URLs, identify them as structured metadata where safe.

Examples may include:

- recognized meeting URLs
- explicit "Join meeting" links
- structured event links

Do not automatically open URLs.

Do not automatically navigate to external services.

Do not validate a meeting link by making a network request.

Store the URL only if it is appropriate and safe.

Treat all URLs as untrusted until user interaction.

---

# 31. Location Extraction

Where explicitly provided:

```text
Location:
Room 203
St. Kuriakose College
```

or:

```text
Meeting location: Google Meet
```

extract structured location information where practical.
