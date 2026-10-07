# PHASE 3 — GOOGLE OAUTH & GMAIL CONNECTION

You are now executing:

**Phase 3 — Google OAuth & Gmail Connection**

This phase must only begin after Phase 2 has been verified as complete.

Do not execute Phase 4 or any later phase automatically.

The project follows a strict sequential execution model:

```text
Phase 0
  ↓
Phase 1
  ↓
Phase 2
  ↓
Phase 3 ← YOU ARE HERE
  ↓
Phase 4
  ↓
...
```

Complete this phase, verify it, update the project documentation, and STOP.

Do not implement Gmail synchronization, classification, Calendar, Tasks, AI, or other future functionality in this phase.

---

# 1. DISCOVER THE PROJECT INSTRUCTION FOLDER

Before doing anything:

1. Locate the Mail Organizer instruction folder.
2. Read the available project `.md` files.
3. Read:
    - `requirements.md`
    - `spec.md`
    - `design.md`
    - `editor-rules.md`
    - relevant architecture/status documents
    - this Phase 3 prompt
4. Determine the actual current implementation state.
5. Confirm Phase 2 is complete.
6. Identify the Mail Organizer project root.

Do not assume the current working directory is the Mail Organizer project.

---

# 2. STRICT MULTI-PROJECT ISOLATION

The workspace may contain multiple unrelated Android applications.

You MUST identify the Mail Organizer project root before running:

- Gradle
- Git
- ADB
- Android tooling
- file modifications
- build commands
- tests

Only operate inside the Mail Organizer project.

Never modify sibling projects.

Never modify another project's:

- source
- Gradle files
- SDK configuration
- build configuration
- dependencies
- manifests
- signing configuration
- environment files
- tests
- resources
- generated files
- Git repository

Never upgrade or downgrade global Android SDK components just because another project uses a different version.

Never run Gradle from a parent workspace directory when that could target another project.

---

# 3. UPDATE `editor-rules.md`

Before implementation, update `editor-rules.md` if necessary.

Add any permanent rules discovered during this phase, especially rules concerning:

- Google OAuth security
- Gmail API security
- least-privilege scopes
- token handling
- account isolation
- OAuth disconnect/revocation
- OAuth failure handling
- test-account handling
- no password collection
- no token logging
- production-vs-development OAuth configuration
- Google Cloud project isolation
- multi-account authorization
- external permission transparency

Do this yourself.

Do not ask the user to manually edit `editor-rules.md`.

Do not replace the whole file unnecessarily.

---

# 4. PHASE OBJECTIVE

The objective of Phase 3 is to establish a secure, production-oriented Google authentication and Gmail authorization foundation.

At the end of this phase, a user should be able to:

```text
Open Mail Organizer
        ↓
Choose "Connect Gmail"
        ↓
Google authentication
        ↓
Grant the requested Gmail permission
        ↓
Return to Mail Organizer
        ↓
See the connected Gmail account
```

The app must NOT yet synchronize the mailbox.

The Gmail connection should only establish authorization and account identity.

---

# 5. IMPORTANT: USE OFFICIAL GOOGLE AUTHENTICATION

Use official Google-supported Android authentication mechanisms and APIs.

Do NOT implement:

- Gmail website scraping
- embedded Gmail login pages
- username/password collection
- cookie extraction
- browser automation for Gmail
- AccessibilityService-based Gmail control
- reverse-engineered Gmail APIs
- unofficial authentication hacks

The user must authenticate through Google's supported OAuth flow.

---

# 6. DETERMINE THE CURRENT GOOGLE AUTHENTICATION STACK

Inspect the project.

Determine whether it currently uses:

- Credential Manager
- Google Identity Services
- Google Sign-In
- OAuth libraries
- Google API client libraries
- custom authentication abstractions

Do not introduce redundant authentication systems.

If an existing supported authentication foundation is present, use it where appropriate.

If it is outdated or incompatible with the requirements, document why it must be replaced before doing so.

---

# 7. GOOGLE CLOUD PROJECT

Establish the development Google Cloud configuration required by Mail Organizer.

Verify or configure the appropriate Google Cloud project.

The project must have the required APIs enabled.

At minimum, Gmail API must be enabled.

Do not enable unrelated Google APIs unnecessarily.

Future APIs such as:

- Google Calendar API
- Google Tasks API

belong to later phases unless the current implementation requires only preliminary configuration.

Do not request unnecessary permissions now.

---

# 8. GOOGLE CLOUD PROJECT ISOLATION

If multiple applications/projects are being developed by the user:

Do not modify another application's Google Cloud configuration.

Verify that the OAuth configuration being used belongs specifically to Mail Organizer.

Do not:

- delete another project's OAuth clients
- modify another app's consent screen
- change another application's test users
- rotate another application's credentials
- change another project's APIs
- alter unrelated Google Cloud resources

Document which Google Cloud project belongs to Mail Organizer without exposing sensitive credentials.

---

# 9. ANDROID APPLICATION IDENTITY

Verify that the Android OAuth configuration matches the Mail Organizer application.

Confirm:

- package/application ID
- signing certificate SHA-1/SHA-256 as appropriate
- debug signing configuration
- release signing configuration
- application identity

The Android OAuth client must correspond to the correct application identity.

Do not change the package ID casually.

Do not use another project's certificate fingerprint.

---

# 10. DEBUG AND RELEASE OAUTH CONFIGURATION

Clearly distinguish:

### Development

Used for:

- local development
- debug builds
- test accounts
- device testing

### Production

Used later for:

- production release
- Play Store
- production OAuth verification
- real users

Do not mix credentials/configuration between unrelated projects.

Do not commit sensitive OAuth secrets.

Remember:

- OAuth client IDs are generally identifiers, not passwords.
- access tokens are sensitive.
- refresh tokens are sensitive.
- signing private keys are sensitive.
- client secrets must be protected where applicable.

Never place actual secrets into source code.

---

# 11. OAUTH CONSENT SCREEN

Configure the Google OAuth consent experience appropriately for development.

Ensure it clearly identifies:

- application name
- developer/app identity
- requested access
- privacy policy information where required
- appropriate contact/support information where required

Do not misrepresent the purpose of the application.

The app should request only the access necessary for the current phase.

---

# 12. LEAST PRIVILEGE — CRITICAL

For Phase 3, use the minimum Gmail scope required.

The initial Gmail integration should use a read-only scope such as:

```text
https://www.googleapis.com/auth/gmail.readonly
```

Do NOT request:

```text
gmail.modify
gmail.send
gmail.insert
gmail.compose
```

unless explicitly required by this phase.

They are not required.

Future write capabilities belong to later phases.

---

# 13. PERMISSION TRANSPARENCY

The user must be able to understand what access the app is requesting.

Do not display misleading language such as:

> "Sign in to continue"

if the actual operation is granting access to Gmail data.

Prefer transparent messaging such as:

> Connect your Gmail account so Mail Organizer can read and organize your email locally.

The exact UI copy should follow the product's design language.

---

# 14. DO NOT REQUEST EVERYTHING AT ONCE

Do not request:

- Calendar access
- Tasks access
- Drive access
- Contacts access
- Gmail write access
- unrelated Google permissions

during this phase.

Each integration should be independently authorized later.

This keeps the permission model understandable and reduces unnecessary access.

---

# 15. ACCOUNT MODEL INTEGRATION

Connect the OAuth result to the Account persistence model created in Phase 2.

After successful authorization, create or update the local account record.

The account record should contain appropriate identity information such as:

- internal account ID
- Google account identity where appropriate
- email address
- display name if available
- provider
- connected state
- timestamps

Do not store OAuth tokens as ordinary account fields.

---

# 16. TOKEN STORAGE — CRITICAL SECURITY REQUIREMENT

Access tokens and refresh tokens are sensitive.

Do NOT store them in:

- Room email tables
- ordinary SharedPreferences
- plaintext files
- logs
- UI state
- analytics
- screenshots
- database debug output

Use an appropriate secure credential/token storage strategy for Android.

The authentication layer must own token lifecycle management.

The rest of the application should not directly manipulate raw token storage.

---

# 17. AUTHENTICATION ABSTRACTION

Create a clean authentication boundary.

For example:

```text
Authentication
    ↓
Google OAuth Provider
    ↓
Secure Token Storage
```

The Gmail client should depend on an authentication abstraction rather than directly knowing where tokens are stored.

This will make future testing and authentication changes safer.

---

# 18. TOKEN REFRESH

The authentication layer must support token expiration/refresh appropriately.

Do not implement fragile logic such as:

```text
if token expired:
    ask user to log in every time
```

Instead, use the supported Google authentication/token lifecycle mechanisms.

Handle:

- expired access token
- refresh
- invalid refresh state
- revoked authorization
- account removal
- user denial

Do not log tokens while debugging these flows.

---

# 19. ACCOUNT SELECTION

Support selecting a Google account during connection.

The user should be able to choose the account they want to connect.

Do not assume the currently signed-in Android account is automatically the desired Gmail account.

Do not silently connect a random account.

---

# 20. MULTI-ACCOUNT FOUNDATION

The app supports multiple Gmail accounts.

Therefore:

```text
Account A
Account B
Account C
```

must be treated as independent authorization contexts.

Phase 3 does not need to implement the full unified inbox.

However, it must establish the foundation for:

- connect account
- disconnect account
- account identity
- account status
- account-specific authorization
- account-specific token handling

Do not mix authorization state between accounts.

---

# 21. CONNECT ACCOUNT UI

Implement a proper connection screen/state.

It should clearly show:

### Not connected

- Connect Gmail action
- explanation of required access

### Connecting

- loading state
- no duplicate connection actions

### Connected

- account email
- connection status
- disconnect action

### Failed

- useful error
- retry option

### Permission denied

- clear explanation
- retry/permission path

Do not show fake connected accounts.

---

# 22. DISCONNECT ACCOUNT

Implement a safe disconnect mechanism.

Disconnect should:

- remove the account's active connection state
- clear associated authentication state appropriately
- prevent future Gmail API use
- preserve or delete local data according to the product's explicit data policy
- clearly communicate what will happen

Do not silently delete local mail data unless the user has explicitly chosen that behavior.

If disconnecting requires local data cleanup, make it explicit.

---

# 23. GOOGLE REVOCATION

Where supported and appropriate, provide a proper revocation path.

Understand the difference between:

```text
Disconnect from Mail Organizer
```

and:

```text
Revoke Google's authorization
```

The UI should not falsely claim that disconnecting locally necessarily revokes Google's authorization.

Handle revocation failures gracefully.

---

# 24. OAUTH FAILURE STATES

Handle at minimum:

- user cancels
- user denies
- network unavailable
- Google authentication failure
- invalid OAuth configuration
- redirect/configuration mismatch
- account unavailable
- token refresh failure
- authorization revoked
- API unavailable
- unsupported account state

Every error must produce a useful user-facing state.

Avoid exposing raw stack traces.

---

# 25. DO NOT LEAK GOOGLE ERRORS

Internal errors may contain technical information.

Do not directly show raw exception messages to users.

Instead map errors to safe categories such as:

```text
Connection cancelled
Permission denied
Google authentication failed
Network unavailable
Authorization expired
Configuration problem
Unexpected error
```

Log technical details safely without exposing secrets.

---

# 26. GMAIL API CLIENT FOUNDATION

Establish the Gmail API client architecture.

The client should eventually support:

```text
GmailService
    ↓
Authenticated Google API Client
    ↓
Gmail API
```

But Phase 3 should not implement mailbox synchronization.

At most, validate that the authorized Gmail API client can perform a minimal safe authenticated operation required to prove authorization.

Do not download the mailbox.

---

# 27. AUTHENTICATED API SMOKE TEST

After OAuth succeeds, perform a minimal authenticated Gmail API validation if appropriate.

The test should prove:

```text
OAuth succeeded
+
token works
+
Gmail API authorization works
```

Do not:

- fetch hundreds/thousands of messages
- synchronize inbox
- classify email
- build local search index
- process email bodies

A minimal account/profile-level request is preferable where supported.

---

# 28. API FAILURE HANDLING

If the Gmail API smoke test fails:

- identify authentication vs permission vs network failure
- show an appropriate UI state
- log only safe diagnostic information
- do not crash the application
- do not retry infinitely

Do not implement the complete retry engine yet.

---

# 29. GOOGLE API RATE LIMITING

Do not aggressively poll Gmail during this phase.

There should be no mailbox polling.

If a test API request is necessary, perform only what is required.

Future synchronization will implement appropriate rate-limit handling.

---

# 30. NO BACKEND

Do not create a custom authentication backend.

Do not send OAuth tokens through your own server.

The initial architecture should be:

```text
Android App
    ↓
Google OAuth
    ↓
Google Gmail API
```

not:

```text
Android App
    ↓
Mail Organizer Server
    ↓
Google
```

unless a later product requirement explicitly introduces a backend.

---

# 31. NETWORK SECURITY

Verify that network communication is appropriate.

Use HTTPS/TLS for network traffic.

Do not enable broad cleartext traffic simply to make development work.

If local development networking is necessary:

- isolate it to debug configuration
- use ADB reverse where appropriate
- do not weaken production network security

---

# 32. ADB / DEVICE VALIDATION

This phase MUST be validated on an Android device/emulator whenever available.

Use:

- Gradle
- ADB
- device installation
- application launch
- logcat
- screenshots
- screen capture/recording where useful

The validation loop is:

```text
Code
 ↓
Gradle build
 ↓
Install
 ↓
Launch
 ↓
Connect Gmail
 ↓
Observe OAuth flow
 ↓
Capture screenshots
 ↓
Inspect logs
 ↓
Disconnect
 ↓
Reconnect
 ↓
Retest
```

---

# 33. INSTALLATION MUST BE PROJECT-SCOPED

Before installing:

1. confirm Mail Organizer package ID
2. confirm APK belongs to Mail Organizer
3. install only that APK
4. do not uninstall unrelated applications
5. do not clear unrelated app data

Do not use broad device reset operations.

---

# 34. ADB REVERSE

If the development environment requires localhost access during OAuth/API development:

Use:

```text
adb reverse
```

appropriately.

Before modifying reverse mappings:

- inspect current mappings where possible
- identify the required Mail Organizer port
- avoid disturbing mappings belonging to another project

If no reverse connection is needed, do not create one.

---

# 35. SCREENSHOT OAUTH STATES

Capture and inspect screenshots for appropriate states:

### State 1

Mail Organizer not connected.

### State 2

Connection explanation.

### State 3

OAuth/account-selection transition if capturable and appropriate.

### State 4

Connected Gmail account.

### State 5

Connection failure.

### State 6

Permission denied.

### State 7

Disconnect confirmation/result.

Do not capture sensitive OAuth tokens or private Google account information unnecessarily.

Blur/redact sensitive information in any documentation if needed.

---

# 36. SCREEN RECORDING

Where useful, record the connection flow to validate:

- transitions
- loading
- returning from Google
- duplicate taps
- navigation
- failure handling

Do not retain unnecessary recordings containing personal account information.

---

# 37. LOGCAT VALIDATION

During OAuth testing inspect logs for:

- crashes
- token errors
- configuration errors
- API errors
- Activity lifecycle problems
- authentication failures

Verify that logs do NOT contain:

- access tokens
- refresh tokens
- authorization headers
- passwords
- sensitive email information

If sensitive logging appears, fix it before completing the phase.

---

# 38. TEST ACCOUNT SAFETY

Use a controlled test Gmail account for development where possible.

Do not use a user's primary personal account simply because it is convenient.

Google Cloud OAuth development may require configured test users.

Configure only the Mail Organizer test account(s).

Do not alter unrelated applications' OAuth test users.

---

# 39. DEVELOPMENT OAUTH LIMITATIONS

Understand and document any development OAuth limitations, including:

- test-user requirements
- consent-screen state
- scope restrictions
- publishing/verification requirements
- developer/testing limitations

Do not work around Google's OAuth security mechanisms.

Do not use unofficial bypasses.

---

# 40. PRODUCTION OAUTH PREPARATION

Do not attempt full production verification during this phase unless required.

However, document what will eventually be needed:

- production consent configuration
- verified domains where applicable
- privacy policy
- app identity
- production OAuth client
- release signing certificate
- Google verification for sensitive/restricted scopes if applicable
- Play Store release configuration

Do not claim production approval has been obtained.

---

# 41. NO GMAIL WRITE ACCESS

This is mandatory.

Phase 3 must not request or implement:

- archive
- delete
- mark read/unread
- label modification
- send
- reply
- move
- trash

Those require additional permissions and belong to later phases.

The current Gmail permission should remain read-only.

---

# 42. NO EMAIL CONTENT PROCESSING

Do not process mailbox contents during Phase 3.

Do not:

- classify messages
- detect companies
- extract meetings
- extract deadlines
- build action cards
- index email
- run AI
- download attachments

The application only needs to establish:

```text
Google account
+
Gmail authorization
+
authenticated API capability
```

---

# 43. TEST MATRIX

Test at minimum:

### Successful authorization

```text
Install→ Launch
→ Connect
→ Google account
→ Grant
→ Return
→ Connected
```

### Cancel

```text
Connect
→ Cancel
→ Return
→ Not connected
```

### Denied

```text
Connect
→ Deny
→ Return
→ Permission denied state
```

### Reconnect

```text
Connect
→ Success
→ Disconnect
→ Connect again
→ Success
```

### Restart

```text
Connected
→ Force stop
→ Relaunch
→ Correct connection state
```

### Token/auth state

Where safely testable:

```text
Expired/invalid authorization
→ appropriate recovery
```

### Offline

```text
No network
→ Connect attempt
→ graceful failure
```

Do not endlessly retry.

---

# 44. MULTI-ACCOUNT TESTING

If the environment permits, test:

```text
Account A → connect
Account B → connect
Account A → remains connected
Account B → remains connected
```

Verify that:

- identities remain distinct
- token state does not mix
- account records remain distinct
- disconnecting A does not disconnect B

Do not implement the full unified inbox yet.

---

# 45. DATABASE VALIDATION

Verify that OAuth/account connection updates the correct account record.

For example:

```text
Google Account A
      ↓
Local Account A
```

and:

```text
Google Account B
      ↓
Local Account B
```

Never create duplicate account records unnecessarily for the same connected account.

---

# 46. SECURITY REVIEW

Before completion verify:

- [ ] no Gmail password collection
- [ ] no Gmail scraping
- [ ] no AccessibilityService
- [ ] no cookies
- [ ] no unofficial Gmail authentication
- [ ] minimum OAuth scope
- [ ] no Gmail write scope
- [ ] no tokens in logs
- [ ] no tokens in Room
- [ ] no tokens in SharedPreferences
- [ ] no tokens in UI state
- [ ] no secrets committed
- [ ] account isolation
- [ ] OAuth errors handled
- [ ] disconnect handled
- [ ] revocation behavior understood

---

# 47. PERFORMANCE / BATTERY

Do not create background Gmail polling in Phase 3.

The app should not:

- continuously poll Gmail
- wake the device unnecessarily
- repeatedly refresh OAuth state
- make repeated API calls

Synchronization belongs to Phase 4 and later.

---

# 48. DO NOT TOUCH OTHER PROJECTS

Before final verification, inspect the workspace and Git state again.

Confirm:

- only Mail Organizer files changed
- only Mail Organizer Gradle files changed
- no sibling project SDK configuration changed
- no sibling project dependencies changed
- no sibling project build was modified
- no unrelated APK was installed/uninstalled
- no unrelated device data was cleared
- no unrelated Google Cloud project was changed

If you discover an accidental modification:

STOP.

Identify it.

Restore only the accidental change if it is safe to do so.

Do not destroy legitimate work belonging to another project.

---

# 49. FINAL BUILD

Run the Mail Organizer project's own Gradle build.

Use its own Gradle wrapper.

Verify:

- compilation
- unit tests
- relevant Android tests
- lint/static checks where configured
- debug APK generation

Fix all issues introduced during Phase 3.

---

# 50. FINAL DEVICE VALIDATION

Install the final debug APK on the test device/emulator.

Run:

```text
Launch
→ Connect Gmail
→ Authenticate
→ Grant permission
→ Return
→ Verify connected account
→ Restart app
→ Verify state
→ Disconnect
→ Verify disconnected state
```

Capture screenshots of important states.

Inspect logcat.

If anything fails:

```text
Fix
→ Build
→ Install
→ Retest
```

Do not mark the phase complete until the flow works.

---

# 51. FINAL GIT REVIEW

From the Mail Organizer Git root:

Inspect:

```text
git status
git diff
```

Verify:

- only intended Mail Organizer files changed
- no secrets
- no OAuth tokens
- no generated credentials
- no APK artifacts accidentally committed
- no unrelated project modifications

Do not commit unless explicitly instructed.

---

# 52. UPDATE `spec.md`

After verification:

Update the Phase 3 section in `spec.md`.

Only mark tasks complete after actual verification.

Do not mark Phase 4 or future phases complete.

---

# 53. UPDATE PROJECT STATUS

Update `docs/development-status.md` if present.

Record:

- OAuth implementation state
- Gmail API authorization state
- test configuration
- known limitations
- security decisions
- unresolved issues

Do not record sensitive credentials.

---

# 54. UPDATE `editor-rules.md`

Before closing Phase 3, update `editor-rules.md` with any permanent rules discovered during this implementation.

Especially preserve:

- OAuth least privilege
- Gmail read-only by default
- token security
- Google Cloud project isolation
- multi-account authorization isolation
- no password collection
- no Gmail scraping
- no AccessibilityService
- no sensitive logging
- development/production OAuth separation
- device/package isolation
- multi-project workspace isolation
- sequential phase execution

Do this yourself.

Do not ask the user to manually copy anything.

---

# 55. PHASE 3 ACCEPTANCE CRITERIA

Phase 3 is complete only when:

### Google Cloud

- [ ] correct Mail Organizer Google Cloud project identified
- [ ] Gmail API enabled
- [ ] OAuth configuration established
- [ ] Android OAuth identity verified
- [ ] development/test configuration established

### Authentication

- [ ] official Google OAuth flow works
- [ ] account selection works
- [ ] successful authorization works
- [ ] cancellation handled
- [ ] denial handled
- [ ] refresh/invalid authorization handled appropriately
- [ ] disconnect works
- [ ] account identity persisted correctly

### Permissions

- [ ] least-privilege Gmail scope used
- [ ] read-only Gmail access only
- [ ] no Gmail write scope
- [ ] no unrelated Google scopes

### Security

- [ ] tokens securely handled
- [ ] tokens never logged
- [ ] tokens not stored in ordinary Room tables
- [ ] no passwords
- [ ] no cookies
- [ ] no Gmail scraping
- [ ] no AccessibilityService
- [ ] no secrets committed

### Gmail API

- [ ] authenticated Gmail API client established
- [ ] minimal API smoke test works
- [ ] API failure handled
- [ ] no mailbox synchronization implemented

### Multi-account

- [ ] account identity isolated
- [ ] authorization state isolated
- [ ] Account A/B testing performed where possible

### UI

- [ ] connect state
- [ ] connecting state
- [ ] connected state
- [ ] denied state
- [ ] error state
- [ ] disconnect flow
- [ ] accessibility
- [ ] light/dark theme

### Device

- [ ] Gradle build passes
- [ ] APK installed
- [ ] app launched
- [ ] OAuth tested on device/emulator
- [ ] screenshots inspected
- [ ] logcat inspected
- [ ] runtime issues fixed

### Workspace safety

- [ ] only Mail Organizer project modified
- [ ] sibling projects untouched
- [ ] sibling Gradle configurations untouched
- [ ] unrelated SDK configurations untouched
- [ ] unrelated device applications untouched
- [ ] unrelated Google Cloud projects untouched

---

# 56. FINAL PHASE REPORT

Provide:

## Phase 3 Status

`COMPLETE` / `PARTIAL` / `BLOCKED`

## OAuth Architecture

Explain the implemented authentication flow.

## Gmail Permission

State exactly which Gmail scope is currently requested.

## Google Cloud Configuration

Describe the configured components without exposing secrets.

## Account Handling

Explain account persistence and isolation.

## Token Security

Explain where token handling occurs without exposing credentials.

## Gmail API

Report the minimal authenticated API test.

## Device Validation

Report:

- device/emulator
- Android/API level
- build
- installation
- OAuth test
- disconnect/reconnect
- screenshots
- logcat

## Security Validation

Report the security checks.

## Workspace Isolation

Explicitly confirm that sibling Android projects were not modified.

## Files Changed

List important files.

## Known Issues

List unresolved issues.

## Deferred Work

Especially:

- mailbox synchronization
- classification
- search
- Calendar
- Tasks
- AI
- Gmail write operations

## Acceptance Criteria

Show every Phase 3 criterion and its status.

## Next Phase

The next incomplete phase is:

**Phase 4 — Gmail Synchronization Engine**

Do not execute Phase 4 automatically.

---

# FINAL OPERATING MODEL

Continue following this exact lifecycle:

```text
DISCOVER INSTRUCTION FOLDER
        ↓
READ PROJECT RULES
        ↓
IDENTIFY MAIL ORGANIZER ROOT
        ↓
IDENTIFY CURRENT PHASE
        ↓
READ CURRENT PHASE PROMPT
        ↓
IMPLEMENT ONLY CURRENT PHASE
        ↓
GRADLE BUILD
        ↓
INSTALL ONLY MAIL ORGANIZER
        ↓
RUN ON DEVICE
        ↓
SCREENSHOT / SCREEN RECORD
        ↓
INSPECT LOGCAT
        ↓
TEST
        ↓
FIX
        ↓
REBUILD
        ↓
REINSTALL
        ↓
RETEST
        ↓
SECURITY REVIEW
        ↓
GIT REVIEW
        ↓
UPDATE RULES + STATUS
        ↓
STOP
```

Do not implement the entire application in one shot.

Do not skip phases.

Do not touch sibling projects.

Do not alter unrelated Android SDK/build configuration.

Do not install/uninstall unrelated applications.

Do not weaken Google's OAuth security.

Do not request permissions that the current phase does not need.

Do not mark a phase complete without real verification.

**Phase 3 ends after Gmail authorization is working and verified.**

Stop there.