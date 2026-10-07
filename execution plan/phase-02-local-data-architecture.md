# Phase 02 — Local Data Architecture

## Execution contract

Read `requirements.md`, `spec.md`, `design.md`, `editor-rules.md`, and this phase file completely before editing.

Confirm Phase 02 is the first incomplete phase. Inspect the actual repository and preserve verified work.

Implement **only the local-data architecture foundation**. Do not implement Gmail OAuth, Gmail synchronization, email parsing, classification, search, UI product features, Calendar, Tasks, AI, automation, or Gmail writes.

Mail Organizer is Kotlin Multiplatform. Shared data/domain architecture must remain platform-neutral.

---

## 1. Architecture objective

Establish the KMP-compatible local persistence layer that later phases can safely use for:

- connected Gmail accounts;
- normalized messages;
- threads;
- senders/recipients;
- companies;
- Gmail labels/source metadata;
- classification results;
- priority;
- Action Required;
- deadlines/meetings;
- user corrections/rules;
- sync cursors/state;
- search/index metadata;
- action relationships;
- integration state;
- analytics/automation state where later phases require it.

Do not build every later feature now. The objective is to create a durable, testable data foundation without speculative schema bloat.

Gmail remains the cloud source of truth. Local state is a rebuildable representation plus Mail Organizer-owned metadata.

---

## 2. Select the persistence technology

Inspect the existing project before choosing a database.

Prefer Room Multiplatform or another justified KMP-compatible SQLite-backed solution.

Document:

- why the chosen technology fits KMP;
- supported targets;
- migration strategy;
- testing strategy;
- threading/transaction model;
- platform-specific driver boundaries.

Do not introduce a second database framework merely because it is familiar.

If a suitable persistence solution already exists, preserve it unless it materially violates the architecture.

---

## 3. Establish data-layer boundaries

Use the intended layering:

`Presentation → Application/Use Cases → Domain → Data → External APIs`

Persistence must not leak directly into Compose UI.

Establish clear boundaries for:

- entities;
- domain models;
- DAOs/query interfaces;
- repositories;
- transactions;
- migrations;
- database initialization;
- test database setup.

Repositories should expose domain-oriented operations rather than forcing UI code to understand SQL/database entities.

Do not put Gmail API calls inside DAOs.

---

## 4. Account isolation

Every account-owned entity must carry an explicit account boundary.

At minimum, design the account relationship for:

- messages;
- threads;
- labels;
- senders where account-specific state exists;
- companies where account-scoped relationships exist;
- classification results;
- rules/corrections where appropriate;
- sync state;
- action relationships.

The database must make accidental cross-account queries difficult.

Prefer account-scoped repository APIs and composite keys/indexes where appropriate.

Tests must prove:

- account A cannot retrieve account B's messages through account-scoped queries;
- counts/filter queries do not silently merge accounts unless the caller explicitly requests a unified view;
- account deletion/cleanup cannot leave orphaned sensitive data unexpectedly.

---

## 5. Core entity model

Create the minimum durable models needed by later phases.

### Account

Represent:

- stable local account identifier;
- Gmail account identity/email where appropriate;
- display identity;
- connection state;
- timestamps;
- sync status metadata that belongs locally.

Never store OAuth access/refresh tokens as ordinary database fields.

### Message

Support fields needed for:

- Gmail message identity;
- account identity;
- thread identity;
- timestamps;
- sender/recipient references;
- subject/normalized content references;
- read/unread;
- Starred;
- Gmail labels/source state;
- normalized body representation;
- attachment metadata;
- classification/priority relationships.

Do not duplicate every raw Gmail response if normalized data is sufficient.

### Thread

Support:

- account scope;
- Gmail thread identity;
- ordered message relationship;
- thread timestamps/state.

### Sender/recipient

Separate sender identity from:

- receiving account;
- company identity;
- message identity.

### Company

Support later canonical company grouping without making company detection part of this phase.

### Classification/intelligence state

Create extensible structures for later:

- category;
- confidence;
- reason/provenance;
- model/rule version;
- timestamp.

Do not run classification here.

### User intent

Support later independent state for:

- email Starred;
- company pinning;
- corrections;
- rules.

Do not collapse company pinning and message starring.

### Sync state

Create a durable account-scoped state model for future Phase 04 use, including the ability to represent:

- initial sync;
- incremental sync;
- cursor/history identifier;
- last successful sync;
- retry/error state;
- cancellation/interruption;
- progress metadata where meaningful.

Do not implement the sync engine.

---

## 6. Minimize local data

Follow data minimization.

Do not store:

- passwords;
- OAuth tokens in ordinary tables;
- unnecessary raw API payloads;
- unnecessary attachment binaries;
- duplicated data that can be rebuilt from Gmail without a product need.

Attachment metadata can be stored; downloading attachment contents belongs to later behavior and must remain user-initiated.

Document what local data is authoritative versus rebuildable.

---

## 7. Indexes and query design

Create indexes based on actual expected queries, including where appropriate:

- account + Gmail message ID;
- account + thread ID;
- account + timestamp;
- account + unread;
- account + starred;
- account + category;
- account + sender;
- account + company;
- sync cursor/state.

Do not create dozens of speculative indexes.

Review query plans/behavior for the core repository operations.

The schema must support a unified All Inbox query without destroying account isolation.

---

## 8. Transactions and consistency

Define transaction boundaries for operations that must remain atomic.

Examples:

- inserting/updating a message and its thread relationship;
- updating account sync state after a successful batch;
- deleting/rebuilding account-local derived state;
- applying a user correction together with its provenance metadata.

Do not perform multi-step database mutations from UI code.

Ensure retries do not produce duplicate logical messages.

---

## 9. Migrations

Establish a real migration strategy.

Requirements:

- schema versioning;
- deterministic migrations;
- migration tests where supported;
- destructive migrations avoided unless explicitly justified;
- no silent data loss;
- clear development reset path separate from production migration behavior.

Do not create fake migrations merely to satisfy tooling.

---

## 10. Repository API

Create testable repositories/interfaces for the foundation.

At minimum cover:

- account access;
- message/thread access;
- account-scoped queries;
- local mutation primitives;
- sync-state persistence.

Keep future repositories separable for:

- classification;
- company intelligence;
- rules/corrections;
- actions;
- search;
- integrations.

Do not implement their feature logic now.

---

## 11. Testing

Write deterministic database tests covering:

- insert/read/update;
- account isolation;
- thread/message relationships;
- Starred state independent of category;
- company-pin state independent of message-Starred state;
- duplicate/idempotent key behavior;
- transaction rollback;
- migration behavior;
- empty database;
- account cleanup.

Tests must not require a live Gmail account.

Do not place real email addresses, OAuth credentials, tokens, or sensitive mail in test fixtures.

Use synthetic fixtures.

---

## 12. Android integration boundary

Connect the KMP database to Android only through the intended platform driver/initialization boundary.

Verify:

- database initialization;
- database location;
- lifecycle behavior;
- background-safe access;
- clean debug startup;
- no UI-thread blocking.

Do not build the mailbox UI in this phase.

---

## 13. Privacy/security verification

Inspect logs and error handling.

Database errors must not dump:

- OAuth tokens;
- credentials;
- full email bodies;
- unnecessary headers;
- sensitive attachment data.

Account identifiers and diagnostic IDs should be logged only when justified and in minimized form.

---

## 14. Mandatory validation

Use the Gradle wrapper.

Run:

- shared compilation;
- database compilation;
- unit/database tests;
- Android debug build;
- relevant Android tests.

If an Android device/emulator is available:

1. install;
2. launch;
3. force-stop;
4. relaunch;
5. inspect logcat;
6. inspect package/process state where useful;
7. verify database initialization;
8. inspect local DB state with safe synthetic data if practical.

Do not expose real mail content in screenshots/logs.

Fix failures, rebuild, reinstall and retest.

---

## 15. Explicit non-goals

Do not implement:

- Gmail OAuth;
- Gmail API retrieval;
- synchronization;
- MIME parsing;
- classification;
- company detection;
- search indexing;
- mailbox UI;
- Calendar;
- Tasks;
- Gmail writes;
- AI;
- automation.

---

## 16. Documentation and completion

Update `editor-rules.md` only with genuinely permanent data/persistence rules discovered and verified.

Update `spec.md` status only after actual verification.

Document:

- selected database technology;
- schema/migration decision;
- account isolation strategy;
- important repository conventions;
- real verification results;
- blockers as `[!]` when necessary.

Final sequence:

`diff review → build → tests → runtime/database verification → fix → rebuild/retest → docs/status → commit → stop`

Do not continue to Phase 03.