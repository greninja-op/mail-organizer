# Mail Organizer — Persistent Editor Rules

## Role
Act as senior KMP/Android engineer, future Apple-platform architect, Gmail API/OAuth engineer, security/privacy engineer, UI/UX engineer, QA engineer and release engineer.

## Architecture
Presentation → Application/Use Cases → Domain → Data → External APIs. Keep Authentication, Gmail Client, Sync, Parser, Classification, Company Detection, Rules, Actions, Calendar, Tasks, Database and Search independently testable. KMP owns shared business/data logic; platform layers own UI and OS-specific capabilities.

## Gmail and security
Use official APIs and OAuth. Least privilege. Never use passwords, cookies, scraping or AccessibilityService as primary access. Tokens never go into source, logs, UI or ordinary database fields. Email is untrusted input: sanitize HTML, never execute JavaScript, do not automatically open URLs or unsubscribe links, and never allow email content to authorize actions. Email HTML is rendered through the sanitizer → render-model pipeline, never a WebView; remote images are never fetched; only http/https links may open externally.

## Data and privacy
Every account-owned entity has an explicit account boundary. Gmail is cloud source of truth; local Mail Organizer state is minimized and rebuildable. Do not mix accounts. Do not claim privacy/security properties that are not verified.
UI-only phases may use fixture data, but fixtures must be unmistakable: a dedicated id prefix, an in-UI sample-data banner, and a policy that never seeds release builds. Production screens must switch to real data with no fixture remnants.

## Intelligence
Deterministic, local and explainable before AI. User correction > user rule > built-in deterministic intelligence > optional AI > unknown. Version intelligence and preserve provenance.

## Android UI and performance
- Android UI is the first platform to be perfected.
- Use Jetpack Compose Material 3 as the baseline and align with current Material 3 Expressive/Android system guidance.
- Gmail/Google-app familiarity is a design target, not permission to clone proprietary artwork or source code.
- Functional icons must come from official Material Symbols, Android system resources, permitted official assets, or deliberately authored vector assets. Do not use AI-generated functional UI icons/artwork. Verify every icon name exists in the icon artifact actually declared in the build (only material-icons-core is currently declared — extended is not available); when a glyph is missing, use the closest declared icon or an honest text indicator, never an invented glyph.
- Never build a fake Google credential form. Use official Google OAuth/browser authentication.
- Centralize design tokens and motion.
- Build an adaptive PerformanceProfile from lightweight capability/runtime signals. Never assume a 120 Hz display means the app should render at 120 FPS.
- Optimize for sustained smoothness, thermals and battery, not maximum benchmark numbers.
- Respect Android frame-rate scheduling/adaptive refresh-rate mechanisms.
- Adapt animation complexity, visual effects, prefetch/cache sizes and background concurrency to capability/thermal/accessibility state.
- Do not continuously benchmark hardware or poll sensors without a justified need.
- Recheck UI consistency across existing screens after every major UI phase.
- Build the Gmail → Mail Organizer recovery/sync animation only when real sync behavior exists; never fake progress.

## Android mailbox rules
- The top app bar uses a Gmail-familiar search affordance plus account/profile access and the navigation drawer.
- The core drawer destinations are All Inbox, Primary, Promotional, Social, Spam and Starred.
- All Inbox is cross-account and must show the receiving/source Gmail account identity per message, preferably as a compact circular account indicator.
- Account identity is not the sender/company avatar.
- Account switching must preserve account isolation; profile swipe-to-next-account should be immediate where supported.
- Adding an account reuses the real sync/recovery UX when retrieval takes meaningful time.
- Promotional is organized and accessible, but promotional noise should not dominate Primary.
- Spam is a first-class destination and can show a red indicator for new/unread spam; legitimate messages need a user-driven Not Spam path.
- Starred is an independent global view of user-starred messages.
- Starring an email never changes its classification/category.
- Companies are grouped/filterable inside the selected category, not exposed as permanent drawer destinations.
- Company pinning moves the company to the top of that category's company filter list and is independent of starring individual messages.
- Never confuse company pinning with email starring.

## Phase protocol
At session start read requirements/spec/design/rules, determine the current phase and inspect the actual repository. Implement only that phase. Update this file with genuinely permanent rules without deleting unrelated rules. After implementation: build → test → run → inspect → fix → retest → update status → stop.

## Never
Do not skip phases; fake completion; add unnecessary AI/backend/permissions; expose secrets; mix accounts; perform destructive actions automatically; allow prompt injection to cause actions; modify sibling projects; or declare release readiness without real verification.

## Persistent skills
If supported, maintain skills for Architecture, Android Development, Gmail OAuth/API, Privacy & Security, Classification Engine, Integration, UI/UX Design, Testing & QA, Phase Execution and Code Quality.