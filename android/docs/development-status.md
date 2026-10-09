# Development Status — Mail Organizer (Android)

**Last updated:** 2026-10-09 (Phase 6 complete)
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

## Next: Phase 7 — Deterministic Classification Engine
Do NOT start unprompted.
