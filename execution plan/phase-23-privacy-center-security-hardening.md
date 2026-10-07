# Phase 23 — Privacy Center & Security Hardening

## Mission

Implement Mail Organizer's **Privacy Center & Security Hardening** system.

This phase must turn the privacy and security principles established throughout the project into a coherent, inspectable, user-facing privacy/security experience and a hardened application architecture.

The objective is not merely to add a Privacy screen.

The objective is to ensure that:

```text
Google account
      ↓
OAuth credentials
      ↓
Gmail data
      ↓
Local database
      ↓
Classification / intelligence
      ↓
Actions / integrations
```

is protected by explicit boundaries, minimum necessary access, safe storage, safe logging, account isolation, secure external actions, and honest user-facing privacy information.

The central principle is:

> **Mail Organizer should minimize what it collects, minimize where it sends data, minimize what it stores, and make the remaining data/control understandable to the user.**

Do not make privacy claims that the implementation cannot substantiate.

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
    - this Phase 23 prompt
    - `docs/development-status.md` if available
4. Verify Phase 22 is genuinely complete.
5. Inspect the actual implementation of:
    - OAuth
    - token storage
    - Gmail read/write permissions
    - Gmail API adapter
    - Integration Manager
    - local database
    - database migrations
    - logging
    - crash/error handling
    - background workers
    - multi-account isolation
    - unified inbox
    - search
    - classification
    - rules
    - cleanup
    - conversation intelligence
    - Calendar
    - Tasks
    - Gmail writes
    - action execution
    - app navigation
    - settings
6. Identify privacy/security gaps from actual code, not assumptions.

---

# 2. Strict Sequential Execution

This session is **Phase 23 only**.

Do not implement:

- Phase 24 Performance & Battery Optimization
- Phase 25 Analytics & Insights
- Phase 26 Optional AI
- Phase 27 Advanced Automation
- Phase 28 Full Testing & QA as the dedicated final QA phase
- Phase 29 Production OAuth / Play Store Preparation
- Phase 30 Final Production Hardening

Phase 23 may fix security/performance issues when they are directly required for security, but do not turn this into a general performance project.

Execution:

```text
Read docs
↓
Verify Phase 22
↓
Security/privacy audit
↓
Threat model
↓
Implement security hardening
↓
Implement Privacy Center
↓
Harden credentials/data/logging
↓
Build
↓
Automated security tests
↓
Device testing
↓
Account isolation testing
↓
Permission/revocation testing
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
- Gradle
- dependencies
- manifests
- resources
- tests
- assets
- SDK/JDK configuration
- signing configuration
- OAuth
- Google Cloud
- Git repository
- generated files

Never build or clean another project.

All ADB operations must target only Mail Organizer.

---

# 4. Android Tooling Requirement

Use:

- project `./gradlew`
- ADB
- device/emulator
- install
- launch
- force-stop
- logcat
- screenshots
- screen recording
- dumpsys
- database inspection
- package inspection
- permission inspection

Use `adb reverse` only if genuinely required.

---

# 5. Threat Model

Create or update a concise project threat model.

Consider at minimum:

### Assets

- OAuth access tokens
- refresh tokens
- Gmail message metadata
- email content
- attachments metadata
- account identities
- user rules
- classification results
- conversation intelligence
- Calendar/Tasks integration state
- action history
- local database
- search index

### Threats

- device compromise
- malicious app on the device
- rooted/debug environment
- accidental logging
- backup leakage
- screenshots/recents exposure
- cross-account data leakage
- OAuth token theft
- compromised network
- malicious email content
- prompt injection
- malicious URLs
- incorrect account routing
- accidental Gmail modification
- exported Android components
- insecure deep links
- insecure WebViews
- dependency vulnerabilities
- secrets committed to Git

Document realistic mitigations.

Do not invent unrealistic threats merely to inflate the document.

---

# 6. Privacy Architecture

The app must follow:

```text
Collect minimum necessary data
↓
Process locally where possible
↓
Store minimum necessary data
↓
Transmit only when required
↓
Retain only as long as useful
↓
Delete safely when requested
```

---

# 7. Data Classification

Classify stored information.

At minimum distinguish:

### Highly sensitive

- OAuth credentials
- refresh tokens
- email content
- authentication information

### Sensitive

- sender/recipient information
- account identity
- Calendar/Tasks relationships
- action history
- conversation state

### Derived intelligence

- categories
- priority
- Action Required
- company detection
- newsletter/noise classification
- conversation state

### Non-sensitive application configuration

- theme
- UI preferences
- layout preferences

Document the handling expectations.

---

# 8. Data Inventory

Create/update a project data inventory.

For each significant data class document:

- what is stored
- why it is stored
- where it is stored
- whether it is account-scoped
- whether it leaves the device
- retention behavior
- deletion behavior

Do not claim that something is local-only if the implementation sends it externally.

---

# 9. OAuth Credential Security

Audit the credential implementation.

Tokens must not be stored in:

- ordinary Room entities
- plain SharedPreferences
- unencrypted files
- logs
- analytics payloads
- UI state
- Git

Use Android/platform-supported secure credential storage appropriate to the implementation.

Do not invent cryptographic schemes when a vetted platform mechanism exists.

---

# 10. Token Separation

Maintain separation between:

```text
Account metadata
```

and:

```text
OAuth secrets
```

Account records may reference a credential identity but must not contain raw secrets.

---

# 11. Token Lifecycle

Verify:

- initial authorization
- refresh
- expiration
- revocation
- disconnect
- account deletion
- reauthorization

When disconnecting:

- invalidate local credential references
- remove secure credential material as appropriate
- cancel future account-specific work
- prevent future API calls

Do not delete remote Gmail data.

---

# 12. Token Logging Audit

Search the codebase for accidental credential logging.

Check:

- access tokens
- refresh tokens
- Authorization headers
- OAuth response objects
- HTTP client logging
- exception messages

Disable sensitive HTTP logging in production.

---

# 13. Debug Logging

Debug builds must still avoid unnecessary sensitive data.

Do not rely on:

```text
DEBUG build = secrets are okay
```

Tokens and email bodies must never be casually logged.

---

# 14. Email Content Logging

Do not log complete:

- subject
- body
- sender/recipient lists
- attachment metadata

unless there is a specific controlled test requirement.

Prefer:

```text
messageId
accountId
operation
error category
```

where useful.

---

# 15. Crash Reporting

Inspect crash/error reporting architecture.

If a third-party crash service exists:

- verify whether email content could be included
- verify tokens cannot be included
- sanitize exceptions
- avoid sending sensitive user content

If no crash service exists, do not introduce one solely in this phase unless explicitly required.

---

# 16. Analytics Privacy

Do not introduce analytics merely to track user behavior.

If analytics already exists:

- audit event names
- audit payloads
- remove email content
- remove tokens
- avoid sender/recipient leakage
- avoid unnecessary account identifiers
- document what is collected

Phase 25 owns broader analytics architecture.

---

# 17. Network Privacy

Audit all network calls.

Expected external communication should be limited to required providers such as:

- Google OAuth
- Gmail API
- Google Calendar API
- Google Tasks API

Do not introduce:

- hidden telemetry
- email enrichment APIs
- remote classification
- external cleanup services
- arbitrary proxy servers

without explicit product requirements.

---

# 18. Network Security

Use secure HTTPS/TLS communication.

Do not:

- disable certificate validation
- accept arbitrary certificates
- downgrade to HTTP
- bypass hostname validation

Avoid custom networking security implementations unless necessary.

---

# 19. Network Debugging

Ensure development tools cannot accidentally leak production data.

Inspect:

- HTTP logging
- debug proxies
- staging endpoints
- test URLs
- hard-coded development hosts

Remove unsafe production configurations.

---

# 20. WebView Security

Avoid WebView for Gmail content unless genuinely necessary.

If WebView exists:

- disable JavaScript unless required
- disable unsafe file access
- prevent arbitrary navigation
- prevent untrusted content from accessing app bridges
- restrict URL schemes
- do not expose sensitive Android interfaces to email content

Prefer native rendering where possible.

---

# 21. Email HTML Security

Email HTML is untrusted.

Verify:

- HTML is sanitized
- JavaScript cannot execute
- dangerous schemes are blocked
- iframe/object/embed content is blocked
- local file access is blocked
- external resources are not automatically loaded
- malicious CSS cannot escape intended boundaries

Reuse Phase 5 parser/sanitizer.

Do not create a second HTML sanitizer.

---

# 22. URL Handling

Links inside emails must be treated as untrusted.

Verify:

- `javascript:` blocked
- dangerous custom schemes blocked
- malformed URLs handled safely
- user confirmation/opening is explicit
- external links do not automatically execute actions

Do not automatically open unsubscribe links.

---

# 23. Deep Links

Audit Android deep links.

Ensure malicious URLs cannot:

- bypass authentication
- select another account
- execute Gmail actions
- invoke destructive actions
- expose message content

Sensitive operations must require normal authorization/confirmation.

---

# 24. Intent Security

Audit exported:

- Activities
- Services
- BroadcastReceivers
- ContentProviders

Only expose components that need external access.

Use appropriate Android permission/export configuration.

---

# 25. PendingIntent Security

If PendingIntent is used:

- use immutable flags where appropriate
- scope intents carefully
- do not expose sensitive account/message data unnecessarily

---

# 26. Clipboard

Audit clipboard usage.

Do not copy:

- OAuth tokens
- full email bodies
- sensitive data

automatically.

If users copy email text intentionally, follow platform behavior and do not retain unnecessary copies.

---

# 27. Notifications

Audit notifications for privacy leakage.

Do not expose sensitive email content on the lock screen by default.

Consider showing:

```text
New important email
```

rather than:

unless the user explicitly chooses detailed notifications.

---

# 28. Recent Apps / Screenshots

Sensitive screens may appear in Android recent-apps screenshots.

Determine whether sensitive surfaces require appropriate protection.

Do not block screenshots everywhere merely for appearance.

Use sensible protection for highly sensitive screens where justified.

---

# 29. Screen Recording

Do not globally disable screen recording unless necessary.

Security-sensitive flows may require additional protection, but preserve normal usability.

Document the decision.

---

# 30. Clipboard / Share Sheet

Audit:

- share intents
- copy actions
- exported files
- attachment sharing

Do not accidentally expose private email data to external applications without explicit user action.

---

# 31. File Storage

Inspect files written by the application.

Do not store sensitive data in:

- public external storage
- shared directories
- predictable world-readable paths

Use app-private storage where appropriate.

---

# 32. Attachments

Phase 6 only exposed attachment metadata.

If attachment downloads exist now, audit:

- storage location
- file naming
- permissions
- sharing
- cleanup
- malicious file handling

Do not introduce broad attachment downloading in this phase.

---

# 33. Database Security

Audit the local database.

Ensure:

- account ownership is explicit
- queries are account-scoped
- migrations do not leak data
- deletion is account-safe
- sensitive data is not unnecessarily duplicated
- debug database exposure is controlled

Do not assume local storage is automatically secure simply because it is on-device.

---

# 34. Database Encryption

Evaluate whether database encryption is justified by the threat model and platform architecture.

If implemented:

- use a vetted library/mechanism
- manage encryption keys securely
- handle migrations
- handle reinstall/data loss expectations
- test performance and recovery

Do not invent custom encryption.

If not implemented, document why and what protections remain.

---

# 35. Backup Policy

Audit Android backup behavior.

Sensitive local data must not unintentionally leak through device backup.

Determine whether:

- database backup is appropriate
- credential backup is prohibited
- sensitive files are excluded
- restore behavior is safe

Configure according to the actual privacy model.

---

# 36. Account Deletion

Verify complete local deletion of an account where the user requests it.

Remove appropriate:

- messages
- threads
- sender/company data
- classifications
- priority
- Action Required
- rules/corrections
- temporal intelligence
- conversation intelligence
- cleanup candidates
- search index entries
- action history
- integration relationships
- sync state

Do not remove another account.

---

# 37. Credential Deletion

Account removal must also invalidate/remove associated OAuth credential material.

Verify that no orphaned credential remains usable by the app.

---

# 38. Account Deletion Confirmation

Explain what is being removed.

Example:

```text
Remove this account from Mail Organizer?

This removes its locally stored Mail Organizer data and disconnects its Google access.

Your Gmail messages are not deleted from Google.
```

Only make claims matching actual behavior.

---

# 39. Gmail Remote Data

Clearly distinguish:

```text
Remove account from Mail Organizer
```

from:

The former must not imply the latter.

---

# 40. Privacy Center

Create a coherent Privacy Center within the existing navigation.

The Privacy Center should communicate:

- connected accounts
- permissions
- what data is stored locally
- what data leaves the device
- integrations
- account removal
- local-data deletion
- security status
- privacy-sensitive settings where appropriate

Follow `design.md`.

---

# 41. Privacy Center Information Architecture

A reasonable structure:

```text
Privacy
├── Your Data
├── Google Access
├── Connected Accounts
├── Integrations
├── Local Storage
├── Data Removal
└── Security
```

Use the existing navigation patterns rather than creating an unrelated settings architecture.

---

# 42. Your Data

Explain categories of data stored locally.

For example:

```text
Mail metadata
Message content
Sender information
Categories
Rules
Conversation intelligence
Action history
```

Only list information actually stored.

---

# 43. Google Access

Show:

- connected Google accounts
- Gmail permission level
- Calendar permission level
- Tasks permission level
- permission status

Do not expose raw OAuth tokens.

---

# 44. Permission Scope Display

Translate technical scopes into understandable language.

Instead of only:

```text
gmail.modify
```

show:

> Can read and make supported changes to your Gmail.

Still allow a technical details view if useful.

---

# 45. Technical Details

Advanced users may need to see:

- provider
- scopes
- account
- connection status
- last permission verification

Do not show secrets.

---

# 46. Integration Privacy

For Calendar/Tasks:

explain:

- what Mail Organizer can access
- why access is needed
- whether data is cached locally
- how to disconnect

Do not overstate data isolation.

---

# 47. Local-First Explanation

The Privacy Center should clearly explain:

> Mail Organizer processes email intelligence locally whenever possible.

Only claim this if actual implementation supports it.

---

# 48. External Data Transmission

If email content is not sent externally, say so accurately.

If any external service receives content:

- identify it
- explain why
- provide appropriate disclosure

Do not claim "nothing leaves your device" if Google APIs obviously receive Gmail requests.

A better distinction may be:

> Mail Organizer does not send your email content to third-party AI or analytics services.

Only make such claims if verified.

---

# 49. Data Retention

Explain retention at a useful level.

Examples:

```text
Cached email data remains on this device until you remove the account or clear application data.
```

Only use this wording if accurate.

---

# 50. Clear Local Data

Provide a controlled local-data removal flow.
