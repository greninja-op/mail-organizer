# CONTEXT.md — Mail Organizer

**Standing rule:** Every agent must READ this file before starting any work
on this repo and UPDATE it after every prompt. It is the continuity
mechanism if the user switches accounts or chats.

## Project
Mail Organizer — Android-first Gmail client (Kotlin + Jetpack Compose +
Material 3). Execution plan: `execution plan/android/` (phases 00–30).

- **Repo:** `greninja-op/mail-organizer`
- **Branch:** `main` (push directly; no PRs)
- **Application ID:** `com.greninjaop.mailorganizer`
- **User directive (2026-10-08):** Build Android UI + foundation now. SKIP
  Google Auth / API keys / OAuth until the very end (user has no computer
  access). Report progress after every phase.
- **Standing go-ahead (2026-10-09):** Push each completed phase directly to
  `main` as you go — do NOT wait for approval taps. Execute phases one by
  one, continuously, without asking.

## Current state (2026-10-09)
- **Phase 0 COMPLETE.** (Correction 2026-10-09: Phase 0's push to `main`
  never completed — it was staged awaiting approval. Phase 0 + Phase 1 are
  pushed together now.)
- **Phase 1 COMPLETE** — Android Application Foundation: spec design tokens,
  type scale (system fallback; Inter not bundled), 4dp spacing, shapes,
  10-route navigation, honest initial screen, state components, dark mode.
  24/24 tests pass (manual JUnitCore run). No device validation possible in
  this sandbox (no adb/emulator) — marked [!], must be done on hardware/CI.
- **Phase 2 COMPLETE** — Local Data Architecture: Room v2, 12 entities,
  11 DAOs, 5 repositories (Account/Mail/Intelligence/Rule/SyncState),
  additive v1→v2 migration (tested), account-isolation test, token boundary
  (no secrets in DB). 47/47 tests pass (24 existing + 23 new, manual
  JUnitCore run). Device criteria [!] blocked-by-environment (no adb).
  Secret audit clean.
- **Phase 5 COMPLETE** — Email Data Model & Parsing: pure-Kotlin
  `core/email/` (`EmailModels` wire→canonical, `EmailParser` total MIME
  parser with RFC 2047/2822 handling, `HtmlSanitizer` dependency-free,
  `AttachmentMetaJson` codec), `data/sync/EmailMappers` (reuses Phase 4
  stable ids), schema v2→v3 (`messages.bodyHtml` nullable TEXT,
  `messages.attachments` JSON TEXT; `MIGRATION_2_3` additive).
  38/38 new tests pass (manual JUnitCore run; 2 real bugs found & fixed).
  Migration SQL validated against real SQLite. `versionName` →
  `0.1.0-phase5`. Device criteria [!] blocked (no adb). Secret audit clean.
  Gradle daemon dispatch failure in this sandbox — used direct kotlinc.
- **Phase 6 COMPLETE** — Core Inbox & Email Viewer (2026-10-09): full
  mailbox UI (`ui/mail/` — MailScreen w/ 6-destination drawer, ThreadScreen,
  ThreadRow, MessageCard, SafeHtmlText, AccountAvatar), MailViewModel +
  ThreadViewModel (sealed UI states, pagination, account scoping, honest
  sync via Phase 4 SyncCoordinator), SafeHtmlRenderer (sanitized HTML →
  Compose, no WebView; remote images never fetched; http/https links only),
  clearly-labeled fixture data (`fixture-` ids, sample banner, debug-only
  seeding; release never seeds). 44/44 new tests pass (manual JUnitCore
  run; 4 real issues found & fixed incl. HTML whitespace gluing words).
  `:app:compileDebugKotlin` BUILD SUCCESSFUL. `versionName` →
  `0.1.0-phase6`. Device criteria [!] blocked (no adb). Secret audit
  clean. Icon note: only material-icons-core is available — missing glyphs
  replaced with core-set icons or honest text indicators, no invented icons.
- Toolchain: AGP 8.13.2, Kotlin 2.3.21, Compose BOM 2026.06.01,
  compileSdk/targetSdk 35, minSdk 26.
- Build: `:app:compileDebugKotlin` SUCCESSFUL (Phase 6).
  Lint Phase 6: SUCCESSFUL (0 errors, 5 warnings — 4 pre-existing, 1 fixed).
- Tests: 16 test files; verified manually (Gradle test worker crashes
  in this sandbox — environment issue, not code).
- Project lives under `android/` in the repo (root keeps the plan README).
- Deferred: Phase 3 (OAuth), 15/16 (Calendar/Tasks), 22 (Gmail write),
  29 (prod OAuth/Play). Interfaces in place; no fake implementations.

## Next
**Phase 3 — Google OAuth & Gmail Connection — DEFERRED BY USER** (no
computer access for credentials/API keys; revisit at the very end).
Next executable phase: **Phase 7 — Deterministic Classification Engine**.
Do NOT start unprompted.

## Key files
- `docs/development-status.md` — per-phase progress log
- `docs/architecture.md` — architecture decisions
- `docs/skills/` — 10 persistent skill docs
- `execution plan/android/editor-rules.md` — master rules reference
