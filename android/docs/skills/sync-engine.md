# Mail Organizer — Sync Engine Skill

Persistent rules for the Gmail synchronization engine (`data/sync/`,
`SyncCoordinator`). Phase 4 owns this skill; later phases extend it.

## Non-negotiables

- Gmail is the source of truth; the local DB is a synchronized
  representation. Never invent local-only mailbox state that contradicts Gmail.
- Read-only: never archive/delete/trash/mark/read/modify labels/send/reply.
  `gmail.modify` must not be requested to simplify sync.
- One account per sync run. Account id flows through the whole pipeline;
  a per-account mutex prevents concurrent syncs of the same account.
- Cursor advances ONLY after the corresponding page is committed. A failed
  sync never advances sync state and never wipes existing data.
- Idempotent: stable local ids (`<accountId>:<gmailId>`), upsert semantics.
  Re-running a sync inserts nothing twice.
- Controlled retries only: transient network / 5xx / rate-limit (honor
  retry-after). NEVER blind-retry auth expiry, permission denial, malformed
  requests, or invalid history cursors. Exponential backoff + jitter, hard
  attempt ceiling, no infinite loops.
- Invalid history cursor → controlled re-baseline (full re-sync), never a
  crash, never a silent "still in sync" lie.
- Cancellation pauses (state → PAUSED), keeps committed data, converts to a
  terminal Cancelled outcome. Never swallow cancellation and keep syncing.
- Bounded runs: page size + max-messages-per-run; page → process → persist
  → release → next. Never load a whole mailbox into memory; never one giant
  transaction.
- Progress reports stages + counts only. Never fabricate percentages.
- No email bodies/subjects/tokens in logs. No AI, no third-party email
  processing, no attachment binaries in Phase 4.
- Normalization boundary: `RemoteMessage` → `MessageRecord`/`ThreadRecord`
  happens only in `SyncMappers`. The rest of the app never sees remote models.
- Gmail-derived fields (unread/starred/labels) sync freely; MO-derived
  metadata (classifications, priorities, rules, corrections) lives in its own
  tables and sync must never touch it.
- `GmailSyncApi` is the only network seam. The real implementation arrives
  with Phase 3 credentials; until then `DeferredGmailSyncApi` fails closed.
  Tests use `FakeGmailSyncApi` with synthetic data only.

## Cursor format (opaque string in `sync_state.cursor`)

- `v1:page:<token>` — initial sync resume point (empty = start).
- `v1:history:<id>` — incremental position (Gmail history id).
- Unknown/corrupt values decode to a safe initial-sync restart.

## Verification bar (every sync change)

- Pagination, duplicate (sync twice), account isolation, failure recovery
  (cursor not advanced, data intact, retry works), cancellation + resume,
  history invalidation → re-baseline, rapid double-tap dedup, bounded-run
  resume. All against the fake API + in-memory Room; never real mailboxes.
- Device criteria (install, logcat, screenshots) remain [!]
  blocked-by-environment until hardware/CI is available.
