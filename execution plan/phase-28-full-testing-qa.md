# Phase 28 — Full Testing & QA

## Execution contract
Read requirements.md, spec.md, design.md and editor-rules.md first. Confirm this is the first incomplete phase. Inspect the actual repository before editing. Work only inside Mail Organizer; never modify sibling projects. Implement only this phase and do not execute later phases.

## Phase objective
Run a full verification campaign for phases 0–27 rather than trusting checkboxes. Cover unit/integration/UI/device/security/privacy/performance/accessibility/background/offline/multi-account/Gmail/Calendar/Tasks/automation/AI regressions. Use Gradle, ADB, install/launch/force-stop/logcat/dumpsys/screencap/screenrecord and database inspection. Test fresh install, upgrade, migration, process death, reboot, permission revocation and account removal. Classify defects P0–P3 and never fake results.

## Mandatory verification
- Build with the repository's official Gradle/KMP tooling.
- Run relevant automated tests and record real results.
- For Android, use ADB for install, launch, force-stop, logcat, dumpsys, screencap and screenrecord where relevant; use adb reverse only when genuinely required.
- Inspect runtime behavior, database state, errors and account boundaries where applicable.
- Perform visual QA against design.md, including dark mode, accessibility and empty/error/offline states.
- Fix failures and rebuild/reinstall/retest before completion.
- Never expose secrets or sensitive email content in logs.

## Documentation and stop condition
Update editor-rules.md with genuinely permanent new rules without deleting unrelated rules. Update spec.md only after verification. Record blockers as [!]; never fake completion. When this phase is verified, stop and do not implement the next phase.