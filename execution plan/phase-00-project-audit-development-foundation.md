# PHASE 0 — PROJECT AUDIT & DEVELOPMENT FOUNDATION

## EXECUTION CONTRACT

Read the complete Mail Organizer instruction set before changing code. Confirm Phase 0 is the first incomplete phase. Work only on Mail Organizer. Do not execute Phase 1 or any later phase automatically.

Required lifecycle:

DISCOVER → READ RULES → IDENTIFY ROOT → AUDIT → IMPLEMENT FOUNDATION → BUILD → TEST → DEVICE/ADB QA → SECURITY/GIT REVIEW → UPDATE DOCS → STOP

Do not claim verification that was not actually performed.

## 1. DISCOVER THE INSTRUCTION FOLDER

Locate the Mail Organizer instruction/execution-plan folder. Read requirements.md, spec.md, design.md, editor-rules.md, architecture/status documents and this phase prompt. Resolve the actual project root before running any tooling.

## 2. ESTABLISH SOURCE-OF-TRUTH PRIORITY

Respect: requirements > spec > design > editor-rules > verified existing architecture > official platform documentation > engineering judgment. Record conflicts rather than silently choosing a lower-priority rule.

## 3. IDENTIFY THE MAIL ORGANIZER ROOT

Confirm the repository containing the Mail Organizer application, Gradle wrapper, settings, source modules and Git metadata. Do not assume the current directory is correct.

## 4. MULTI-PROJECT ISOLATION

Inspect the workspace before Gradle, Git, ADB or file edits. Never modify sibling Android projects, their SDK/build configuration, dependencies, manifests, signing, tests or generated files.

## 5. GIT BASELINE

Inspect branch, status, recent history and diff. Record pre-existing modifications. Never overwrite unrelated user work.

## 6. REPOSITORY STRUCTURE AUDIT

Map modules, source sets, resources, tests, execution-plan documents, documentation, assets and build tooling.

## 7. KMP ARCHITECTURE AUDIT

Confirm Kotlin Multiplatform is the intended architecture. Identify shared/domain/data boundaries and Android presentation boundaries. Do not convert the project to Android-only architecture.

## 8. ANDROID FOUNDATION AUDIT

Inspect application ID, namespace, manifests, activities, Compose setup, SDK levels, build types, variants and resources.

## 9. GRADLE AUDIT

Inspect Gradle wrapper, settings, version catalog/build logic, repositories, plugins, Kotlin/KMP/Compose/Android versions and dependency graph. Do not perform unrelated upgrades.

## 10. DEPENDENCY AUDIT

Identify authentication, database, networking, serialization, UI, testing and utility dependencies. Flag redundant, obsolete or unsafe libraries without replacing them unnecessarily.

## 11. PACKAGE AND IDENTITY AUDIT

Confirm application/package identity and signing configuration. Document mismatches before changing them.

## 12. UI/UX AUDIT

Compare existing UI against design.md: Material 3, Gmail familiarity, Mail Organizer identity, dynamic color, light/dark, accessibility, responsive layout and centralized tokens.

## 13. NAVIGATION AUDIT

Identify existing navigation and confirm the future shell can support All Inbox, Primary, Promotional, Social, Spam and Starred without implementing later mailbox functionality.

## 14. DATA LAYER AUDIT

Identify database/local persistence, repositories, serialization and migration strategy. Establish whether Phase 2 foundations exist without implementing Phase 2 work early.

## 15. NETWORK/AUTH AUDIT

Identify network clients and authentication boundaries. Do not implement Gmail OAuth in Phase 0.

## 16. SECURITY AUDIT

Search source/config/logging for hard-coded secrets, tokens, passwords, unsafe logging, cleartext network configuration and credential handling.

## 17. PRIVACY AUDIT

Confirm email is treated as untrusted/private data. Establish minimization, account isolation and no-sensitive-logging expectations.

## 18. TEST AUDIT

Identify unit, integration, UI, instrumentation and shared KMP tests. Determine current test execution commands.

## 19. BUILD BASELINE

Run the Mail Organizer Gradle wrapper only. Establish a clean baseline or record pre-existing failures.

## 20. STATIC QUALITY BASELINE

Run configured lint/static analysis/check tasks where practical. Do not modify unrelated global SDK components to satisfy another project.

## 21. ANDROID DEVICE TOOLING BASELINE

Where a device/emulator exists, identify the Mail Organizer package only. Inspect connected devices without resetting or modifying unrelated applications.

## 22. ADB VALIDATION BASELINE

Use project-scoped ADB for install/launch/force-stop/logcat/dumpsys/screencap/screenrecord where relevant. Use adb reverse only when genuinely required.

## 23. RUNTIME BASELINE

Launch the current Mail Organizer build and record startup behavior, crashes, navigation and obvious UI failures.

## 24. ACCESSIBILITY BASELINE

Inspect semantics, touch targets, scalable text, contrast and reduced-motion behavior where UI exists.

## 25. PERFORMANCE BASELINE

Identify obvious main-thread work, continuous polling, unnecessary animations and expensive startup behavior. Do not create a continuous benchmark.

## 26. OFFLINE/EMPTY/ERROR BASELINE

Verify the foundation can represent no-account, offline, loading, empty and error states without fake Gmail data.

## 27. ARCHITECTURE FOUNDATION

Implement only foundation changes required to preserve clean Presentation → Application → Domain → Data → External API boundaries.

## 28. DEVELOPMENT STATUS

Create/update development-status documentation if the repository uses it. Record real baseline findings, blockers and decisions.

## 29. EDITOR RULES

Update editor-rules.md only with genuinely permanent rules discovered during the audit. Never delete unrelated rules or regenerate the whole document unnecessarily.

## 30. TEST MATRIX

At minimum validate repository structure, Gradle build, tests, app launch, package identity, logs, Git diff and workspace isolation.

## 31. SECURITY VERIFICATION

Confirm no credentials/secrets/tokens are introduced. Confirm logs do not expose sensitive data.

## 32. GIT VERIFICATION

Re-run git status/diff from the Mail Organizer root. Confirm no sibling project changed.

## 33. FINAL BUILD

Rebuild after all Phase 0 changes. Fix only issues caused by this phase.

## 34. FINAL DEVICE CHECK

Reinstall only Mail Organizer when appropriate, launch, force-stop/relaunch and inspect logcat. Capture screenshots/screen recording if UI changes require visual validation.

## 35. ACCEPTANCE CRITERIA

- instruction folder discovered;
- correct root identified;
- KMP architecture confirmed;
- Gradle/build baseline established;
- Android identity verified;
- dependency/security/privacy audit completed;
- test baseline established;
- device tooling validated where available;
- no sibling project modified;
- permanent rules/status documented;
- no future phase implemented.

## 36. FINAL REPORT

Report Phase 0 status, repository/root, architecture findings, build/test results, device/API results, security findings, workspace isolation, files changed, known blockers, deferred work and acceptance-criterion status.

## 37. STOP

Phase 0 ends here. Do not implement Phase 1 or later functionality.
