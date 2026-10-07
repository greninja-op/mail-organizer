# Phase 16 — Google Tasks Integration

## Execution contract
Read requirements.md, spec.md, design.md and editor-rules.md first. Confirm this is the first incomplete phase. Inspect the actual repository before editing. Work only inside Mail Organizer; never modify sibling projects. Implement only this phase and do not execute later phases.

## Phase objective
Implement official Google Tasks API with least privilege. Support task-list selection and proposals for suitable Action Engine candidates with title, notes and justified due dates. Require confirmation, prevent duplicates, handle token expiry/offline/errors and keep Calendar and Tasks complementary. Never invent due dates or claim success without verified API results.

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