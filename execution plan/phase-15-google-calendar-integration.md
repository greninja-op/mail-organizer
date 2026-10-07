# Phase 15 — Google Calendar Integration

## Mission

Implement the **Google Calendar Integration** for Mail Organizer.

This phase connects Mail Organizer's existing:

- Gmail account identity
- local email data
- meeting/deadline extraction
- user corrections
- Action Cards
- Action Engine

to Google Calendar through an official Google API integration.

The integration must allow users to review and, when explicitly confirmed, create/manage supported Calendar actions.

The core principle is:

```text
Email
 ↓
Meeting / Deadline Intelligence
 ↓
Action Candidate
 ↓
Calendar Action Proposal
 ↓
User Review
 ↓
Explicit Confirmation
 ↓
Google Calendar API
 ↓
Verified Result
 ↓
Local Action State
```

Never skip:

```text
Proposal → Review → Confirmation
```

The system must never silently create Calendar events merely because an email contains a meeting or deadline.

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
    - this Phase 15 prompt
    - `docs/development-status.md` if present
4. Verify Phase 14 is actually complete.
5. Inspect the real implementation of:
    - Google OAuth
    - Gmail account management
    - account IDs
    - Action Engine
    - Action Cards
    - temporal extraction
    - database
    - network layer
    - integration architecture
    - navigation/UI
6. Determine whether Google authentication already provides a reusable account identity/token architecture.

Do not assume Phase 3's Gmail OAuth implementation is automatically suitable for Calendar.

---

# 2. Strict Sequential Execution

This session is:

> **Phase 15 only.**

Do not implement Phase 16 or later.

Do not implement:

- Google Tasks integration
- Integration Manager
- multi-account redesign
- Gmail write features
- cleanup automation
- AI
- analytics
- advanced automation
- production OAuth/release preparation

At the end:

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
Calendar validation
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

Identify the Mail Organizer project root before editing.

Never modify another project.

Never touch sibling projects':

- source
- Gradle
- wrapper
- dependencies
- manifests
- resources
- assets
- tests
- SDK configuration
- signing configuration
- OAuth configuration
- Git state
- generated files

Never build, clean, install, uninstall, or test another application.

Never modify another project's Google Cloud project, OAuth client, consent screen, APIs, test users, redirect configuration, or credentials.

Do not modify global Android/JDK/Gradle configuration merely to support Mail Organizer.

---

# 4. Android and ADB Tooling

Android tooling remains mandatory.

Use:

- project Gradle wrapper
- ADB
- device/emulator inspection
- package inspection
- APK installation
- launch
- force-stop
- logcat
- screenshots
- screen recording
- dumpsys
- database inspection where available

ADB operations must target only the Mail Organizer package.

If `adb reverse` is required:

- inspect existing mappings if necessary
- use only what Mail Organizer requires
- do not disturb other projects

---

# 5. Official Google APIs Only

Calendar integration must use Google's official supported Calendar API.

Do not use:

- scraping
- browser automation
- web-page parsing
- AccessibilityService
- unofficial endpoints
- cookies
- session hijacking
- embedded browser authentication hacks

Use Google's supported Android authentication and API mechanisms appropriate to the existing project architecture.

---

# 6. OAuth Scope

Request only the minimum Calendar scope required.

Determine the exact required scope from the official API and the intended operations.

Do not request broad permissions merely because they are convenient.

If only event creation is required, do not automatically request unrelated Calendar permissions.

If the selected scope permits more than Mail Organizer currently needs, document why.

Never request:

- Gmail send
- Gmail modify
- Gmail delete
- unrelated Google service scopes

as part of Calendar integration.

Maintain least privilege.

---

# 7. Google Cloud Configuration

Calendar API configuration must belong exclusively to Mail Organizer.

Verify:

- correct Google Cloud project
- Calendar API enabled
- correct Android application identity
- correct package ID
- correct signing certificate/fingerprint
- correct OAuth client
- correct consent screen
- correct test-user configuration where applicable

Never modify another project's Google Cloud resources.

Do not create unnecessary OAuth clients.

Do not commit:

- client secrets
- refresh tokens
- access tokens
- authorization codes
- private keys
- service-account credentials

---

# 8. Existing Gmail OAuth Relationship

Determine whether the existing Mail Organizer Google account can be reauthorized for Calendar.

Do not assume a Gmail token automatically has Calendar permissions.

If Calendar scope is not currently granted:

```text
Existing Google account
        ↓
Calendar authorization required
        ↓
User grants permission
```

Handle this as a clean permission expansion.

Do not force the user to disconnect Gmail unnecessarily if the authentication architecture can safely add Calendar authorization.

---

# 9. Account Identity

Calendar actions must use the correct Google account.

Example:

```text
Mail Organizer account:
personal@gmail.com

Calendar operation:
personal@gmail.com
```

must not accidentally execute against:

```text
work@gmail.com
```

Store/validate account identity throughout the entire flow.

---

# 10. Multi-Account Safety

For every Calendar action validate:

```text
Mail account
↓
Google account
↓
OAuth credential
↓
Calendar account
↓
Target calendar
```

All must belong to the intended account.

Never silently use:

- whichever Google account is currently active on Android
- whichever credential happens to exist
- the first account returned by the API

Account selection must be explicit and deterministic.

---

# 11. Calendar Selection

A Google account may have multiple calendars.

Do not automatically assume the first calendar is correct.

Provide an appropriate mechanism to select the target calendar.

At minimum consider:

- primary calendar
- calendar name
- calendar ID
- writable vs read-only status
- timezone
- selected/default calendar

Do not allow users to select a calendar that the authenticated account cannot modify.

---

# 12. Calendar Metadata

Retrieve only the Calendar metadata required to support the feature.

For each relevant calendar, consider:

- calendar ID
- summary/name
- description if needed
- timezone
- access role
- writable capability
- primary status

Do not download unnecessary calendar data.

Do not sync every event into Mail Organizer in this phase unless explicitly required.

This phase is primarily about **creating Calendar actions from Mail Organizer intelligence**.

---

# 13. Calendar Architecture

Keep Calendar integration modular.

Recommended architecture:

```text
Presentation
    ↓
Application / Use Case
    ↓
Action Engine
    ↓
Calendar Action Adapter
    ↓
Calendar Repository / Client
    ↓
Google Calendar API
```

Keep Google-specific API models isolated from domain models.

Do not leak Google API classes throughout the application.

---

# 14. Calendar Adapter

Create a dedicated Calendar integration boundary.

Conceptually:

```text
CalendarIntegration
 ├── authenticate()
 ├── getCalendars()
 ├── validateEvent()
 ├── createEvent()
 └── optionally update/delete where explicitly supported
```

Follow the project's actual language and architecture.

Do not copy this interface literally if a better existing abstraction exists.

---

# 15. Action Engine Integration

Connect Phase 14 Calendar actions to the actual Calendar integration.

Example:

```text
Action:
ADD_TO_CALENDAR

↓
Calendar integration available?

YES
↓
Validate event
↓
Show confirmation
↓
Create event
```

If unavailable:

```text
Calendar not connected
```

The action must remain non-executed.

---

# 16. Calendar Event Proposal

Create a structured event proposal before making the API call.

Example:

```text
CalendarEventProposal
 ├── accountId
 ├── calendarId
 ├── title
 ├── description
 ├── start
 ├── end
 ├── timezone
 ├── location
 ├── meetingUrl
 ├── sourceMessageId
 └── sourceActionId
```

Only include data actually available.

Do not invent missing values.

---

# 17. Event Title

Use a deterministic title strategy.

Possible source hierarchy:

```text
Explicit event/meeting title
↓
Email subject
↓
Sender + event type
↓
Safe generic title
```

Never create meaningless titles such as:

```text
Meeting
Meeting
Meeting
```

if better information exists.

Do not include sensitive email content unnecessarily.

---

# 18. Event Description

Include only useful information.

Potential content:

```text
Source:
Mail Organizer

From:
Recruiting Team

Original subject:
Interview confirmation

Source email:
[Open in Mail Organizer]
```

Do not dump the entire email body into the Calendar event.

Do not include OAuth tokens or internal database IDs.

If including a Mail Organizer deep link, ensure it is actually supported before generating one.

Never create a fake link.

---

# 19. Date and Time

Use Phase 13 temporal intelligence.

Do not reinterpret dates independently inside the Calendar integration unless required for API conversion.

For example:

```text
2026-10-15
10:30 AM
Asia/Kolkata
```

should become the appropriate Calendar event representation.

Do not invent:

- duration
- timezone
- date
- start time

---

# 20. Missing Time

If a meeting has a date but no time:

```text
Interview
October 15
Time unknown
```

do not arbitrarily create:

Possible safe options:

- represent as an all-day event only if the semantics genuinely support it
- ask the user for the missing information
- leave the Calendar action unavailable until sufficient information exists

Do not guess.

---

# 21. Missing End Time

If a start time exists but no end time:

Do not silently assume an arbitrary duration unless the product has explicitly defined and documented a safe policy.

Prefer:

- user selection
- explicit duration from source
- all-day event where appropriate

Do not invent a one-hour meeting.

---

# 22. Timezone

Respect the timezone determined by Phase 13.

Priority should be based on:

```text
Explicit event timezone
↓
Explicit email timezone
↓
Calendar timezone
↓
User/application timezone
```

Do not silently convert times incorrectly.

Test:

- IST
- UTC
- another explicit timezone
- missing timezone

---

# 23. Location

If Phase 13 extracted a location:

```text
Room 203
```

use it when appropriate.

Do not:

- geocode it
- query Maps
- infer coordinates
- replace it with a guessed location

---

# 24. Meeting Links

If Phase 13 identified a meeting URL:

- include it only when safe
- preserve the exact user-facing URL
- do not visit it
- do not validate it through a network request

If the Calendar API has an appropriate event-link representation, use the supported field only when the semantics are correct.

Do not invent Google Meet information.

---

# 25. Explicit Confirmation

Calendar creation requires explicit user confirmation.

The user should see a preview such as:

```text
Add to Calendar?

Interview with Acme

Monday, October 12
10:30 AM – 11:30 AM
IST

Calendar:
Personal

From:
Acme Recruiting

[Cancel]
[Add to Calendar]
```

The exact UI should follow `design.md`.

The button must clearly communicate the external action.

---

# 26. No Background Calendar Creation

Never create Calendar events:

- during Gmail sync
- during parsing
- during classification
- during Action generation
- when the dashboard opens
- during background WorkManager execution
- because an email was detected

Calendar creation is user-authorized only.

---

# 27. Duplicate Prevention

Creating the same Calendar event twice is unacceptable.

Before creating an event, establish a deterministic idempotency strategy.

Potential identity components:

```text
accountId
calendarId
sourceMessageId
sourceActionId
event fingerprint
```

Use the safest mechanism supported by the Calendar API and local architecture.

Store the resulting:

- Calendar event ID
- account ID
- calendar ID
- source action ID/message ID

where appropriate.

---

# 28. Existing Event Detection

Where practical, detect whether a matching event already exists.

Possible matching signals:

- stored event ID
- source action ID
- source message ID
- normalized title
- start time
- calendar ID

Do not rely solely on title matching.

Avoid creating duplicate events when the same action is retried.

---

# 29. Retry Safety

Network failures may happen after the Calendar server creates an event but before the app receives the response.

The implementation must account for this.

Do not blindly retry:

```text
createEvent()
```

because that may create duplicates.

Use a safe recovery strategy such as:

- lookup
- deterministic fingerprint
- stored local execution state
- API-supported idempotency mechanisms where available

Document the selected approach.

---

# 30. Action State

Update Phase 14 action state only after verified outcomes.

Example:

```text
SUGGESTED
↓
CONFIRMED
↓
EXECUTING
↓
COMPLETED
```

If the API fails:

```text
FAILED
```

Do not mark:

until the Calendar API actually confirms success.

---

# 31. Error Handling

Handle:

- no internet
- expired token
- revoked permission
- insufficient scope
- Calendar not found
- calendar read-only
- invalid event
- invalid timezone
- rate limiting
- server error
- malformed API response
- account mismatch

Errors should be understandable.

Example:

```text
Couldn't add the event.

Your Google Calendar permission may have expired.

[Reconnect Calendar]
```

Do not expose raw API stack traces to normal users.

---

# 32. Offline Behavior

When offline:

- show existing local action cards
- show Calendar connection state
- allow event preview
- do not attempt external execution
- preserve the pending action
- retry only through an explicit safe mechanism

Do not repeatedly hammer the network in the background.

---

# 33. Token Security

Never store OAuth access tokens in:

- email database
- logs
- screenshots
- action cards
- event descriptions
- Git
- plaintext source code

Use the existing secure authentication/token architecture.

Refresh tokens securely.

Handle token expiration.

---

# 34. Permission Revocation

Test:

```text
Calendar permission granted
↓
user revokes access
↓
Mail Organizer attempts Calendar action
↓
permission failure detected
↓
user prompted to reconnect
```

Do not crash.

Do not repeatedly ask for permission without user intent.

---

# 35. Disconnect Behavior

Provide a safe Calendar disconnect mechanism if supported by the existing integration architecture.

Disconnecting Calendar should not:

- delete Gmail data
- delete local email
- delete unrelated account data
- modify another Google service

Clearly communicate what disconnecting means.

Do not automatically delete existing Calendar events created by Mail Organizer unless the user explicitly chooses such behavior and the feature is intentionally supported.

---

# 36. Account Switching

Test:

```text
Account A
↓
Calendar action
↓
switch to Account B
↓
Account B Calendar
```

The pending action must remain correctly tied to Account A.

Never execute Account A's action using Account B's credential.

---

# 37. Calendar Selection UI

If multiple writable calendars exist, provide a clear selection mechanism.

Show:

- calendar name
- account
- writable state
- timezone where relevant

Do not overwhelm users with API identifiers.

Never show raw Calendar IDs as the primary user-facing label.

---

# 38. Calendar Integration Status

Expose useful states:

```text
Not connected
Connecting
Connected
Permission required
Permission revoked
Error
Offline
```

Do not claim:

if the integration cannot actually make a valid API request.

---

# 39. Settings / Integration UI

Integrate Calendar connection into the existing settings/integration architecture without prematurely implementing Phase 17's full Integration Manager.

A basic Calendar integration entry is appropriate.

It should communicate:

```text
Google Calendar