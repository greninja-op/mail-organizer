# PHASE 6 — CORE INBOX & EMAIL VIEWER

## EXECUTION CONTRACT

Confirm Phase 6 is first incomplete and Phase 5 is verified. Read all project rules. Implement the core local mailbox/thread viewer only. Do not add sync, classifier, company intelligence, search, Calendar, Tasks, AI, automation or Gmail writes.

## 1. DISCOVER ROOT
Locate instruction folder and Mail Organizer root before tooling.

## 2. CONFIRM PHASE 5
Verify normalized parsing/security foundation is complete.

## 3. WORKSPACE ISOLATION
Never modify sibling projects.

## 4. GIT BASELINE
Inspect status/diff and preserve unrelated work.

## 5. LOCAL-FIRST UI
UI reads local repositories/database. Never call Gmail APIs from composables.

## 6. STATE ARCHITECTURE
Use UI state → ViewModel/use case → repository → local DB.

## 7. ALL INBOX
Implement unified cross-account presentation with receiving-account identity on every row.

## 8. ACCOUNT IDENTITY
Receiving Gmail account must remain distinct from sender, avatar and company.

## 9. PRIMARY
Display local Primary category state where available.

## 10. PROMOTIONAL
Display organized Promotional state without deleting or hiding promotional mail.

## 11. SOCIAL
Display Social state from available local labels/state.

## 12. SPAM
Provide first-class Spam destination and safe red attention/new indicator where applicable.

## 13. STARRED
Provide global Starred view. Starred messages remain in original category.

## 14. STAR ACTION
Implement local Star state without pretending Gmail has been modified.

## 15. COMPANY CONTEXT
Preserve selected category/company context if already represented, without implementing company intelligence.

## 16. MAILBOX ROW
Render sender, subject, preview, time, unread state, Star and source account.

## 17. THREAD INDICATION
Represent thread/message relationships without inventing thread identity.

## 18. PAGINATION
Use local pagination/windowing and stable lazy-list keys.

## 19. EMPTY STATES
Distinguish no local mail, empty category and not-yet-synchronized state.

## 20. ERROR STATES
Distinguish repository/database/parse failures from network unavailability.

## 21. OFFLINE
Allow local mail reading offline where local data exists.

## 22. THREAD VIEWER
Open a thread and display participants, message order, timestamps and normalized bodies.

## 23. READABILITY
Maintain Gmail familiarity while preserving Mail Organizer identity.

## 24. HTML RENDERING
Render Phase 5 sanitized content only. Never execute email scripts.

## 25. LINK SAFETY
Require deliberate interaction before navigation. Never auto-open links or execute javascript URLs.

## 26. ATTACHMENTS
Display metadata only. Do not auto-download binaries.

## 27. READ/UNREAD
Display state clearly. Local mutations must not be represented as Gmail writes.

## 28. ACCOUNT SCOPING
Every mailbox/thread action must preserve account ownership.

## 29. LARGE THREADS
Avoid loading unnecessarily large content into memory.

## 30. PERFORMANCE PROFILE
Respect the adaptive PerformanceProfile and motion tiers from Phase 1.

## 31. ACCESSIBILITY
Provide semantics for sender/subject/preview, Star, account identity, unread, Spam, links and attachments.

## 32. LARGE TEXT
Validate message rows/thread layout at large font scales.

## 33. REDUCED MOTION
Ensure transitions remain understandable without expressive motion.

## 34. LIGHT/DARK
Validate themes, dynamic color and semantic attention colors.

## 35. RESPONSIVE
Validate supported portrait, landscape and window widths.

## 36. TEST DATA
Use synthetic local data for ordinary tests; never depend on a personal Gmail account.

## 37. UNIT TESTS
Test list state, Star, category preservation, account identity and repository errors.

## 38. THREAD TESTS
Test ordering, multiple messages, malformed content and attachment metadata.

## 39. SECURITY TESTS
Prove sanitized HTML, unsafe links and account boundaries remain safe.

## 40. PERFORMANCE TESTS
Use large synthetic datasets to detect excessive allocations/recomposition.

## 41. DEVICE INSTALL
Build and install only the Mail Organizer APK.

## 42. DEVICE FLOW
Launch → All Inbox → categories → Star → Starred → thread → back navigation.

## 43. OFFLINE FLOW
Force offline/local-only conditions and verify local data remains readable.

## 44. RESTART FLOW
Force-stop/relaunch and verify state restoration.

## 45. SCREENSHOTS
Capture important mailbox/thread states for visual inspection without retaining unnecessary private data.

## 46. SCREEN RECORDING
Use where helpful to inspect navigation, transitions and duplicate taps.

## 47. LOGCAT
Inspect crashes, renderer errors, DB errors and confirm no tokens/private bodies leak.

## 48. ADB SAFETY
Use package-scoped commands; no unrelated uninstall/data clearing/reverse mapping changes.

## 49. FINAL BUILD
Run Gradle, shared/unit/UI/static checks and fix phase-caused failures.

## 50. FINAL RETEST
Rebuild, reinstall and repeat device/visual/security tests after fixes.

## 51. GIT REVIEW
Confirm only Mail Organizer files changed and no secrets/generated artifacts are committed.

## 52. DOCUMENTATION
Update spec/status with real viewer behavior, limitations and validation results.

## 53. EDITOR RULES
Add only permanent viewer/security/accessibility rules genuinely discovered.

## 54. ACCEPTANCE CRITERIA
- local mailbox works;
- required categories work;
- All Inbox account indicator works;
- Starred/global Star works locally;
- thread viewer works;
- sanitized email renders safely;
- attachment metadata works without auto-download;
- offline/empty/error states work;
- accessibility/themes/responsive behavior verified;
- tests/build/device QA pass;
- no later phase implemented.

## 55. FINAL REPORT
Report mailbox architecture, UI states, thread viewer, security, tests, device/API, screenshots/logcat, workspace isolation, files, known issues, deferred work and acceptance status.

## 56. STOP
Do not execute Phase 7.
