# Phase 06 — Core Inbox & Email Viewer

## Execution contract
Read requirements.md, spec.md, design.md and editor-rules.md before starting. Confirm this is the first incomplete phase. Inspect the actual repository before editing. Work only inside Mail Organizer; never modify sibling projects. Implement only this phase and do not execute later phases.

## Phase objective
Build the local-data Mail/Inbox/thread/email viewer. Use synchronized local data, safe HTML rendering, thread timelines, sender metadata and Gmail read-only state. Provide loading/empty/error/offline states, attachment metadata without automatic downloads, safe user-initiated links, accessibility, dark mode and pagination. No new sync logic or Gmail writes.

## UI requirements
- All Inbox must expose source Gmail account identity on every message row through a compact circular account indicator or equivalent.
- The account indicator represents the receiving/source mailbox, not the sender/company avatar.
- Message rows expose a familiar Star control.
- Starring an email preserves its existing category and adds it to the global Starred view.
- The top app bar uses the shared Gmail-familiar search/account/navigation shell defined in design.md.
- Category/company filter context must remain visible when the user drills into a company within a category.

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