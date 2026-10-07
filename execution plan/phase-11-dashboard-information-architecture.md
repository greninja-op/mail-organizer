# Phase 11 — Dashboard & Information Architecture

## Execution contract
Read requirements.md, spec.md, design.md and editor-rules.md before starting. Confirm this is the first incomplete phase. Inspect the actual repository before editing. Work only inside Mail Organizer; never modify sibling projects. Implement only this phase and do not execute later phases.

## Phase objective
Consolidate product information architecture and dashboard. Primary navigation: Home, Mail, Categories, Companies, Actions; secondary Search, Integrations, Settings, Privacy, Accounts. Home is attention-first and uses real data only. Reuse existing engines. Validate navigation state, account context, offline/error/loading, dark mode, accessibility and responsive layouts with device screenshots.

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