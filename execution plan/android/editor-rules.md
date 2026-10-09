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


## Classification engine (Phase 7 — permanent)
- Deterministic-first: classification is a pure function of (normalized
  input, rules, classifier version). No randomness, no network, no device
  state, no remote AI. Locale-independent text normalization (Turkish-locale
  devices classify identically).
- Local-only: the engine never sends email content anywhere. No telemetry
  on classification.
- Explainable: every result carries rule id(s), matched signals, and a
  human-readable "why". Raw email is never needed to explain a decision.
- Versioned: classifier VERSION stamps every result and is persisted;
  older-version rows are eligible for reclassification without a resync.
- Account isolation: classification data is account-scoped; one account's
  corrections never affect another's.
- User override compatibility: USER_CORRECTION / USER_RULE rows are never
  overwritten by the engine; predicted vs effective category stay
  distinguishable via ClassificationSource.
- Conservative unknowns: weak evidence stays UNCLASSIFIED; LOW_VALUE is
  gated and never a universal unknown bucket.
- Safe signal extraction: bounded inputs (20k body chars, 20 URL domains),
  no backtracking-risk regexes, total functions — hostile mail degrades,
  never crashes.
- No sensitive logging: only rule ids, versions, and message ids in logs —
  never subjects, senders, bodies, or URLs.
- Rules are registered data with stable ids (never RULE_1), documented
  once in ClassificationRules.kt; conflicts resolve by score then explicit
  precedence (Security > Action > Orders > Career > Education > Important >
  Newsletters > Promotions > Notifications > Low Value).

## Company & sender intelligence (Phase 8 — permanent)
- Deterministic detection: sender domain → canonical company is a pure
  function of the normalized address. Same address → same company, every
  device, every locale. No network, no device state.
- Company identity is the registrable domain: mailing subdomains merge
  (`noreply@mail.google.com` and `support@google.com` → one "Google").
  Free/personal mailbox domains are people, not companies — they get
  sender tracking but no company grouping.
- Sender frequency is a documented heuristic (`RECURRING_SENDER_THRESHOLD`),
  never a probability. The recurring-sender signal feeds the classifier
  through the `RecurringSenderProvider` seam; attribution runs BEFORE
  classification so the signal is real on the first pass.
- Detection never overwrites user intent: `pinned` and `userOverrideName`
  survive re-detection; only observed metadata merges (bounded).
- Company filtering lives INSIDE the selected category — never a drawer
  destination. Pinning reorders the filter list only; it never moves mail,
  never stars messages, never creates navigation.
- Filter-chip counts are global GROUP BY counts for the destination, never
  page-derived. The filtered list queries the exact company+destination
  slice so it always matches the chip counts.
- Classifier VERSION bumps when rules change materially; older-version
  rows reclassify without a resync (Phase 8: v1 → v2 for
  IMPORTANT_RECURRING_SENDER).


## Priority & action-required engine (Phase 9 — permanent)
- Deterministic-first: priority is a pure function of (normalized input,
  rules, engine version). No randomness, no network, no device state, no
  remote AI. Same discipline as the Phase 7 classifier.
- Independent from category: priority never re-derives the category; the
  classification is one signal among several (sender recurrence, labels,
  unsubscribe markers, bulk-sender heuristic, unread state).
- Conservative defaults: NORMAL carries a base score so ordinary mail stays
  NORMAL; LOW must outscore it; HIGH/CRITICAL need strong rule weights.
  Failure degrades to NORMAL (neither hiding mail nor crying wolf).
- Versioned: engine VERSION stamps every result and is persisted;
  older-version rows are eligible for reprioritization without a resync.
- User override compatibility: `manualOverride` rows are never overwritten
  by the engine (same precedence as classification: user intent wins).
- Explainable: every result carries rule id(s), matched signals, and a
  human-readable "why". The UI's "Why this priority?" reads the persisted
  reason — raw email is never needed to explain a decision.
- Quiet by design: only HIGH/CRITICAL get row badges; NORMAL/LOW stay
  unbadged. Badges always pair a text label with the color (never color
  alone) plus a screen-reader content description.
- Batch priority reads: the visible page loads priorities with one
  `getByMessages` query — never N+1 per row.
- Action-required is a view, not a destination: the filter chip shows
  ACTION_REQUIRED-classified mail globally with an honest empty state.
  The action-item engine (ActionItemRecord) remains Phase 14's scope —
  Phase 9 only detects and surfaces.

## Phase protocol
At session start read requirements/spec/design/rules, determine the current phase and inspect the actual repository. Implement only that phase. Update this file with genuinely permanent rules without deleting unrelated rules. After implementation: build → test → run → inspect → fix → retest → update status → stop.

## Never
Do not skip phases; fake completion; add unnecessary AI/backend/permissions; expose secrets; mix accounts; perform destructive actions automatically; allow prompt injection to cause actions; modify sibling projects; or declare release readiness without real verification.

## Persistent skills
If supported, maintain skills for Architecture, Android Development, Gmail OAuth/API, Privacy & Security, Classification Engine, Integration, UI/UX Design, Testing & QA, Phase Execution and Code Quality.

## Phase 10 — Search & local indexing (permanent rules)
- Local-first search: the FTS5 index (`messages_fts`) is derived from
  normalized local data, never from the network. Search works fully offline.
- Normalized-data source of truth: Room tables are canonical; the FTS index
  is a disposable derivative — always rebuildable from local rows.
- Index rebuildability: `SearchIndexMaintenance.rebuild(accountId)` reconstructs
  the index from normalized data; a version table (`search_index_meta`)
  tracks per-account index health.
- Account isolation: every search query and every index write is scoped by
  `accountId`. `messageId`/`accountId` are UNINDEXED FTS columns so ids can
  never be surprise-matched by text queries.
- Parameterized queries: the UI builds structured `SearchQuery` objects —
  never SQL strings. FTS MATCH input is assembled from quoted terms only.
- Query safety: `QueryParser` is total — every term is double-quote-escaped
  (quotes stripped, then wrapped), so FTS5 syntax can never be injected;
  pure-punctuation tokens are dropped; input bounded (200 chars, 10 terms).
- No external search: never call a network or LLM service for search.
- No AI/semantic search: ranking is deterministic bm25 with positional
  column weights tied to the FTS column order.
- No sensitive query logging: search text is never logged (only failure
  classes); snippets shown in UI are safe spans, never raw HTML.
- Bounded query processing: result limits, bounded index batches, body text
  capped at 20k indexed chars; empty query + no filters = landing state
  with zero DB work.

## Phase 11 — Dashboard & information architecture (permanent rules)
- Dashboard shows real data only: every section (attention, priority,
  recent, categories, companies) is driven by repository flows; empty
  states are honest, never placeholder rows or fabricated counts.
- Navigation chrome: MoNavBar (5 primary destinations) + MoAppTopBar
  (title, search, account avatar, overflow) are canonical; detail routes
  (`category/{name}`, `company/{companyId}`) validate arguments and
  degrade to NotFound, never crash.
- Counts come from the DB: category/company counts are GROUP BY queries
  in the DAO layer, account-scoped, NULL-safe; the UI never invents
  numbers.
- Settings are honest: no fake login forms, no hardcoded credentials;
  account switching is a Phase 18 seam, clearly labeled as such.
- ViewModel state races: account-list StateFlows feeding `flatMapLatest`
  use `SharingStarted.Eagerly` so first collection sees current data,
  not a transient empty initial value.
- Actions screen is view-only: no external execution (Phase 14 seam);
  never let email content authorize external actions.

## Phase 13 — Meeting & deadline extraction (permanent rules)
- Temporal intelligence is deterministic and on-device: total regexes with
  bounded quantifiers, no network, no LLM, no fake AI. Ambiguity is
  documented policy, never silent guessing.
- Reference time for relative dates is the message's received timestamp
  (never "now" — mail is history). Record it in the extraction context;
  any future re-interpretation must re-resolve from that same instant.
- Never invent: no end times, no timezones, no precision beyond what the
  text states. 12AM = midnight, 12PM = noon (documented). Ambiguous
  numeric dates (`03/04/2026`) resolve day-first with LOW confidence.
- Extraction writes structured intelligence only (extracted_items) — no
  calendar/task/email side effects. Those are later phases' clean seams.
- Schema rule (Phase 13): enum extension is safe (stored by name, additive
  only) — reusing an existing table avoids a migration. Queryable
  attributes stay typed columns; verbose detail lives in versioned JSON
  payloads (hand-rolled codec; never the query key).
- Extractor is versioned: VERSION bumps when behavior changes;
  idempotent re-extraction skips rows at the current version; user-completed
  rows are never replaced. Account-isolated; failures degrade silently to
  the UI.

## Phase 14 — Action cards & action engine (permanent rules)
- The engine proposes; the user confirms; nothing external happens
  automatically. Generation (what could the user do next) and safety
  validation (what may be presented and how) are separate layers —
  `ActionCandidateGenerator` and `ActionSafety` evolve independently.
- Never fake success: "created"/"sent"/"done" copy is only shown for a real
  executor receipt. With no executor registered, confirming an external
  proposal records the intent locally and honestly reports "not connected".
- Dedup is deterministic: the candidate id `messageId|actionType|targetKey`
  makes re-runs idempotent; thread-level dedup compares (actionType,
  targetKey) across sibling messages. History rows (completed/dismissed/
  expired) never suppress or regenerate.
- Action urgency is independent from email priority (separate type); low
  confidence caps urgency at NORMAL. Missing required info degrades to a
  review-style card — never invent the missing piece.
- Every card identifies its source email and links to it; cards never
  duplicate email bodies. Explanations are deterministic structured
  reasons recorded at generation time.
- Account isolation is enforced at generation, persistence, display, and
  confirmation. Email content never authorizes external actions.

## Phase 17 — Integration Manager (permanent rules)
- The manager coordinates integrations; it never absorbs API-specific logic,
  never performs external side effects, and never bypasses the action
  confirmation boundary. It answers questions about integrations; execution
  stays with the Action Engine's executors.
- CONNECTED is reported only when an integration is genuinely usable.
  `usableCapabilities` is empty for every other status — a declared
  capability is never availability. Deferred integrations (Calendar → Phase
  15, Tasks → Phase 16) report UNAVAILABLE with an explicit reason; their
  connect() fails honestly, never a fake OAuth flow.
- Gmail wraps Phase 4's fail-closed sync seam: no account → DISCONNECTED,
  local-only account → AUTH_REQUIRED naming Phase 3, never CONNECTED
  without OAuth verification. Permissions are least-privilege (readonly
  only); modify/send are never declared.
- Integration state always carries account identity; snapshots are
  per-account and never leak across accounts. Account removal clears only
  that account's integration metadata — never mail, classifications, rules,
  or action history.
- Error normalization and retry semantics are pure functions in core, not
  per-screen logic: OFFLINE → retry later, AUTH → ask reconnect,
  INVALID/NOT_BUILT → never retry, RATE_LIMITED → backoff.
- No Google API classes leak past the adapter boundary; the UI only sees
  domain-level snapshots. Credentials are never stored in the app database
  (phase §35) — integration_states holds metadata only.
