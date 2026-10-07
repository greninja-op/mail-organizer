# PHASE 10 — SEARCH & LOCAL INDEXING

## EXECUTION CONTRACT

Confirm Phase 10 is first incomplete and Phase 9 is verified. Read all project rules. Implement local indexing/search only. Do not implement Calendar, Tasks, Gmail writes, AI or automation.

## 1. DISCOVER ROOT
Locate instruction folder and Mail Organizer root.

## 2. CONFIRM PHASE 9
Stop if prerequisite is incomplete.

## 3. WORKSPACE ISOLATION
Protect sibling projects, SDKs, device apps and unrelated cloud resources.

## 4. GIT BASELINE
Inspect status/diff.

## 5. SEARCH OBJECTIVE
Provide fast local-first search over normalized and derived mail data.

## 6. INDEX TECHNOLOGY
Use SQLite/Room FTS or another justified local indexed mechanism. Do not create a second database.

## 7. INDEX BOUNDARY
Index only data required for product search. Avoid unnecessary copies of sensitive content.

## 8. SEARCHABLE FIELDS
Support sender, recipient where appropriate, subject, safe body text, thread, company/domain, category, priority, Action Required and trusted Gmail labels/categories.

## 9. ACCOUNT ISOLATION
Every search query must enforce account boundaries.

## 10. ALL INBOX
Cross-account All Inbox search must retain receiving-account identity.

## 11. CATEGORY FILTER
Allow search within the selected category while preserving category context.

## 12. COMPANY FILTER
Allow company/domain filtering within the selected category.

## 13. PRIORITY FILTER
Support priority filters from Phase 9.

## 14. ACTION FILTER
Support Action Required state filters.

## 15. STARRED FILTER
Allow Starred as a local query/view boundary.

## 16. QUERY PARSING
Define deterministic query parsing for supported search fields. Do not create an opaque natural-language search engine.

## 17. PARAMETERIZATION
Use parameterized queries. Never concatenate untrusted query text into raw SQL.

## 18. SPECIAL CHARACTERS
Handle punctuation, Unicode, quotes, whitespace and pathological search strings safely.

## 19. EMPTY QUERY
Define safe behavior for empty/blank queries.

## 20. LONG QUERY
Bound or safely handle excessively long queries.

## 21. RANKING
Implement deterministic relevance ranking based on documented signals.

## 22. TIE BREAKING
Use deterministic tie-breakers such as timestamp/ID.

## 23. SNIPPETS
Generate safe snippets from indexed content without exposing hidden HTML.

## 24. HIGHLIGHTING
If highlighting is used, ensure markup cannot become executable content.

## 25. INDEX BUILD
Create/rebuild the index from local normalized data.

## 26. INCREMENTAL INDEXING
Update only changed/created/deleted local records where possible.

## 27. REBUILD RECOVERY
Provide a safe rebuild path for corrupted/stale indexes.

## 28. VERSIONING
Version index schema/configuration so migrations are explicit.

## 29. SYNC INTEGRATION
Allow Phase 4/5 local updates to trigger indexing without adding a new Gmail sync engine.

## 30. OFFLINE
Ordinary search must work without network when local data/index exists.

## 31. NO REMOTE SEARCH
Do not send search queries or email content to a server for ordinary search.

## 32. PRIVACY
Do not log raw queries alongside sensitive email content unnecessarily.

## 33. SEARCH UI
Integrate with the existing Gmail-familiar top search affordance.

## 34. SEARCH STATES
Implement idle, typing, loading/indexing, results, no results, invalid query and index unavailable states.

## 35. ACCOUNT CONTEXT
Make the active account context clear while preserving All Inbox cross-account behavior.

## 36. RESULT ROWS
Use the same sender/subject/preview/time/Star/source-account conventions as mailbox rows.

## 37. COMPANY CONTEXT
Selecting a company filter must remain scoped to the current category/search context.

## 38. ACCESSIBILITY
Provide semantic search controls, filter labels, result counts where safe and large-text support.

## 39. PERFORMANCE
Test realistic mail volumes, query latency, scrolling and index rebuild cost.

## 40. MEMORY
Avoid loading all indexed records into memory for ordinary queries.

## 41. TEST FIXTURES
Create synthetic datasets across multiple accounts, categories, companies, priorities and action states.

## 42. QUERY TESTS
Test exact terms, partial terms where supported, Unicode, punctuation, empty, long and pathological queries.

## 43. FILTER TESTS
Test combinations of account/category/company/priority/action/Starred filters.

## 44. ACCOUNT TESTS
Prove cross-account leakage is impossible.

## 45. INDEX TESTS
Test initial build, incremental update, deletion, rebuild, migration and duplicate handling.

## 46. RANKING TESTS
Prove deterministic ordering and tie-breaking.

## 47. SECURITY TESTS
Test SQL injection-like input, HTML snippets and sensitive logging.

## 48. DEVICE VALIDATION
Build/install Mail Organizer only. Exercise search, filters, no-results and offline search. Capture screenshots and inspect logcat.

## 49. ADB SAFETY
Use package-scoped commands only.

## 50. FINAL BUILD
Run Gradle/shared/unit/database/UI/static tests.

## 51. FINAL RETEST
Fix, rebuild, reinstall and repeat search/device/security validation.

## 52. GIT REVIEW
Confirm only Mail Organizer changed and no sensitive index dumps/credentials were committed.

## 53. DOCUMENTATION
Update spec/status with index technology, searchable fields, ranking, rebuild behavior and limitations.

## 54. EDITOR RULES
Add only permanent search/privacy rules genuinely discovered.

## 55. ACCEPTANCE CRITERIA
- local index exists;
- supported fields searchable;
- filters work;
- account isolation verified;
- deterministic ranking;
- incremental/rebuild paths work;
- offline search works with local data;
- safe snippets;
- accessibility/performance verified;
- no remote search or future feature implemented.

## 56. FINAL REPORT
Report index architecture, fields, ranking, tests, device/API, screenshots/logcat, security, workspace isolation, files, issues, deferred work and acceptance status.

## 57. STOP
Do not execute Phase 11.
