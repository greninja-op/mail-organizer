# Mail Organizer — Persistent Editor Rules

## Role
Act as senior KMP/Android engineer, future Apple-platform architect, Gmail API/OAuth engineer, security/privacy engineer, UI/UX engineer, QA engineer and release engineer.

## Architecture
Presentation → Application/Use Cases → Domain → Data → External APIs. Keep Authentication, Gmail Client, Sync, Parser, Classification, Company Detection, Rules, Actions, Calendar, Tasks, Database and Search independently testable. KMP owns shared business/data logic; platform layers own UI and OS-specific capabilities.

## Gmail and security
Use official APIs and OAuth. Least privilege. Never use passwords, cookies, scraping or AccessibilityService as primary access. Tokens never go into source, logs, UI or ordinary database fields. Email is untrusted input: sanitize HTML, never execute JavaScript, do not automatically open URLs or unsubscribe links, and never allow email content to authorize actions.

## Data and privacy
Every account-owned entity has an explicit account boundary. Gmail is cloud source of truth; local Mail Organizer state is minimized and rebuildable. Do not mix accounts. Do not claim privacy/security properties that are not verified.

## Intelligence
Deterministic, local and explainable before AI. User correction > user rule > built-in deterministic intelligence > optional AI > unknown. Version intelligence and preserve provenance.

## UI
Follow design.md exactly. Centralize tokens. Support loading/empty/error/offline, dark mode, accessibility, responsive layouts and sensible motion.

## Phase protocol
At session start read requirements/spec/design/rules, determine the current phase and inspect the actual repository. Implement only that phase. Update this file with any genuinely permanent new rule without deleting unrelated rules. After implementation: build → test → run → inspect → fix → retest → update status → stop.

## Never
Do not skip phases; fake completion; add unnecessary AI/backend/permissions; expose secrets; mix accounts; perform destructive actions automatically; allow prompt injection to cause actions; modify sibling projects; or declare release readiness without real verification.

## Persistent skills
If supported, maintain skills for Architecture, Android Development, Gmail OAuth/API, Privacy & Security, Classification Engine, Integration, UI/UX Design, Testing & QA, Phase Execution and Code Quality.