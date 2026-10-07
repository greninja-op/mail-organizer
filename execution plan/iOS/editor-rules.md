# Mail Organizer iOS — Editor Rules

## 0. Mandatory platform isolation — FIRST RULE OF EVERY PHASE

This is an **iOS / iPhone** phase.

- Put all iOS-specific documents and implementation artifacts under repository-root `iOS/`.
- Never create or modify `iPadOS/`, `AndroidTablet/`, `macOS/`, `Windows/`, or `Linux/` during this plan.
- Do not place iOS-specific code in a sibling platform folder.
- Shared KMP code remains shared; do not duplicate it into `iOS/` just to satisfy folder separation.
- If shared KMP code changes, prove that the iOS phase requires the change.
- Never touch unrelated platform projects.
- Persist this rule in the root repository editor rules if a root rules file exists.

## Architecture

Presentation → Application/Use Cases → Domain → Data → External APIs.

KMP owns shared business/data behavior. SwiftUI owns iPhone presentation and iOS platform concerns.

Reuse existing systems before creating replacements. Do not build duplicate sync engines, classification engines, Action Engines, search engines, rule engines, AI routers, or persistence systems.

## Security

Use official OAuth/browser authentication. Never collect passwords. Least privilege. Secure token storage. No secrets in source/log/UI/ordinary DB.

Email is hostile/untrusted input. Sanitize HTML. No JavaScript execution from email. No automatic URL activation or external action authorization based solely on email content.

Explicit account boundaries are mandatory.

## Product behavior

Gmail is cloud source of truth. Local state is minimized and rebuildable where possible.

User correction > user rule > deterministic intelligence > optional AI > unknown.

AI never authorizes external actions.

Automation is finite, explainable, account-scoped, idempotent, and safety-validated.

## iOS UI/performance

Use SwiftUI and native iOS conventions. Support Dynamic Type, VoiceOver, Reduce Motion, dark mode, responsive iPhone sizes, and real error/offline states.

Optimize sustained performance, memory, battery, thermal behavior, and scheduler-friendly background work.

Do not claim 120 FPS merely because hardware supports 120 Hz.

## Phase protocol

Read the root instructions and iOS master docs, identify the first incomplete phase, inspect the actual repository, implement only that phase, validate it, update documentation/status, and stop.

When a genuinely permanent rule is discovered, add it without deleting or rewriting unrelated rules.

Never claim unrun tests or device verification. Never commit secrets. Never auto-publish.
