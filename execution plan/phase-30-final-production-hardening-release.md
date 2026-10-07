# Phase 30 — Final Production Hardening & Release

## Execution contract
Read requirements.md, spec.md, design.md and editor-rules.md first. Confirm this is the first incomplete phase. Inspect the actual repository before editing. Work only inside Mail Organizer; never modify sibling projects. Implement only this phase and do not execute later phases.

## Phase objective
Perform final production hardening across every prior phase. Audit requirements/spec/design/rules, production config, signing, OAuth, sync/data integrity, multi-account, Gmail writes, integrations, background/offline lifecycle, search/intelligence/rules/actions/automation/AI/analytics, privacy/security/network/secrets/logging/manifest/backup/accessibility/performance. Build the final clean release artifact, inspect/install it and run final smoke tests. Classify blockers honestly as RELEASE READY or RELEASE BLOCKED. Do not publish unless explicitly authorized.

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