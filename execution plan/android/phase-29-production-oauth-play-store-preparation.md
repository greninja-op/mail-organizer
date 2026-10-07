# Phase 29 — Production OAuth / Play Store Preparation

## Mission

Prepare Mail Organizer for **real production distribution**, with particular focus on:

- production Google OAuth
- Gmail API production configuration
- Calendar/Tasks production configuration where implemented
- Android release configuration
- Play Store technical requirements
- privacy/compliance preparation
- production-safe configuration
- release signing
- app metadata
- store-ready artifacts

This phase is about **production preparation**, not final release.

Do not publish the application or perform irreversible production actions unless explicitly authorized.

The application must remain technically safe to distribute after this phase.

***

# 1. Mandatory Instruction-Folder Discovery

Before doing anything:

1. Identify the **Mail Organizer project root**.
2. Locate the instruction/documentation folder.
3. Read: 
   - `requirements.md`
   - `spec.md`
   - `design.md`
   - `editor-rules.md`
   - `docs/development-status.md`
   - Phase 29 prompt
4. Verify Phase 28 was actually completed.
5. Inspect the real repository and current release configuration.

Do not assume Phase 28 passed merely because `spec.md` says `[x]`.

If Phase 28 is incomplete, stop and mark the blocker appropriately.

***

# 2. Strict Sequential Execution

This session is **Phase 29 only**.

Do not execute Phase 30.

Allowed work:

- production configuration
- release configuration
- OAuth configuration
- Play Store preparation
- compliance preparation
- release artifact generation
- production smoke testing
- documentation
- bug fixes required for production preparation

Do not:

- redesign the application
- add unrelated product features
- rewrite working architecture
- introduce new AI capabilities
- add unnecessary permissions
- publish to Play Store without authorization

***

# 3. Permanent Multi-Project Isolation

The parent workspace may contain several Android applications.

Identify the Mail Organizer root before editing.

Never modify sibling projects.

Never touch another project's:

- Gradle files
- source
- dependencies
- SDK configuration
- manifests
- signing
- resources
- tests
- Git repository
- OAuth
- Google Cloud project
- Play Store configuration
- generated files

Never run release builds for sibling applications.

***

# 4. Production Readiness Principle

The objective is:

> **A production build must use production-safe configuration without weakening privacy, security, account isolation, or user control.**

Never solve production configuration by disabling security.

***

# 5. Production Identity

Confirm:

- application name
- package/application ID
- version name
- version code
- launcher icon
- application label
- supported Android versions
- target SDK
- namespace

These must be internally consistent.

Do not change the application ID casually.

Changing package identity at this stage can break:

- OAuth
- signing
- Play Store identity
- existing installations
- release configuration

If a change is genuinely necessary, document the impact before making it.

***

# 6. Application ID Verification

Verify the production application ID is exactly the intended Mail Organizer package.

Check:

- Gradle
- manifest
- OAuth Android client
- Google Cloud configuration
- signing certificate
- Play Store configuration

All must refer to the correct application.

***

# 7. Build Variants

Review:

- debug
- release
- any staging variant
- flavor configuration

Ensure development configuration cannot accidentally ship in release.

***

# 8. Production Configuration Separation

Separate:

```text
development
staging
production
```

only where necessary.

Do not create unnecessary complexity.

Production must not depend on:

- local machine files
- developer-only environment variables
- localhost
- test endpoints
- debug-only services
- test OAuth clients

***

# 9. Secrets

Never put production secrets directly into:

- source code
- Git
- `BuildConfig`
- resources
- manifest
- APK
- AAB
- screenshots
- documentation

Remember:

> An Android application cannot safely hide a client-side secret from a determined user.

Use appropriate public/client identifiers only.

Never embed:

- OAuth client secrets
- server private keys
- API secrets
- refresh tokens
- service-account private keys

***

# 10. Google Cloud Project

Identify the Google Cloud project used by Mail Organizer.

Verify it belongs exclusively to this application.

Do not modify another project's:

- OAuth clients
- test users
- credentials
- APIs
- consent configuration

***

# 11. Google API Inventory

Review enabled APIs.

At minimum, only enable APIs genuinely required by implemented features:

- Gmail API
- Google Calendar API if implemented
- Google Tasks API if implemented

Disable unnecessary APIs where appropriate.

Do not enable broad Google services "just in case."

***

# 12. OAuth Client Inventory

Review every OAuth client associated with Mail Organizer.

Classify:

- Android debug
- Android release
- Web client if genuinely required
- other client types

Remove or disable obsolete development credentials where appropriate.

Do not delete credentials blindly.

***

# 13. Production Android OAuth Client

Verify production Android OAuth client:

- package name
- release SHA-1/SHA-256 fingerprint
- application identity
- Google Cloud project

matches the actual release signing certificate.

***

# 14. Debug vs Release OAuth

Ensure:

```text
Debug signing fingerprint
≠
Production signing fingerprint
```

and each corresponding OAuth client is configured correctly.

Do not use a debug OAuth client for production.

***

# 15. Release Signing

Inspect signing configuration.

Verify:

- production signing key exists
- signing is not committed to Git
- keystore/passwords are not logged
- release build uses intended signing configuration
- signing key backup/recovery process exists

Never expose the keystore or passwords.

***

# 16. Play App Signing

If Play App Signing is intended:

verify the architecture between:

- upload key
- Play app-signing key
- Android OAuth fingerprint

is understood.

Do not rotate production signing keys without explicit authorization and a migration plan.

***

# 17. OAuth Consent Screen

Review production OAuth consent configuration.

Verify:

- application name
- application logo
- support email
- developer contact information
- privacy policy URL
- authorized domains where required
- scopes
- application description

All information must accurately describe Mail Organizer.

***

# 18. OAuth Scope Minimization

Review every requested scope.

Use the smallest scope necessary.

Examples:

```text
Gmail read-only
Calendar only when Calendar integration is enabled
Tasks only when Tasks integration is enabled
Gmail modify only when write functionality requires it
Gmail send only if sending/replying/forwarding is genuinely implemented
```

Never request:

- unrelated Gmail scopes
- broad Google scopes
- permissions merely for future features

***

# 19. Incremental Authorization

Where practical, request sensitive permissions when the user actually enables the related capability.

For example:

```text
Gmail connection
↓
read-only Gmail access

User enables Calendar
↓
Calendar permission

User enables Gmail modifications
↓
Gmail modification permission
```

Do not request every permission on first launch unnecessarily.

***

# 20. OAuth Explanation

Before sensitive authorization, the UI must explain:

- what access is requested
- why it is needed
- what Mail Organizer will do with it

Avoid misleading wording.

***

# 21. OAuth Disconnect

Verify disconnect behavior.

When a user disconnects:

- stop future API access
- invalidate local authorization state
- cancel relevant background work
- do not silently reconnect
- do not delete unrelated accounts

***

# 22. OAuth Revocation

Test revoked permissions.

Expected behavior:

```text
permission revoked
↓
detect failure
↓
AUTH_REQUIRED / PERMISSION_REQUIRED
↓
user-controlled reconnect
```

No endless retries.

***

# 23. Production OAuth Smoke Test

Using a controlled production-like account:

1. Install release build.
2. Connect Gmail.
3. Verify correct Google account.
4. Verify requested scopes.
5. Sync.
6. Disconnect.
7. Reconnect.
8. Verify token refresh.
9. Verify account isolation.

Do not use personal/private production mailbox data for screenshots or documentation.

***

# 24. Calendar Production Configuration

If Calendar is implemented:

Verify:

- Calendar API enabled
- production OAuth configuration
- correct release application identity
- correct scopes
- calendar selection
- event creation
- duplicate protection
- permission revocation

***

# 25. Tasks Production Configuration

If Tasks is implemented:

Verify:

- Tasks API enabled
- production OAuth configuration
- correct scopes
- task list selection
- task creation
- duplicate protection
- permission revocation

***

# 26. Gmail Production Write Configuration

If Phase 22 implemented Gmail writes:

verify:

- `gmail.modify` is only requested when necessary
- write operations are explicitly confirmed
- production OAuth is configured correctly
- no send scope exists unless sending is implemented

***

# 27. Production API Quotas

Review API quotas and limits for:

- Gmail
- Calendar
- Tasks

Verify the application does not depend on unrealistic quotas.

Do not attempt to bypass API limits.

***

# 28. Rate Limiting

Production code must respect provider rate limits.

Verify:

- exponential backoff
- bounded retries
- cancellation
- account-level throttling
- no retry storm

***

# 29. API Error Mapping

Production UI must correctly distinguish:

- unauthorized
- permission denied
- rate limited
- unavailable
- network failure
- malformed response
- account removed

***

# 30. Production Endpoints

Search the project for:

- localhost
- `127.0.0.1`
- development URLs
- staging URLs
- test endpoints
- hard-coded temporary domains

Ensure release builds cannot accidentally use development endpoints.

***

# 31. Local-Only Architecture

Confirm no unnecessary Mail Organizer backend exists.

The production architecture should remain:

```text
Android
├── Local database
├── Local processing
├── Gmail API
├── Calendar API
├── Tasks API
└── Optional AI provider
```

Only actual external services should receive data.

***

# 32. AI Production Configuration

If AI is implemented:

Verify:

- opt-in remains explicit
- default remains OFF where specified
- provider is clearly identified
- privacy disclosure is accurate
- API credentials are not hard-coded
- no provider is silently substituted
- AI cannot execute actions

If AI is architecture-only, do not add production AI configuration.

***

# 33. AI Provider Disclosure

If remote AI is available, clearly disclose:

- data may be sent to the selected AI provider
- which content is transmitted
- how the user controls the feature

Do not make unsupported claims about:

- provider retention
- training usage
- encryption
- compliance
- deletion

***

# 34. Privacy Policy Preparation

Prepare or review the production privacy policy.

It must accurately describe:

- Gmail access
- Calendar access
- Tasks access
- local processing
- optional AI
- data storage
- account information
- deletion
- logging
- analytics
- third-party services
- permissions

Never claim:

> "No data leaves the device"

when Google APIs or remote AI are used.

***

# 35. Data Safety Accuracy

Prepare Google Play Data Safety information based on actual implementation.

Do not guess.

Inventory:

- data collected
- data shared
- data processed
- purpose
- retention
- deletion
- security practices

***

# 36. Data Minimization Review

Verify the app only stores what it actually needs.

Review:

- email bodies
- attachments
- sender metadata
- derived intelligence
- analytics
- automation history
- AI cache
- logs

***

# 37. Account Deletion

Verify the user can remove an account from Mail Organizer.

Document:

- what local data is deleted
- what remains at Google
- what external Calendar/Tasks objects remain
- what permissions are revoked
- what background work is cancelled

Never claim that deleting an account from the app deletes the Gmail account itself.

***

# 38. External Data Deletion

Clearly distinguish:

```text
Remove account from Mail Organizer
```

from:

```text
Delete Gmail account
```

and:

```text
Delete Google Calendar event
```

and:

```text
Delete Google Task
```

***

# 39. Privacy Center

Verify the Privacy Center communicates accurately:

- local-first processing
- Gmail data access
- external integrations
- AI settings
- account removal
- data deletion
- permission management

***

# 40. Play Store Account

If Play Console access is available through the configured environment, inspect only the Mail Organizer application.

Do not modify unrelated applications.

If Play Console access is unavailable, document that as an external prerequisite.

***

# 41. Store Listing Preparation

Prepare:

- application title
- short description
- full description
- category
- tags/keywords where applicable
- contact information
- privacy policy
- support URL

Descriptions must accurately reflect implemented functionality.

***

# 42. Store Description Safety

Do not claim:

- "100% private"
- "completely offline"
- "AI never sees your data"
- "zero data transmission"

unless technically true.

Use precise language.

***

# 43. Screenshots

Prepare production-quality screenshots.

Include representative screens such as:

- Home
- Mail
- Categories
- Companies
- Actions
- Search
- Insights
- Integrations
- Privacy

Use controlled/synthetic account data.

Never expose real personal email.

***

# 44. Screenshot Design

Screenshots must follow `design.md`.

Verify:

- typography
- colors
- spacing
- dark mode
- hierarchy
- readable content

Do not add fake UI solely for marketing screenshots.

***

# 45. Feature Graphic / Store Assets

If required, prepare:

- feature graphic
- app icon
- promotional assets

Use the actual Mail Organizer brand.

Do not create misleading feature representations.

***

# 46. App Icon

Verify:

- correct launcher icon
- adaptive icon
- foreground/background
- Android compatibility
- no unintended debug branding

***

# 47. Versioning

Review:

- `versionCode`
- `versionName`

Verify versioning strategy for future releases.

Do not reset production version numbers.

***

# 48. Target SDK

Verify the project targets the current Play Store-required Android API level applicable at the time of submission.

Use official Android/Google Play requirements rather than assumptions.

If a current requirement cannot be verified from available tooling/documentation, document it as a release prerequisite rather than guessing.

***

# 49. Minimum SDK

Review the minimum supported Android version.

Ensure it matches:

- requirements
- actual APIs used
- testing coverage
- product target

***

# 50. Permission Declaration Review

Review Play Store-sensitive permissions.

Remove unused permissions.

Do not add permissions merely to simplify implementation.

***

# 51. Manifest Review

Final production manifest review:

- exported components
- intent filters
- permissions
- services
- receivers
- providers
- backup
- deep links
- network security

***

# 52. Debug Component Audit

Ensure release does not expose:

- debug activities
- test providers
- development menus
- test accounts
- mock repositories
- fake data
- debug endpoints

***

# 53. Logging

Production logging must be safe.

Do not log:

- email body
- complete subject
- tokens
- credentials
- API keys
- private Calendar data
- private Tasks data
- AI prompts containing unnecessary email data

***

# 54. Crash Reporting

If crash reporting exists:

review exactly what data is transmitted.

Do not introduce a crash-reporting SDK solely for this phase unless genuinely required.

If none exists, do not add one unnecessarily.

***

# 55. Analytics

Mail Organizer is local-first.

Do not introduce third-party analytics merely for Play Store release.

If analytics already exist:

- verify privacy
- verify consent/controls where required
- verify no sensitive data
- verify privacy policy accuracy

***

# 56. Network Security Configuration

Verify:

- HTTPS
- certificate validation
- no unnecessary cleartext traffic
- no debug certificate exceptions in release

***

# 57. Backup Configuration

Review Android backup behavior.

Ensure production backup does not unintentionally expose:

- sensitive email data
- tokens
- automation configuration
- AI credentials
- local private information

***

# 58. Device Compatibility

Review supported device configurations.

Test release build on representative Android devices.

At minimum document:

- physical device
- emulator where available
- Android versions
- screen sizes

***

# 59. Production Performance Smoke Test

Run release-like build and measure:

- cold startup
- inbox rendering
- search
- sync
- account switching
- dashboard
- analytics

Compare against Phase 28 results.

***

# 60. Production Memory Smoke Test

Verify no significant regression in:

- startup memory
- inbox
- sync
- search
- analytics
- automation

***

# 61. Production Battery Smoke Test

Verify:

- background sync
- WorkManager
- automation
- indexing

do not create excessive background activity.

***

# 62. Release Build

Build the production release artifact using the project's intended process.

Use:

```text
./gradlew
```

Do not use another project's Gradle wrapper.

***

# 63. Release Installation

Install the release artifact onto a controlled test device.

Verify:

- installation
- launch
- OAuth
- sync
- primary UI
- external integrations
- critical actions

***

# 64. Release Smoke Test

Run:

```text
Install release
↓
Launch
↓
Connect Gmail
↓
Sync
↓
Open Mail
↓
Search
↓
Open category
↓
Open company
↓
Open action
↓
Use Calendar/Tasks if implemented
↓
Test account switching
↓
Test offline
↓
Test reconnect
↓
Disconnect
↓
Reconnect
```

***

# 65. Release OAuth Smoke Test

Verify the **release-signed application**, not merely the debug application.

This distinction is mandatory.

***

# 66. OAuth Fingerprint Verification

Confirm the fingerprint used by Google OAuth exactly matches the certificate actually signing the tested release artifact.

Do not assume.

***

# 67. ADB Verification

Use ADB to verify:

- installed package
- version
- process
- permissions
- activity
- logs

Only inspect Mail Organizer.

***

# 68. Logcat Verification

Run release smoke tests while observing logcat.

Verify:

- no secrets
- no unexpected exceptions
- no OAuth failures
- no database errors
- no repeated network failures
- no background worker loops

***

# 69. Screenshots

Capture release-build screenshots.

Verify visual output is identical to intended production UI.

***

# 70. Screen Recording

Record the critical release smoke flow where useful:

```text
launch
→ OAuth
→ sync
→ inbox
→ search
→ action
→ disconnect
```

Do not include private credentials/data.

***

# 71. Play Integrity / Related Requirements

Review whether the current distribution strategy requires:

- Play Integrity
- app signing
- integrity configuration
- related Google Play services

Do not introduce unnecessary anti-tampering mechanisms.

***

# 72. App Links / Deep Links

If supported:

verify production domains and association files.

Do not configure unrelated domains.

***

# 73. Web Domains

Inventory every production domain.

Verify ownership and purpose.

Do not claim ownership or configure domains that are not controlled by the project.

***

# 74. Support

Prepare a production support mechanism:

- support email
- bug reporting path
- privacy contact
- account/access help

Do not hard-code personal credentials.

***

# 75. Account Access Troubleshooting

Document common production issues:

- OAuth denied
- wrong Google account
- permission revoked
- Gmail API unavailable
- Calendar permission missing
- Tasks permission missing
- offline
- sync failure

***

# 76. Privacy Troubleshooting

Document:

- how to disconnect Google
- how to remove an account
- what happens to local data
- what remains at Google
- how optional AI is disabled

***

# 77. Production Configuration Documentation

Create/update documentation describing:

- production package
- release signing
- OAuth clients
- Google APIs
- scopes
- release variants
- Play configuration
- privacy configuration

Never put secrets in the documentation.

***

# 78. Environment Checklist

Create a production checklist containing:

```text
Google Cloud
OAuth
Signing
Build
Permissions
Privacy
Play Store
Testing
Screenshots
Support
Release artifacts
```

***

# 79. Reproducible Release

A clean checkout of Mail Organizer should be able to reproduce the release build using documented prerequisites.

Do not depend on undocumented local files.

***

# 80. Build Reproducibility

Test release generation from a clean state.

Verify:

- dependencies resolve
- configuration is available
- no hidden local paths
- no developer-only files required

***

# 81. Git Cleanliness

Before finalizing:

```text
git status
git diff
```

Verify only intended Mail Organizer changes exist.

***

# 82. Secret Scan Again

After production configuration:

repeat secret scanning.

Pay particular attention to:

- Gradle
- XML
- JSON
- properties
- environment files
- generated artifacts
- Git history

***

# 83. Generated Artifact Review

Inspect release artifacts for accidental inclusion of:

- API keys
- OAuth secrets
- test URLs
- debug menus
- test data
- private certificates

***

# 84. APK/AAB Inspection

Inspect the final artifact.

Verify:

- application ID
- version
- permissions
- signing
- embedded resources
- network configuration
- debug components

***

# 85. ProGuard/R8 Release Test

If minification is enabled:

run the production smoke test against the minified build.

Verify:

- Room
- serialization
- OAuth
- Gmail
- Calendar
- Tasks
- WorkManager
- automation
- AI if implemented

***

# 86. Obfuscation

Do not disable R8 merely because a release issue occurs.

Fix correct keep rules instead.

***

# 87. Production Error Handling

Verify user-facing production errors contain no:

- stack traces
- internal class names
- tokens
- endpoints
- debug information

***

# 88. Production Permissions

Verify first launch requests only appropriate permissions.

Do not request unrelated Android runtime permissions.

***

# 89. Notification Permission

If notifications are used:

request permission contextually and explain why.

Do not request it without a real notification feature.

***

# 90. Storage Permission

Do not request broad storage permissions for ordinary app-private database/data storage.

***

# 91. Contacts/Location/Camera/Microphone

Verify none are requested unless an actual implemented requirement needs them.

***

# 92. Accessibility

Verify no AccessibilityService is required for Gmail functionality.

***

# 93. Privacy Claims vs Implementation

Perform a literal comparison:

```text
privacy policy
↕
Privacy Center
↕
requirements.md
↕
actual code
```

Every major statement must agree.

***

# 94. Third-Party Data Sharing

Inventory external providers:

- Google
- AI provider
- any other actual provider

Document exactly what data is sent and why.

***

# 95. AI Data Boundary

If AI exists, define:

```text
Mail Organizer
↓
minimal selected context
↓
AI provider
↓
structured result
↓
validation
↓
Mail Organizer
```

AI must never receive unnecessary account data.

***

# 96. AI Opt-Out

Verify the user can disable AI without disabling core Gmail organization.

***

# 97. Offline Without AI

Verify core functionality remains useful if:

- AI disabled
- AI provider unavailable
- network unavailable

***

# 98. Google API Failure Without Core Failure

Verify Calendar/Tasks failures do not break:

- Mail
- Search
- Categories
- Companies
- local rules
- analytics

***

# 99. OAuth Failure Without Data Corruption

Verify authentication failure does not corrupt local data.

***

# 100. Production Migration

If release introduces database migrations, test them against realistic existing data.

***

# 101. Rollback Planning

Document what happens if the production release must be withdrawn.

Do not perform an actual rollback unless explicitly requested.

***

# 102. Version Rollback Safety

Ensure future database migrations do not assume users can freely downgrade.

Document irreversible schema changes.

***

# 103. Update Strategy

Document:

- version numbering
- migration process
- release artifact process
- signing
- OAuth configuration
- Play release workflow

***

# 104. Store Review Risk

Review for obvious rejection risks:

- unnecessary permissions
- misleading claims
- inaccessible privacy policy
- broken OAuth
- missing account deletion explanation
- unsafe user-generated content handling
- deceptive functionality
- hidden behavior

Do not attempt to bypass Play policies.

***

# 105. Sensitive Permissions Review

If any sensitive/restricted permission exists, verify the corresponding Play Console declaration requirements before submission.

Do not submit until requirements are satisfied.

***

# 106. OAuth Verification Preparation

If Google OAuth verification is required:

prepare:

- application description
- scopes justification
- privacy policy
- demo/test account if required
- verification materials
- explanation of Gmail usage

Do not falsely claim verification is complete.

***

# 107. Gmail Restricted/Sensitive Scope Review

Determine whether current Gmail scopes trigger additional Google verification requirements.

Do not assume read-only automatically means no verification.

Document the exact status.

***

# 108. Production Test Account

Maintain a controlled account for release smoke testing.

Never embed its credentials.

***

# 109. Test Data Cleanup

After production smoke tests:

clean controlled test data created by the application where appropriate.

***

# 110. Play Store Listing Accuracy

Every screenshot and description must correspond to actual functionality in the tested build.

No fake:

- AI
- integrations
- categories
- automation
- analytics
- actions

***

# 111. Store Screenshots

Ensure screenshot data is:

- synthetic
- anonymized
- fictional
- non-sensitive

***

# 112. Store Description

Do not describe future features as current functionality.

***

# 113. Release Notes

Prepare initial release notes describing actual implemented capabilities.

***

# 114. Support Documentation

Prepare concise user-facing documentation for:

- first launch
- Gmail connection
- permissions
- synchronization
- account management
- Calendar/Tasks
- optional AI
- privacy
- disconnect/delete

***

# 115. Production Checklist

Create a final checklist:

```text
[ ] Application identity
[ ] Version
[ ] Release signing
[ ] OAuth
[ ] Google APIs
[ ] Scopes
[ ] Privacy policy
[ ] Data Safety
[ ] Account deletion
[ ] Permissions
[ ] Manifest
[ ] Release build
[ ] Release install
[ ] Release smoke test
[ ] Device verification
[ ] Screenshots
[ ] Store metadata
[ ] Support
[ ] Security
[ ] Secrets
[ ] Git review
```

***

# 116. Final Release Artifact

Generate the intended release artifact(s).

Do not upload or publish them unless explicitly authorized.

***

# 117. Artifact Integrity

Record:

- artifact name
- version
- application ID
- build variant
- signing identity
- checksum if appropriate

Do not expose private signing information.

***

# 118. Final Device Validation

Install the exact release artifact that would be distributed.

Verify:

- launch
- OAuth
- Gmail
- search
- categories
- actions
- integrations
- multi-account
- offline
- background
- privacy

***

# 119. Final Release Logs

Inspect logcat after the release smoke test.

No:

- crashes
- ANRs
- token leakage
- debug output
- repeated retry loops

***

# 120. Final Security Review

Recheck:

- secrets
- OAuth
- signing
- network
- manifest
- permissions
- WebView
- database
- account isolation
- external actions
- automation
- AI

***

# 121. Final Privacy Review

Recheck:

- local processing
- Google data usage
- AI data usage
- analytics
- storage
- deletion
- privacy policy
- Privacy Center

***

# 122. Final Store Compliance Review

Review applicable current requirements from official Google Play/Android documentation.

Do not rely on outdated assumptions.

If a requirement cannot be verified, record it as:

```text
NOT VERIFIED — external confirmation required
```

***

# 123. No Publication

This phase must not:

- publish the app
- roll out a production release
- send production users to an unfinished build
- permanently alter unrelated Play Store applications

Publishing belongs to an explicitly authorized release operation.

***

# 124. Final Documentation

Update:

```text
docs/development-status.md
```

with:

- production readiness
- OAuth status
- Play preparation status
- release artifact
- known blockers
- unresolved external requirements

***

# 125. Editor Rules Update

Update `editor-rules.md` with permanent production lessons discovered during this phase.

Examples:

- production OAuth must match release signing
- production configuration must never contain secrets
- store claims must match actual behavior
- release builds require real-device verification
- sensitive scopes require explicit justification
- production artifacts must be inspected before distribution

Do not regenerate the entire file.

***

# 126. Spec Update

Update `spec.md` only after genuine verification.

Phase 29 may be marked `[x]` only when the acceptance criteria are actually satisfied.

If blocked by Google verification, Play Console access, domain ownership, or another external prerequisite:

mark the relevant state clearly as blocked rather than pretending it is complete.

***

# 127. Git Review

Run:

```text
git status
git diff
```

Verify:

- only Mail Organizer changes
- no secrets
- no test credentials
- no private test data
- no unrelated project changes
- no generated junk

***

# 128. Final Acceptance Criteria

Phase 29 is complete only when:

- Production application ID verified.
- Release variant verified.
- Debug/release separation verified.
- Release signing verified.
- Signing secrets protected.
- Google Cloud project verified.
- Required Google APIs verified.
- OAuth clients verified.
- Release fingerprint verified.
- OAuth consent configuration reviewed.
- OAuth scopes minimized.
- Incremental authorization implemented where appropriate.
- Gmail production OAuth verified.
- Calendar production configuration verified if implemented.
- Tasks production configuration verified if implemented.
- Gmail write authorization verified if implemented.
- AI production configuration reviewed if implemented.
- Privacy policy prepared/reviewed.
- Data Safety information prepared.
- Account deletion behavior documented.
- Privacy Center reviewed.
- Manifest reviewed.
- Permissions reviewed.
- Backup behavior reviewed.
- Network security reviewed.
- Debug components excluded from release.
- Release endpoints verified.
- Release logging reviewed.
- Release build generated.
- Release artifact inspected.
- Release artifact installed on a real device/emulator.
- Release OAuth smoke test passed.
- Release Gmail smoke test passed.
- Calendar smoke test passed if implemented.
- Tasks smoke test passed if implemented.
- Multi-account smoke test passed.
- Offline smoke test passed.
- Background smoke test passed.
- Security smoke test passed.
- Privacy smoke test passed.
- Store screenshots prepared.
- Store metadata prepared.
- Support information prepared.
- Release notes prepared.
- Production requirements reviewed.
- Google OAuth verification status documented.
- Play Store prerequisites documented.
- No publication performed.
- Git review completed.
- Sibling projects untouched.
- `editor-rules.md` updated where necessary.
- `spec.md` updated.
- `docs/development-status.md` updated.

***

# 129. Completion Protocol

When finished:

1. Verify Phase 28 was genuinely complete.
2. Verify production application identity.
3. Verify release signing.
4. Verify Google Cloud configuration.
5. Verify OAuth clients and fingerprints.
6. Verify OAuth scopes.
7. Verify Gmail production connection.
8. Verify Calendar/Tasks production configuration where applicable.
9. Review AI production configuration where applicable.
10. Review privacy policy.
11. Review Data Safety information.
12. Review permissions.
13. Review manifest.
14. Review backup behavior.
15. Review network security.
16. Build release artifact.
17. Inspect artifact.
18. Install exact release artifact.
19. Run release smoke tests.
20. Verify OAuth.
21. Verify Gmail.
22. Verify Calendar/Tasks.
23. Verify multi-account.
24. Verify offline.
25. Verify background behavior.
26. Inspect logcat.
27. Capture production-quality screenshots.
28. Review store metadata.
29. Review support documentation.
30. Perform final security review.
31. Perform final privacy review.
32. Perform current Play Store requirement review.
33. Document external blockers.
34. Update `editor-rules.md`.
35. Update `spec.md`.
36. Update development status.
37. Review Git.
38. Confirm no sibling project was modified.
39. Mark Phase 29 complete only when genuinely verified.
40. Report the production-readiness result and any remaining blockers.
41. **STOP.**

***
 
# 130. Next Phase

The next phase is:

**Phase 30 — Final Production Hardening & Release**

Do not execute Phase 30 during this session.