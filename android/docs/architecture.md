# Mail Organizer — Architecture (Phase 0)

Authoritative product contract: `execution plan/android/{requirements,spec,design,editor-rules}.md`
in `greninja-op/mail-organizer` (priority: requirements → spec → design →
editor-rules → existing code → official docs → engineering judgment).

## Layer map

```
Presentation (ui/)            Compose screens, theme tokens, navigation
        ↓
Application / Use Cases       (Phase 1+; thin orchestration, ViewModels)
        ↓
Domain (core/)                MoResult/MoError, AccountId boundary, pure logic
        ↓
Data (data/)                  Room (local/), DataStore (prefs/), repositories (Phase 2+)
        ↓
External APIs / Platform      Deferred: Google OAuth (Phase 3), Gmail API (Phase 4),
                              Calendar/Tasks adapters (Phases 15/16)
```

Platform layers own: UI, OAuth browser presentation, secure credential storage,
OS scheduling. Shared/KMP logic is introduced when the second platform needs it;
until then the Android module carries the domain/data code with KMP-compatible
structure (pure Kotlin, no Android imports in `core/`).

## Module responsibilities

| Area | Owner (now) | Owner (later phases) |
|---|---|---|
| Authentication | — (deferred, Phase 3) | `AuthRepository`: OAuth, token lifecycle, auth state |
| Gmail client | — (deferred, Phase 4) | Gmail API adapter, request/response models |
| Sync | — (deferred, Phase 4) | pagination, incremental sync, retry, per-account state |
| Parser | — (deferred, Phase 5) | message normalization, headers, bodies, URLs |
| Classification | — (deferred, Phase 7) | deterministic categories, confidence, explainability |
| Company intel | — (deferred, Phase 8) | sender/domain normalization, grouping |
| Rules | — (deferred, Phase 12) | user rules, overrides |
| Actions | — (deferred, Phase 14) | meetings, deadlines, tasks, reminders |
| Integrations | — (deferred, Phases 15–17) | Calendar, Tasks, manager |
| Persistence | `data/local` (accounts seed) | full mailbox schema (Phase 2) |
| Search | — (deferred, Phase 10) | local index, filters |
| Presentation | `ui/` (theme + foundation) | mailbox UI (Phase 1+) |

## Account isolation

`AccountId` (inline value class) is the data boundary. Every account-owned row is
keyed by account id; preference keys go through `scopedKey()`. Account-scoped
DataStores (one file per account) arrive in Phase 2. No code path may mix
accounts — DAO tests assert per-id isolation.

## Local-first data flow

```
Gmail (source of truth)
  ↓  official Gmail API (Phase 4+)
Android app
  ↓
Room database (rebuildable; only what offline/search/intelligence needs)
  ↓
Local deterministic processing
  ↓
Compose UI
```

No custom backend for core functionality. Mail Organizer-only user data
(rules/corrections) needs a backup strategy before it must survive uninstall
(deferred, noted in development-status.md).

## Privacy model (Phase 0)

- Minimize stored data; never log email bodies, tokens, auth headers.
- `MoLogger` is the single choke point: debug output only on debuggable builds.
- Email is untrusted input: sanitize HTML, no JS execution, never let content
  authorize actions (enforced from Phase 5+).
- No privacy/security property is claimed unless implemented and verified.

## Error handling

`MoResult<T>` + `MoError` taxonomy (network, authentication, permission-denied,
API, rate-limited, parsing, database, invalid-configuration, integration,
unexpected). Integration failures degrade gracefully — they never crash the core
experience.

## Design system

Centralized tokens in `ui/theme/`: `MoLightColorScheme` / `MoDarkColorScheme`
fallbacks + dynamic color on Android 12+. `MoTypography` baseline. Motion system
and full component library land with the UI phases (Phase 1+); the rule is
established now: no ad-hoc colors/type outside `ui/theme/`.

## Architectural decisions (Phase 0)

1. **AGP 8.13.2 + Gradle 8.14.6 + Kotlin 2.3.21 + KSP 2.3.12** — latest stable
   releases verified against Maven metadata 2026-10-08. AGP 9.x exists but was
   avoided: its migration unknowns add risk to a foundation whose acceptance
   criterion is a *working* build. Revisit when AGP 9 matures.
2. **Manual DI (`AppContainer`)** — graph is small; every dependency must earn
   its place. Adopt Hilt/Koin only if Phase 1+ growth justifies it.
3. **applicationId `com.greninjaop.mailorganizer`** — recorded; may change before
   Play Store (Phase 29).
4. **Room `exportSchema = false`** until Phase 2 introduces the mailbox schema
   and migrations.
5. **Robolectric for DB integration tests** — proves Room/KSP on JVM without an
   emulator; device tests remain mandatory in later phases.
6. **MockK deferred** — cataloged in `libs.versions.toml`, added as a
   dependency only when a test first needs mocking.
7. **No KMP module yet** — single Android app module; `core/` is pure Kotlin
   (no Android imports) so extraction to a KMP shared module is mechanical when
   the second platform arrives.
8. **compileSdk/targetSdk 35, minSdk 26** — current stable target; minSdk 26
   covers adaptive icons and modern security APIs.

## Data layer (Phase 2)

Room 2.8.5, database version 2. Twelve entities (`accounts`, `threads`,
`messages`, `senders`, `companies`, `classifications`, `priorities`,
`action_items`, `sync_state`, `user_rules`, `user_corrections`,
`extracted_items`) — see `docs/data-model.md` for the full model, indexing
rationale, and migration notes.

Layering actually enforced:

```text
UI (Compose)
  ↓  Flow / suspend, never DAOs
Repository interfaces (data/repository)
  ↓
Room DAO implementations (data/local)
  ↓
AppDatabase (v2, schema exported, migrations tested)
```

- **Account isolation** is structural: every account-owned row carries
  `accountId` with `FOREIGN KEY … ON DELETE CASCADE` from `accounts`.
  `AccountIsolationTest` proves cross-table separation and cascade delete.
- **No main-thread DB**: repositories dispatch all suspend work to
  `AppDispatchers.io`; Room would otherwise throw.
- **Bounded queries**: every list query takes a `LIMIT`; pagination cursors
  arrive with the sync engine (Phase 4).
- **Transactions**: `MailRepository.saveThreadWithMessages` (thread + messages
  atomically — the Phase 4 sync pattern); single-current-row tables
  (classification/priority/correction) use `@Transaction` delete+insert.
- **Token boundary**: the database never holds OAuth tokens, refresh tokens,
  or passwords. `accounts.googleAccountId` is the non-secret identifier only;
  Phase 3 uses Android secure credential storage.
- **Intelligence tables store decisions, not algorithms**: category,
  priority, and action rows record *what was decided* (with source, version,
  explanation); the engines that decide land in Phases 7/9/13/14.

## Sync engine (Phase 4)

The Gmail synchronization engine lives in `data/sync/` and is the only
network-touching subsystem. Its pipeline:

```text
SyncCoordinator
  ↓ (per-account mutex; rapid-tap dedup)
GmailSyncApi          ← seam; DeferredGmailSyncApi fails closed until Phase 3
  ↓ pages / history deltas
SyncMappers           ← normalization boundary (RemoteMessage → records)
  ↓
MailRepository        ← transactional thread+message writes, aggregates
  ↓
Room (v2 schema, unchanged)
  ↓
SyncStateRepository   ← opaque cursor, advanced only after commit
```

Key decisions:

1. **Seam over SDK**: `GmailSyncApi` models app concepts (pages, change
   sets, cursors), not Gmail REST shapes. The Phase 3 implementation will map
   `users.messages.list/get` and `users.history.list` onto it. Phase 3 is
   user-deferred (no credentials), so the engine was built and verified
   against `FakeGmailSyncApi` — real logic, no live-API verification yet.
2. **Cursor codec** (`SyncCursor`): `v1:page:<token>` for initial-sync resume,
   `v1:history:<id>` for incremental position. Corrupt values degrade to a
   safe initial restart (idempotent, so always safe).
3. **Bounded runs**: `SyncConfig(pageSize=50, maxMessagesPerRun=500)` — a run
   stops at a page boundary with the cursor saved; the next run resumes.
4. **History invalidation** → controlled re-baseline (full re-sync), never a
   crash or a false "in sync" state.
5. **Retry policy** (`SyncRetryPolicy`): exponential backoff + jitter, ceiling
   of 5 attempts; only transient failures retried; rate-limit hints honored.
6. **Thread aggregates** are recomputed from the DB after each page persist
   (`MailRepository.updateThreadAggregates`), so counts stay truthful across
   paged and incremental syncs.
7. **Background seam** (`SyncScheduler` interface + `SyncWorkSpec`): WorkManager
   is the selected mechanism; wiring is deferred to Phase 19, which owns the
   background strategy (cannot be verified without a device).
8. **No new DB version**: sync needed only DAO/repository additions
   (`updateCursor`, `existingGmailIds`, `getByThread`,
   `threadIdsForGmailIds`, `deleteByGmailId`, thread aggregate refresh) —
   schema v2 unchanged, no migration required.
