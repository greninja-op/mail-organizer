# Phase 25 — Analytics & Insights

## Execution contract
Read requirements.md, spec.md, design.md and editor-rules.md first. Confirm this is the first incomplete phase. Inspect the actual repository before editing. Work only inside Mail Organizer; never modify sibling projects. Implement only this phase and do not execute later phases.

## Phase objective
Implement local product insights rather than generic telemetry. Provide deterministic inbox health, organization, sources, noise, Action Required, priority, category, company, sender, newsletter, promotion, notification, low-value, cleanup, conversation, deadline and meeting insights with useful date ranges. Respect account/unified scope and incomplete-sync states. No third-party telemetry by default and no gamification.

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