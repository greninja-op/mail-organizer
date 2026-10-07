# Phase 11 — Dashboard & Information Architecture

## Execution contract
Read requirements.md, spec.md, design.md and editor-rules.md before starting. Confirm this is the first incomplete phase. Inspect the actual repository before editing. Work only inside Mail Organizer; never modify sibling projects. Implement only this phase and do not execute later phases.

## Phase objective
Consolidate product information architecture and dashboard around an Android Gmail-familiar mailbox shell. Primary drawer destinations are All Inbox, Primary, Promotional, Social, Spam and Starred. The drawer is not a company directory. Company grouping/filtering lives inside the selected category. The top bar contains navigation, search and account switching.

## Navigation
Required drawer destinations:
- All Inbox
- Primary
- Promotional
- Social
- Spam
- Starred

Folder counts and unread indicators must reflect real state. Spam may show a red indicator for new/unread spam.

## All Inbox
All Inbox is the unified cross-account mailbox view. Each row exposes the receiving Gmail account via a compact circular source-account indicator. This is distinct from the sender/company avatar.

## Search
The top app bar includes the shared Gmail-familiar search affordance. Search operates against the local index when available and preserves account/category/company context.

## Account switcher
The profile/account affordance opens the account switcher. It shows the current account, other connected accounts and add-account. Swiping the profile affordance can switch immediately to the next account where supported. Adding an account uses the real Gmail → Mail Organizer recovery/sync UX.

## Categories and company filtering
Promotional and Social remain organized destinations but should not dominate Primary.

Within a selected category, show company/sender groups or filters. For example:
- pinned: Google, Facebook, Amazon
- other: Canva, Notion, n8n

Selecting Google filters the current category to Google mail. The category context remains visible.

Company pinning is not a navigation destination. It only changes ordering within the current category's company filter list.

## Individual starring
Every mail row may expose a Star control. Starred messages remain in their existing category and also appear in Starred. Company pinning and email starring are separate.

## Spam
Spam is a first-class destination. New/unread spam should show a red indicator. Legitimate spam must have a clear user-driven Not Spam/recovery path.

## Attention-first Home
Home may surface important/actionable information but must not replace the explicit mailbox destinations above. Reuse existing engines and real data only. Validate navigation state, account context, offline/error/loading, dark mode, accessibility and responsive layouts with device screenshots.

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