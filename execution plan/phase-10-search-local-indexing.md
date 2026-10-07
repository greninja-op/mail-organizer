# PHASE 10 — SEARCH & LOCAL INDEXING

You are now executing:

**Phase 10 — Search & Local Indexing**

This phase may begin only after Phase 9 — Categories, Priority & Action Required has been completed and verified.

Do not execute Phase 11 or any later phase automatically.

The project continues to use strict sequential phase execution.

```text
Phase 0
  ↓
Phase 1
  ↓
Phase 2
  ↓
Phase 3
  ↓
Phase 4
  ↓
Phase 5
  ↓
Phase 6
  ↓
Phase 7
  ↓
Phase 8
  ↓
Phase 9
  ↓
Phase 10 ← YOU ARE HERE
  ↓
Phase 11
  ↓
...
```

Complete only Phase 10.

Verify it.

Update persistent project rules and documentation.

Then STOP.

---

# 1. DISCOVER THE PROJECT INSTRUCTION FOLDER

Before modifying anything:

1. Locate the Mail Organizer instruction folder.
2. Read:
    - `requirements.md`
    - `spec.md`
    - `design.md`
    - `editor-rules.md`
    - development-status documentation
    - architecture documentation
    - Phase 10 instructions
3. Inspect the actual repository.
4. Verify Phase 9 is genuinely complete.
5. Identify:
    - Mail Organizer project root
    - Git root
    - Gradle root
    - Android package ID
6. Confirm all operations target only Mail Organizer.

Do not assume the current directory is the project root.

---

# 2. STRICT MULTI-PROJECT ISOLATION

The workspace may contain multiple Android projects.

Only Mail Organizer may be modified.

Never modify sibling:

- source
- Gradle files
- dependencies
- manifests
- resources
- tests
- generated artifacts
- SDK/JDK configuration
- signing configuration
- Git repositories

ADB operations must target only the Mail Organizer package.

Do not install, uninstall, clear, force-stop, launch, or inspect unrelated applications.

---

# 3. PHASE OBJECTIVE

Build the local search system for Mail Organizer.

The user should be able to search locally synchronized email without requiring Gmail/network access.

Conceptually:

```text
Search Query
     ↓
Query Parser
     ↓
Local Search Index
     ↓
Message / Thread / Sender / Company data
     ↓
Filters
     ↓
Ranked Results
     ↓
Mail UI
```

Search must work primarily from locally available data.

---

# 4. CORE PRODUCT PRINCIPLE

The local database is the searchable representation of synchronized Gmail data.

The architecture should support:

```text
Gmail
 ↓
Synchronization
 ↓
Parsing
 ↓
Local Database
 ↓
Derived Intelligence
 ↓
Local Search
```

Do not make the search screen depend on a live Gmail API request.

---

# 5. OFFLINE-FIRST SEARCH

After synchronization, search must work without internet.

Test:

```text
Network ON
 ↓
Sync
 ↓
Network OFF
 ↓
Search
```

Results should remain available for locally synchronized messages.

---

# 6. DO NOT IMPLEMENT REMOTE SEARCH

Do not send queries or email content to:

- Gmail search endpoints
- Google search
- external search engines
- AI services
- third-party search APIs

The local search engine is the source for Phase 10.

---

# 7. SEARCH SCOPE

Search should be able to cover appropriate locally stored fields.

At minimum consider:

- sender name
- sender email
- subject
- normalized plain-text body
- thread information
- company name
- domain
- category
- priority
- Action Required
- Gmail labels/categories where available

Do not expose every internal field to users.

---

# 8. SEARCH RESULT TYPES

Results should preserve the distinction between:

```text
Message
Thread
Sender
Company
```

The default search experience should prioritize useful mail results.

Do not create an unrelated universal search engine.

---

# 9. SEARCH UI

Add a dedicated search experience consistent with `design.md`.

It should support:

- search field
- query editing
- clear action
- result list
- loading state
- empty state
- error state
- offline state
- filter controls where appropriate

Keep the design calm and uncluttered.

---

# 10. SEARCH ENTRY POINT

Integrate search into the existing navigation without redesigning the entire application.

Use the Phase 1 navigation architecture.

Do not create duplicate search destinations.

---

# 11. SEARCH FIELD

The search field should:

- accept normal text
- support long queries
- provide clear/close actions
- handle keyboard correctly
- preserve query when navigating into a result where practical

Use appropriate IME behavior.

---

# 12. SEARCH DEBOUNCING

Do not execute a database query on every individual keystroke if unnecessary.

Use a small debounce for live search where appropriate.

However, do not introduce noticeable delay for short/local queries.

---

# 13. QUERY PARSER

Create a structured query layer.

Do not let the UI directly construct SQL strings.

Conceptually:

```text
User Query
    ↓
Query Parser
    ↓
SearchQuery
    ↓
Repository
    ↓
Database
```

---

# 14. BASIC FREE-TEXT SEARCH

Support normal queries such as:

```text
interview
amazon
college
invoice
password reset
```

Search relevant normalized fields.

---

# 15. MULTI-WORD SEARCH

Handle queries such as:

```text
job interview
college assignment
order confirmation
password reset
```

Define whether matching means:

- all terms
- any terms
- phrase matching

Use a predictable documented strategy.

Prefer useful search behavior over arbitrary SQL semantics.

---

# 16. PHRASE SEARCH

If practical, support quoted phrases:

```text
"job interview"
```

The implementation should treat the quoted content as a phrase rather than unrelated individual terms.

If phrase search is deferred, document it clearly rather than creating misleading behavior.

---

# 17. CASE INSENSITIVITY

Search should normally be case-insensitive.

For example:

```text
Amazon
amazon
AMAZON
```

should produce equivalent results unless a future advanced search mode explicitly defines otherwise.

---

# 18. UNICODE SEARCH

Test Unicode.

Especially test:

- Malayalam
- emoji
- accented Latin
- non-Latin sender names
- non-Latin subjects

Example:

```text
കോളേജ്
```

must be searchable if that text exists locally.

Do not corrupt Unicode during indexing.

---

# 19. NORMALIZATION

Search indexing should use appropriate normalized representations.

Preserve the original display data separately.

Do not rewrite stored email content just to improve search.

---

# 20. SEARCH INDEX ARCHITECTURE

Choose an appropriate local indexing strategy.

Potential options include:

- SQLite FTS
- Room + FTS
- another local embedded search mechanism already supported by the project

Prefer the simplest reliable architecture compatible with the existing database.

Do not introduce a large search dependency without justification.

---

# 21. FULL-TEXT SEARCH

If SQLite FTS is appropriate, use it correctly.

Potential indexed fields:

```text
subject
senderName
senderEmail
bodyText
companyName
```

Do not blindly index every database field.

---

# 22. INDEX SOURCE OF TRUTH

The search index is derived data.

The normalized database remains the source of truth.

Conceptually:

```text
Normalized Email
      ↓
Search Index
```

If the index is deleted/corrupted, it should be possible to rebuild it from local data.

---

# 23. INDEX VERSIONING

Consider an index schema/version.

If the indexing strategy changes:

- invalidate/rebuild safely
- do not lose normalized email data
- do not require Gmail resynchronization

---

# 24. INDEX UPDATE STRATEGY

When a message is:

- inserted
- updated
- deleted locally
- re-parsed

the search index should stay synchronized.

Do not require a complete mailbox reindex after every email change.

---

# 25. IDEMPOTENCY

Repeated indexing of the same message must not create duplicate search records.

Use stable IDs.

For example:

```text
messageId + accountId
```

must identify the appropriate searchable record.

---

# 26. ACCOUNT ISOLATION

This is mandatory.

Search must never leak results between accounts.

Example:

```text
Account A
query: invoice
→ only Account A results
```

and:

```text
Account B
query: invoice
→ only Account B results
```

Unless an explicitly implemented unified-account search mode exists.

---

# 27. UNIFIED SEARCH PREPARATION

The architecture should eventually support:

```text
All accounts
```

but do not implement a complex unified search experience unless the existing multi-account architecture already supports it.

If unified search is implemented, every result must retain account identity.

---

# 28. FILTERS

Basic structured filters may be introduced.

Useful filters include:

- category
- priority
- Action Required
- account
- sender
- company
- unread state
- attachment presence
- date range where already supported by stored data

Keep filters local.

---

# 29. FILTER ARCHITECTURE

Do not create separate search implementations for each filter.

Prefer:

```text
SearchQuery
 ├── text
 ├── account
 ├── category
 ├── priority
 ├── actionRequired
 ├── sender
 ├── company
 └── other supported filters
```

Adapt to the existing architecture.

---

# 30. CATEGORY FILTER

Allow filtering by Mail Organizer category where appropriate.

Examples:

```text
Career
Education
Security
Receipts & Orders
```

Use the effective category if user overrides are already supported by the architecture.

Do not create a second category representation.

---

# 31. PRIORITY FILTER

Allow appropriate filtering by:

- Critical
- High
- Medium
- Low

Do not hardcode these filters separately from the priority model.

---

# 32. ACTION REQUIRED FILTER

Allow:

```text
Action Required
```

as a filter where useful.

Do not make this a separate search database.

Use the derived local data.

---

# 33. SENDER FILTER

Support searching/filtering by:

- sender name
- sender email
- domain

Where possible, selecting a sender should constrain results efficiently.

---

# 34. COMPANY FILTER

If Phase 8 company data exists:

Allow filtering by company.

Example:

```text
Company:
GitHub
```

must retrieve locally known associated messages.

Do not use external company lookup.

---

# 35. ATTACHMENT FILTER

If attachment metadata exists:

Support a local:

```text
Has attachment
```

filter if appropriate.

Do not download attachments merely to determine whether one exists.

---

# 36. DATE FILTER

Only implement basic date filtering if the existing timestamp model supports it reliably.

Potential options:

- today
- last 7 days
- last 30 days
- custom range

Do not build a complex natural-language date parser.

That can be added later if needed.

---

# 37. SEARCH RESULT RANKING

Results should be useful rather than simply database order.

A reasonable ranking strategy may consider:

- subject match
- sender match
- exact phrase
- body match
- recency
- category/priority relevance

The ranking must remain deterministic.

---

# 38. DO NOT USE AI FOR RANKING

Do not use:

- LLM embeddings
- remote semantic search
- vector APIs
- cloud ranking
- AI agents

for Phase 10.

A deterministic local search system is sufficient.

---

# 39. EXACT MATCH VS PARTIAL MATCH

Define matching behavior.

For example:

```text
invoice
```

should match:

```text
Invoice
invoice
INVOICE
```

as appropriate.

Avoid surprising substring behavior that creates excessive false positives.

---

# 40. EMAIL ADDRESS SEARCH

Searching:

```text
john@example.com
```

should be able to find the sender.

Searching:

```text
example.com
```

should find messages associated with that domain where appropriate.

---

# 41. SUBJECT WEIGHTING

A subject match should generally be more relevant than a deep body match.

Example:

```text
Subject: Interview invitation
Body: ...
```

should rank highly for:

```text
interview
```

even if another message mentions "interview" once in a long quoted thread.

---

# 42. SENDER WEIGHTING

A sender match should be strongly relevant.

For:

```text
github
```

messages from a known GitHub sender/company should rank appropriately.

Do not blindly prioritize every domain match over a direct subject match.

Document the ranking.

---

# 43. QUOTED REPLY CONTENT

Be careful with long conversation histories.

Searching:

```text
invoice
```

should not produce excessive matches simply because an old quoted message contained the word.

Where possible, distinguish:

- current message content
- quoted/previous content

Do not rebuild the Phase 5 parser unless necessary.

---

# 44. THREAD SEARCH

Searching should be useful at thread level.

If one message in a thread matches:

```text
interview
```

the thread may appear as a result.

Opening the result must take the user to the relevant thread/message context where practical.

---

# 45. RESULT HIGHLIGHTING

If practical, highlight matched terms in:

- subject
- snippet

Do not inject unsafe markup into email HTML.

Search highlighting must operate on safe display text.

---

# 46. SEARCH SNIPPETS

Generate snippets from normalized local text.

Do not expose huge bodies.

For example:

```text
...your interview is scheduled for...
```

The snippet must not leak unsafe HTML.

---

# 47. NO AUTOMATIC NETWORK ACCESS

Search must never:

- fetch missing content
- request remote images
- contact sender domains
- contact Gmail
- contact company websites

just to improve a result.

---

# 48. SEARCH SECURITY

Treat search queries as user input.

Do not construct unsafe SQL.

Use:

- parameterized queries
- safe Room/SQLite APIs
- validated query structures

Do not concatenate arbitrary SQL from the search field.

---

# 49. SQL INJECTION TESTING

Test queries containing:

```text
'
"
;
--
/*
*/
```

and other unusual characters.

The app must not crash.

The query must not escape its intended parameter boundary.

---

# 50. SEARCH DENIAL-OF-SERVICE

Test:

- extremely long query
- many repeated terms
- unusual Unicode
- huge wildcard-like input

Bound query size appropriately.

Do not allow a pathological search to freeze the application.

---

# 51. EMPTY QUERY

Define behavior for empty query.

Possible behavior:

- show recent mail
- show search landing state
- show suggestions

Do not execute an expensive full-database query accidentally.

---

# 52. NO RESULTS

Provide a useful empty state.

For example:

```text
No messages found
```

Optionally show:

```text
Try a different search
```

Do not fabricate results.

---

# 53. SEARCH ERROR

If the index is unavailable:

- show a clear error
- provide retry/rebuild where safe
- do not crash
- do not silently show incomplete results as complete

---

# 54. INDEX REBUILD

Provide an internal/application-level way to rebuild the local search index if necessary.

It may be:

- automatic recovery
- settings/debug action
- migration process

Do not require the user to resynchronize Gmail just to rebuild local search.

---
# 55. INDEX CORRUPTION

If the search index is corrupted:

```text
Normalized data
     ↓
Rebuild index
```

must be possible.

Do not delete the user's local email database merely because the index failed.

---

# 56. BACKGROUND INDEXING

Large indexing operations must not block the UI.

Use appropriate background execution.

Respect battery constraints.

Do not start unlimited background jobs.

---

# 57. SYNC + INDEX COORDINATION

When synchronization occurs:

```text
Gmail sync
 ↓
Normalization
 ↓
Database
 ↓
Index update
```

Avoid races such as:

```text
UI searches while half of a transaction is indexed
```

Use appropriate transaction/consistency boundaries.

---

# 58. CONSISTENCY

Search results should correspond to committed local data.

Do not expose an index entry that points to a missing message.

If eventual consistency is intentionally used, handle the transition safely.

---

# 59. MULTI-ACCOUNT TESTING

Test:

```text
Account A
  email: invoice-a@example.com

Account B
  email: invoice-b@example.com
```

Search:

from Account A.

Verify Account B data is not returned.

---

# 60. OFFLINE TESTING

Perform:

```text
Sync data
 ↓
Disable network
 ↓
Search
 ↓
Open result
 ↓
Open thread
 ↓
Read message
```

All local operations should work.

---

# 61. PERFORMANCE TESTING

Test with realistic data sizes:

- 100 messages
- 1,000 messages
- 10,000 messages
- larger dataset where practical

Measure:

- index creation
- index updates
- search latency
- memory
- database size
- UI responsiveness

Do not optimize blindly.

---

# 62. SEARCH BENCHMARK

Establish a simple local benchmark.

For example:

```text
query:
interview

results:
N

search latency:
X ms
```

Use this to detect obvious regressions.

Do not introduce a heavy benchmarking framework unless necessary.

---

# 63. UI PERFORMANCE

Search UI should remain responsive during:

- typing
- query parsing
- database search
- result rendering
- scrolling

Do not perform expensive search work on the main thread.

---

# 64. ACCESSIBILITY

Search must support:

- screen readers
- accessible search field
- clear button
- filter controls
- result semantics
- keyboard navigation where appropriate
- dynamic text sizing
- adequate touch targets

---

# 65. DARK MODE

Verify:

- search field
- filters
- results
- highlighted terms
- empty state
- error state

in dark mode.

Use design tokens.

---

# 66. VISUAL QA

Capture screenshots for:

1. Search landing state
2. Active search
3. Results
4. Filtered results
5. No results
6. Error state
7. Dark mode
8. Long query/result
9. Small screen

Inspect:

- spacing
- typography
- clipping
- result hierarchy
- filter usability
- keyboard interaction
- system bars

---

# 67. SCREEN RECORDING

Use screen recording where useful to validate:

- typing
- search transition
- filtering
- opening a result
- returning to results
- scrolling

Look for:

- lag
- flicker
- keyboard/layout issues
- unexpected navigation
- stale results

---

# 68. REGRESSION — PHASE 6

Verify:

- mail list
- thread
- message detail
- HTML safety
- offline reading
- navigation

still work.

---

# 69. REGRESSION — PHASE 7

Verify:

- categories
- classification explanations
- category persistence

still work.

---

# 70. REGRESSION — PHASE 8

Verify:

- sender
- company
- domain
- account isolation

still work.

---

# 71. REGRESSION — PHASE 9

Verify:

- priority
- Action Required
- explanations
- derived data

still work.

---

# 72. NO ADVANCED RULES

Do not implement the Phase 12 user rule engine.

Do not create:

- if/then rule editor
- sender automation rules
- custom category rules
- newsletter rules

Only expose search/filter capabilities.

---

# 73. NO DASHBOARD

Do not implement the Phase 11 dashboard redesign.

Search may have its own screen, but do not reorganize the entire Home experience.

---

# 74. NO AI

Do not add:

- embeddings
- vector databases
- semantic search
- LLM search
- remote AI

Phase 26 handles optional AI architecture.

---

# 75. NO EXTERNAL SEARCH

Do not use the internet to fill search gaps.

If an email was not synchronized locally, search should not magically retrieve it from Gmail.

---

# 76. DATA PRIVACY

Search queries may reveal sensitive information.

Do not log:

- search query
- result contents
- sender
- subject

unless absolutely required for safe diagnostics, and then use redacted/safe diagnostics.

Do not send search queries to analytics.

---

# 77. QUERY HISTORY

Do not automatically persist search history unless explicitly required.

Search history can itself be sensitive.

If architecture leaves room for future search history, do not implement it now.

---

# 78. CLIPBOARD

Do not automatically copy:

- search queries
- email addresses
- subjects
- message content

to the clipboard.

User-initiated copy behavior should remain explicit.

---

# 79. DATABASE MIGRATION

If FTS/search tables require schema changes:

- create proper migrations
- test existing database migration
- preserve normalized email
- preserve classification
- preserve sender/company
- preserve priority/action state

Do not wipe the database.

---

# 80. INDEX REBUILD SAFETY

A rebuild must be transactional or otherwise safely recoverable.

If rebuild fails halfway:

- existing normalized data must remain intact
- the app must recover
- partial index state must not masquerade as complete

---

# 81. GIT REVIEW

From the Mail Organizer Git root:

```text
git status
git diff
```

Verify:

- only Mail Organizer files changed
- no real email data committed
- no search queries committed
- no private mailbox dumps
- no secrets
- no generated APKs
- no sibling project changes

Do not commit unless explicitly instructed.

---

# 82. UPDATE `editor-rules.md`

Before completing Phase 10, update `editor-rules.md` with permanent search/indexing rules discovered.

At minimum preserve:

- local-first search
- offline search
- normalized-data source of truth
- derived-index architecture
- index rebuildability
- account isolation
- parameterized queries
- query safety
- no external search
- no AI search
- no sensitive query logging
- bounded query processing

Do this yourself.

---

# 83. UPDATE `spec.md`

Only after verification:

- mark Phase 10 tasks complete
- record search architecture
- record indexing strategy
- record ranking strategy
- record known limitations
- record deferred features

Do not mark Phase 11 or later complete.

---

# 84. UPDATE DEVELOPMENT STATUS

If `docs/development-status.md` exists, update:

- search architecture
- index architecture
- supported filters
- ranking
- performance
- offline behavior
- account isolation
- known limitations

Do not include private email content.

---

# 85. PHASE 10 ACCEPTANCE CRITERIA

Phase 10 is complete only when:

### Search

- [ ] free-text search works
- [ ] multi-word queries work
- [ ] case-insensitive search works
- [ ] Unicode search works
- [ ] sender search works
- [ ] subject search works
- [ ] body search works
- [ ] company search works where available
- [ ] domain search works
- [ ] thread results work
- [ ] results open correctly

### Filters

- [ ] category filter works
- [ ] priority filter works
- [ ] Action Required filter works
- [ ] sender filter works
- [ ] company filter works
- [ ] attachment filter works if implemented
- [ ] account filter/scope works

### Index

- [ ] local full-text/index architecture exists
- [ ] index is derived from source data
- [ ] index is rebuildable
- [ ] index updates incrementally
- [ ] repeated indexing is idempotent
- [ ] index versioning/recovery is handled

### Security

- [ ] parameterized database queries
- [ ] SQL injection tests pass
- [ ] pathological query tests pass
- [ ] no external search
- [ ] no AI search
- [ ] no sensitive query logging
- [ ] account isolation verified

### Offline

- [ ] search works without network after synchronization
- [ ] results open offline
- [ ] threads open offline
- [ ] local filters work offline

### Performance

- [ ] large dataset tested
- [ ] search latency measured
- [ ] indexing tested
- [ ] UI remains responsive
- [ ] background work does not block UI

### UI

- [ ] search field works
- [ ] results UI works
- [ ] empty state works
- [ ] error state works
- [ ] filter UI works
- [ ] dark mode works
- [ ] accessibility works
- [ ] long queries work

### Regression

- [ ] Phase 6 passes
- [ ] Phase 7 passes
- [ ] Phase 8 passes
- [ ] Phase 9 passes

### Device

- [ ] Gradle build succeeds
- [ ] APK installs
- [ ] application launches
- [ ] search tested on device/emulator
- [ ] offline search tested
- [ ] screenshots inspected
- [ ] screen recording used where useful
- [ ] logcat inspected

### Workspace safety

- [ ] sibling projects untouched
- [ ] sibling Gradle files untouched
- [ ] sibling SDK configuration untouched
- [ ] unrelated apps untouched
- [ ] unrelated Git repositories untouched

---

# 86. FINAL PHASE REPORT

Provide:

## Phase 10 Status

`COMPLETE` / `PARTIAL` / `BLOCKED`

## Search Architecture

Explain:

- query model
- parser
- indexing
- repository
- ranking

## Index

Explain:

- technology
- indexed fields
- update strategy
- rebuild strategy
- versioning

## Filters

List supported filters.

## Ranking

Explain deterministic ranking behavior.

## Offline

Confirm search works without network after synchronization.

## Security

Report:

- SQL/query safety
- account isolation
- privacy
- query logging policy

## Performance

Report:

- dataset size tested
- indexing performance
- search latency
- UI responsiveness

## Device Validation

Report:

- device/emulator
- Android/API level
- build
- installation
- search tests
- offline tests
- screenshots
- screen recording where used
- logcat
- regression results

## Workspace Isolation

Explicitly confirm unrelated projects were not modified.

## Files Changed

List important files.

## Known Issues

List unresolved issues.

## Deferred Work

Especially:

- Dashboard
- advanced rules
- user corrections UI
- deadline extraction
- meeting extraction
- action engine
- Calendar
- Tasks
- Gmail write operations
- multi-account unified search enhancements
- AI/semantic search
- analytics

## Acceptance Criteria

Show every Phase 10 criterion and its status.

## Next Phase

The next incomplete phase is:

**Phase 11 — Dashboard & Information Architecture**

Do not execute it automatically.

---

# FINAL OPERATING MODEL

Continue following:

```text
DISCOVER INSTRUCTION FOLDER
        ↓
READ REQUIREMENTS
        ↓
READ SPEC
        ↓
READ DESIGN
        ↓
READ EDITOR RULES
        ↓
IDENTIFY MAIL ORGANIZER ROOT
        ↓
VERIFY PREVIOUS PHASE
        ↓
READ ONLY CURRENT PHASE
        ↓
INSPECT ACTUAL PROJECT STATE
        ↓
IMPLEMENT PHASE 10 ONLY
        ↓
RUN UNIT TESTS
        ↓
RUN ANDROID TESTS
        ↓
GRADLE BUILD
        ↓
INSTALL ONLY MAIL ORGANIZER
        ↓
RUN ON DEVICE / EMULATOR
        ↓
VERIFY SEARCH
        ↓
VERIFY FILTERS
        ↓
VERIFY OFFLINE SEARCH
        ↓
VERIFY ACCOUNT ISOLATION
        ↓
CAPTURE SCREENSHOTS
        ↓
SCREEN RECORD WHERE USEFUL
        ↓
INSPECT LOGCAT
        ↓
FIX
        ↓
REBUILD
        ↓
REINSTALL
        ↓
RETEST
        ↓
REGRESSION TEST PHASES 6–9
        ↓
SECURITY REVIEW
        ↓
MULTI-PROJECT ISOLATION REVIEW
        ↓
UPDATE EDITOR-RULES.MD
        ↓
UPDATE SPEC.MD
        ↓
UPDATE DEVELOPMENT STATUS
        ↓
STOP
```

**Do not implement Phase 11.**

**Do not redesign the dashboard.**

**Do not implement the user rules engine.**

**Do not implement user correction workflows.**

**Do not implement deadline/meeting extraction.**

**Do not implement Action Cards or external actions.**

**Do not create Calendar or Tasks actions.**

**Do not modify Gmail data.**

**Do not add Gmail write scopes.**

**Do not add AI or semantic/vector search.**

**Do not send search queries or email data to external services.**

**Do not touch sibling Android projects.**

**Do not consider Gradle success alone sufficient validation.**

Phase 10 is complete only when Mail Organizer provides a fast, safe, deterministic, account-isolated, genuinely offline-capable local search experience over the email data it has already synchronized.