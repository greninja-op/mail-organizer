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
- **Phase 4 COMPLETE** — Gmail Synchronization Engine: `data/sync/`
  (`GmailSyncApi` seam + `DeferredGmailSyncApi` fail-closed, `SyncCoordinator`
  with per-account mutex/rapid-tap dedup/bounded paged initial sync/history
  incremental sync/controlled re-baseline/cursor-after-commit/idempotent
  upserts/honest progress/cancellation→PAUSED, `SyncCursor` codec,
  `SyncMappers` normalization boundary, `SyncRetryPolicy`, `SyncScheduler`
  seam for Phase 19). DAO/repo additions only — schema still v2.
  92/92 tests pass (47 pre-existing + 45 new, manual JUnitCore run).
  BUILD SUCCESSFUL, lint clean (4 benign warnings). Device + live-API
  criteria [!] blocked (no adb; Phase 3 credentials user-deferred).
  Secret audit clean.
- Toolchain: AGP 8.13.2, Kotlin 2.3.21, Compose BOM 2026.06.01,
  compileSdk/targetSdk 35, minSdk 26.
- Build: `:app:assembleDebug` SUCCESSFUL. Lint: SUCCESSFUL (4 warnings).
- Tests: 16 test files; verified manually (Gradle test worker crashes
  in this sandbox — environment issue, not code).
- Project lives under `android/` in the repo (root keeps the plan README).
- Deferred: Phase 3 (OAuth), 15/16 (Calendar/Tasks), 22 (Gmail write),
  29 (prod OAuth/Play). Interfaces in place; no fake implementations.

## Next
**Phase 3 — Google OAuth & Gmail Connection — DEFERRED BY USER** (no
computer access for credentials/API keys; revisit at the very end).
Next executable phase: **Phase 5 — Email Data Model & Parsing**.
Start it without waiting for a prompt (standing user go-ahead 2026-10-09).

## Key files
- `docs/development-status.md` — per-phase progress log
- `docs/architecture.md` — architecture decisions
- `docs/skills/` — 10 persistent skill docs
- `execution plan/android/editor-rules.md` — master rules reference
