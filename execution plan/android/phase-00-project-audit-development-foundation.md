# PHASE 0 — PROJECT AUDIT & DEVELOPMENT FOUNDATION

You are now beginning **Phase 0** of the Mail Organizer Android application.

This phase is **not** about building Gmail features yet.

Your responsibility in this phase is to deeply understand the existing project, establish the development foundation, configure the persistent engineering rules/skills, identify architectural risks, and make the repository ready for the remaining implementation phases.

Do not rush into feature development.

---

## 1. READ THE PROJECT CONTRACT FIRST

Before modifying any code, read these four project documents completely:

1. `requirements.md`
2. `spec.md`
3. `design.md`
4. `editor-rules.md`

Treat them as the authoritative product and engineering contract.

The priority order is:

1. `requirements.md`
2. `spec.md`
3. `design.md`
4. `editor-rules.md`
5. Existing architecture/implementation
6. Official Android/Google API documentation
7. Engineering judgment

Do not silently contradict these documents.

If the repository contains other planning documents, architecture documents, README files, ADRs, or existing project instructions, inspect them too and identify any conflicts.

Do not delete or overwrite existing project documentation unless there is a clear reason.

---

# 2. ESTABLISH THE PERSISTENT EDITOR SKILLS

The project requires persistent engineering skills/rules.

The following conceptual skills must exist for this project:

- Mail Organizer — Architecture Skill
- Mail Organizer — Android Development Skill
- Mail Organizer — Gmail OAuth/API Skill
- Mail Organizer — Privacy & Security Skill
- Mail Organizer — Classification Engine Skill
- Mail Organizer — Integration Skill
- Mail Organizer — UI/UX Design Skill
- Mail Organizer — Testing & QA Skill
- Mail Organizer — Phase Execution Skill
- Mail Organizer — Code Quality Skill

Use the editor's **native persistent skill/rule mechanism** if one exists.

If the current editor supports project-level skills, create these as actual persistent skills.

If it does not support native skills, create the closest supported persistent project instruction mechanism instead.

Do NOT invent fake configuration formats that the editor will not understand.

The important requirement is that these rules must remain available in future sessions.

Each skill should contain the relevant rules from `editor-rules.md`, while `editor-rules.md` remains the master reference.

The skills must reinforce:

- phase-based development
- architecture separation
- Gmail API usage
- OAuth security
- least privilege
- local-first processing
- privacy
- deterministic classification
- modular integrations
- account isolation
- UI/design consistency
- testing
- code quality
- release safety

After creating them, verify that they are actually recognized/available by the current editor environment if the editor provides a way to do so.

Do not merely create files and assume they work.

---

# 3. DETERMINE THE CURRENT REPOSITORY STATE

Before changing anything, perform a complete repository audit.

Inspect:

### Project structure

Identify:

- root directory
- Android modules
- Gradle modules
- source sets
- resources
- assets
- test directories
- build scripts
- configuration files
- documentation
- generated files
- local configuration
- CI/CD configuration
- GitHub configuration if present

Produce a concise structural map.

Example:

```text
project/
├── app/
│   ├── src/main/
│   ├── src/test/
│   └── src/androidTest/
├── ...
├── gradle/
├── build.gradle...
└── ...
```

Use the actual repository structure.

---

# 4. IDENTIFY THE EXISTING ANDROID STACK

Determine exactly what the project currently uses.

Inspect and record:

### Language

- Kotlin version
- Java version if applicable
- Kotlin/JVM configuration
- Kotlin compiler configuration

### Android

- compileSdk
- targetSdk
- minSdk
- buildTools if explicitly configured
- Android Gradle Plugin version
- Gradle version
- namespace
- application ID/package name

### Architecture

Determine whether the project currently uses:

- MVVM
- MVI
- Clean Architecture
- layered architecture
- repository pattern
- use cases/interactors
- dependency injection
- service locator
- direct Activity/Fragment logic
- Compose
- XML layouts
- mixed UI architecture

Do not replace the architecture just because you personally prefer another pattern.

First understand what exists.

---

# 5. AUDIT DEPENDENCIES

Inspect every important dependency.

Group them into:

### UI

For example:

- Jetpack Compose
- Material
- Material 3
- XML
- Navigation

### Architecture

For example:

- ViewModel
- Lifecycle
- Hilt
- Koin
- Coroutines
- Flow

### Persistence

For example:

- Room
- DataStore
- SQLite
- Realm
- other storage

### Networking

For example:

- Retrofit
- OkHttp
- Ktor
- Google APIs

### Authentication

Identify existing:

- Google Sign-In
- Credential Manager
- OAuth libraries
- token libraries

### Testing

Identify:

- JUnit
- MockK
- Mockito
- Turbine
- Compose UI tests
- Espresso
- Robolectric

### Other dependencies

Identify anything else that may affect architecture, performance, security, licensing, or future Gmail integration.

For each important dependency, determine whether it should remain, be upgraded, replaced, or deferred.

Do not upgrade dependencies simply for the sake of upgrading them.

---

# 6. AUDIT THE CURRENT BUILD

Before making changes, run the project's current build.

At minimum determine:

- whether the project compiles
- whether unit tests compile
- whether unit tests pass
- whether Android tests compile if practical
- whether lint/static analysis is configured
- whether there are warnings/errors
- whether Gradle configuration is healthy

Record the baseline result.

If the project already fails to build:

DO NOT pretend this is caused by Phase 0.

Identify whether the failure is:

- pre-existing
- caused by environment
- caused by missing configuration
- caused by dependency resolution
- caused by broken project code

Document it.

---

# 7. AUDIT GIT STATE

Inspect:

- current branch
- working tree
- uncommitted changes
- untracked files
- recent commits
- remotes
- ignored files
- repository cleanliness

DO NOT destroy existing user changes.

Do not reset the repository.

Do not run destructive Git commands.

Do not rewrite history.

If there are pre-existing uncommitted changes, record them and preserve them.

Before finishing Phase 0, inspect the final diff carefully.

---

# 8. PERFORM A SECURITY/SECRET AUDIT

Search the repository for accidentally committed secrets.

Look for:

- API keys
- OAuth client secrets
- service account JSON
- private keys
- signing keys
- passwords
- tokens
- refresh tokens
- access tokens
- `.env` files
- local credential files
- Firebase/private configuration where applicable
- hardcoded credentials
- private certificates

Do not print actual secrets into the phase report.

If you find one:

- do not expose it
- identify the file and type of secret
- recommend/remediate the exposure safely
- ensure it is ignored appropriately
- determine whether rotation is necessary

Remember:

An Android OAuth client ID is not equivalent to a secret.

OAuth access/refresh tokens and signing private keys must be treated as sensitive.

Never commit the Android signing private key.

---

# 9. CHECK PROJECT CONFIGURATION HYGIENE

Inspect:

- `.gitignore`
- Gradle properties
- local properties
- environment configuration
- build variants
- debug/release configuration
- signing configuration
- ProGuard/R8 configuration
- manifest configuration
- network security configuration
- backup configuration
- exported Android components
- permissions

Do not add Gmail permissions yet unless the project already requires them.

Phase 0 is preparation.

---

# 10. ESTABLISH THE TARGET ARCHITECTURE

Based on the project documents and existing code, establish a clean architecture suitable for the Mail Organizer.

The target logical separation should be approximately:

```text
Presentation
    ↓
Application / Use Cases
    ↓
Domain
    ↓
Data
    ↓
External APIs / Platform
```

The exact implementation can differ if the existing project has a better compatible architecture.

The following responsibilities must remain clearly separated:

### Authentication

Responsible for:

- Google OAuth
- account authorization
- token lifecycle
- authorization state

### Gmail Client

Responsible for:

- Gmail API communication
- Gmail-specific API models
- request/response handling

### Synchronization

Responsible for:

- mailbox synchronization
- pagination
- incremental synchronization
- sync state
- retries
- account-specific synchronization

### Email Parser

Responsible for:

- normalizing Gmail messages
- sender extraction
- recipients
- subject
- timestamps
- headers
- body
- HTML/plain text
- URLs
- unsubscribe information

### Classification Engine

Responsible for:

- category
- confidence
- priority
- action-required state
- explainability

### Company Intelligence

Responsible for:

- sender/domain normalization
- company identification
- company grouping
- sender intelligence

### Rules

Responsible for:

- user-defined rules
- sender overrides
- category overrides
- priority overrides

### Action Engine

Responsible for:

- meetings
- deadlines
- tasks
- reminders
- payments
- travel
- applications
- other structured actions

### Integration Layer

Responsible for:

- Google Calendar
- Google Tasks
- future integrations

### Persistence

Responsible for:

- local database
- local settings
- account-scoped data
- sync state

### Search

Responsible for:

- local indexing
- structured search
- filtering
- sorting

### Presentation

Responsible for:

- screens
- components
- navigation
- UI state
- accessibility
- user interaction

Do not tightly couple these systems.

---

# 11. ESTABLISH ACCOUNT ISOLATION AS A FOUNDATION RULE

The application will support multiple Gmail accounts.

Therefore, account identity must be treated as a first-class data boundary.

Design the foundation so future data can be scoped by account.

For example:

```text
Account
 ├── Gmail authorization
 ├── Sync state
 ├── Emails
 ├── Threads
 ├── Sender intelligence
 ├── Company intelligence
 ├── Rules
 └── Classification state
```

Do not allow future code to accidentally mix data between accounts.

Do not implement the full multi-account system yet.

Simply establish the architectural rule now.

---

# 12. ESTABLISH LOCAL-FIRST PRINCIPLES

The application must not depend on a custom backend for basic functionality.

The target architecture is:

```text
Gmail
   ↓
Gmail API
   ↓
Android application
   ↓
Local database
   ↓
Local processing
   ↓
UI
```

Do not introduce:

- unnecessary backend servers
- unnecessary cloud databases
- analytics servers
- third-party email processing
- unnecessary AI APIs

The application should eventually be able to provide useful functionality from locally synchronized data even when temporarily offline.

Phase 0 only establishes the foundation for this.

---

# 13. ESTABLISH PRIVACY ARCHITECTURE

Create/document the initial privacy model.

Email content is sensitive.

Therefore:

- minimize stored data
- store only what is required
- avoid unnecessary logging
- never log email bodies
- never log OAuth tokens
- never log authorization headers
- never send email content to third-party services without an explicit product decision
- keep processing local where practical
- isolate accounts
- provide future deletion/disconnect paths

If encrypted storage is required, document where it will be introduced.

Do not claim the application is encrypted or private if the implementation does not actually provide it.

---

# 14. ESTABLISH LOGGING RULES

Create a safe logging strategy.

Logs may contain:

- lifecycle information
- sync state
- non-sensitive diagnostic information
- error categories
- performance measurements

Logs must NOT contain:

- email bodies
- OAuth tokens
- refresh tokens
- passwords
- authorization headers
- private user content
- unnecessary sender information
- full URLs containing sensitive query parameters

Create a clear distinction between:

```text
DEBUG
INFO
WARNING
ERROR
```

and prepare the project so sensitive logs can be disabled/removed from release builds.

---

# 15. ESTABLISH ERROR HANDLING PRINCIPLES

Create the foundation for consistent error handling.

Errors should distinguish between:

- network failure
- authentication failure
- permission denial
- API failure
- rate limiting
- parsing failure
- database failure
- invalid user configuration
- integration failure
- unexpected application failure

Do not allow external integration failures to crash the core email experience.

---

# 16. ESTABLISH TESTING FOUNDATION

Verify the project has a usable testing structure.

At minimum establish the ability to test:

### Unit

- domain logic
- parsers
- classifiers
- repositories
- use cases

### Integration

- database
- repository implementations
- synchronization logic

### UI

- important screen behavior
- navigation
- loading/error/empty states

### Security-sensitive behavior

- account isolation
- token handling
- permission state
- sensitive logging

Do not attempt to write the entire application's tests in Phase 0.

Only establish the foundation and identify gaps.

---

# 17. ESTABLISH DESIGN SYSTEM FOUNDATION

Read `design.md` carefully.

If the project already has a design system:

Audit it.

If it does not:

Establish the basic structure for centralized:

- colors
- typography
- spacing
- corner radius
- elevation
- icon sizing
- light theme
- dark theme
- semantic status colors

Do not build the full Mail Organizer UI yet.

The purpose is to prevent every future screen from inventing its own styles.

Use the design tokens from `design.md` unless there is a documented reason to change them.

---

# 18. ESTABLISH NAVIGATION FOUNDATION

Determine whether navigation already exists.

If it does:

Audit it and preserve compatible existing work.

If it does not:

Create only the minimal navigation foundation needed for future phases.

The future primary information architecture is:

```text
Home
Mail
Categories
Companies
Actions
```

Secondary destinations include:

```text
Search
Integrations
Settings
Privacy
Accounts
```

Do not fully implement these screens during Phase 0.

Only establish the navigation architecture if required.

---

# 19. ESTABLISH ENVIRONMENT SEPARATION

Prepare for:

```text
Debug
Release
```

and, if appropriate:

```text
Development
Production
```

The project must eventually be able to distinguish:

- development OAuth configuration
- production OAuth configuration
- debug signing
- release signing
- debug logging
- release logging

Do not place production secrets directly into source code.

Do not create fake production credentials.

---

# 20. DOCUMENT GOOGLE INTEGRATION REQUIREMENTS — BUT DO NOT IMPLEMENT THEM YET

Phase 0 should identify what will be required later.

Document the future Google Cloud configuration:

- Google Cloud project
- Gmail API
- OAuth consent configuration
- Android OAuth client
- application package ID
- signing certificate fingerprints
- test users during development
- production OAuth verification requirements
- Calendar API
- Tasks API

However:

**DO NOT implement Gmail OAuth in Phase 0.**

**DO NOT request Gmail permissions yet.**

**DO NOT connect a real Gmail account yet.**

Those belong to later phases.

---

# 21. DO NOT IMPLEMENT PRODUCT FEATURES PREMATURELY

The following are explicitly OUT OF SCOPE for Phase 0:

- Gmail OAuth implementation
- Gmail synchronization
- Gmail API mailbox fetching
- email classification
- company detection
- priority calculation
- action-required detection
- meeting extraction
- deadline extraction
- Calendar integration
- Tasks integration
- AI classification
- automation engine
- full dashboard
- advanced search
- Gmail modification
- email sending
- full analytics

Do not create fake placeholder implementations just to claim these tasks are complete.

---

# 22. IF THE REPOSITORY IS EMPTY

If the repository is empty or only contains minimal scaffolding:

Bootstrap a production-quality Android foundation.

Choose the technology stack that best fits the requirements, preferably:

- Kotlin
- modern Android SDK
- Jetpack
- Compose if appropriate
- Coroutines
- Flow
- Room for local persistence
- DataStore where appropriate
- dependency injection where justified

However, do not blindly install every library.

Every dependency must have a reason.

Keep the initial dependency graph small and maintainable.

---

# 23. IF THE REPOSITORY ALREADY CONTAINS AN APPLICATION

Do NOT rebuild it from scratch.

Preserve:

- existing screens
- working functionality
- existing architecture where compatible
- useful components
- assets
- tests
- project configuration

Refactor only where necessary to align the project with the product contract.

Do not perform unrelated cleanup.

Do not turn Phase 0 into a giant rewrite.

---

# 24. CREATE/UPDATE PROJECT DOCUMENTATION

After the audit, create or update a project architecture document if one does not already exist.

Recommended:

```text
docs/
    architecture.md
    development-status.md
```

Use existing documentation conventions if the repository already has them.

`architecture.md` should explain:

- application layers
- module responsibilities
- data flow
- account isolation
- local-first approach
- external integrations
- authentication boundary
- synchronization boundary
- classification boundary
- action boundary

`development-status.md` should record:

- baseline build state
- current phase
- known issues
- technical decisions
- deferred work
- important risks

Do not duplicate the entire `requirements.md`.

---

# 25. CREATE ARCHITECTURAL DECISIONS WHERE NEEDED

If important architectural decisions are made during Phase 0, document them.

Examples:

- why Gmail API is used instead of AccessibilityService
- why processing is local-first
- why classification begins deterministically
- why external integrations are adapters
- why account isolation is mandatory
- why Gmail write permissions are deferred
- why AI is optional rather than mandatory

Use ADRs if the repository already has an ADR convention.

Otherwise keep these decisions in `architecture.md`.

---

# 26. VERIFY THE FOUNDATION

After making the Phase 0 changes:

Run:

1. clean/build
2. unit tests
3. lint/static checks if configured
4. relevant Android compilation
5. any available architecture/configuration checks

Then inspect:

- Git diff
- Git status
- generated files
- accidental secrets
- debug/release configuration- documentation
- dependency changes

Fix any issues introduced by Phase 0.

---

# 27. PHASE 0 ACCEPTANCE CRITERIA

Phase 0 may only be marked complete when ALL applicable conditions are satisfied.

### Documentation

- [ ] `requirements.md` read and understood
- [ ] `spec.md` read and understood
- [ ] `design.md` read and understood
- [ ] `editor-rules.md` read and understood
- [ ] architecture documented
- [ ] development status documented

### Editor Skills

- [ ] persistent skills/rules established
- [ ] architecture rules available
- [ ] Android rules available
- [ ] Gmail/OAuth rules available
- [ ] privacy/security rules available
- [ ] classification rules available
- [ ] integration rules available
- [ ] UI/UX rules available
- [ ] testing rules available
- [ ] phase execution rules available
- [ ] code quality rules available

### Repository

- [ ] repository structure audited
- [ ] Android stack identified
- [ ] dependencies audited
- [ ] build system audited
- [ ] Git state audited
- [ ] existing work preserved

### Security

- [ ] secret exposure checked
- [ ] logging risks checked
- [ ] signing configuration checked
- [ ] OAuth architecture documented
- [ ] sensitive data handling documented

### Architecture

- [ ] presentation boundary defined
- [ ] domain boundary defined
- [ ] data boundary defined
- [ ] external API boundary defined
- [ ] account isolation principle established
- [ ] local-first principle established
- [ ] integration architecture established
- [ ] error handling strategy established
- [ ] testing strategy established

### Design

- [ ] design tokens foundation established
- [ ] theme strategy established
- [ ] dark mode strategy established
- [ ] navigation foundation established if required

### Verification

- [ ] baseline build understood
- [ ] final build passes
- [ ] relevant tests pass
- [ ] final Git diff reviewed
- [ ] no accidental secrets added
- [ ] no unrelated features implemented

---

# 28. IMPORTANT: DO NOT FAKE COMPLETION

A checkbox may only become:

```text
[x]
```

after the work has actually been completed and verified.

If something cannot be completed:

```text
[!]
```

and explain why.

If something is intentionally deferred:

```text
[-]
```

with an explanation.

Never mark something complete because the code "looks ready".

---

# 29. UPDATE `spec.md`

After successful verification, update the Phase 0 section in `spec.md`.

Only check tasks that were actually completed.

If Phase 0 acceptance criteria are fully satisfied, change its status to completed.

Do not modify future phase checkboxes.

Do not mark Phase 1 or any later phase as started/completed.

---

# 30. FINAL PHASE REPORT

At the end of the work, provide a Phase 0 report using this structure:

## Phase 0 Status

`COMPLETE` / `PARTIAL` / `BLOCKED`

## Repository Baseline

- project type
- Android stack
- architecture
- build state
- test state
- Git state

## What Was Changed

List concrete changes.

## Architecture Established

Explain the final architecture briefly.

## Skills / Rules Established

List the persistent skills/rules created.

## Security Findings

List findings without exposing secrets.

## Known Issues

List unresolved issues.

## Deferred Work

List anything intentionally postponed to later phases.

## Validation

Include:

- build result
- tests
- lint/static analysis
- relevant verification

## Files Changed

List important files.

## Phase 0 Acceptance Criteria

Show each criterion and its final status.

## Next Phase

The next phase must be:

**Phase 1 — Android Application Foundation**

Do not start Phase 1 automatically unless explicitly instructed.

---

# FINAL OPERATING RULE

From this point forward, this project must be treated as a serious production application.

Do not optimize for speed at the expense of architecture.

Do not optimize for the number of completed checkboxes.

Optimize for:

**correctness → privacy → security → maintainability → testability → UX → performance → speed of development.**

Remember the central product principle:

```text
Gmail
  ↓
Synchronization
  ↓
Local Processing
  ↓
Company / Category / Priority
  ↓
Structured Information
  ↓
Actions / Search / Insights
  ↓
User Confirmation
  ↓
External Integrations
```

And remember:

**Mail Organizer is NOT a Gmail clone.**

Gmail remains the source of truth.

Mail Organizer is the intelligent organization, understanding, prioritization, and action layer on top of Gmail.

Execute Phase 0 completely, verify it, document it, and stop at the Phase 0 boundary.