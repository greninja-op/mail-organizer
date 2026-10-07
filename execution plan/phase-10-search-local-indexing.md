# Phase 10 — Search & Local Indexing

## Execution contract
Read requirements.md, spec.md, design.md and editor-rules.md before starting. Confirm this is the first incomplete phase. Inspect the actual repository before editing. Work only inside Mail Organizer; never modify sibling projects. Implement only this phase and do not execute later phases.

## Phase objective
Implement fully local search and indexing over normalized and derived data. Support sender, subject, body, thread, company/domain, category, priority, action and Gmail labels/categories with practical filters. Use SQLite/Room FTS or equivalent, parameterized queries, deterministic ranking, incremental updates, account isolation and safe snippets. Test empty/pathological queries, rebuild and realistic scales.

## UI requirements
- The Android top app bar must expose a Gmail-familiar search affordance.
- Search must preserve account isolation and support All Inbox across accounts without losing source-account identity.
- Search should be able to filter by company/domain and category while retaining the selected category/company context.
- Search results must retain the same compact source-account indicator used by All Inbox.
- Search should be local-first once the index is available; avoid unnecessary network requests for ordinary local search.

## Mandatory verification
- Build with the repository's official Gradle/KMP tooling.
- Run relevant automated tests and record real results.
- For Android, use ADB for install, launch, force-stop, logcat, dumpsys, screencap and screenrecord where relevant; use adb reverse only when genuinely required and scoped to this project.
- Inspect runtime behavior, local database state, errors and account boundaries where applicable.
- Perform visual QA against design.md, including dark mode, accessibility and empty/error/offline states where relevant.
- Fix failures and rebuild/reinstall/retest before declaring completion.
- Never expose secrets or sensitive email content in logs.

## Documentation and stop condition
Update editor-rules.md with any genuinely permanent rule without deleting unrelated rules. Update spec.md only after verification and add development-status documentation when appropriate. Record blockers as [!]; never fake completion. When this phase is verified, stop. Do not implement the next phase in the same session.