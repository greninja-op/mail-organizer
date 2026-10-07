# PHASE 3 — GOOGLE OAUTH & GMAIL CONNECTION

## EXECUTION CONTRACT
Read the Mail Organizer instruction folder and confirm Phase 2 is verified complete. Implement Phase 3 only. Do not execute Phase 4 or any later phase. The required lifecycle is DISCOVER → READ RULES → IDENTIFY ROOT → IMPLEMENT ONLY PHASE 3 → BUILD → TEST → DEVICE QA → SECURITY REVIEW → GIT REVIEW → UPDATE DOCS → STOP.

## 1. DISCOVER THE PROJECT INSTRUCTION FOLDER
Locate the instruction/execution-plan folder. Read requirements.md, spec.md, design.md, editor-rules.md, relevant architecture/status documents and this phase prompt before editing.

## 2. STRICT MULTI-PROJECT ISOLATION
Identify the Mail Organizer root before Gradle, Git, ADB, Android tooling or file changes. Never modify sibling projects or unrelated SDK/build configuration.

## 3. UPDATE editor-rules.md
Before and after implementation, add only genuinely permanent OAuth/Gmail security rules. Preserve unrelated rules; never replace the whole document unnecessarily.

## 4. PHASE OBJECTIVE
Establish secure production-oriented Google authentication and Gmail authorization so the user can connect a Gmail account and see its identity without synchronizing the mailbox.

## 5. OFFICIAL GOOGLE AUTHENTICATION
Use Google's supported Android authentication mechanisms. Do not use Gmail scraping, embedded Gmail login pages, password collection, cookie extraction, browser automation, AccessibilityService or reverse-engineered authentication.

## 6. DETERMINE THE CURRENT AUTH STACK
Inspect Credential Manager, Google Identity Services, Google Sign-In, OAuth libraries, Google API clients and custom auth abstractions. Reuse a supported existing foundation; replace outdated components only with documented justification.

## 7. GOOGLE CLOUD PROJECT
Identify/configure the Google Cloud project belonging to Mail Organizer. Enable Gmail API only as required. Do not enable unrelated APIs.

## 8. GOOGLE CLOUD PROJECT ISOLATION
Do not alter OAuth clients, consent screens, test users, APIs or credentials belonging to other applications. Document the Mail Organizer project identity without exposing secrets.

## 9. ANDROID APPLICATION IDENTITY
Verify application/package ID and debug/release signing certificate fingerprints. Ensure the Android OAuth client matches Mail Organizer exactly. Do not casually change package identity.

## 10. DEBUG AND RELEASE CONFIGURATION
Keep development/test and future production OAuth configuration clearly separated. Never commit secrets, private keys, access tokens or refresh tokens.

## 11. OAUTH CONSENT SCREEN
Configure truthful app identity, requested access, support/contact and privacy information where required. Never misrepresent Gmail access as ordinary sign-in.

## 12. LEAST PRIVILEGE
Use only the minimum read-only Gmail scope required for this phase, such as https://www.googleapis.com/auth/gmail.readonly. Do not request gmail.modify, gmail.send, gmail.insert or gmail.compose.

## 13. PERMISSION TRANSPARENCY
Explain why Gmail access is requested. Use transparent product copy rather than misleading “Sign in to continue” language when the real operation is granting Gmail data access.

## 14. NO UNRELATED PERMISSIONS
Do not request Calendar, Tasks, Drive, Contacts or other Google permissions in Phase 3.

## 15. ACCOUNT MODEL INTEGRATION
Connect successful OAuth identity to the Phase 2 Account persistence model. Store internal identity, provider, email/display identity, connected state and timestamps as appropriate. Never treat tokens as ordinary account fields.

## 16. TOKEN STORAGE
Keep access/refresh tokens out of Room mail/account tables, SharedPreferences, plaintext files, logs, UI state, analytics and screenshots. Use the secure credential/token mechanism owned by the authentication layer.

## 17. AUTHENTICATION ABSTRACTION
Create a clean Authentication → Google OAuth Provider → Secure Token Storage boundary. Gmail clients consume an authentication abstraction rather than knowing raw token storage.

## 18. TOKEN REFRESH
Support the chosen Google authentication/token lifecycle for expiration, refresh, invalid credentials, revoked authorization and account removal. Never ask the user to log in on every normal expiration.

## 19. ACCOUNT SELECTION
Allow explicit Google account selection. Do not silently connect a random or merely currently signed-in Android account.

## 20. MULTI-ACCOUNT FOUNDATION
Treat Account A/B/C as independent authorization contexts. Establish connect, disconnect, identity, status and account-specific token handling without implementing the unified inbox.

## 21. CONNECT ACCOUNT UI
Implement disconnected, connecting, connected and failed/denied states using the Phase 1 UI foundation. Do not show fake accounts or fake connection success.

## 22. DISCONNECT ACCOUNT
Disconnect must remove active local connection state and prevent future Gmail API use. Preserve/delete local mail data only according to explicit product policy; never silently delete it.

## 23. GOOGLE REVOCATION
Distinguish local “Disconnect from Mail Organizer” from revoking Google's authorization. Where supported, provide a truthful revocation path and handle revocation failure safely.

## 24. OAUTH FAILURE STATES
Handle cancellation, denial, network failure, Google auth failure, configuration mismatch, unavailable account, refresh failure, revoked authorization, API failure and unsupported state.

## 25. SAFE ERROR MAPPING
Do not show raw Google/exception messages. Map failures to safe categories such as cancelled, denied, network unavailable, authorization expired, configuration problem and unexpected error.

## 26. GMAIL API CLIENT FOUNDATION
Create a read-only Gmail client boundary behind authenticated Google API access. Do not implement mailbox synchronization.

## 27. AUTHENTICATED API SMOKE TEST
After OAuth, perform only a minimal account/profile-level read-only operation sufficient to prove authorization and identity. Do not fetch the mailbox, classify, index or process bodies.

## 28. API FAILURE HANDLING
Differentiate authentication, permission, network and API failures. Do not crash or retry indefinitely.

## 29. RATE LIMITING
Do not poll Gmail. Perform only the minimum smoke-test request and respect API limits.

## 30. NO BACKEND
Do not create a Mail Organizer authentication backend or proxy OAuth tokens through a custom server. The initial architecture is Android → Google OAuth → Gmail API.

## 31. NETWORK SECURITY
Use HTTPS/TLS. Do not enable broad cleartext traffic. If local development networking is genuinely required, isolate it to debug configuration and use adb reverse only when appropriate.

## 32. ADB / DEVICE VALIDATION
When a device/emulator is available, validate with Gradle, ADB install/launch/force-stop/logcat/dumpsys/screencap/screenrecord as appropriate.

## 33. INSTALLATION MUST BE PROJECT-SCOPED
Verify the Mail Organizer package and APK before installation. Install only that APK. Never uninstall, clear or reset unrelated applications.

## 34. ADB REVERSE
Only use adb reverse if the Mail Organizer flow genuinely requires localhost access. Inspect mappings where possible and do not disturb other projects' ports.

## 35. SCREENSHOT OAUTH STATES
Capture appropriate not-connected, connection explanation, transition, connected, failure, denial and disconnect states. Do not retain unnecessary sensitive account information.

## 36. SCREEN RECORDING
Where useful, inspect OAuth transitions, duplicate taps, loading, browser return and failure handling. Avoid retaining unnecessary private account data.

## 37. LOGCAT VALIDATION
Inspect crashes, lifecycle issues, configuration failures and API errors. Verify access tokens, refresh tokens, authorization headers, passwords and private email data are absent.

## 38. TEST ACCOUNT SAFETY
Prefer a controlled development/test Gmail account. Configure only Mail Organizer test users. Do not alter another application's OAuth test-user list.

## 39. DEVELOPMENT OAUTH LIMITATIONS
Document test-user requirements, consent-screen state, scope restrictions and Google's development/verification limitations. Never bypass them.

## 40. PRODUCTION OAUTH PREPARATION
Document future production consent, verified domains where applicable, privacy policy, production client, release certificate, verification and Play configuration. Do not claim production approval.

## 41. NO GMAIL WRITE ACCESS
Do not implement archive, delete, mark read/unread, label modification, send, reply, move or trash. Read-only access is mandatory.

## 42. NO EMAIL CONTENT PROCESSING
Do not classify, detect companies, extract meetings/deadlines, build action cards/search, run AI or download attachments. Phase 3 establishes authorization only.

## 43. TEST MATRIX — SUCCESS
Install → Launch → Connect → Select Google account → Grant read-only access → Return → Verify connected account identity.

## 44. TEST MATRIX — CANCEL/DENY
Test OAuth cancellation and denial. The app must return to a safe not-connected/permission-denied state with retry.

## 45. TEST MATRIX — RECONNECT
Connect successfully → Disconnect → Connect again → verify the same account can be authorized again without duplicate local account records.

## 46. TEST MATRIX — RESTART
Connected → force-stop → relaunch → verify connection state is restored through the supported credential lifecycle without exposing tokens.

## 47. TEST MATRIX — INVALID/OFFLINE
Test invalid/revoked authorization and no-network connection attempts. Verify safe recovery and bounded retries.

## 48. MULTI-ACCOUNT TESTING
Where available: connect Account A → connect Account B → verify both identities remain distinct → disconnect A → verify B remains connected.

## 49. DATABASE VALIDATION
Verify OAuth updates the correct Phase 2 Account record, avoids duplicate identities and never stores raw tokens in ordinary database fields.

## 50. SECURITY REVIEW
Confirm no passwords, cookies, scraping, AccessibilityService, unofficial auth, broad scopes, Gmail write scopes, token logs, token DB fields or committed secrets.

## 51. PERFORMANCE / BATTERY
Do not create Gmail polling, repeated OAuth refresh loops or unnecessary background work. Phase 4 owns synchronization.

## 52. WORKSPACE SAFETY REVIEW
Before completion inspect workspace and Git state again. Confirm only Mail Organizer changed and no unrelated device apps, ADB mappings, SDK configuration or Google Cloud project changed.

## 53. FINAL BUILD
Run the Mail Organizer Gradle wrapper, compilation, unit tests, relevant Android tests, lint/static checks where configured and debug APK generation. Fix phase-caused failures and rebuild.

## 54. FINAL DEVICE VALIDATION
Install final APK → launch → connect → authenticate → grant → verify account → restart → verify state → disconnect → verify disconnected state. Inspect screenshots and logcat. If anything fails: fix → rebuild → reinstall → retest.

## 55. FINAL GIT REVIEW
From the Mail Organizer Git root inspect git status and git diff. Verify no secrets, tokens, generated credentials, unrelated changes or accidental APK artifacts are present.

## 56. DOCUMENTATION, ACCEPTANCE CRITERIA & STOP
Update spec.md Phase 3 status only after real verification. Update docs/development-status.md if present with OAuth state, scope, test configuration, security decisions and known limitations. Update editor-rules.md with permanent rules. Report: Phase status; OAuth architecture; exact Gmail scope; Google Cloud configuration; account handling; token security; Gmail smoke test; device/API/build results; screenshots/logcat; security validation; workspace isolation; files changed; known issues; deferred work; acceptance-criterion status. The next phase is Phase 4 — Gmail Synchronization Engine. Do not execute it automatically.
