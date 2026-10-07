# Phase 04 — Gmail Synchronization Engine

## Execution contract

Read all source-of-truth documents before editing and confirm Phase 04 is the first incomplete phase.

Implement **read-only Gmail synchronization only**.

Do not implement parsing intelligence, classification, search, Calendar, Tasks, Gmail writes, AI or automation.

---

## 1. Synchronization contract

Gmail remains the cloud source of truth.

The local database is an account-scoped, rebuildable representation used for:

- offline access;
- local organization;
- search/indexing later;
- derived intelligence later;
- sync state.

Synchronization must be:

- account-scoped;
- idempotent;
- resumable;
- cancellable;
- retryable;
- observable;
- safe under interruption;
- safe under repeated execution.

---

## 2. Initial synchronization

Implement the initial mailbox recovery pipeline.

It should:

1. establish account sync state;
2. determine required mailbox data;
3. retrieve Gmail data using official read-only APIs;
4. paginate safely;
5. persist batches transactionally;
6. update sync state only after successful persistence;
7. recover after interruption;
8. expose accurate progress/state to the application.

Do not claim completion before the local transaction has succeeded.

Do not fake progress.

---

## 3. Pagination and batching

Handle Gmail pagination correctly.

Requirements:

- continue until the API indicates completion;
- never assume one page is complete;
- persist page/batch boundaries safely;
- avoid unbounded memory use;
- retry transient page failures;
- avoid duplicate records.

Batch sizes should be configurable and compatible with the PerformanceProfile.

Do not load the entire mailbox into memory.

---

## 4. Message/thread/label identity

Use stable Gmail identifiers.

Correctly maintain:

- account ID;
- message ID;
- thread ID;
- label IDs;
- internal/local IDs where needed.

Use account + remote identifier as the effective identity boundary.

Do not assume Gmail message IDs are globally sufficient across all local data contexts without account scoping.

---

## 5. Idempotent persistence

Repeated sync of the same data must not create duplicate logical messages/threads.

Implement:

- upsert/update behavior;
- stable uniqueness constraints;
- safe transaction boundaries;
- reconciliation of changed message metadata.

Test:

- same page twice;
- interrupted batch retry;
- duplicate API response;
- message metadata update.

---

## 6. Incremental synchronization foundation

Implement the Gmail incremental synchronization mechanism appropriate to the authorized API.

Persist the required history/cursor state.

The engine must distinguish:

- no prior sync;
- initial sync;
- incremental sync;
- stale/invalid cursor requiring recovery;
- interrupted sync;
- failed sync.

When incremental history cannot safely be applied, recover through the appropriate rebuild/resync path rather than silently losing changes.

Do not move into background scheduling; that belongs to Phase 19.

---

## 7. Sync state machine

Represent states such as:

- idle;
- preparing;
- initial_sync;
- incremental_sync;
- persisting;
- completed;
- paused;
- retrying;
- cancelled;
- authentication_required;
- network_unavailable;
- failed;
- recovery_required.

State transitions must be deterministic.

Expose safe progress information.

Only expose determinate percentages when the engine has a trustworthy denominator.

Otherwise expose stage-based/indeterminate progress.

---

## 8. Retry and backoff

Handle transient failures with bounded retry/backoff.

Differentiate:

- authentication failure;
- permission failure;
- rate limiting;
- transient network failure;
- server error;
- malformed/unexpected response;
- local database failure;
- cancellation.

Do not retry permanent authorization errors indefinitely.

Do not retry after explicit cancellation.

Do not create retry storms across multiple accounts.

---

## 9. Concurrency and account isolation

Protect each account's sync state from concurrent conflicting runs.

Requirements:

- at most one conflicting sync operation per account;
- multiple accounts may sync independently where safe;
- one account failure must not corrupt another account's state;
- cancellation must be account-scoped;
- database transactions must preserve account boundaries.

Do not implement the final multi-account UX here.

---

## 10. Local data fidelity

Persist only what the current synchronization phase actually needs.

Store:

- message/thread identity;
- relevant Gmail metadata;
- labels;
- timestamps;
- basic sender/recipient references;
- state needed for later parsing/normalization.

Do not duplicate raw Gmail payloads unnecessarily.

Do not download attachment binaries automatically.

Do not perform classification.

---

## 11. Network/offline behavior

The engine must distinguish:

- offline before sync;
- network lost during sync;
- server unavailable;
- authorization expired;
- local persistence failure.

A network failure must not leave the account falsely marked as fully synchronized.

Successful persisted batches may remain available locally.

Resume from the correct durable state rather than starting over unnecessarily.

---

## 12. Sync progress and UI integration

Expose a testable sync-status stream/state model for the Android UI.

The Phase 01 recovery animation must consume this real state.

When synchronization is effectively instant, the UI should be able to skip the blocking animation.

When synchronization is slow:

- show truthful stage text;
- show determinate progress only when trustworthy;
- otherwise show indeterminate progress;
- allow safe entry into the app if the architecture supports it;
- never fabricate percentages.

Do not create a second independent sync-progress system in the UI.

---

## 13. Logging and privacy

Logs may contain:

- safe account-local diagnostic identifiers;
- sync stage;
- counts where not sensitive;
- error categories;
- timing metrics.

Never log:

- OAuth tokens;
- full email bodies;
- complete sensitive headers;
- attachment contents;
- authorization codes.

Be conservative with account email addresses in logs.

---

## 14. Testing

Build deterministic tests for:

- pagination;
- empty mailbox;
- multi-page mailbox;
- duplicate pages;
- idempotent upserts;
- interrupted sync;
- retry;
- cancellation;
- rate limiting;
- authorization failure;
- network loss;
- stale history cursor;
- incremental history;
- account isolation;
- transaction rollback;
- sync-state restoration.

Use fake Gmail API responses.

Do not make unit tests depend on live Gmail.

---

## 15. Integration/manual validation

Where an authorized development Gmail account is available:

- connect;
- perform initial sync;
- verify local counts;
- repeat sync;
- confirm no duplicates;
- make a controlled Gmail-side metadata change where appropriate;
- run incremental sync;
- confirm local state updates;
- interrupt sync;
- resume;
- test network/auth failure safely.

Never use a destructive Gmail write merely to create a test condition.

---

## 16. Mandatory verification

Run:

- shared tests;
- database tests;
- synchronization tests;
- Android build;
- relevant UI/runtime tests.

On device:

- install;
- launch;
- initiate sync;
- observe recovery animation;
- inspect logcat;
- force-stop during sync where safe;
- relaunch;
- verify durable sync state;
- inspect local DB;
- repeat sync.

Fix, rebuild, reinstall and retest.

---

## 17. Explicit non-goals

Do not implement:

- classification;
- company intelligence;
- search;
- Calendar;
- Tasks;
- background scheduling;
- Gmail writes;
- AI;
- automation.

Parsing/normalization beyond the minimum storage needed for synchronization belongs to Phase 05.

---

## 18. Completion

Update permanent editor rules only where genuinely justified.

Update `spec.md` only after actual verification.

Document sync-state decisions, cursor/recovery strategy and known limitations.

Final sequence:

`diff review → build → tests → live/fake sync verification → DB/log inspection → fix → rebuild/retest → docs/status → commit → stop`

Do not continue to Phase 05.