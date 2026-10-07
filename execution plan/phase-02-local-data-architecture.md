# PHASE 2 — LOCAL DATA ARCHITECTURE

## EXECUTION CONTRACT

Confirm Phase 2 is the first incomplete phase. Read requirements.md, spec.md, design.md, editor-rules.md, relevant architecture/status files and this prompt. Implement only local persistence architecture. Do not implement OAuth, synchronization, classification, search, Calendar, Tasks, AI or Gmail writes.

## 1. DISCOVER INSTRUCTION FOLDER
Locate the instruction folder and Mail Organizer root before Gradle, Git, ADB or edits.

## 2. READ PROJECT RULES
Reconcile requirements, spec, design and permanent editor rules before implementation.

## 3. CONFIRM PHASE ORDER
Verify Phase 1 is actually complete. If not, stop and report the blocker.

## 4. WORKSPACE ISOLATION
Inspect sibling projects and protect them from Gradle, SDK, Git and ADB changes.

## 5. GIT BASELINE
Record branch/status/diff and preserve pre-existing work.

## 6. KMP PERSISTENCE BOUNDARY
Implement persistence through shared KMP-compatible interfaces and platform-neutral domain/data code.

## 7. DATABASE TECHNOLOGY
Prefer Room Multiplatform or another justified KMP SQLite-backed solution already compatible with the repository. Do not introduce a second database.

## 8. DATABASE OWNERSHIP
Define one Mail Organizer local persistence boundary. Keep UI independent of database implementation.

## 9. ACCOUNT ENTITY
Create the account model required by future multi-account Gmail support: internal ID, provider, stable external identity where appropriate, display identity, connected state and timestamps.

## 10. ACCOUNT ISOLATION
Every account-owned record must be traceable to exactly the correct account context. Never mix Account A and Account B records.

## 11. MESSAGE ENTITY
Define stable message identity, account ownership, thread identity, subject/preview fields, timestamps, read/star state and minimal metadata needed by later phases.

## 12. THREAD ENTITY
Represent Gmail thread identity and account ownership. Do not infer thread identity solely from subject lines.

## 13. PARTICIPANT MODELS
Define normalized sender/recipient structures without coupling them to company intelligence.

## 14. COMPANY FOUNDATION
Reserve persistence structures for later company intelligence without implementing detection/classification.

## 15. CLASSIFICATION STATE
Reserve deterministic classification/category state and provenance without implementing the classifier.

## 16. USER INTENT
Model local user intent separately from Gmail state: Star, company pin, corrections and future rules.

## 17. SYNC STATE
Create account-scoped sync state/cursor/error/status structures for Phase 4 without implementing synchronization.

## 18. TEMPORAL/ACTION PLACEHOLDERS
Only establish extensible boundaries needed by later deadlines/actions. Do not implement those features.

## 19. DATA MINIMIZATION
Persist only what the product needs. Do not duplicate raw Gmail payloads or attachment binaries unnecessarily.

## 20. IDENTIFIERS
Use stable identifiers and deterministic uniqueness constraints. Avoid accidental duplicates on reprocessing.

## 21. INDEX DESIGN
Add indexes for account, thread, timestamp, unread/star/category and other verified future query paths without premature over-indexing.

## 22. RELATIONSHIPS
Define foreign-key/relationship behavior carefully. Avoid cascade deletion that could cross account boundaries.

## 23. ACCOUNT-SCOPED QUERIES
Repository APIs must make account context explicit for account-owned operations.

## 24. GLOBAL VIEWS
Unified views such as All Inbox and Starred must be built through safe account-aware queries, not by bypassing isolation.

## 25. REPOSITORY INTERFACES
Create clean repository APIs for accounts, messages, threads and sync state.

## 26. TRANSACTION BOUNDARIES
Use transactions for logically atomic multi-table updates.

## 27. IDEMPOTENCY
Repeated inserts/upserts must not create duplicate accounts, messages or threads.

## 28. MIGRATIONS
Establish versioned schema migrations. Never use destructive migration merely to make development build.

## 29. SEED/FIXTURE DATA
Use synthetic fixtures only for tests/development. Never present fake Gmail content as connected user mail.

## 30. SERIALIZATION
Keep transport/API models separate from database/domain models.

## 31. ERROR MODEL
Define deterministic persistence errors and safe user-facing categories.

## 32. CORRUPTION/RECOVERY
Establish behavior for migration failure, inaccessible DB and rebuildable local state.

## 33. PRIVACY
No tokens, authorization codes, passwords or sensitive credentials in the database.

## 34. LOGGING
Never log full email bodies or sensitive database rows. Diagnostics must be safe.

## 35. TEST FIXTURES
Create deterministic multi-account, duplicate, empty, large and malformed-data fixtures.

## 36. UNIT TESTS
Test entities, mappings, repositories, uniqueness, account isolation, transactions and migrations.

## 37. MULTI-ACCOUNT TESTS
Prove Account A queries cannot accidentally return Account B records.

## 38. REBUILDABILITY
Document that Gmail/cloud data is the source of truth while local intelligence/user-state may be rebuildable or separately recoverable.

## 39. ANDROID INTEGRATION
Wire the Android application to the shared persistence implementation without putting DB logic in Compose.

## 40. OFFLINE BEHAVIOR
Verify local reads work without network when data exists. Do not add background sync.

## 41. PERFORMANCE
Test realistic synthetic volumes and verify list/query access does not load an entire mailbox into memory.

## 42. DEVICE VALIDATION
Use Gradle wrapper, install Mail Organizer only, launch, inspect DB-backed states, logcat, screenshots and dumpsys as appropriate.

## 43. ADB SAFETY
Use package-scoped commands. Do not uninstall/clear unrelated applications. Use adb reverse only if required.

## 44. DATABASE INSPECTION
Inspect schema/data in a controlled development environment. Ensure no credentials/tokens are persisted.

## 45. SECURITY REVIEW
Verify account isolation, token exclusion, data minimization, migration safety and logging rules.

## 46. GIT REVIEW
Inspect git status/diff and confirm only Mail Organizer files changed.

## 47. FINAL BUILD
Run compilation, shared tests, Android tests and configured static checks. Fix phase-caused failures and rebuild.

## 48. TEST MATRIX
Validate empty DB, migration, duplicate upsert, multi-account isolation, transaction rollback, offline local read and large synthetic dataset.

## 49. DOCUMENTATION
Update spec.md and development-status documentation only after real verification. Record schema decisions and limitations.

## 50. EDITOR RULES
Add only genuinely permanent local-data rules to editor-rules.md; preserve all unrelated rules.

## 51. ACCEPTANCE CRITERIA
- KMP-compatible local DB established;
- account/message/thread foundations exist;
- account isolation enforced;
- repositories are explicit and testable;
- transactions/idempotency verified;
- migrations verified;
- no token/secret storage;
- deterministic tests pass;
- Android integration works;
- no Gmail OAuth/sync/classification/future feature implemented.

## 52. FINAL REPORT
Report architecture, schema/entities, repository boundary, migrations, tests, device validation, security, workspace isolation, files changed, known issues and deferred work.

## 53. STOP
Do not execute Phase 3.
