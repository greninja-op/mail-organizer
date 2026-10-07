# Phase 13 — Meeting & Deadline Extraction

## Execution contract
Read requirements.md, spec.md, design.md and editor-rules.md before starting. Confirm this is the first incomplete phase. Inspect the actual repository before editing. Work only inside Mail Organizer; never modify sibling projects. Implement only this phase and do not execute later phases.

## Phase objective
Implement deterministic local temporal intelligence for deadlines, meetings, appointments, interviews, submissions, applications, payments and registrations. Extract dates/times/ranges/timezones, location and meeting URLs with source, confidence, explanation and version. Handle IST/Asia-Kolkata, relative dates, historical vs future, rescheduling/cancellation and thread context. No external execution yet.

## Mandatory verification
- Build with the repository's official Gradle/KMP tooling.
- Run relevant automated tests and record real results.
- For Android, use ADB for install, launch, force-stop, logcat, dumpsys, screencap and screenrecord where relevant; use adb reverse only when genuinely required and scoped to this project.
- Inspect runtime behavior, local database state, errors and account boundaries where applicable.
- Perform visual QA against design.md, including dark mode, accessibility and empty/error/offline states where relevant.
- Fix failures and rebuild/reinstall/retest before declaring completion.
- Never expose secrets or sensitive email content in logs.

## Documentation and stop condition
Update editor-rules.md with any genuinely permanent new rule without deleting unrelated rules. Update spec.md only after verification and add development-status documentation when appropriate. Record blockers as [!]; never fake completion. When this phase is verified, stop. Do not implement the next phase in the same session.