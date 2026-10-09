# CONTEXT.md — Mail Organizer (canonical continuity file)

## How to use this file
1. **READ this file fully before starting ANY work** on this repo (phase, fix, doc, push — anything). If the user ever says "read CONTEXT.md", stop everything and re-read it before continuing.
2. **UPDATE it after every prompt/request is handled**: update `Current build state`, append dated entries to `Decision log`, keep `Standing rules` current.
3. Never delete history — append and update. Keep entries short and factual.
4. When updating: fetch the REMOTE version of this file first and merge — never push a stale local copy over a newer remote one. (A stale push once wiped Phase 5's entry; don't repeat that.)
5. Purpose: this file alone must let any agent reconstruct every conversation, decision, and rule — even with zero prior context (e.g. user switches accounts/chats).

## Standing rules (non-negotiable)
- **Branch:** `main`. Push DIRECTLY to main — no pull requests, no separate branches.
- **Push as you go:** push each completed phase immediately; do NOT wait for approval taps. Standing user authorization (2026-10-09).
- **One push call per phase** = at most one approval prompt per phase. (Batch a phase's files into a single `push_files` call.)
- **Phases run one by one, continuously**, without asking. Next phase starts when the previous is pushed.
- **Deferred to the very end** (user has no computer access for keys): Phase 3 (Google OAuth & Gmail connection), Phases 15/16 (Calendar/Tasks), Phase 22 (Gmail write), Phase 29 (production OAuth/Play Store). Build clean seams/interfaces for them; never fake them.
- **Reports:** after EVERY phase, post a completion report in the side chat "Mail Organizer — build progress" (chat_id cc10335a-62bf-4626-becc-abbd55b0613c) — what was added/changed, what was verified, honest caveats. During long phases, post periodic progress updates too (user asked after a 2.5h silent Phase 0).
- **Honesty:** never fake completion — only real, verified work counts. Never put secrets (API keys, tokens, credentials) in the repo. Secret-audit every phase.
- **Speed:** run at FULL SPEED, but only within the free weekly quota.
- **Quota guardrail (HARD):** before spawning each phase agent, run `subscription-status status`. At **98%+ free-weekly usage: STOP EVERYTHING** — no new phases, no pushes. Post "weekly quota exhausted" in the build-progress side chat. **Never touch the 1B additional-token pool without the user's explicit approval.** Quota errors mid-phase = stop signal, never auto-retry. (Free quota was 42% used on 2026-10-09 ~13:35 IST; resets Oct 15 9:36 PM IST; 1B pool untouched.)
- **Approval prompts** come from the app's runtime safety system, not the agent — the user only ever sees an "allow" button (no always-allow option). The agent cannot disable them; never re-ask from its side.

## User
- **Arjun Sabu** (GitHub: `greninja-op`, India). Primary contact for this build.
- Writes casually in English (sometimes Spanish); keep reports in **plain language, no jargon**.
- This side chat is the dedicated build-progress thread — all phase reports and push confirmations go here.

## Project
- **Mail Organizer** — Android-first Gmail client. Kotlin + Jetpack Compose + Material 3. Gmail is the source of truth; the app adds local organization, intelligence, search, prioritization.
- **Repo:** `greninja-op/mail-organizer`. Layout: Android project under `android/`; repo root keeps the execution-plan README (`README.md`, 808 bytes — do NOT overwrite) and this file; plan docs under `execution plan/`.
- **App ID:** `com.greninjaop.mailorganizer`. **Toolchain:** AGP 8.13.2, Kotlin 2.3.21, Compose BOM 2026.06.01, compileSdk/targetSdk 35 (35 kept deliberately over 36 — reason in version catalog), minSdk 26.
- **Architecture notes:** account isolation is a first-class DB boundary (`AccountId`, FK-enforced); privacy-aware logging (never email bodies/tokens); no tokens/passwords in DB by schema design; email treated as untrusted input (HTML sanitized).

## Current build state (2026-10-09)
- **Phase 0** — Project Audit & Development Foundation: COMPLETE, pushed.
- **Phase 1** — Android Application Foundation (design tokens, Material 3 light/dark, 10-route nav, foundation screen, zero-permission manifest): COMPLETE, pushed. 24/24 tests.
- **Phase 2** — Local Data Architecture (Room v2, 12 tables, v1→v2 migration, 5 repositories): COMPLETE, pushed. 47/47 tests.
- **Phase 3** — Google OAuth & Gmail Connection: **DEFERRED** (see Standing rules).
- **Phase 4** — Gmail Synchronization Engine (sync coordinator, cursors, retry policy, scheduler seam): COMPLETE, pushed. 92/92 tests.
- **Phase 5** — Email Data Model & Parsing (`core/email/`: canonical model, total MIME parser, HTML sanitizer, v2→v3 migration): COMPLETE, pushed (2 commits). 38/38 tests (caught 2 real bugs, fixed).
- **Phase 6** — Core Inbox & Email Viewer: NEXT (pipeline status unconfirmed at redesign time — next agent must verify and update this line).
- Push history: phases 0/1/2/4 in commits cc38873, 855ff48, 6f00813, 8a9267f; Phase 5 in 2 commits.

## Decision log
- **2026-10-08:** User greenlit Android execution. Build UI + foundation now; skip all Google Auth/API-key/OAuth work to the very end. Report after every phase. Push directly to `main`.
- **2026-10-09:** Dedicated side chat "Mail Organizer — build progress" created for reports/push confirmations.
- **2026-10-09:** User required periodic progress updates during long phases (after Phase 0 ran 2.5h silently).
- **2026-10-09:** CONTEXT.md rule created at user's request: read before work, update after every prompt; README links to it; continuity across accounts/chats.
- **2026-10-09:** Standing push authorization: push as you go, no approval waits; phases one by one, continuously.
- **2026-10-09:** 4 approval prompts in a row were a one-time 105-file catch-up split (arg-size limit) — not the norm; one prompt per phase going forward.
- **2026-10-09:** Quota guardrail set: hard stop at 98% free-weekly usage, notify in side chat, never touch 1B pool without explicit approval. (Supersedes earlier "full speed even if it burns the 1B pool" wording.)
- **2026-10-09:** User emphasized CONTEXT.md is THE continuity file — redesigned for agent-efficient understanding. Phase 5's agent had failed to update it (stale push); rule 4 above added to prevent repeats.

## Environment limitations (sandbox)
- No emulator/adb — device install/launch/screenshot checks blocked until real hardware or CI. Mark device criteria `[!]`.
- Gradle test worker crashes on startup — run tests manually (JUnitCore); environment issue, not code.
- JVM network blocked — dependencies pre-fetched locally; standard Maven remotes configured for normal machines.
- Full Gradle build/lint sometimes skipped (daemon issues); per-file compilation verified instead.

## Key files
- `android/docs/development-status.md` — per-phase progress log
- `android/docs/architecture.md` — architecture decisions
- `android/docs/data-model.md` — every table, index, token boundary
- `android/docs/skills/` — 10 persistent engineering skill docs
- `execution plan/android/` — phase specs 00–30; `execution plan/android/editor-rules.md` — master rules
