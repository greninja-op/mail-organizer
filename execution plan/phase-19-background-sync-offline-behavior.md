# Phase 19 — Background Sync & Offline Behavior

## Execution contract
Read requirements.md, spec.md, design.md and editor-rules.md first. Confirm this is the first incomplete phase. Inspect the actual repository before editing. Work only inside Mail Organizer; never modify sibling projects. Implement only this phase and do not execute later phases.

## Phase objective
Implement background sync with platform-supported scheduling, preferably WorkManager on Android. Use unique account-scoped work, network constraints, periodic/manual/launch/resume/connectivity/reboot triggers, incremental sync, safe retries and process-death recovery. Cached data remains usable offline. Never execute external actions automatically in background. Validate lifecycle behavior with dumpsys/logcat.

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