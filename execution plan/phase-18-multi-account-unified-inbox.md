# Phase 18 — Multi-Account & Unified Inbox

## Execution contract
Read requirements.md, spec.md, design.md and editor-rules.md first. Confirm this is the first incomplete phase. Inspect the actual repository before editing. Work only inside Mail Organizer; never modify sibling projects. Implement only this phase and do not execute later phases.

## Phase objective
Make account identity a first-class boundary across every entity, repository, use case, search result, rule, action and integration. Add multiple-account switching and unified inbox as a presentation layer over account-owned data. Preserve source-account context everywhere, isolate OAuth/sync state, handle partial failures and make cross-account destinations explicit and safe.

## UI requirements
- All Inbox rows visibly identify their source Gmail account with the shared compact circular account indicator.
- The account/profile affordance opens the account switcher with current account, connected accounts and add-account.
- Swiping the profile affordance can immediately switch to the next account where supported.
- Adding an account uses the shared Gmail → Mail Organizer recovery/sync animation when actual retrieval takes meaningful time.
- Unified Inbox must never visually or logically mix account identity with sender/company identity.
- Account-specific unread/count state remains isolated while aggregate All Inbox counts may be shown.

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