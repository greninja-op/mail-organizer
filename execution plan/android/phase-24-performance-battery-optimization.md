# Phase 24 — Performance & Battery Optimization

## Mission

Optimize Mail Organizer for **real-world Android performance, memory usage, database efficiency, synchronization efficiency, battery consumption, startup time, UI responsiveness, and large-mailbox scalability** without changing product behavior or weakening privacy/security guarantees.

This phase is about measured optimization, not speculative micro-optimization.

The central principle is:

> **Measure first. Optimize the actual bottlenecks. Re-measure after every meaningful change.**

Do not optimize based only on source-code assumptions.

---

# 1. Mandatory Instruction-Folder Discovery

Before changing anything:

1. Identify the **Mail Organizer project root**.
2. Locate the instruction/documentation folder.
3. Read:
    - `requirements.md`
    - `spec.md`
    - `design.md`
    - `editor-rules.md`
    - this Phase 24 prompt
    - `docs/development-status.md` if available
4. Verify Phase 23 is genuinely complete.
5. Inspect the actual current architecture and implementation.
6. Establish a measurable performance baseline before optimization.

Do not assume that previous performance claims are correct.

---

# 2. Strict Sequential Execution

This session is **Phase 24 only**.

Do not implement:

- Phase 25 Analytics & Insights
- Phase 26 Optional AI
- Phase 27 Advanced Automation
- Phase 28 Full Testing & QA
- Phase 29 Production OAuth / Play Store Preparation
- Phase 30 Final Production Hardening

The only exception is a small adjacent fix required to preserve correctness while performing a performance optimization.

Execution:

```text
Read docs
↓
Verify Phase 23
↓
Inspect actual implementation
↓
Establish performance baseline
↓
Profile
↓
Identify bottlenecks
↓
Optimize highest-impact areas
↓
Build
↓
Install
↓
Run on device
↓
Measure again
↓
Fix regressions
↓
Stress test
↓
Battery test
↓
Memory test
↓
UI responsiveness test
↓
Rebuild/reinstall/retest
↓
Security/privacy review
↓
Update rules/spec/status
↓
STOP
```

---

# 3. Permanent Multi-Project Isolation

Multiple Android projects may exist in the same workspace.

First identify the Mail Organizer project root.

Never modify sibling projects.

Do not touch another project's:

- source
- Gradle files
- dependencies
- SDK/JDK configuration
- manifests
- resources
- tests
- assets
- signing
- OAuth
- Google Cloud configuration
- Git repository
- generated files

Never build or clean sibling projects.

---

# 4. Android Tooling Requirement

Use the actual project tooling:

- `./gradlew`
- ADB
- Android device/emulator
- logcat
- dumpsys
- package inspection
- screenshots
- screen recording
- database inspection
- profiling tools available in the environment

Use `adb reverse` only if required.

---

# 5. Establish a Baseline

Before optimization, record a baseline for the current application.

At minimum measure where practical:

### Startup

- cold startup
- warm startup
- hot startup

### UI

- frame responsiveness
- jank
- slow screens
- scroll performance
- search latency

### Database

- common query latency
- synchronization query latency
- indexing latency
- database size

### Synchronization

- initial sync duration
- incremental sync duration
- message throughput
- thread throughput
- parser throughput
- classification throughput

### Memory

- baseline memory
- peak memory during sync
- peak memory during search
- peak memory during large thread rendering

### Battery

- background sync behavior
- network activity
- wakeups
- CPU usage
- battery impact

Do not fabricate benchmark numbers.

---

# 6. Device Baseline

Use an actual Android device/emulator.

Record:

- Android version
- available RAM
- CPU architecture
- screen resolution
- Mail Organizer version/build
- test mailbox size

Where possible, use a consistent device for before/after comparison.

---

# 7. Test Dataset

Create controlled synthetic datasets where practical.

At minimum test:

```text
100 messages
1,000 messages
5,000 messages
10,000+ messages where practical
```

Include:

- short messages
- long messages
- HTML messages
- multipart messages
- threads
- attachments metadata
- multiple senders
- multiple companies
- categories
- rules
- action-required items
- deadlines
- conversation states

Do not put real private email content into benchmark fixtures.

---

# 8. Large Mailbox Principle

The application must not assume that a user's mailbox is small.

Avoid architecture that requires:

```text
load everything
→ process everything in memory
→ render everything
```

Prefer:

```text
paged data
→ bounded processing
→ incremental persistence
→ targeted queries
```

---

# 9. Startup Performance

Measure startup.

Inspect:

- Application initialization
- dependency injection
- database opening
- migrations
- preference loading
- authentication checks
- repository initialization
- navigation setup
- background scheduling
- indexing

Do not perform expensive Gmail synchronization during UI startup.

---

# 10. Startup Rule

The first frame should appear quickly.

Prefer:

```text
Application launch
↓
minimal initialization
↓
show UI
↓
perform deferred work
```

over:

```text
Application launch
↓
open database
↓
run migrations
↓
scan mailbox
↓
classify everything
↓
build search index
↓
show UI
```

---

# 11. Main-Thread Safety

No expensive work should run on the main/UI thread.

Audit:

- database queries
- parsing
- classification
- company detection
- rule evaluation
- search indexing
- synchronization
- JSON parsing
- MIME parsing
- HTML sanitization
- large list transformations
- attachment processing

Move appropriate operations to background execution.

---

# 12. Main-Thread Profiling

Use profiling/logging to identify actual main-thread stalls.

Do not simply wrap everything in asynchronous calls.

Correctness and structured concurrency must be preserved.

---

# 13. UI Rendering Performance

Inspect:

- Home
- Mail list
- Thread view
- Email detail
- Categories
- Companies
- Actions
- Search
- Privacy
- Accounts
- Integrations

Look for:

- unnecessary recompositions
- repeated database queries
- expensive formatting
- excessive layout nesting
- unnecessary animations
- large object creation
- repeated image loading
- unbounded lists

---

# 14. Lazy Lists

Large collections must use appropriate lazy/paginated rendering.

Do not render thousands of email rows simultaneously.

Ensure stable item identity.

---

# 15. Pagination

Use pagination for:

- mail lists
- threads
- search results
- companies
- categories where necessary
- action lists

Do not fetch unnecessary rows.

---

# 16. Database Query Optimization

Inspect common Room/SQLite queries.

Look for:

- full-table scans
- unnecessary joins
- repeated queries
- N+1 queries
- missing indexes
- oversized projections
- unnecessary sorting

Optimize based on measured evidence.

---

# 17. Database Indexes

Ensure indexes support real access patterns such as:

- accountId
- Gmail message ID
- Gmail thread ID
- sender
- company
- category
- priority
- Action Required
- timestamps
- sync state

Do not create indexes blindly.

Indexes have storage/write costs.

---

# 18. Composite Indexes

Where query patterns justify them, use appropriate composite indexes.

For example:

```text
accountId + timestamp
accountId + category
accountId + priority
accountId + threadId
```

Only add combinations supported by actual queries.

---

# 19. Database Projection

Do not load entire entities when only a few fields are required.

Example:

Home may need:

```text
messageId
sender
subject
timestamp
category
priority
actionRequired
```

rather than the complete email body.

---

# 20. Email Body Loading

Do not load large email bodies for list screens.

Use:

- summary/snippet
- metadata
- lazy detail loading

---

# 21. Thread Loading

Thread screens should avoid loading enormous conversations unnecessarily.

Use:

- pagination where practical
- collapsed messages
- lazy content loading

---

# 22. Search Performance

Measure local search latency.

Optimize:

- FTS/index structure
- query parsing
- ranking
- result limits
- snippet generation

Do not scan all email bodies manually for every query if an indexed mechanism exists.

---

# 23. Search Index Maintenance

Index updates should be incremental.

Avoid:

```text
every sync
→ rebuild entire search index
```

unless the mailbox is genuinely small and measurements prove it acceptable.

---

# 24. Search Index Recovery

If the index is corrupted or version-invalid:

- rebuild in background
- keep the app usable where possible
- expose appropriate status
- avoid blocking startup

---

# 25. Synchronization Performance

Inspect Phase 4/19 synchronization.

Optimize:

- pagination
- batch sizes
- database transactions
- duplicate checks
- network calls
- parsing
- indexing
- classification

Do not reduce correctness for speed.

---

# 26. Gmail API Calls

Avoid unnecessary API calls.

Use:

- local cache
- known IDs
- incremental history
- batching where officially supported and appropriate

Do not repeatedly request information already available locally.

---

# 27. Gmail Source-of-Truth Rule

Performance optimizations must not make local data falsely authoritative.

Gmail remains the source of truth.

If local data becomes stale:

- mark it appropriately
- reconcile through sync
- do not hide inconsistencies

---

# 28. Sync Batching

Process messages in bounded batches.

Avoid loading an entire mailbox into memory.

Example conceptual flow:

```text
fetch page
↓
normalize bounded batch
↓
persist batch
↓
derive intelligence
↓
index batch
↓
release memory
↓
next page
```

---

# 29. Transaction Optimization

Use database transactions for coherent batches where appropriate.

Avoid:

```text
one database transaction per message
```

when measurements show excessive overhead.

Also avoid:

```text
one transaction for the entire mailbox
```

if it causes excessive locks/memory/recovery risk.

Choose bounded transactions.

---

# 30. Classification Performance

Phase 7 classification is deterministic.

Optimize it without changing results.

Look for:

- repeated normalization
- repeated regex evaluation
- duplicate signal extraction
- repeated sender/company lookups

Cache only where safe and invalidatable.

---

# 31. Company Detection Performance

Avoid repeatedly resolving the same sender/domain.

Use appropriate local caching/indexes.

Do not introduce remote enrichment.

---

# 32. Rules Performance

User rules must remain deterministic.

Optimize rule evaluation using:

- indexed conditions
- precomputed signals
- rule ordering
- early exits

Do not change precedence semantics.

---

# 33. Temporal Intelligence Performance

Phase 13 extraction can be expensive.

Do not repeatedly parse the same email.

Use:

- versioned derived results
- incremental processing
- bounded background work

---

# 34. Conversation Intelligence Performance

Phase 21 should operate at thread level where possible.

Avoid reprocessing the entire mailbox when only one thread changed.

---

# 35. Action Engine Performance

Phase 14 should operate incrementally.

Do not regenerate all action candidates after every small change.

Use dependency-aware invalidation where practical.

---

# 36. Privacy Constraints

Do not introduce performance optimizations that weaken privacy.

Never optimize by:

- sending email content to a remote service
- adding third-party analytics
- uploading data for processing
- weakening credential storage
- disabling sanitization

---

# 37. Memory Management

Measure memory usage.

Look for:

- large collections
- retained email bodies
- duplicated normalized/raw data
- bitmap leaks
- cached screen state
- oversized coroutine scopes
- stale references

---

# 38. Large Email Test

Test emails containing:

- very large HTML
- long plain text
- many MIME parts
- many URLs
- many headers
- large attachment metadata

The app must remain responsive.

---

# 39. Parser Memory

Phase 5 parsing must not create unnecessary copies of huge email content.

Use streaming/bounded processing where appropriate.

Do not redesign the parser without evidence.

---

# 40. Image Memory

If avatars/images are used:

- constrain image size
- avoid loading full-resolution images
- cache responsibly
- release unused resources

Do not introduce external image fetching merely for optimization.

---

# 41. Cache Strategy

Audit caches.

Every cache should have:

- purpose
- scope
- size expectations
- invalidation strategy
- retention behavior

Avoid caching sensitive content indefinitely.

---

# 42. Memory Cache vs Disk Cache

Use the appropriate cache layer.

Do not put large datasets in memory simply to make screens faster.

---

# 43. Background Work

Review WorkManager usage.

Ensure workers:

- have appropriate constraints
- process bounded work
- support cancellation
- resume safely
- use account-scoped unique work
- avoid excessive frequency

---

# 44. Battery Optimization

Measure:

- sync frequency
- network requests
- CPU processing
- database writes
- wakeups
- background work duration

---

# 45. No Always-On Service

Do not introduce a foreground service merely to make synchronization easier.

WorkManager/background mechanisms should remain the default unless there is a genuinely justified requirement.

---

# 46. Sync Frequency

Do not sync aggressively.

Respect:

- user-configured behavior
- Android background restrictions
- battery state
- network constraints
- server/API limits

---

# 47. Network Constraints

Background synchronization should consider:

- network availability
- metered connection
- battery constraints where appropriate

Do not prevent manual user-requested sync unnecessarily.

---

# 48. Manual Refresh

Manual refresh should:

- provide visible state
- avoid duplicate concurrent sync
- support cancellation where practical
- not trigger multiple identical workers

---

# 49. Duplicate Work

Verify unique work identifiers.

Example conceptual requirement:

```text
MailOrganizerSync:<accountId>
```

must not collide across accounts.

---

# 50. Process Death

Test:

```text
sync starts
↓
process killed
↓
app restarts
```

Verify:

- no corruption
- no duplicate processing
- safe resume
- correct sync state

---

# 51. Reboot

Test background sync after device reboot where practical.

Verify scheduled work remains account-scoped.

---

# 52. Connectivity Changes

Test:

```text
online
↓
sync
↓
network disappears
↓
network returns
```

Verify safe recovery.

---

# 53. Offline Performance

Offline usage should remain responsive.

Users should be able to:

- browse cached mail
- search local data
- inspect categories
- inspect companies
- review actions
- edit rules
- inspect privacy/settings

without unnecessary network waits.

---

# 54. Offline UI

Do not block the whole application merely because Gmail is unreachable.

Clearly distinguish:

- offline
- authentication required
- permission revoked
- server error
- disconnected account

---

# 55. UI Responsiveness Target

Use practical responsiveness targets.

Aim for:

- no obvious frame drops during normal navigation
- no long blocking operations
- smooth scrolling