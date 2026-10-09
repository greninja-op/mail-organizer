# Data Model — Mail Organizer (Android)

**Last updated:** 2026-10-09 (Phase 2 — Local Data Architecture)
**Database:** Room 2.8.5 · **Version:** 2 · **Schema export:** `app/schemas/`

## Entities

| Table | Purpose | Account scoping |
|---|---|---|
| `accounts` | Gmail account identity + connection bookkeeping | PK itself |
| `threads` | Conversations | `accountId` FK CASCADE; unique `(accountId, gmailThreadId)` |
| `messages` | Email messages (minimal body) | `accountId` FK CASCADE; unique `(accountId, gmailMessageId)` |
| `senders` | Normalized senders | `accountId` FK CASCADE; unique `(accountId, normalizedEmail)` |
| `companies` | Company grouping + pinning | `accountId` FK CASCADE; unique `(accountId, normalizedDomain)` |
| `classifications` | Current category per message | FK message CASCADE; unique `messageId`; `accountId` denormalized |
| `priorities` | Current priority per message | FK message CASCADE; unique `messageId`; `accountId` denormalized |
| `action_items` | Action-required detections | FKs message + account CASCADE |
| `sync_state` | Per-account sync bookkeeping | `accountId` PK/FK CASCADE |
| `user_rules` | User-defined rules | `accountId` FK CASCADE |
| `user_corrections` | Authoritative user overrides | `accountId` FK CASCADE; unique `(accountId, scope, scopeKey, field)` |
| `extracted_items` | Meetings/deadlines/payments/… | FKs message + account CASCADE |

Gmail ids are only unique *within* an account — never assume global uniqueness
(phase §17/§19). `classifications.accountId` / `priorities.accountId` are
deliberately denormalized so category/priority mailbox queries don't join
`messages` (§31).

## Indexing decisions (§29–§30)

Indexes exist for the query patterns the product needs; no blanket
per-column indexes:

- Mailbox: `messages(accountId, timestampEpochMs)`, `threads(accountId, latestMessageEpochMs)`
- Thread view: `messages(threadId)`
- Sender/company lookup: `messages(fromAddress)`, `senders(accountId, normalizedEmail)`, `companies(accountId, normalizedDomain)`
- Filters: `messages(unread)`, `classifications(category)`, `priorities(priority)`
- Sync upsert path: unique `(accountId, gmailMessageId)`, `(accountId, gmailThreadId)`
- Correction lookup: unique `(accountId, scope, scopeKey, field)`

## Normalization vs performance (§31)

Normalized where identity matters (accounts → threads → messages → senders).
Denormalized where it avoids hot-path joins (`accountId` on classifications /
priorities, `domain` on senders). Lists that are never query keys
(recipients, labels, known domains) use the U+001F-separated converter;
anything filtered or sorted by is a real column — the JSON `payload` on
`extracted_items` is never the query key (§25).

## Migrations (§32)

- v1 (Phase 0): `accounts` seed only.
- v2 (Phase 2): purely additive — new columns on `accounts` (all defaulted),
  plus the eleven new tables and indexes. No destructive fallback is enabled;
  production migrations must preserve user data.
- `Migrations.MIGRATION_1_2` is covered by `MigrationTest` (genuine v1 DB
  built with raw SQLite → Room validates the migrated schema at open).

## Transaction strategy (§42)

- `MailRepository.saveThreadWithMessages` — thread + messages in one
  `withTransaction` (the pattern the Phase 4 sync engine reuses).
- `ClassificationDao.setClassification` / `PriorityDao.setPriority` /
  `UserCorrectionDao.upsertCorrection` — delete+insert in `@Transaction` so
  "single current row" is never half-updated.
- Future sync application follows the same rule: sync-state update +
  imported rows commit together.

## Concurrency (§41)

- No main-thread database access: repositories dispatch to `AppDispatchers.io`;
  Room would otherwise throw. (Tests use `allowMainThreadQueries` only.)
- All list queries are bounded (`LIMIT`); pagination cursors arrive with the
  sync engine (Phase 4).

## Token boundary (§39–§40)

OAuth tokens, refresh tokens, and passwords are NEVER stored in this
database — not in `accounts`, not anywhere. Phase 3 will use Android's
secure credential storage (EncryptedSharedPreferences / Keystore). The
`accounts.googleAccountId` column holds only the non-secret account
identifier. Attachments: metadata model only; no binaries in Phase 2.

## Data minimization (§38)

Stored: what offline use, search, derived intelligence, rules/corrections,
and sync state need. NOT stored: full MIME payloads, attachment binaries,
OAuth secrets, tracking data. `bodyText` is minimal plain text and nullable;
sanitized HTML arrives in a later phase.
