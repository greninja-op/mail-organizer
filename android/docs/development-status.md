# Development Status — Mail Organizer (Android)

**Last updated:** 2026-10-09 (Phase 9 complete)
**Branch:** `main`
**Application ID:** `com.greninjaop.mailorganizer`

## Phase 0 — Project Audit & Development Foundation: COMPLETE

### What was built
Production-quality Android foundation scaffolded from an empty repository:
- Gradle 8.14.6 + AGP 8.13.2 + Kotlin 2.3.21 + KSP 2.3.12
- Jetpack Compose (BOM 2026.06.01) + Material 3
- Room 2.8.5 (with KSP), DataStore Preferences 1.2.1
- Coroutines 1.11.0, Navigation Compose 2.9.8
- Manual DI via `AppContainer` (no framework)
- compileSdk 35, targetSdk 35, minSdk 26
- 17 Kotlin sources, 4 test files, 10 skill docs, architecture doc

### Validation
- `:app:assembleDebug` — **BUILD SUCCESSFUL** (2026-10-08)
- `:app:lintDebug` — **BUILD SUCCESSFUL** (4 warnings, 0 errors)
- `:app:testDebugUnitTest` — Tests compile; Gradle test worker has an
  environment issue in this sandbox (JVM crashes on startup). Tests were
  verified manually via direct `java` invocation and pass. This is a
  sandbox environment limitation, not a code issue.

### Version decisions
All versions verified against Google Maven AAR metadata (2026-10-08).
The very latest AndroidX releases (core-ktx 1.19.x, Compose BOM 2026.09.00,
lifecycle 2.11.0 compose artifacts) require compileSdk 37 + AGP 9.1.0,
but SDK 37 is not publicly available (max is 36) and AGP 9.x migration
risks the foundation. This set pins the newest versions compatible with
AGP 8.13.2 + SDK 35:
- core-ktx 1.16.0, activity-compose 1.10.0, lifecycle 2.10.0
- navigation-compose 2.9.8, compose-bom 2026.06.01 (ui 1.11.4)
- room 2.8.5, datastore 1.2.1, coroutines 1.11.0

### Deferred work (user-approved)
- Phase 3 (Google OAuth & Gmail Connection)
- Phases 15/16 (Calendar/Tasks integrations)
- Phase 22 (Gmail write features)
- Phase 29 (production OAuth/Play Store)
These require OAuth credentials / API keys the user will provide at the end.
Code is structured with interfaces so they plug in later. No fake
implementations, no hardcoded secrets.

### Known issues
1. Gradle test worker crashes in this sandbox (JVM exits code 1 on startup).
   Tests verified manually; will work on a normal machine/CI.
2. 4 lint warnings (OldTargetApi, GradleDependency, ObsoleteSdkInt,
   MonochromeLauncherIcon) — all benign for Phase 0.
3. Sandbox blocks JVM network access; dependencies pre-fetched via curl
   into a local file:// Maven repo. See `~/local-m2` on the build machine.
   On a normal machine, remove the `file://` repo from `settings.gradle.kts`.

## Phase 1 — Android Application Foundation: COMPLETE (2026-10-09)

### What was built
- **Design tokens** per execution plan (phase-01 §20–23): spec-verbatim color
  palette (Primary `#5B5CE2`, Primary Container `#E8E8FF`, Light Bg `#F8F9FC`,
  Dark Bg `#101114`, surfaces `#FFFFFF`/`#1A1B20`, dark elevated `#222329`,
  semantic Success/Warning/Error/Info) as full Material 3 light/dark schemes;
  4dp spacing grid (`MoSpacing`); shape radii 8/12/16/20/pill (`MoShapes`,
  wired into `MaterialTheme`); complete Material 3 type scale.
- **Typography**: full Inter-preferred type hierarchy (display → caption).
  Inter is NOT bundled: the GitHub push path available here cannot
  verifiably transport binary font files, so bundling risked a corrupt repo.
  The plan explicitly allows a system fallback — using it (reversible; see
  Decisions). No `res/font` dir; no network font fetching (privacy §39).
- **Navigation foundation**: 10 routes registered —
  Home/Mail/Categories/Companies/Actions (primary) +
  Search/Integrations/Settings/Privacy/Accounts (secondary). Home is the
  §26 initial screen: "Mail Organizer", tagline
  "Organize your Gmail into a clearer, action-first workspace.",
  "Foundation build · Phase 1 — no account connected yet", theme switcher,
  and a destination list proving navigation works. Other destinations are
  honest placeholders naming their future phase. **No fake data anywhere.**
- **Reusable state components**: `MoLoadingState`, `MoEmptyState`,
  `MoErrorState` (retry), `MoSuccessState` — 48dp touch targets,
  screen-reader labels.
- **Dark mode**: complete dark palette; dynamic color on Android 12+
  retained per design.md.
- **Accessibility**: content descriptions, 48dp minimum targets, semantic
  loading labels.
- versionName → `0.1.0-phase1`. Manifest unchanged: **no new permissions**
  (no INTERNET, no Gmail scopes — those belong to Phase 3+).

### Validation
- `:app:assembleDebug` — **BUILD SUCCESSFUL**
- `:app:lintDebug` — **BUILD SUCCESSFUL** (4 warnings, 0 errors — same as
  Phase 0 baseline)
- **24/24 unit tests pass** via direct `java` JUnitCore run (7 classes;
  3 new: color tokens, design tokens, navigation). The Gradle test worker
  still crashes on startup in this sandbox (environment limitation,
  unchanged from Phase 0). Manual-run recipe: extract `classes.jar` from
  each AAR on the unit-test runtime classpath, add `android.jar`, and point
  Robolectric at a local `android-all-instrumented` jar
  (`-Drobolectric.dependency.dir`).
- **Device validation: NOT POSSIBLE** — no adb, no emulator, no KVM in this
  sandbox. All device/install/screenshot criteria are marked [!]
  blocked-by-environment (the phase doc conditions them on availability).
  Must be validated on a real device or CI before release phases.

### Decisions
- Inter via system fallback (see above); reversible.
- Non-spec color companions (onPrimaryContainer, surface variants, dark
  error `#FFB4AB`) chosen for contrast and documented as derived in
  `Color.kt`.
- Project lives under `android/` in the repo: the repo root keeps the
  execution-plan README (the project README links it via `../README.md`),
  and `CONTEXT.md` sits at the repo root per the continuity rule.

### Known issues
1. Gradle test worker crashes in this sandbox (pre-existing, Phase 0).
2. No device/emulator — install/launch/screenshot validation deferred to
   real hardware/CI.
3. 4 benign lint warnings (unchanged from Phase 0).

## Next: Phase 2 — Local Data Architecture
Do NOT start unprompted. Awaits user instruction.

---

## Phase 2 — Local Data Architecture: COMPLETE (2026-10-09)

**Last updated:** 2026-10-09 (Phase 2 complete)
**Database:** Room 2.8.5, version 2 (v1 → v2 additive migration)

### What was built
- **12 entities**: accounts (expanded: provider, connectionState, lastSync,
  enabled), threads, messages, senders, companies, classifications,
  priorities, action_items, sync_state, user_rules, user_corrections,
  extracted_items. All account-owned rows carry `accountId` with FK CASCADE
  from `accounts`; Gmail ids unique per account, never globally.
- **11 DAOs** with bounded (`LIMIT`) queries, Flow observables, and
  transactional single-current-row upserts (classification/priority/
  correction). No main-thread DB access (repositories dispatch to IO).
- **Room v2**: `exportSchema = true` (`app/schemas/`), `Migrations.MIGRATION_1_2`
  (purely additive), no destructive fallback. Converters: enums by name
  (never ordinal), string lists via U+001F separator.
- **5 repositories** (interface + Room impl): Account, Mail (transactional
  thread+message save — the Phase 4 sync pattern), Intelligence
  (senders/companies/classification/priority/actions/extracted),
  Rule (rules+corrections), SyncState. Wired into `AppContainer`.
- **Token boundary**: no OAuth tokens/passwords/secrets anywhere in the DB
  (documented in `docs/data-model.md`); Phase 3 uses Android secure storage.
- **Docs**: new `docs/data-model.md` (entities, indexing rationale,
  migrations, transaction strategy, privacy); architecture.md §Phase 2;
  `execution plan/android/editor-rules.md` + `spec.md` updated in repo.
- **Deferred** (unchanged): Phase 3 (OAuth), 15/16 (Calendar/Tasks),
  22 (Gmail write), 29 (prod OAuth/Play). No fake implementations.

### Validation
- `:app:assembleDebug` — **BUILD SUCCESSFUL** (APK built)
- `:app:lintDebug` — **BUILD SUCCESSFUL** (4 warnings, 0 errors — same baseline)
- **47/47 unit tests pass** via direct `java` JUnitCore (12 classes):
  24 pre-existing + 23 new (converters 4, DAO/CRUD 11, account-isolation 2,
  v1→v2 migration 1, repositories 5). The Gradle test worker still crashes
  on startup in this sandbox (environment limitation, unchanged); the
  manual-run recipe is in `docs/development-status.md` (Phase 0).
  **Notable:** the account-isolation test proves cross-table per-account
  separation, and the migration test builds a genuine v1 DB with raw SQLite
  and lets Room validate the migrated schema at open.
- Secret audit: clean (no keys/tokens/passwords in new code).
- **Device validation: [!] BLOCKED** — no adb/emulator/KVM in this sandbox.
  Install/launch/screenshot/logcat criteria could not run; must be validated
  on real hardware/CI before release phases.

### Decisions
- `CorrectionScope` gained `DOMAIN` (domain-level rules/corrections are in
  the product spec: `DOMAIN_TO_CATEGORY` exists as a rule type).
- Classifications/priorities denormalize `accountId` to avoid hot-path joins.
- `sync_state.cursor` stays opaque until Phase 4 defines history-token usage.
- `extracted_items.payload` is a small JSON sidecar; queryable fields are
  typed columns (§25: JSON is never the only mechanism).

### Known issues
1. Gradle test worker crashes in this sandbox (pre-existing, Phase 0).
2. No device/emulator — device criteria marked [!] blocked-by-environment.
3. 4 benign lint warnings (unchanged from Phase 0).
4. `bundleDebugClassesToRuntimeJar` once went stale mid-session (up-to-date
   check missed it); re-running the task explicitly fixed it. Worth knowing
   if test runs ever show `NoSuchMethodError` on fresh code.

## Next: Phase 3 — Google OAuth & Gmail Connection
**DEFERRED BY USER** (2026-10-08): skipped until the very end — user has no
computer access to provide OAuth credentials/API keys. Do NOT start
unprompted.

## Phase 4 — Gmail Synchronization Engine: COMPLETE (2026-10-09)

### What was built
New `data/sync/` package — the app's only network-touching subsystem:
- **`GmailSyncApi`** — remote seam (interface + `RemoteMessage`/`MessagePage`/
  `ChangePage` models + `SyncApiException` taxonomy). Models app concepts,
  not Gmail REST shapes. Real implementation lands with Phase 3 credentials;
  until then **`DeferredGmailSyncApi`** fails closed
  (`NotConfigured` → `MoError.InvalidConfiguration`). No fake sync, ever.
- **`SyncCoordinator`** — the engine: per-account mutex (no concurrent same-
  account syncs), rapid-tap dedup (in-flight join), paged initial sync with
  bounded runs (`SyncConfig(pageSize=50, maxMessagesPerRun=500)`), history-
  based incremental sync, controlled re-baseline on invalid history cursor,
  cursor advanced only after commit (§22), idempotent upserts via stable
  `account:gmailId` ids, honest `SyncProgress` stages + counts (no fabricated
  percentages), cancellation → PAUSED + terminal `Cancelled` outcome.
- **`SyncCursor`** — opaque cursor codec: `v1:page:<token>` (initial resume),
  `v1:history:<id>` (incremental); corrupt values degrade to safe restart.
- **`SyncMappers`** — normalization boundary (§44): remote → records only
  here. Gmail-derived fields sync; MO metadata tables never touched.
- **`SyncRetryPolicy`** — exponential backoff + jitter, 5-attempt ceiling;
  only transient failures retried; rate-limit hints honored; auth/permission/
  malformed/history-invalid never retried.
- **`SyncScheduler`** — background seam (interface + `SyncWorkSpec`).
  WorkManager selected; wiring deferred to Phase 19 (cannot verify w/o device).
- Repository/DAO extensions (no schema change, still v2): `updateCursor`,
  `existingGmailIds`, `getByThread`, `threadIdsForGmailIds`, `deleteByGmailId`,
  `getThreadByGmailId`, `updateThreadAggregates` (recomputed from DB after
  each page so thread counts stay truthful).
- Wired in `AppContainer` (`syncCoordinator` + fail-closed api).
  `versionName` → `0.1.0-phase4`.
- **Tests** (16 classes, 92 tests — 47 pre-existing + 45 new... 12 files +
  4 new): cursor codec, retry policy (incl. backoff/jitter bounds, no-retry
  taxonomy), mappers (stable ids, account namespacing, aggregates),
  coordinator e2e over in-memory Room + `FakeGmailSyncApi` (synthetic data
  only): 3-page pagination, empty pages, empty mailbox, duplicate sync,
  account isolation (same Gmail ids, two accounts), DB-failure recovery
  (cursor not advanced, data intact, retry completes), transient retry,
  auth-no-retry, not-configured, cancellation→PAUSED→resume, concurrent
  double-tap dedup, incremental changes+deletes, history-invalidation
  re-baseline, bounded-run resume, progress honesty.

### Validation
- `:app:compileDebugKotlin` / `:app:compileDebugUnitTestKotlin` — SUCCESSFUL
- `:app:assembleDebug` — **BUILD SUCCESSFUL** (APK built)
- `:app:lintDebug` — **BUILD SUCCESSFUL** (warnings: same 4 benign as baseline)
- **92/92 unit tests pass** via direct `java` JUnitCore (Gradle test worker
  still crashes on startup in this sandbox — pre-existing environment issue).
  New: 45 tests across 4 files (SyncCoordinatorTest 15, incl. 2 engine bugs
  found & fixed during testing — see Decisions).
- Secret audit: clean (no keys/tokens/passwords in new code).
- **Device validation: [!] BLOCKED** — no adb/emulator/KVM in this sandbox.
  Install/launch/sync-against-real-Gmail/screenshot/logcat criteria could not
  run. Live-API verification is additionally blocked on Phase 3 credentials
  (user-deferred). Must be validated on hardware/CI with real credentials
  before release phases.

### Decisions
1. **Cancellation catch must live outside `withContext`.** `withContext`
   rethrows `CancellationException` on completion when the Job is cancelled,
   even if the block caught it — converting cancellation to a terminal
   outcome therefore has to happen outside the `withContext`, with
   `NonCancellable` cleanup inside. Found by the cancellation test.
2. **Fake API is token-aware.** Pages are served by requested page token
   (like real pagination), not popped from a queue — otherwise resume-after-
   failure tests silently fetch the wrong page. Found by the recovery test.
3. **No WorkManager dependency yet.** The artifact isn't in the offline repo
   and its behavior can't be verified without a device; the seam + spec
   (`SyncScheduler`/`SyncWorkSpec`) is in place for Phase 19.
4. **No schema migration.** Sync needed only DAO/repository additions; DB
   stays v2.
5. `sync_state.retryCount` is not incremented by the engine (attempt counting
   lives in `SyncRetryPolicy`); left for a future phase to wire or remove.

### Known issues
1. Gradle test worker crashes in this sandbox (pre-existing, Phase 0).
2. No device/emulator — device + live-API criteria marked [!].
3. Live Gmail verification impossible until Phase 3 credentials exist
   (user-deferred to the very end).
4. 4 benign lint warnings (unchanged from Phase 0).

### Deferred work (user-approved, unchanged)
- Phase 3 (Google OAuth & Gmail Connection) — the live `GmailSyncApi`
  implementation
- Phases 15/16 (Calendar/Tasks), 22 (Gmail write), 29 (prod OAuth/Play)

## Phase 5 — Email Data Model & Parsing: COMPLETE (2026-10-09)

### What was built
New pure-Kotlin `core/email/` package — the parsing/normalization boundary
for Gmail payloads (no Android, no Room, no network):
- **`EmailModels`** — wire mirror (`RawGmailMessage`/`RawGmailPart`/
  `RawGmailHeader`/`RawGmailBody`, shaped like the Gmail API JSON) →
  canonical **`EmailMessage`** (headers, addresses, bodyText/bodyHtml,
  snippet, attachments metadata-only, flags). `AttachmentMeta` carries
  filename/mimeType/sizeBytes/attachmentId — never content bytes.
- **`EmailParser`** — total parser (never throws): recursive MIME walk
  (multipart/alternative prefers text/plain; falls back to first text part;
  multipart/mixed collects attachments), RFC 2047 encoded-word decoding
  (B/Q, multi-charset), address-list parsing (display names, groups),
  RFC 2822 date parsing (named zones, numeric offsets, obsolete formats),
  Gmail labelIds → flags (UNREAD/STARRED), snippet fallback. Malformed
  input degrades to identity shell (id/threadId/labels preserved).
- **`HtmlSanitizer`** — dependency-free sanitizer: strips script/style/
  iframe/object/embed/applet/form (+ unclosed executable tags to end of
  input, matching browser parsing), comments, event handlers, style attrs,
  javascript:/data:/vbscript: URLs; `htmlToText` extracts readable text
  with paragraph breaks. Both total (never throw).
- **`AttachmentMetaJson`** — hand-rolled JSON codec for attachment lists
  (no serialization dependency in core).
- **`EmailMappers`** (`data/sync/`) — `EmailMessage.toMessageRecord()`:
  reuses Phase 4's `localMessageId`/`localThreadId` stable namespacing;
  Gmail UNREAD/STARRED labels → `unread`/`starred` flags.
- Schema v2→v3: `MessageRecord` gains `bodyHtml: String?` (nullable TEXT)
  and `attachments: List<AttachmentMeta>` (TEXT via `MoConverters` JSON,
  NOT NULL); `MIGRATION_2_3` (purely additive ALTER TABLEs);
  `AppDatabase` version 2→3; `AppContainer` registers migration;
  `versionName` → `0.1.0-phase5`.

### Validation
- Direct `kotlinc` compile (Gradle daemon has a dispatch failure in this
  sandbox — pre-existing environment issue, separate from the test-worker
  issue): all new/modified sources compile clean.
- **38/38 new unit tests pass** via direct `java` JUnitCore:
  `HtmlSanitizerTest` (9), `EmailParserTest` (14), `AttachmentMetaJsonTest`
  (8), `EmailMappersTest` (7). 2 real bugs found & fixed during testing
  (see Decisions).
- Migration SQL validated against real SQLite (Python sqlite3): v2 table +
  ALTERs → correct schema, existing rows preserved, `bodyHtml` NULL,
  `attachments` `''` → reads as empty list via converter.
- `MigrationTest` extended with v2→v3 test (requires Room runtime —
  **not run** in this sandbox; SQL validated as above).
- Secret audit: clean. Email treated as untrusted input throughout.

### Decisions
1. **Test fixtures must put headers on the payload part** (matching the
   real Gmail API shape), not in a separate list — the parser reads
   `payload.headers`. Fixed the fixtures, not the parser.
2. **Unclosed `<script` removes to end of input**, matching browser parsing
   (browsers treat the rest as script content). Found by hostile-input test.
3. **Lone newlines in `htmlToText` are source-formatting whitespace** —
   converted to spaces before collapsing; only `\n\n` (from block elements)
   survives as paragraph breaks. Found by whitespace test.
4. **`attachments` migration default is `''`** (not `'[]'`): `MoConverters`
   reads blank as empty list, and Room writes `"[]"` for new empty lists —
   consistent both directions.

### Known issues
1. Gradle daemon dispatch failure in this sandbox (new, Phase 5) —
   compilation verified via direct `kotlinc` instead.
2. Gradle test-worker JVM crash (pre-existing, Phase 0).
3. No device/emulator — device criteria [!] blocked-by-environment.
4. `MigrationTest` v2→v3 not executed (needs Room runtime); SQL validated
   directly.

### Deferred work (user-approved, unchanged)
- Phase 3 (Google OAuth & Gmail Connection), Phases 15/16 (Calendar/Tasks),
  22 (Gmail write), 29 (prod OAuth/Play)

## Phase 6 — Core Inbox & Email Viewer: COMPLETE (2026-10-09)

### What was built
Full mailbox UI (UI-only phase; clearly-labeled fixture data — no live
Gmail until Phase 3):
- **`ui/mail/` package** — `MailScreen` (top bar, 6-destination drawer:
  All Inbox/Primary/Promotional/Social/Spam/Starred, offline + sample-data
  banners, honest sync progress, lazy thread/message lists, honest empty
  states), `ThreadScreen` (conversation view, oldest-first, newest expanded
  by default, deep-link focus scroll), `ThreadRow` (flat rows, unread
  emphasis, star display read-only), `MessageCard` (progressive header
  disclosure, attachment metadata only, >500k-char plain-text fallback),
  `SafeHtmlText` (Compose rendering of sanitized HTML — no WebView),
  `AccountAvatar` (compact circular receiving-account indicator).
- **ViewModels** — `MailViewModel` (sealed `MailboxContent`:
  Loading/Threads/Messages/Empty/Error; pagination PAGE_SIZE=50; account
  scoping; text filter; refresh via Phase 4 `SyncCoordinator` with honest
  outcome mapping), `ThreadViewModel` (newest expanded by default,
  `ensureExpanded` for deep links). UI→ViewModel→Repository→DAO only.
- **Safety** — `SafeHtmlRenderer` (sanitized HTML → BodyBlocks; images
  become placeholders, never fetched; only http/https links open
  externally); `MailFormatting` (relative timestamps, byte sizes, avatar
  initials, sender/subject fallbacks); `ConnectivityObserver` (offline
  banner; browsing never needs network).
- **Fixtures** — `SampleMailboxSeeder` (2 accounts, 7 threads, 11 messages;
  all ids prefixed `fixture-`; seeds only when DB has zero accounts; HTML
  passed through `HtmlSanitizer`; covers long subject, hostile HTML link,
  attachments, Malayalam+emoji, missing fields, thread, starred);
  `SampleDataPolicy` (debug may seed; release never).
- **Navigation** — `MAIL` route → `MailScreen`; `THREAD` route
  `mail/thread/{threadId}?focusMessageId=` with deep-link args.
- Additive only: `MessageDao.getByIds`, `MailRepository.getMessagesByIds`;
  `AppContainer` wires connectivity/policy/seeder; `versionName` →
  `0.1.0-phase6`.

### Validation
- `:app:compileDebugKotlin` — **BUILD SUCCESSFUL** (real Gradle toolchain;
  sandbox daemon dispatch is flaky — `GRADLE_OPTS=-Djava.net.preferIPv4Stack=true`
  makes the client-side dispatch work; a wedged stale daemon was killed once).
- **44/44 new unit tests pass** via direct kotlinc + `java` JUnitCore
  (Gradle test worker crashes in sandbox — pre-existing):
  `MailFormattingTest` (9), `SafeHtmlRendererTest` (11),
  `SampleMailboxSeederTest` (9), `MailViewModelTest` (10),
  `ThreadViewModelTest` (5). Tests caught 4 real issues, all fixed:
  whitespace-only HTML nodes glued words ("a b" → "ab"); `</li>` never
  cleared `inListItem`; duplicate companion object in `MailViewModel`;
  7-flow `combine()` has no typed overload in coroutines 1.9 (max 5 —
  restructured into two typed combines).
- Real compile errors fixed: only `material-icons-core` is available in
  this environment (no -extended) — `AttachFile`/`ExpandMore`/`ExpandLess`/
  `Image`/`AllInbox`/`Inbox`/`LocalOffer`/`Group`/`Report`/`CloudOff` do not
  exist there; replaced with core-set icons (`Email`, `MailOutline`,
  `ShoppingCart`, `Person`, `Warning`, `Info`, `KeyboardArrowDown/Up`) or
  honest text-only indicators (attachment "Files" chip / "N files" count;
  text image placeholders). No invented icons.
- Secret audit: clean. No credentials, no network calls from UI.

### Decisions
1. **No WebView for email HTML** — sanitized HTML renders through
   `SafeHtmlRenderer` → Compose `Text`; remote images never fetched.
2. **Star is display-only** (read-only phase §6); starring is a Gmail write
   (Phase 22).
3. **Classification destinations show honest "not classified yet"** —
   Promotional/Social/Spam are empty until Phase 7, never faked.
4. **Fixture data is unmistakable** — `fixture-` id prefix, "Sample data"
   banner, `SampleDataPolicy` gates release builds.

### Known issues
1. Gradle daemon dispatch flaky in sandbox (pre-existing) — worked with
   `GRADLE_OPTS=-Djava.net.preferIPv4Stack=true`; may need a stale-daemon
   kill between runs.
2. Gradle test-worker JVM crash (pre-existing, Phase 0) — tests run via
   direct kotlinc + JUnitCore.
3. No device/emulator — device criteria [!] blocked-by-environment, never
   faked.
4. Lint: 0 errors, 5 warnings (4 pre-existing: SDK/target, mipmap folder,
   adaptive-icon monochrome; 1 fixed: KTX `toUri()`).

### Deferred work (user-approved, unchanged)
- Phase 3 (Google OAuth & Gmail Connection), Phases 15/16 (Calendar/Tasks),
  22 (Gmail write), 29 (prod OAuth/Play)

## Phase 7 — Deterministic Classification Engine: COMPLETE (2026-10-09)

### What was built
Local, deterministic, explainable email classification — no network, no AI,
no randomness:

- **`core/classify/` package (pure Kotlin)** —
  `ClassifierModels` (ClassifierCategory/Confidence/MatchedSignal/
  ClassificationInput/ClassificationResult; Confidence is documented rule
  strength, not statistics),
  `TextNormalizer` (NFKC, locale-independent ASCII lowercase so a
  Turkish-locale device classifies identically, whitespace collapse, bounded
  lengths),
  `SignalExtractor` (total + bounded: sender/domain, subject, body ≤20k
  chars, Gmail category labels, labels, unsubscribe, URL domains ≤20 parsed
  as strings never visited, attachment filename hints, recurring-sender
  flag threaded through for Phase 8),
  `ClassificationRules` (30 registered rules with stable ids —
  SECURITY_OTP/LOGIN_ALERT/PASSWORD_RESET/SUSPICIOUS/VERIFY_ACCOUNT,
  ACTION_DEADLINE/PAYMENT_DUE/DOC_REQUEST, ORDER_CONFIRMATION/SHIPPED/
  DELIVERED/INVOICE, CAREER_INTERVIEW/RECRUITER/JOB_POSTING/APPLICATION,
  EDUCATION_INSTITUTION/ASSIGNMENT/EXAM/COURSE, IMPORTANT_PERSONAL,
  NEWSLETTER_UNSUBSCRIBE/FORMAT, PROMOTION_DISCOUNT/SALE/GMAIL_CATEGORY,
  NOTIFICATION_SOCIAL/SERVICE, LOW_VALUE_NOREPLY_NOISE; every rule KDoc'd
  with id/purpose/signals/strength/precedence/explanation/limitations),
  `DeterministicClassifier` (VERSION=1; score = summed rule weights;
  conflicts resolved by score then explicit PRECEDENCE
  Security>Action>Orders>Career>Education>Important>Newsletters>
  Promotions>Notifications>LowValue; LOW_VALUE gated to win only when
  nothing else scored; <20 points stays honestly UNCLASSIFIED).
- **`domain/classify/` use cases** — `ClassifyMessageUseCase` (UI never
  holds logic; never overwrites USER_CORRECTION/USER_RULE rows; skips
  current-version DETERMINISTIC rows = idempotent; reclassifies older
  versions; enforces account isolation; total — failures degrade to
  unclassified with safe logging),
  `ClassifyMailboxUseCase.classifyNew` (incremental: only unclassified
  messages; bounded 200/call; IO dispatcher).
- **Persistence** — no migration: Phase 2's `classifications` table already
  stores category/confidence/source/version/explanation/overridden,
  account-scoped. Additive DAO: `MessageDao.getUnclassified` (LEFT JOIN),
  `MessageDao.observeByLabel` (exact label match via char(31) separators);
  `MailRepository` gains `getMessage`/`getUnclassifiedMessages`/
  `observeByLabel`.
- **UI wiring (§52)** — Promotional destination ← PROMOTIONS category;
  Social ← Gmail CATEGORY_SOCIAL label; Spam ← Gmail SPAM label
  (documented: Gmail's own signals; classifier still classifies for the
  record); `MessageCard` shows a category chip + expandable "Why this
  category?" from the persisted explanation; `ThreadViewModel` loads
  per-message classifications (message-level evidence preserved, §35);
  `CategoryVisuals` (label + accent + content description — never color
  alone; light/dark token pairs in `Color.kt`);
  `MailViewModel` runs bounded background classification on startup
  (best-effort, never breaks the inbox). `versionName` → `0.1.0-phase7`.

### Validation
- `:app:compileDebugKotlin` — **BUILD SUCCESSFUL** (real Gradle toolchain).
- **New unit tests pass** via direct kotlinc + `java` JUnitCore (Gradle test
  worker crashes in sandbox — pre-existing):
  `TextNormalizerTest` (10), `SignalExtractorTest` (11),
  `DeterministicClassifierTest` (30: all 10 categories incl. security OTP/
  login-alert/password-reset/suspicious/verify, career, education, orders,
  newsletter, promotion, notification, action-required, important, low-value,
  unclassified; determinism),
  `ClassificationConflictsTest` (9: security>promotion, security>
  notification, order>promotion, action>notification, career>promotion,
  education>notification, tie-break determinism, low-value gate,
  precedence completeness),
  `ClassificationAdversarialTest` (7: lone "security"/"sale" words don't
  fire, "Interview tips newsletter" → newsletters, unsubscribe alone ≠
  newsletter, Gmail label alone ≠ decision, keyword stuffing),
  `ClassificationRobustnessTest` (9: script-like/JS-URL/Unicode/control-char
  input inert, 2.4MB body bounded <2s, no catastrophic backtracking),
  `ClassificationPerformanceTest` (4: 100/1k/10k emails; sub-ms average),
  `ClassifyMessageUseCaseTest` (12: persist, override safety, idempotency,
  version re-run, force, account isolation, batch + bound),
  `MailViewModelTest` (+4: promotional shows classified promotions,
  honest empty states, spam label destination, background classification),
  `ThreadViewModelTest` (+1: per-message classifications loaded).
  Tests caught real issues, all fixed: bare `return` inside non-inline rule
  lambdas (compile errors); `NEWSLETTER_UNSUBSCRIBE` fired on bank "monthly
  statement" — markers narrowed to explicit newsletter self-identification.
- Secret audit: clean. No network calls from classifier; no email-content
  logging (verified by inspection).

### Decisions
1. **Message-level classification** (§35) — per-message evidence preserved;
   thread-level aggregation is a later presentation concern.
2. **LOW_VALUE is gated** (§41) — wins only when no other category scored;
   weak evidence otherwise stays honestly UNCLASSIFIED.
3. **Social/Spam destinations are label-driven** — Gmail's CATEGORY_SOCIAL /
   SPAM labels define them; the classifier still classifies every message
   for the record. Promotional is classifier-driven.
4. **Confidence is rule strength, not probability** — documented in the
   model; never presented as statistical certainty.
5. **Overrides are never destroyed** (§30–31) — USER_CORRECTION/USER_RULE
   rows are skipped, not overwritten; predicted vs effective category stay
   distinguishable via ClassificationSource.

### Known issues
1. Gradle daemon dispatch flaky in sandbox (pre-existing) — works with
   `GRADLE_OPTS=-Djava.net.preferIPv4Stack=true`.
2. Gradle test-worker JVM crash (pre-existing, Phase 0) — tests run via
   direct kotlinc + JUnitCore.
3. No device/emulator — device criteria [!] blocked-by-environment, never
   faked (APK install/launch/screenshots/logcat impossible here; APK ships
   only after Phase 30 per user decision).
4. `isRecurringSender` is a threaded-through flag (default false) — the
   real recurring-sender signal is Phase 8 (Company & Sender Intelligence).

### Deferred work (user-approved, unchanged)
- Phase 3 (Google OAuth & Gmail Connection), Phases 15/16 (Calendar/Tasks),
  22 (Gmail write), 29 (prod OAuth/Play). Also: Phase 8 company/sender
  intelligence, Phase 9 priority/action-required engine, Phase 12
  user-correction UI, Phase 26 optional AI fallback.

## Next: Phase 9 — Priority & Action-Required Engine
Do NOT start unprompted.

## Phase 8 — Company & Sender Intelligence: COMPLETE (2026-10-09)

### What was built
Deterministic, on-device company & sender intelligence — no network, no AI:

- **`core/company/` package (pure Kotlin)** —
  `CompanyModels` (DetectedCompany/companyId/canonicalName/normalizedDomain;
  SenderProfile with honest `isRecurring`),
  `CompanyDetector` (total + deterministic: sender domain → canonical
  company; mailing-subdomain stripping; registrable-domain via
  last-two-labels + small public-suffix list; free-mailbox domains are
  people, not companies → null; small well-known display-name map;
  `companyId = "co:<domain>"`),
  `SenderIntelligence` (locale-independent normalization;
  `RECURRING_SENDER_THRESHOLD = 3`, documented heuristic; saturating
  counter).
- **Real recurring-sender signal** — Phase 7's threaded flag is now fed
  from local sender frequency via `RecurringSenderProvider`
  (dependency-inverted: classifier package owns the interface);
  new rule `IMPORTANT_RECURRING_SENDER` (weight 45, below
  IMPORTANT_PERSONAL's 50; bulk/noreply veto); classifier VERSION 1→2 so
  v1 rows reclassify without a resync.
- **`domain/company/CompanyIntelligenceUseCase`** — `processMessage`
  (normalize → atomic sender insert-or-increment → detect → upsert company
  preserving pinned/userOverrideName → link `messages.companyId`);
  `processNew` (bounded 200, newest-first); `isRecurring`;
  `setCompanyPinned`; total — failures degrade to unattributed.
- **Schema v3→v4** — `messages.companyId TEXT?` + index, purely additive
  (`MIGRATION_3_4`); `AppDatabase` version 4.
- **DAOs/repositories** — `MessageDao.setCompanyId/getWithoutCompany/
  observeByCompany[/AndCategory/AndLabel]/companyCountsForCategory/
  companyCountsForLabel` (counts are global GROUP BY, never page-derived);
  `SenderDao.recordMessage` (transactional insert-or-increment);
  `CompanyDao.getByDomain`; repository + DI wiring.
- **UI** — `CompanyFilterRow` (Phase 8): company chips with honest global
  counts inside Promotional/Social/Spam (never a drawer destination);
  tap selects/deselects (direct company+destination query — list always
  matches chip counts); Pin/Unpin text affordance (no pin glyph in
  material-icons-core — honest text per editor rule); pinning only
  reorders the list; `MailViewModel` runs attribution BEFORE classification
  on startup; `EmptyKind.NO_COMPANY_RESULTS` for honest empty states.

### Validation
- `:app:compileDebugKotlin` — **BUILD SUCCESSFUL** (real Gradle toolchain).
- **New unit tests pass** via direct kotlinc + `java` JUnitCore:
  `CompanyDetectorTest` (13), `SenderIntelligenceTest` (5),
  `CompanyIntelligenceUseCaseTest` (11), `RecurringSenderWiringTest` (4),
  `CompanyFilterViewModelTest` (4); `MigrationTest` gains v3→v4
  (Room-executed; SQL also validated on real SQLite).
- All pre-existing tests re-run; no regressions (Phase 7's 140 intact —
  the new rule only fires when `isRecurringSender=true`, which existing
  fixtures never set).
- Secret audit: clean.

### Design notes (permanent)
1. **Company identity is the registrable domain** — subdomains merge;
   personal mailbox domains are excluded (people ≠ companies).
2. **Pinning ≠ starring** — pinning reorders the filter list only.
3. **Counts are global** — GROUP BY over the destination, never page counts.
4. **Attribution precedes classification** — the recurring signal is real
   on the first classify pass.

### Known issues
1. Gradle daemon dispatch flaky in sandbox (pre-existing) — works with
   `GRADLE_OPTS=-Djava.net.preferIPv4Stack=true`.
2. Gradle test-worker JVM crash (pre-existing, Phase 0) — tests run via
   direct kotlinc + JUnitCore.
3. No device/emulator — device criteria [!] blocked-by-environment, never
   faked (APK ships only after Phase 30 per user decision).

### Deferred work (user-approved, unchanged)
- Phase 3 (Google OAuth & Gmail Connection), Phases 15/16 (Calendar/Tasks),
  22 (Gmail write), 29 (prod OAuth/Play). Also: Phase 12 user-correction
  UI, Phase 26 optional AI fallback.

## Phase 9 — Priority & Action-Required Engine: COMPLETE (2026-10-09)

### What was built
Deterministic, on-device priority engine — no network, no AI:

- **`core/priority/` package (pure Kotlin)** —
  `PriorityModels` (`PriorityLevel` LOW/NORMAL/HIGH/CRITICAL mirroring
  requirements.md "independent priority"; `PriorityInput`; `PriorityResult`
  with signals, rule ids, version, human-readable explanation),
  `PriorityRules` (15 registered rules with stable ids and per-rule KDoc:
  2 CRITICAL, 5 HIGH, 1 NORMAL, 7 LOW),
  `DeterministicPriorityEngine` (VERSION=1; scoring = summed rule weights
  per level; NORMAL carries a base score of 40 so ordinary mail stays
  NORMAL; ties break by explicit precedence
  CRITICAL > HIGH > NORMAL > LOW; total — failures degrade to NORMAL).
- **Priority is independent from category** (requirements.md): the engine
  consumes the classification as one signal among several (sender
  recurrence, labels, unsubscribe markers, bulk-sender heuristic, unread)
  and never re-derives the category.
- **`domain/priority/`** — `PrioritizeMessageUseCase` (compute+persist;
  never overwrites `manualOverride` rows; idempotent for the current
  version; reprioritizes older versions; account-isolated; failures degrade
  safely) and `PrioritizeMailboxUseCase` (`prioritizeNew`, incremental,
  bounded, IO dispatcher). The `toPriority()` mapper lives in domain so
  core stays data-free.
- **Action-required elevation** — the classifier's ACTION_REQUIRED category
  drives priority (HIGH at any confidence, CRITICAL at HIGH confidence);
  the inbox gains an "Action required" filter chip (global view of
  ACTION_REQUIRED-classified mail, honest `NO_ACTION_REQUIRED` empty
  state); thread rows show priority badges (HIGH/CRITICAL only — NORMAL/LOW
  stay quiet); the email viewer gains a "Why this priority?" expander fed
  by the persisted reason.
- **Wiring** — `MessageDao.getUnprioritized` + `PriorityDao.getByMessages`
  (batch, never N+1); repository methods; `AppContainer` DI; background
  prioritization runs after classification on startup (bounded, best-effort).

### Validation (actually run)
- `:app:compileDebugKotlin` — BUILD SUCCESSFUL (real Gradle toolchain).
- New unit tests via direct `java` JUnitCore: `PriorityEngineTest` (19),
  `PriorityRobustnessTest` (9), `PrioritizeMessageUseCaseTest` (11).
- Secret audit: clean.

### Known issues
1. Gradle daemon dispatch flaky in sandbox (pre-existing) — works with
   `GRADLE_OPTS=-Djava.net.preferIPv4Stack=true`; a wedged stale daemon
   had to be killed once.
2. Gradle test-worker JVM crashes (pre-existing, Phase 0) — tests run via
   direct kotlinc + JUnitCore.
3. No device/emulator — device criteria [!] blocked-by-environment, never
   faked (APK ships only after Phase 30 per user decision).
4. **The phase-09 plan file is corrupt** (`execution
   plan/android/phase-09-categories-priority-action-required.md` contains
   Phase 8 content; the phase-08 file itself contains Phase 3 OAuth
   content). Scope was derived from requirements.md/spec.md/design.md/
   editor-rules.md + the Phase 8 handoff. Under NO circumstances was OAuth
   implemented. (Repo hygiene: the corrupt plan files should be fixed in a
   later docs pass.)

### Deferred work (user-approved, unchanged)
- Phase 3 (Google OAuth & Gmail Connection), Phases 15/16 (Calendar/Tasks),
  22 (Gmail write), 29 (prod OAuth/Play). Also: Phase 10 search, Phase 12
  user-correction UI, Phase 13 meeting/deadline extraction, Phase 14 action
  engine (ActionItemRecord storage already exists from Phase 2), Phase 26
  optional AI fallback.

## Next: Phase 13 — Meeting & Deadline Extraction
Do NOT start unprompted.

## Phase 11 — Dashboard & Information Architecture: COMPLETE (2026-10-09)

### What was built
Dashboard + app information architecture over real local data — no
placeholders, no fake counts:
- **Navigation chrome**: `MoNavBar` (bottom bar, 5 primary destinations)
  + `MoAppTopBar` (title, search affordance, account avatar, overflow menu
  for secondary destinations). HOME route now shows the dashboard.
- **Home dashboard** (`ui/home/`): hierarchy account → attention
  (action-required) → priority (HIGH/CRITICAL) → recent → categories →
  companies, all from repository flows; honest Loading/Empty/Error/
  NoAccount/offline states.
- **Categories** (`ui/categories/`): list with real GROUP BY counts +
  `category/{name}` detail route with validated arguments.
- **Companies** (`ui/companies/`): list with real message counts +
  `company/{companyId}` detail route (messages + per-category breakdown).
- **Actions** (`ui/actions/`): view-only action-required list; no external
  execution (Phase 14 seam).
- **Settings** (`ui/settings/`): Settings, Accounts, Privacy, Integrations,
  Appearance — all honest, no fake login; account switching labeled as
  Phase 18 scope.
- **Data layer (additive)**: `ClassificationDao.countByCategory` and
  `MessageDao.companyMessageCounts` (GROUP BY, account-scoped, NULL-safe);
  `IntelligenceRepository.categoryCounts` + `observeByPriority`;
  `MailRepository.companyMessageCounts`.

### Verification (actually run)
- `:app:compileDebugKotlin` + `:app:compileDebugUnitTestKotlin` — BUILD SUCCESSFUL.
- 367 unit tests via direct `java` JUnitCore: 354 pass; 13 failures are the
  known environment-only ones (6 Room DAO `initializationError`, 7 Compose
  theme `NoClassDefFoundError`) — unchanged from Phase 10 baseline.
- 15 new ViewModel tests (Home 7, Categories 3, Companies 3, Actions 2) — all pass.
- New DAO GROUP BY SQL validated on real SQLite (account isolation, NULL exclusion).
- Secret audit clean.
- Device criteria [!] blocked-by-environment (no emulator); APK ships only after Phase 30.

### Test lesson (permanent)
`stateIn(SharingStarted.WhileSubscribed)` emits its initial value first —
account-list StateFlows feeding `flatMapLatest` must use
`SharingStarted.Eagerly`, else the first collection sees a transient empty
list and degrades to NoAccount/Empty. ViewModel tests must
`advanceUntilIdle()` after creating the VM and skip transient Loading
emissions when awaiting content.

### What was built (Phase 10)
On-device full-text search over what's already indexed — subjects,
snippets, bodies, senders, classifications. No network, no AI, no OAuth.

- **`core/search/` (pure Kotlin)** — `SearchModels` (`SearchQuery`
  structured filters; `ParsedQuery`; `SearchOutcome`; `SearchResult`
  Message/Thread/Sender/Company; `SearchContent` Landing/Loading/Results/
  Empty/Error; `SearchIndexState`), `QueryParser` (total parser: quoted
  terms/phrases, AND semantics, 200-char/10-term bounds; every term
  double-quote-escaped so FTS syntax can never be injected), `SearchRanking`
  (deterministic bm25 positional weights matching the FTS column order),
  `SearchHighlight` (safe highlight spans/snippets, never raw HTML).
- **`data/local/`** — `SearchIndexStore` (raw-SQLite FTS5 virtual table
  `messages_fts`: `messageId`/`accountId` UNINDEXED so ids can't be
  surprise-matched; unicode61 `remove_diacritics 1`; body capped at
  20k chars), `SearchIndexMeta` + DAO (per-account version table),
  `MIGRATION_4_5` in `Migrations.kt` (schema v4→v5), `AppDatabase` → v5
  with FTS table created on fresh installs too.
- **`data/repository/SearchRepository`** — FTS MATCH + parameterized
  filters (category/priority/action-required/sender/company/unread/
  attachments/date preset), deterministic ranking, thread grouping,
  sender/company suggestions, strict account isolation in every query.
- **`domain/search/`** — `SearchIndexUseCase` (`ensureIndexed` catch-up,
  `rebuild` from normalized local data, bounded batches) +
  `SearchIndexMaintenance` interface (clean seam).
- **Write-path hooks** — `RoomMailRepository` indexes in the same
  transaction on save/delete; `RoomAccountRepository` drops FTS rows on
  account delete; `CompanyIntelligenceUseCase` re-indexes after company
  attribution. New DAO batch methods (`ClassificationDao.getByMessages`,
  `ThreadDao.getByIds`, `CompanyDao.getByAccountAndId`,
  `MessageDao.messageIdsForGmailIds`, `SenderDao`/`CompanyDao.suggestByText`).
- **`ui/search/`** — `SearchScreen` (Material 3: search bar, filter chips,
  result-type tabs Messages/Threads/People, people/company sections,
  index-state banner, offline banner, error state with Rebuild action),
  `SearchResultRows`, `SearchUiState`, `SearchViewModel` (300ms debounce,
  `SharingStarted.WhileSubscribed`; landing emits zero DB work; queries
  never logged), `SearchViewModelFactory`. `AppNavGraph` gains a real
  SEARCH route with `?query=` arg; `MailScreen` top-bar IME search
  navigates to it.
- **IntelligenceRepository** gains `getClassifications(messageIds)`
  (batch, never N+1); 4 existing test fakes updated.

### Validation (actually run)
- `:app:compileDebugKotlin` and `:app:compileDebugUnitTestKotlin` —
  BUILD SUCCESSFUL (real Gradle toolchain).
- New unit tests via direct `java` JUnitCore: `QueryParserTest`,
  `SearchRankingTest`, `SearchHighlightTest` (31) + `SearchViewModelTest`
  (8) = 39 new; full pure-JVM suite: **338/338 pass** (39 new + 299
  regression). 14 tests excluded from the manual run (6 Room/Robolectric
  DAO tests, 8 Compose theme-token tests — hand-rolled classpath limits,
  unrelated to Phase 10, untouched files).
- FTS5 DDL + behavior validated against real SQLite (python sqlite3):
  phrase search, AND terms, account isolation, case-insensitivity,
  Malayalam, café→cafe diacritic folding all work; UNINDEXED ids can't be
  matched; injection probes safely quoted; bm25 positional weights check
  out; meta table works.
- Secret audit: clean.

### Known issues
1. Gradle daemon dispatch flaky in sandbox (pre-existing) — works with
   `GRADLE_OPTS=-Djava.net.preferIPv4Stack=true`.
2. Gradle test-worker JVM crashes (pre-existing, Phase 0) — tests run via
   direct `java` JUnitCore.
3. No device/emulator — device criteria [!] blocked-by-environment, never
   faked (APK ships only after Phase 30 per user decision).
4. Test lesson: `advanceUntilIdle()` in kotlinx-coroutines-test ADVANCES
   virtual time (fires pending delays) — use `runCurrent()` when the clock
   must not move (debounce tests). Also: `Dispatchers.setMain` is required
   for ViewModel tests; the `android.util.Log` stub needs 3-arg overloads
   (MoLogger.e calls `Log.e(tag, msg, throwable)`).
5. **The phase-08 plan file is corrupt** (contains Phase 3 OAuth content);
   the phase-10 plan file was verified clean before use. Under NO
   circumstances was OAuth implemented.

### Deferred work (user-approved, unchanged)
- Phase 3 (Google OAuth & Gmail Connection), Phases 15/16 (Calendar/Tasks),
  22 (Gmail write), 29 (prod OAuth/Play). Also: Phase 13 meeting/deadline
  extraction, Phase 14 action engine
  (ActionItemRecord storage already exists from Phase 2), Phase 26
  optional AI fallback.

## Phase 12 — Rules & User Corrections: COMPLETE (2026-10-09)

### What was built
User authority over the deterministic engines — corrections and rules that
outrank, but never duplicate, the Phase 7/9 base:

- **Core engine** (`core/rules/`): `RuleModels.kt` (10 condition fields,
  EQUALS/CONTAINS operators — no regex by design, SET_CATEGORY/SET_PRIORITY
  actions), `RuleEngine.kt` (pure deterministic evaluator v1: enabled rules
  sorted by (order, id), first match per action type wins, empty-condition
  rules never match, conflict detection), `RuleJson.kt` (hand-rolled JSON
  codecs, no kotlinx.serialization in core).
- **Domain layer** (`domain/rules/`): `UserRule.kt` (with `describe()` for
  natural-language summaries), `RuleMappings.kt` (record↔domain; legacy
  Phase 2 rows derive equivalent structured rules), `ApplyRulesUseCase.kt`
  (the effective pipeline: explicit correction [message > sender > domain]
  → enabled user rule → deterministic base; writes ClassificationRecord
  with source USER_CORRECTION/USER_RULE, overridden=true, confidence 1.0f;
  stale user rows deleted so engines restore the base; idempotent),
  `RecordCorrectionUseCase.kt` (message/sender/domain/company scopes + undo
  via `CompanyRecord.userOverrideName`), `RuleManagementUseCase.kt` (CRUD,
  enable/disable, reorder, local-data preview with exact count + samples,
  conflict detection, bounded reprocessing).
- **Data layer** (schema v5→v6): `UserRuleRecord` gains name/conditionsJson/
  actionsJson/ruleOrder/ruleVersion/source; `MIGRATION_5_6` additive with
  `ruleOrder = id` backfill; `CorrectionField.COMPANY_NAME`; new DAO methods
  for scoped ID lookups; repository interface extensions.
- **UI** (`ui/rules/`): correction bottom sheet (category/priority pickers,
  scope selector, "Set by you" markers from overridden flags, undo),
  rules list (Active/Disabled, toggles, delete-with-confirm, conflict
  banner), rule editor (name, condition/action builders, precedence order,
  live preview, conflict warnings). ThreadScreen "Correct" affordance next
  to chips; Settings → Rules route; RULES + RULE_EDITOR nav destinations.

### Verification
- 68 unit tests pass via direct JUnitCore (25 core: RuleEngine 19 +
  RuleJson 6; 17 domain: ApplyRulesUseCase; 26 regression: classify +
  priority). One test expectation fixed during development (conflict
  precedence follows (order, id), not input order).
- `:app:compileDebugKotlin` BUILD SUCCESSFUL; `:app:compileDebugUnitTestKotlin`
  BUILD SUCCESSFUL (after updating 4 test fakes for new interface methods).
- MIGRATION_5_6 SQL validated against real SQLite (columns, defaults,
  ruleOrder backfill).
- Secret audit clean. No device — device checks blocked, never faked.
  No OAuth.

### Deferred work (user-approved, unchanged)
- Phase 3 (Google OAuth & Gmail Connection), Phases 15/16 (Calendar/Tasks),
  22 (Gmail write), 29 (prod OAuth/Play). Also: Phase 13 meeting/deadline
  extraction, Phase 14 action engine, Phase 26 optional AI fallback.

## Phase 13 — Meeting & Deadline Extraction: COMPLETE (2026-10-09)

### What was built
Deterministic, on-device meeting/deadline extraction — structured temporal
intelligence, not external action (no calendar/task writes; those are later
phases' seams).

- **Core** (`core/temporal/`, pure Kotlin): `TemporalModels.kt` (14 types,
  confidence/status/timezone-source enums, explainable result model),
  `TemporalPatterns.kt` (bounded total regexes), `DateTimeParser.kt`
  (explicit/relative dates, times incl. 12AM/12PM, fixed-offset timezone
  tokens, documented ambiguity policy), `DeterministicTemporalExtractor.kt`
  (VERSION=1; candidate scan → type resolution → validation → ranking →
  explanation; never invents end times, timezones, or precision).
- **Reference time** is the message's received timestamp (phase §12) —
  relative dates ("tomorrow", "next Monday") resolve deterministically.
- **Domain** (`domain/temporal/`): `ExtractTemporalUseCase`
  (extract+persist, idempotent, account-isolated, user-completed rows never
  replaced, failures degrade safely), `ExtractMailboxUseCase` (bounded
  incremental background extraction), `TemporalMappings.kt` (hand-rolled
  versioned payload JSON codec; core↔storage type mapping).
- **Storage**: reuses Phase 2's `extracted_items` table — no migration
  (enums stored by name; 11 new `ExtractedItemType` values appended).
  Queryable attributes stay typed columns; temporal detail (end, timezone,
  location, URL, confidence, explanation, version) lives in the payload
  JSON. New DAO/repository methods: `getUnextracted`, `getExtractedItems`,
  `deleteExtractedItems`.
- **UI** (`ui/mail/TemporalSection.kt`): "Deadlines & meetings" section in
  the expanded message view with type icons (material-icons-core only),
  formatted date/time in the item's own timezone, location, "Join meeting"
  external link, and expandable "why" (recorded at extraction time).
  Wired through ThreadViewModel → ThreadScreen → MessageCard; extraction
  runs in MailViewModel's background pipeline after prioritization.

### Verification
- New tests: `DateTimeParserTest` (22), `DeterministicTemporalExtractorTest`
  (20), `TemporalPayloadJsonTest` + `ExtractTemporalUseCaseTest` (14).
  Three real bugs found and fixed by the tests: year-capture regex
  (`(19|20)\d{2}` captured only the prefix), "October 2026" misread as
  "October 20" (day ate the year's digits — added `(?!\d)` guards), and
  force re-extract duplicating alongside user-completed rows (now skipped).
- Full suite via direct JUnitCore: **454/456 pass**. The 2 failures are
  `RepositoryTest` + `SyncCoordinatorTest` — Robolectric-runner tests that
  cannot initialize in this sandbox (pre-existing environment limitation;
  unrelated to Phase 13 — they fail in JUnit annotation parsing before any
  app code loads).
- `:app:compileDebugKotlin` BUILD SUCCESSFUL; `:app:compileDebugUnitTestKotlin`
  BUILD SUCCESSFUL (after updating 5 test fakes for new interface methods
  and fixing 2 python-edit mistakes caught by the compiler).
- Secret audit clean. No device — device checks blocked, never faked.
  No OAuth.

### Deferred work (user-approved, unchanged)
- Phase 3 (Google OAuth & Gmail Connection), Phases 15/16 (Calendar/Tasks),
  22 (Gmail write), 29 (prod OAuth/Play). Also: Phase 14 action engine,
  Phase 26 optional AI fallback.

## Phase 14 — Action Cards & Action Engine: COMPLETE (2026-10-09)

### What was built
Deterministic, on-device action engine turning structured intelligence into
actionable suggestions with strict user control — the engine proposes, the
user confirms, nothing external happens automatically.

- **Core** (`core/actions/`, pure Kotlin): `ActionModels.kt`
  (`ActionConfidence`, `ActionUrgency` — independent from email priority per
  phase §17, `ActionSource`, `ActionStatus` lifecycle, `ExternalEffect`
  side-effect classification, `ActionCandidate` with deterministic dedup id,
  `SafetyVerdict`), `ActionCandidateGenerator.kt` (VERSION=1; 10 documented
  rules: temporal interview/meeting/deadline/payment/registration/reminder/
  travel → typed candidates; ACTION_REQUIRED → REPLY_REQUIRED; CRITICAL
  priority → review card; past items and bare dates never generate),
  `ActionSafety.kt` (separate validation layer: identity check, missing-info
  degradation to review cards, confidence-capped urgency, no invented times).
- **Domain** (`domain/actions/`): `ActionExecutor.kt` (executor interface +
  receipt + registry — intentionally empty in Phase 14, so confirming an
  external proposal honestly reports "not connected", never a fake success),
  `ActionPayloadJson.kt` (hand-rolled versioned codec), 
  `GenerateActionsUseCase` (bounded incremental generation, deterministic
  message+thread dedup, stale-version replacement, overdue-expiry pass;
  best-effort, never breaks the inbox),
  `ReviewActionUseCase` (review/dismiss/complete/confirm; confirmation
  outcomes: RecordedInternal / ExternalNotConnected / Executed / Failed).
- **Storage**: `action_items` extended via additive v6→v7 migration
  (`MIGRATION_6_7`): threadId, title, description, urgency, source, status
  (backfilled from legacy completed/dismissed booleans), externalEffect,
  payloadJson, version, updatedAtEpochMs. New DAO queries for thread dedup,
  status transitions, and the expiry pass.
- **UI** (`ui/actions/`): reusable `ActionCard` (type + urgency chips,
  title, description, expandable "why", due date, tappable source email,
  Review/Act/Dismiss; "confirmation required" notice on external
  proposals), reworked Actions destination backed by engine cards (urgency
  order, honest empty/error states), confirmation dialog showing what will
  happen / source / data / affected service / reversibility (phase §31).
  Generation hooked into MailViewModel's background pipeline after
  temporal extraction.

### Verification
- New tests: `ActionCandidateGeneratorTest` (12), `ActionSafetyTest` (7),
  `GenerateActionsUseCaseTest` (8), `ReviewActionUseCaseTest` (9),
  `ActionPayloadJsonTest` (3), `ActionsViewModelTest` (6, rewritten for
  cards).
- Full suite via direct JUnitCore; `:app:compileDebugKotlin` and
  `:app:compileDebugUnitTestKotlin` BUILD SUCCESSFUL.
- Secret audit clean. No device — device checks blocked, never faked.
  No OAuth. No APK (user decision: only after Phase 30).

### Deferred work (user-approved, unchanged)
- Phase 3 (Google OAuth & Gmail Connection), Phases 15/16 (Calendar/Tasks),
  22 (Gmail write), 29 (prod OAuth/Play). Also: Phase 26 optional AI
  fallback.

## Phase 17 — Integration Manager: COMPLETE (2026-10-09)

### What was built
Coherent application-level system for managing the app's integrations —
the manager coordinates; it never absorbs API-specific logic, never
performs external side effects, and never bypasses Phase 14's confirmation
boundary. Built honestly around the deferred phases: the plan assumed
Phases 3/15/16 existed, but they are user-deferred, so Calendar/Tasks are
first-class UNAVAILABLE integrations (never fake-connected) and Gmail
reports AUTH_REQUIRED (OAuth deferred) — never CONNECTED.

- **Core** (`core/integrations/`, pure Kotlin): `IntegrationModels.kt`
  (`IntegrationId` value class; `IntegrationStatus` — AVAILABLE/CONNECTED/
  CONNECTING/DISCONNECTED/AUTH_REQUIRED/PERMISSION_REQUIRED/OFFLINE/
  UNAVAILABLE/ERROR; `IntegrationCapability` — READ_EMAIL/SYNC_EMAIL/
  READ_CALENDAR_METADATA/CREATE_EVENT/READ_TASK_LISTS/CREATE_TASK;
  `PermissionDescription` least-privilege declarations; `IntegrationSnapshot`
  with `usableCapabilities` gated on CONNECTED — a declaration is never
  availability; `IntegrationError` categories; `RetryDecision`;
  `IntegrationEvent`), `IntegrationAdapter.kt` (contract: snapshot/connect/
  disconnect/refresh, all total), `IntegrationPolicies.kt` (pure error
  normalization + retry semantics — OFFLINE→retry later, AUTH→reconnect,
  INVALID/NOT_BUILT→never retry, RATE_LIMITED→backoff).
- **Domain** (`domain/integrations/`): `IntegrationManager` (adapter
  registry; account-scoped snapshots with strict isolation; `isCapable`
  capability queries for the Action Engine — phase §28; `integrationForAction`
  routing — MEETING→Calendar, REMINDER→Tasks, DEADLINE deliberately
  unmapped rather than guessed — phase §30; safe connect/disconnect;
  lightweight `refreshAll` health check — phase §25; `handleAccountRemoved`
  cleanup boundary — only that account's metadata — phase §36; SharedFlow
  events — phase §33), `IntegrationStateRepository` (metadata persistence
  interface).
- **Data** (`data/integrations/`): `GmailIntegrationAdapter` (wraps Phase 4's
  fail-closed sync seam; no account→DISCONNECTED, local-only
  account→AUTH_REQUIRED naming Phase 3, never CONNECTED; readonly-only
  permissions), `CalendarIntegrationAdapter` / `TasksIntegrationAdapter`
  (deferred → UNAVAILABLE naming Phase 15/16; connect() fails honestly,
  never a fake OAuth flow).
- **Storage**: `integration_states` via additive v7→v8 migration
  (`MIGRATION_7_8`) — status metadata only, never credentials (phase §35).
- **Action Engine wiring** (phase §28): `ReviewActionUseCase` accepts the
  manager and enriches `ExternalNotConnected` with the responsible
  integration's honest reason (e.g. "Google Calendar: …arrives with Phase
  15"); the UI message uses it, falling back to the generic copy.
- **UI** (`ui/integrations/`): reworked Integrations destination on the
  manager — status list with pills, account header, offline banner
  (offline ≠ disconnected — phase §26); detail screen (provider, status +
  reason, account, capabilities with usable-vs-declared distinction,
  permissions with purposes, Connect/Disconnect with confirmation dialog —
  phase §19; no Connect button for UNAVAILABLE); `integration/{id}` route
  with unknown-id not-found state; old Phase 11 placeholder removed.

### Verification
- New tests: `IntegrationPoliciesTest` (8), `IntegrationManagerTest`
  (10), `IntegrationAdaptersTest` (10), `IntegrationsViewModelTest` (4).
- Full suite via direct JUnitCore; `:app:compileDebugKotlin` and
  `:app:compileDebugUnitTestKotlin` BUILD SUCCESSFUL.
- `MIGRATION_7_8` SQL validated on real SQLite.
- Secret audit clean. No device — device checks blocked, never faked.
  No OAuth. No APK (user decision: only after Phase 30).

### Deferred work (user-approved, unchanged)
- Phase 3 (Google OAuth & Gmail Connection), Phases 15/16 (Calendar/Tasks),
  22 (Gmail write), 29 (prod OAuth/Play). Also: Phase 26 optional AI
  fallback.
