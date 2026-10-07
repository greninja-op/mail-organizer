# Phase 03 — Google OAuth & Gmail Connection

## Execution contract

Read `requirements.md`, `spec.md`, `design.md`, `editor-rules.md`, and this phase completely.

Confirm Phase 03 is the first incomplete phase. Inspect the actual repository before editing.

Implement **official Google OAuth and read-only Gmail connection only**.

Do not implement mailbox synchronization, parsing, classification, search, Calendar, Tasks, Gmail writes, AI, or automation.

---

## 1. Authentication architecture

Establish the Android/KMP authentication boundary.

Shared code should represent:

- account connection state;
- authenticated account identity;
- connection/reconnection state;
- disconnect state;
- authentication errors.

Android owns the browser/OAuth presentation and secure credential/token handling.

Do not place OAuth tokens in:

- source code;
- logs;
- UI state;
- ordinary database columns;
- test fixtures;
- analytics.

Use the platform's secure credential/token storage mechanism appropriate to the final implementation.

---

## 2. Google Cloud configuration

Configure the project for the actual Android application identity.

Verify:

- package/application ID;
- signing certificate identities needed by the environment;
- OAuth client configuration;
- consent configuration;
- required APIs;
- development/test users where applicable.

Use least privilege.

Do not commit Google Cloud secrets or downloaded credential JSON containing secrets.

Document required developer configuration without embedding private values.

---

## 3. Scope discipline

Phase 03 is read-only Gmail access.

Request only the minimum Gmail scopes required for the current connection/smoke-test purpose.

Do not request:

- Gmail modification scopes;
- Calendar scopes;
- Tasks scopes;
- unrelated Google API scopes.

Do not broaden permissions simply because later phases may need them.

---

## 4. Official authentication flow

Implement the real Google OAuth/browser flow.

The app must:

- present a clear connect-account action;
- launch the official authentication mechanism;
- handle successful authentication;
- handle user cancellation;
- handle denial;
- handle invalid configuration;
- handle expired/revoked authorization;
- support retry;
- support reconnect;
- support disconnect.

Never build a fake Google sign-in form.

Never ask for or store a Google password.

Never embed a copied Google login page.

Never use Gmail scraping, cookies or AccessibilityService.

---

## 5. Account creation and local identity

After successful OAuth:

- obtain the authenticated Gmail account identity through the authorized API;
- create/update the local account record from Phase 02;
- maintain account isolation;
- avoid duplicate account records when reconnecting the same account;
- preserve connection state separately from mail synchronization state.

Do not download the mailbox in this phase.

Do not implement the Phase 04 sync engine.

---

## 6. Secure token lifecycle

Implement the minimum secure token lifecycle needed by the application:

- acquire;
- securely store;
- retrieve for authorized API calls;
- refresh when required by the chosen auth library;
- handle revoked/invalid credentials;
- disconnect/revoke/clear local credential state as appropriate.

Never log token values.

Do not persist tokens in the ordinary Mail Organizer database.

When logging auth failures, log safe error categories rather than raw authorization payloads.

---

## 7. Gmail client boundary

Create a testable Gmail client abstraction.

Separate:

`OAuth/token provider → Gmail client → application use case → UI`

The Gmail client must not be called directly from composables.

Keep the client read-only in this phase.

A minimal API smoke-test operation may be implemented, such as retrieving authenticated account/profile identity or another minimal authorized endpoint.

Do not retrieve the mailbox.

---

## 8. Authentication state UX

Use the Android UI foundation from Phase 01.

Provide deliberate states for:

- disconnected;
- connecting;
- browser/authentication in progress;
- connected;
- cancelled;
- denied;
- configuration failure;
- network failure;
- expired/revoked authorization;
- reconnecting;
- disconnecting.

The user must understand whether the account is connected without exposing sensitive credentials.

Respect light/dark mode, accessibility and reduced motion.

---

## 9. Account switcher boundary

Integrate authentication state with the existing account switcher foundation without implementing multi-account synchronization.

The switcher must distinguish:

- connected account;
- disconnected account state;
- currently selected account.

Do not claim an account is synchronized merely because OAuth succeeded.

Authentication and synchronization are separate states.

---

## 10. Minimal Gmail smoke test

After successful authorization, perform only a minimal read-only Gmail API call sufficient to prove:

- token works;
- account identity is correct;
- API configuration is correct;
- account boundary is correct.

Do not fetch messages/threads/history.

Do not persist mailbox content.

Do not begin background sync.

The smoke test must be cancellable/fail safely.

---

## 11. Testing

Create deterministic tests for:

- authentication state transitions;
- cancellation;
- denial;
- reconnect;
- disconnect;
- duplicate account connection;
- invalid/expired credentials;
- account isolation;
- Gmail client error mapping.

Use fake OAuth/token providers in unit tests.

Never place real OAuth tokens in tests.

For integration/manual testing, use a dedicated authorized development account where appropriate.

---

## 12. Security verification

Inspect:

- APK/build outputs;
- source tree;
- logs;
- local DB;
- configuration files.

Confirm no credentials/tokens were committed.

Confirm tokens are not visible in logcat.

Confirm email content is not downloaded or dumped during the smoke test.

Review permissions/scopes and remove anything not required.

---

## 13. Mandatory runtime verification

Using the Gradle wrapper:

- build;
- run unit tests;
- install APK;
- launch;
- connect a development Gmail account;
- complete official OAuth;
- verify the authenticated identity;
- cancel/deny flow where testable;
- disconnect/reconnect;
- inspect logcat;
- force-stop/relaunch;
- verify authentication state restoration without exposing tokens.

If the required Google Cloud/OAuth environment is unavailable, record the exact blocker as `[!]`; do not mark success from compilation alone.

---

## 14. Explicit non-goals

Do not implement:

- mailbox synchronization;
- Gmail message retrieval;
- Gmail history;
- MIME parsing;
- classification;
- search;
- Calendar;
- Tasks;
- Gmail writes;
- AI;
- automation.

---

## 15. Completion

Update permanent editor rules only when justified.

Update `spec.md` only after real verification.

Record exact OAuth scopes/configuration decisions and safe setup instructions without committing secrets.

Final sequence:

`diff review → build → tests → OAuth/runtime verification → security inspection → fix → rebuild/retest → docs/status → commit → stop`

Do not continue to Phase 04.