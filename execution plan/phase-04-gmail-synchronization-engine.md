# PHASE 4 — GMAIL SYNCHRONIZATION ENGINE

## EXECUTION CONTRACT

Confirm Phase 4 is the first incomplete phase and Phase 3 is verified. Read all source-of-truth documents and this prompt. Implement Gmail synchronization only. Do not implement classification, company intelligence, search, Calendar, Tasks, AI, automation or Gmail write operations.

## 1. DISCOVER INSTRUCTION FOLDER
Locate instructions and Mail Organizer root before tooling.

## 2. CONFIRM PHASE 3
Verify OAuth/account authorization is actually complete. If not, stop.

## 3. WORKSPACE ISOLATION
Never modify sibling projects, SDKs, builds or unrelated Google Cloud resources.

## 4. GIT BASELINE
Inspect status/diff and preserve existing work.

## 5. CLOUD SOURCE OF TRUTH
Gmail remains authoritative for Gmail data. Local DB is a rebuildable synchronized representation plus Mail Organizer state.

## 6. SYNC BOUNDARY
Define SyncEngine → Gmail API → normalized local persistence boundaries.

## 7. ACCOUNT SCOPE
Every sync operation must execute inside one explicit account context.

## 8. INITIAL SYNC
Implement controlled initial mailbox retrieval with pagination.

## 9. PAGINATION
Handle Gmail pagination tokens safely. Never assume one page contains the mailbox.

## 10. BATCHING
Use bounded batches appropriate to API limits and device resources.

## 11. STABLE IDENTITIES
Persist stable Gmail message/thread/label/account identifiers.

## 12. IDEMPOTENT UPSERTS
Repeated sync must not create duplicate records.

## 13. THREAD HANDLING
Preserve Gmail thread identity. Do not infer threads solely from subjects.

## 14. LABEL/METADATA FIDELITY
Persist only metadata required by product behavior and later parsing.

## 15. BODY/ATTACHMENT BOUNDARY
Do not indiscriminately download attachment binaries. Respect Phase 5 parsing boundaries.

## 16. SYNC STATE MACHINE
Represent idle, preparing, retrieving, persisting, complete, paused, retrying, auth-required, offline and failed states.

## 17. CURSOR STATE
Persist incremental sync/history cursor state per account.

## 18. INCREMENTAL SYNC
Use Gmail history mechanisms where supported by the architecture.

## 19. STALE CURSOR
Handle invalid/expired history state by safely falling back to an appropriate resynchronization path.

## 20. INTERRUPT/RESUME
Persist enough state to recover from process death or cancellation without corrupting data.

## 21. RETRY
Implement bounded retry/backoff for transient failures. Never retry indefinitely.

## 22. ERROR CLASSIFICATION
Differentiate network, authorization, quota/rate, invalid state and unexpected failures.

## 23. OFFLINE
Do not claim synchronization while offline. Preserve local data and truthful state.

## 24. RATE LIMITS
Respect Gmail API quotas and server responses. Avoid aggressive polling.

## 25. CONCURRENCY
Bound per-account and cross-account concurrency. Do not starve the UI.

## 26. MULTI-ACCOUNT
Sync accounts independently. A failure in Account A must not corrupt or disable Account B.

## 27. TRANSACTIONS
Persist each logical batch transactionally where appropriate.

## 28. CONSISTENCY
Ensure partial batches cannot leave impossible account/message relationships.

## 29. PROGRESS
Expose real sync progress to the Phase 1 recovery animation. Never fabricate percentages.

## 30. INDETERMINATE PROGRESS
Use indeterminate state when total work is not trustworthy.

## 31. UI ENTRY
Provide a state-driven sync status surface without putting Gmail API calls in composables.

## 32. LOGGING
Log safe stages/counts/errors only. Never log tokens, authorization headers or full email bodies.

## 33. PRIVACY
Minimize local copies and avoid unnecessary payload retention.

## 34. TEST FAKES
Build fake Gmail API responses for deterministic tests.

## 35. PAGINATION TESTS
Test empty, single-page, multi-page, duplicate-page and malformed-page responses.

## 36. RETRY TESTS
Test transient failure, quota, network loss, cancellation and bounded retry.

## 37. CURSOR TESTS
Test incremental history, stale cursor and recovery.

## 38. ACCOUNT ISOLATION TESTS
Prove data and sync state never cross account boundaries.

## 39. DATABASE TESTS
Verify transactions, idempotent upserts and recovery after interruption.

## 40. LIVE DEVELOPMENT TEST
Where an authorized test account exists, perform a controlled initial sync and repeat it to verify no duplicates.

## 41. INCREMENTAL TEST
Make a controlled non-destructive mailbox change and verify incremental sync where appropriate.

## 42. DEVICE VALIDATION
Build, install Mail Organizer only, launch, connect/sync, inspect progress, force-stop safely, relaunch and verify durable state.

## 43. ADB/LOGCAT
Use package-scoped ADB, screenshots/screenrecord where useful, logcat and dumpsys. Do not alter unrelated mappings.

## 44. PERFORMANCE
Test large synthetic mailboxes, UI responsiveness, memory and bounded concurrency.

## 45. SECURITY
Confirm OAuth tokens remain outside ordinary DB/log/UI state and Gmail remains read-only.

## 46. FINAL BUILD
Run Gradle compilation, shared tests, Android tests and configured static checks.

## 47. FINAL RETEST
Fix phase-caused failures, rebuild, reinstall and repeat runtime/device validation.

## 48. GIT REVIEW
Inspect status/diff and confirm only Mail Organizer files changed.

## 49. DOCUMENTATION
Update spec.md and development-status with real sync architecture, cursor/recovery strategy and limitations.

## 50. EDITOR RULES
Add only permanent synchronization/privacy rules discovered.

## 51. ACCEPTANCE CRITERIA
- initial sync works;
- pagination works;
- persistence is idempotent;
- incremental cursor exists;
- stale cursor recovery exists;
- retry/backoff bounded;
- multi-account isolation verified;
- truthful progress works;
- offline/error states work;
- tests and device validation pass;
- no classification/search/write/AI/future phase implemented.

## 52. FINAL REPORT
Report sync architecture, API behavior, cursor strategy, test results, device/API level, screenshots/logcat, security, workspace isolation, files, known issues and deferred work.

## 53. STOP
Do not execute Phase 5.
