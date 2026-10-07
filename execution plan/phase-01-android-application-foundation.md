# PHASE 1 — ANDROID APPLICATION FOUNDATION

You are now beginning **Phase 1 — Android Application Foundation** of the Mail Organizer project.

Phase 0 established the project audit, architecture, persistent skills/rules, security foundation, and development environment.

Now establish the actual Android application foundation.

This phase is still foundational. Do not jump ahead into Gmail synchronization, classification, Calendar, Tasks, AI, or advanced product functionality.

The objective is to create a **stable, buildable, installable, testable, visually inspectable Android application foundation** that all later phases can safely build upon.

---

# 1. READ THE PROJECT CONTRACT BEFORE WORKING

Before modifying anything, review:

- `requirements.md`
- `spec.md`
- `design.md`
- `editor-rules.md`
- `docs/architecture.md` if created in Phase 0
- `docs/development-status.md` if created in Phase 0

Also load/use the persistent Mail Organizer skills established during Phase 0.

Do not assume the previous session's implementation is correct.

Inspect the actual repository state.

The repository is the source of truth for implementation.

---

# 2. IMPORTANT: YOU ARE AN ANDROID DEVELOPMENT + DEVICE VALIDATION AGENT

For this project, you are not only a code-generation assistant.

You must use the available Android development and device-control tooling whenever applicable.

Your workflow must include:

```text
Inspect
  ↓
Modify
  ↓
Gradle Build
  ↓
Install APK
  ↓
Launch App
  ↓
Inspect Device
  ↓
Capture Screenshots / Screen Recordings when useful
  ↓
Test
  ↓
Identify Problems
  ↓
Fix
  ↓
Rebuild
  ↓
Reinstall
  ↓
Retest
```

Do not consider an Android UI implementation complete merely because the Kotlin/Compose/XML code compiles.

It must be validated on an actual Android device/emulator whenever one is available.

---

# 3. USE THE AVAILABLE ANDROID TOOLCHAIN

Use the project's available Android tooling and the environment's supported device-control mechanisms.

This includes, where available:

### Gradle

Use the project's:

```text
./gradlew
```

or the appropriate Gradle wrapper.

Do not depend on a globally installed Gradle version when the project provides a wrapper.

Use Gradle for:

- compilation
- unit tests
- Android tests where applicable
- lint
- packaging
- APK generation
- build variants
- dependency verification
- other project tasks

---

# 4. ADB / ANDROID DEVICE CONTROL

When an Android device or emulator is available, use ADB or the environment's equivalent Android device-control tooling.

You should be capable of performing operations such as:

```text
adb devices
adb install
adb uninstall
adb shell
adb shell am start
adb shell am force-stop
adb logcat
adb shell pm
adb shell settings
adb shell dumpsys
adb exec-out screencap
adb shell screenrecord
```

Use the exact supported tooling available in the environment.

Do not assume a command exists if the environment does not provide it.

---

# 5. ADB REVERSE / LOCAL DEVELOPMENT NETWORKING

When the application requires communication with a development machine during development, use the appropriate ADB networking mechanism.

In particular, understand and use:

```text
adb reverse
```

when appropriate.

Examples of cases where this may become relevant:

- local development server
- local API during development
- localhost debugging
- local inspection tools
- development-only network services

Do not introduce a backend merely because ADB reverse exists.

For this project, the production architecture remains **local-first**.

ADB reverse is a development/testing mechanism, not a production architecture.

If no local network service is required in Phase 1, do not create one just to demonstrate ADB reverse.

---

# 6. INSTALL THE APPLICATION ON THE ANDROID DEVICE

If a physical Android device or emulator is connected and available:

1. Detect the device.
2. Verify the device is authorized.
3. Build the application.
4. Locate the generated APK.
5. Install it.
6. Confirm installation succeeded.
7. Launch the application.
8. Verify the application starts successfully.

Use the appropriate debug build for development.

Do not install an unverified release build merely for convenience.

---

# 7. DEVICE INFORMATION AUDIT

Before UI validation, inspect the connected device/emulator where practical.

Record relevant information such as:

- Android version
- API level
- device/emulator model
- screen dimensions
- density
- orientation
- available storage if relevant
- debug/development status

Do not collect unnecessary personal information from the device.

---

# 8. CREATE A RELIABLE BUILD → INSTALL → RUN LOOP

The project should have a predictable development loop.

The expected loop is:

```text
./gradlew assembleDebug
        ↓
APK generated
        ↓
ADB install
        ↓
Launch application
        ↓
Verify process
        ↓
Capture evidence
        ↓
Inspect
```

If installation fails:

- inspect the error
- determine the cause
- fix it
- rebuild
- retry

Do not simply report that installation failed without investigating.

---

# 9. SCREENSHOT AND SCREEN CAPTURE VALIDATION

When validating Android UI, use available screenshot/screen-capture capabilities.

Capture screenshots of important states.

For example:

- first launch
- home screen
- empty state
- loading state
- error state
- dark mode
- navigation
- dialogs
- permission screens where appropriate
- important interactive components

Use screenshots to visually inspect:

- alignment
- spacing
- typography
- clipping
- overflow
- incorrect colors
- incorrect theme behavior
- navigation issues
- system bar issues
- keyboard behavior
- accessibility issues
- responsive behavior

Do not rely solely on source code to judge visual correctness.

---

# 10. SCREEN RECORDING

Use screen recording when it materially helps validate:

- navigation
- transitions
- animations
- gestures
- scrolling
- interactive components
- startup behavior
- unexpected UI changes

Do not record sensitive user information.

If a device contains personal information, do not capture it unnecessarily.

For this project, privacy applies to development tooling too.

---

# 11. USE LOGCAT FOR DEBUGGING

When the application crashes, behaves incorrectly, or produces unexpected results, inspect Android logs.

Use appropriate logcat filtering.

Look for:

- crashes
- exceptions
- ANRs
- lifecycle errors
- rendering errors
- database errors
- network errors
- permission errors

Never blindly paste sensitive log output into documentation.

Do not log:

- OAuth tokens
- refresh tokens
- email bodies
- passwords
- authorization headers
- sensitive user data

---

# 12. APPLICATION IDENTITY

Verify the application has a stable package/application ID.

The package ID must be treated as an important long-term identity because it will eventually be associated with:

- Android OAuth configuration
- release signing
- Play Store identity
- app data
- update compatibility

Do not casually change it.

If Phase 0 already established the package ID, preserve it.

If it is missing, establish an appropriate production-ready ID based on the actual project identity.

Do not use a temporary random package name.

---

# 13. APPLICATION NAME

Establish the user-facing application name:

**Mail Organizer**

Ensure the name is configured consistently where appropriate:

- application label
- launcher metadata
- debug build if applicable
- UI branding

Do not create a full branding system in this phase.

---

# 14. ANDROID MANIFEST FOUNDATION

Audit and establish the AndroidManifest configuration.

Verify:

- application declaration
- launcher Activity
- exported components
- permissions
- theme
- configuration
- backup behavior where appropriate
- cleartext traffic policy
- network security configuration if applicable

Do not request permissions that are not currently required.

In particular:

**Do not add Gmail permissions yet.**

Gmail OAuth belongs to Phase 3.

---

# 15. BUILD VARIANTS

Establish a clean debug/release foundation.

At minimum:

```text
debug
release
```

Ensure:

### Debug

May contain:

- development logging
- debugging support
- development configuration
- device testing support

### Release

Must be prepared for:

- minimized logging
- production configuration
- R8/ProGuard where appropriate
- release signing
- production-safe configuration

Do not put production secrets in either build.

---

# 16. DEPENDENCY MANAGEMENT

Use the dependency-management strategy established in Phase 0.

Keep dependencies intentional.

Do not add a library for something that can reasonably be implemented with Android/Jetpack APIs already in use.

Before adding a dependency, determine:

- why it is required
- whether it is maintained
- whether it affects app size
- whether it introduces privacy/security concerns
- whether it conflicts with architecture
- whether an existing dependency already provides the capability

---

# 17. ESTABLISH THE APPLICATION ARCHITECTURE

Implement the minimum structural foundation necessary for:

```text
Presentation
    ↓
Application / Use Cases
    ↓
Domain
    ↓
Data
    ↓
Platform / External APIs
```

The exact package/module organization should follow the architecture established in Phase 0.

Avoid unnecessary abstraction.

Do not create dozens of empty interfaces simply to make the architecture look sophisticated.

The architecture should be useful.

---

# 18. ESTABLISH CORE PACKAGE/MODULE BOUNDARIES

Where appropriate, create logical boundaries for future systems such as:

```text
auth
gmail
sync
email
classification
company
rules
actions
integrations
search
database
settings
privacy
ui
```

Do not implement those systems yet.

The goal is to make future development predictable.

Do not create fake implementations.

---

# 19. ESTABLISH UI FOUNDATION

Use the technology selected in Phase 0.

If Compose is the chosen UI technology, establish:

- Material 3 foundation
- theme
- typography
- colors
- shapes
- spacing conventions
- dark mode
- system bar handling
- navigation foundation

If XML is already the established architecture, follow that architecture instead.

Do not migrate technologies during Phase 1 without a strong reason.

---

# 20. IMPLEMENT THE DESIGN TOKENS

Use `design.md` as the source of truth.

Establish centralized tokens for:

### Colors

Primary:

```text
#5B5CE2
```

Primary Container:

```text
#E8E8FF
```

Light Background:

```text
#F8F9FC
```

Dark Background:

```text
#101114
```

Light Surface:

```text
#FFFFFF
```

Dark Surface:

```text
#1A1B20
```

Dark Elevated Surface:

```text
#222329
```

Success:

```text
#16A34A
```

Warning:

```text
#D97706
```

Error:

```text
#DC2626
```

Info:

```text
#2563EB
```

Do not scatter raw color values throughout the UI.

---

# 21. TYPOGRAPHY FOUNDATION

Use Inter where available/preferred by the design contract, with an appropriate system fallback.

Establish the typography hierarchy defined by `design.md`.

At minimum account for:

- display
- large title
- title
- section title
- body
- secondary text
- caption

Typography should be centralized rather than manually recreated on every screen.

---

# 22. SPACING FOUNDATION

Use the 4dp spacing grid.

Primary spacing values:

```text
4
8
12
16
20
24
32
40
48
64
```

Do not randomly introduce values such as:

```text
13dp
17dp
19dp
23dp
```

unless there is a genuine component-specific reason.

---

# 23. SHAPE FOUNDATION

Establish the project's standard radius tokens:

```text
8dp
12dp
16dp
20dp
pill
```

Use consistent component shapes.

Avoid arbitrary corner-radius values throughout the application.

---

# 24. DARK MODE

Dark mode is a first-class requirement.

Verify that the foundation supports:

- light mode
- dark mode
- system theme
- readable text
- sufficient contrast
- appropriate surfaces
- appropriate status colors

Do not simply invert colors.

Dark mode should use the tokens from `design.md`.

---

# 25. ESTABLISH NAVIGATION

Create the minimal navigation foundation.

The future primary destinations are:

```text
Home
Mail
Categories
Companies
Actions
```

Secondary destinations:

```text
Search
Integrations
Settings
Privacy
Accounts
```

For Phase 1, these can remain minimal foundation destinations.

Do not implement their complete functionality.

Navigation must be structured so later phases can add real screens without requiring a navigation rewrite.

---

# 26. CREATE THE INITIAL APPLICATION SCREEN

Create a clean initial application screen that proves the foundation works.

It should communicate:

**Mail Organizer**

and may include a simple foundational state such as:

> Organize your Gmail into a clearer, action-first workspace.

Do not pretend the application is already connected to Gmail.

Do not display fake emails.

Do not display fake classifications.

Do not create fake analytics.

Do not create fake account data.

The screen should make it obvious that this is a foundation build.

---

# 27. LOADING / EMPTY / ERROR FOUNDATION

Create reusable UI patterns for:

### Loading

Clear, calm loading state.

### Empty

Explain what the user can do next.

### Error

Explain the problem in user-friendly language.

### Success

Provide confirmation when an action succeeds.

These components will later be reused by Gmail synchronization, integrations, search, etc.

---

# 28. ACCESSIBILITY FOUNDATION

Ensure the foundation supports:

- content descriptions where required
- semantic UI
- screen reader compatibility
- sufficient touch target sizes
- readable typography
- contrast
- dynamic font scaling where practical
- keyboard/navigation support where relevant

Do not treat accessibility as a final-stage cleanup.

---

# 29. RESPONSIVE FOUNDATION

Test the foundation on available configurations.

At minimum consider:

- normal Android phone
- larger phone
- portrait
- landscape where appropriate
- different density

If only one physical device is available, use it and test additional emulator configurations if practical.

Do not hardcode screen dimensions.

---

# 30. DEVICE INSTALLATION VALIDATION

After implementation:

### Step 1

Build:

```text
./gradlew assembleDebug
```

or the correct project equivalent.

### Step 2

Locate the APK.

### Step 3

Verify APK exists.

### Step 4

Install:

```text
adb install -r <apk>
```

or the environment's equivalent.

### Step 5

Launch the app.

### Step 6

Verify the package is running.

### Step 7

Capture screenshots.

### Step 8

Inspect the screenshots.

### Step 9

Check logcat.

### Step 10

Fix any discovered issues.

### Step 11

Rebuild.

### Step 12

Reinstall.

### Step 13

Repeat until stable.

---

# 31. DO NOT STOP AT "BUILD SUCCESSFUL"

A successful Gradle build is necessary but NOT sufficient.

The following all matter:

```text
Code correctness
+
Build correctness
+
Installation correctness
+
Runtime correctness
+
Visual correctness
+
Navigation correctness
+
Accessibility correctness
```

A phase is not complete simply because:

```text
BUILD SUCCESSFUL
```

appears in the terminal.

---

# 32. TEST REAL INTERACTION

On the installed application, interact with the UI.

Test:

- launching
- navigation
- back button
- scrolling if applicable
- theme switching if exposed
- configuration changes where practical
- screen rotation where applicable
- app background/foreground
- process restart
- relaunch
- basic accessibility

If a button looks clickable, verify it.

If navigation exists, navigate through it.

If a state exists, trigger it.

Do not merely inspect source code.

---

# 33. SCREENSHOT-BASED UI QA

For each foundational screen:

1. Open the screen.
2. Capture screenshot.
3. Inspect visually.
4. Compare against `design.md`.
5. Identify deviations.
6. Fix them.
7. Capture again.
8. Repeat.

Pay particular attention to:

- spacing
- alignment
- typography
- hierarchy
- contrast
- component sizing
- status/navigation bars
- dark mode
- clipping
- unexpected padding
- overflow
- visual consistency

---

# 34. DO NOT USE FAKE DATA TO HIDE MISSING FUNCTIONALITY

Never create fake Gmail emails merely to make the UI appear finished.

Never create fake:

- Gmail accounts
- email counts
- company names
- classifications
- action cards
- calendar events
- tasks
- analytics

unless the UI is explicitly a design/demo state and clearly labeled as such.

Production behavior must eventually use real data.

---

# 35. PREPARE FOR FUTURE GOOGLE OAUTH

Do not implement Gmail OAuth in Phase 1.

However, make sure the application foundation will support:

```text
Google Account
      ↓
OAuth      ↓
Authorized Gmail account
      ↓
Account-scoped local data
```

Phase 3 will implement this.

Do not request:

```text
gmail.readonly
gmail.modify
gmail.send
```

yet.

---

# 36. PREPARE FOR LOCAL DATABASE

Do not implement the complete email database schema in Phase 1.

That belongs to Phase 2.

However, make sure the architecture has a clear location for:

- database
- entities
- DAOs/repositories
- migrations
- account-scoped persistence

Do not create a temporary database architecture that will need to be thrown away immediately.

---

# 37. PREPARE FOR FUTURE BACKGROUND WORK

Do not implement Gmail synchronization yet.

But ensure the application architecture can later support:

- WorkManager
- background synchronization
- retry
- network constraints
- battery constraints
- cancellation
- account-specific jobs

Do not run unnecessary background jobs in Phase 1.

---

# 38. PERFORMANCE FOUNDATION

Avoid obvious performance mistakes.

Do not:

- block the main thread
- perform expensive work during Activity startup
- load unnecessary resources
- introduce excessive recompositions
- create unnecessary global singletons
- perform network calls directly from UI
- store large objects unnecessarily in UI state

The application should start quickly and remain responsive.

---

# 39. PRIVACY FOUNDATION

Verify that the Phase 1 application does not accidentally collect or transmit anything.

There should be:

- no Gmail content
- no Gmail token
- no unnecessary analytics
- no unnecessary network requests
- no third-party tracking
- no hidden telemetry

If the application performs network calls for tooling/development, document them.

---

# 40. GIT SAFETY

Before finishing:

Run a complete Git inspection.

Check:

```text
git status
git diff
```

and equivalent commands as appropriate.

Verify:

- no secrets
- no generated junk
- no APKs accidentally committed
- no IDE-specific private configuration accidentally committed
- no unrelated files modified
- no user changes lost

Do not commit unless the project workflow explicitly requires it.

---

# 41. PHASE 1 ACCEPTANCE CRITERIA

Phase 1 can only be marked complete when the applicable criteria are verified.

### Project

- [ ] Android project builds
- [ ] package/application ID is stable
- [ ] debug build works
- [ ] release configuration exists
- [ ] dependency structure is sane

### UI Foundation

- [ ] application theme established
- [ ] design tokens established
- [ ] typography established
- [ ] spacing system established
- [ ] shapes established
- [ ] dark mode works
- [ ] navigation foundation works
- [ ] loading/empty/error foundation exists
- [ ] accessibility foundation exists

### Android

- [ ] manifest reviewed
- [ ] permissions minimized
- [ ] launcher works
- [ ] app launches
- [ ] app survives basic lifecycle operations

### Device

- [ ] device/emulator detected
- [ ] APK built
- [ ] APK installed
- [ ] application launched
- [ ] runtime inspected
- [ ] screenshots captured
- [ ] UI visually inspected
- [ ] logcat inspected where relevant
- [ ] issues found during device testing fixed

### Development Tooling

- [ ] Gradle workflow verified
- [ ] ADB workflow verified where device is available
- [ ] ADB reverse understood/configured where actually needed
- [ ] device installation workflow documented
- [ ] screenshot workflow verified
- [ ] screen-recording workflow verified where useful
- [ ] runtime debugging workflow verified

### Architecture

- [ ] presentation boundary established
- [ ] application/domain boundary established
- [ ] data boundary established
- [ ] external API boundary established
- [ ] future Gmail integration has a clear location
- [ ] future database integration has a clear location
- [ ] future integrations have a clear location

### Security

- [ ] no unnecessary permissions
- [ ] no secrets committed
- [ ] no sensitive logs
- [ ] no unnecessary network traffic
- [ ] debug/release boundaries checked

---

# 42. PHASE STATUS RULE

Only after all applicable acceptance criteria have been verified should you update:

`spec.md`

and mark Phase 1 complete.

If something is incomplete:

```text
[!]
```

or:

```text
[-]
```

must be used appropriately.

Do not mark Phase 1 complete simply because the application compiles.

---

# 43. FINAL PHASE REPORT

When Phase 1 is finished, report:

## Phase 1 Status

`COMPLETE` / `PARTIAL` / `BLOCKED`

## Android Stack

- Kotlin
- Android SDK
- Gradle
- UI framework
- architecture
- important dependencies

## Application Foundation

Explain what was established.

## Device Validation

Report:

- device/emulator used
- Android version/API
- APK build
- installation result
- launch result
- screenshots captured
- runtime testing
- issues found/fixed

Do not include sensitive device information.

## UI Validation

Report:

- light mode
- dark mode
- navigation
- responsive behavior
- accessibility
- visual issues fixed

## Development Tooling

Report:

- Gradle workflow
- ADB workflow
- ADB reverse if used
- installation workflow
- logcat/debugging workflow
- screenshot workflow
- screen recording if used

## Files Changed

List important files.

## Known Issues

List unresolved issues.

## Deferred Work

Explicitly list work belonging to later phases.

## Validation

Report:

- build
- tests
- lint
- installation
- runtime
- visual QA

## Acceptance Criteria

Show every Phase 1 criterion and its final status.

## Next Phase

The next phase is:

**Phase 2 — Local Data Architecture**

Do not automatically start Phase 2.

---

# FINAL RULE FOR THIS PROJECT

From Phase 1 onward, development is **implementation + real-device validation**, not implementation alone.

Always think:

```text
WRITE CODE
   ↓
BUILD WITH GRADLE
   ↓
INSTALL WITH ANDROID TOOLING
   ↓
RUN ON DEVICE/EMULATOR
   ↓
INTERACT WITH IT
   ↓
SCREENSHOT / RECORD WHEN USEFUL
   ↓
INSPECT LOGS
   ↓
FIND REAL PROBLEMS
   ↓
FIX
   ↓
REBUILD
   ↓
REINSTALL
   ↓
RETEST
```

Use ADB, ADB reverse, Gradle, logcat, screenshots, screen capture, device inspection, and all other supported Android development capabilities whenever they are relevant.

**Do not treat source-code completion as application completion.**

The goal is a real, installable, runnable, visually verified Android application.

Stop strictly at the Phase 1 boundary after completing and verifying the foundation.