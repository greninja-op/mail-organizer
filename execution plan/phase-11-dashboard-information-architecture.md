# PHASE 11 — DASHBOARD & INFORMATION ARCHITECTURE

## EXECUTION CONTRACT

Confirm Phase 11 is first incomplete and Phase 10 is verified. Read requirements.md, spec.md, design.md, editor-rules.md and relevant status/architecture files. Implement the dashboard and information architecture only. Do not implement Calendar, Tasks, Gmail writes, AI or automation.

## 1. DISCOVER ROOT
Locate the instruction folder and Mail Organizer root before tooling.

## 2. CONFIRM PHASE 10
Stop if local search/indexing is not verified.

## 3. WORKSPACE ISOLATION
Protect sibling projects, device apps, SDK configuration and unrelated cloud resources.

## 4. GIT BASELINE
Inspect branch/status/diff.

## 5. IA OBJECTIVE
Consolidate the mailbox hierarchy into a clear Android Gmail-familiar Mail Organizer information architecture.

## 6. PRIMARY NAVIGATION
Required drawer destinations are All Inbox, Primary, Promotional, Social, Spam and Starred.

## 7. DRAWER IS NOT COMPANY DIRECTORY
Companies must remain inside selected category filters, never become top-level drawer destinations.

## 8. ALL INBOX
All Inbox is unified across connected accounts. Every row exposes the receiving Gmail account through the established compact source-account indicator.

## 9. SOURCE ACCOUNT IDENTITY
Receiving account identity must remain distinct from sender/company identity.

## 10. PRIMARY
Primary is a first-class mailbox destination using real local state.

## 11. PROMOTIONAL
Promotional mail remains accessible and organized rather than silently deleted.

## 12. SOCIAL
Social mail remains a first-class destination using available local state.

## 13. SPAM
Spam is a first-class destination with clear red new/unread attention semantics where appropriate.

## 14. SPAM RECOVERY
Provide a clear user-driven Not Spam/recovery boundary without implementing Gmail write operations early.

## 15. STARRED
Starred is a global view of individually starred messages. A starred message remains in its original category.

## 16. COMPANY GROUPING
Within the selected category, show company filters/groups from Phase 8.

## 17. PINNED COMPANIES
Pinned companies appear at the top of the category's company filter list.

## 18. COMPANY PIN VS STAR
Company pinning is separate from individual email starring. Do not conflate them.

## 19. CATEGORY CONTEXT
Selecting a company must preserve the current category context.

## 20. SEARCH
Top app bar search uses Phase 10 local index and preserves account/category/company context.

## 21. ACCOUNT SWITCHER
Profile/account affordance opens current account, connected accounts and add-account.

## 22. PROFILE SWIPE
Where supported, profile swipe can move to the next account without confusing sender/company identity.

## 23. ADD ACCOUNT
Add account uses the real OAuth/recovery foundation, not a fake credential form.

## 24. ACCOUNT STATUS
Authentication state and synchronization state must remain separate.

## 25. HOME/DASHBOARD
Create an attention-first dashboard/home surface only from real existing data. It must not replace explicit mailbox destinations.

## 26. ATTENTION PRIORITY
Use Action Required and Priority from Phase 9 to surface meaningful attention without fabricating tasks.

## 27. NO DUPLICATE ENGINES
Do not implement a second classifier, company resolver or search index inside the dashboard.

## 28. REAL DATA ONLY
Counts, unread indicators, company counts and dashboard summaries must use real local state or truthful empty/zero state.

## 29. LOADING
Dashboard and destinations need deliberate loading states.

## 30. EMPTY
Provide meaningful empty states for no accounts, no mail, empty categories and no actionable mail.

## 31. ERROR
Map repository/index errors to safe user-facing states.

## 32. OFFLINE
Show locally available data offline and clearly communicate unavailable network-dependent operations.

## 33. ACCOUNT CONTEXT
Ensure switching accounts updates the visible context without leaking another account's data.

## 34. UNIFIED VIEWS
All Inbox/Starred queries must preserve account boundaries.

## 35. COUNTS
Folder counts/unread indicators must be computed from real state and must not be stale due to duplicate local queries.

## 36. PERFORMANCE
Avoid recomputing dashboard summaries on every recomposition. Use stable state/repository flows.

## 37. RESPONSIVE LAYOUT
Validate portrait, landscape and supported window widths. Use adaptive layouts rather than device-name checks.

## 38. ACCESSIBILITY
Provide semantics for navigation, current destination, counts, account identity, company filters, Starred and Spam attention.

## 39. LARGE TEXT
Validate drawer, dashboard cards/sections, company filters and top bar at large font scales.

## 40. REDUCED MOTION
Respect the Phase 1 motion/performance system. Dashboard must not animate continuously.

## 41. LIGHT/DARK
Validate themes and semantic attention colors.

## 42. DYNAMIC COLOR
Use dynamic color where appropriate without destroying spam/error/unread semantics.

## 43. VISUAL CONSISTENCY
Review against design.md and reuse centralized tokens/components. Do not create one-off visual systems.

## 44. NAVIGATION TESTS
Test every drawer destination, back navigation, deep state restoration and selected-state semantics.

## 45. ALL INBOX TESTS
Test multiple accounts, source-account indicators and cross-account isolation.

## 46. COMPANY TESTS
Test category → company filter → results → clear filter → pinned ordering.

## 47. STARRED TESTS
Test star/unstar, category preservation and Starred global results.

## 48. SPAM TESTS
Test unread/red indicator semantics and safe recovery boundary.

## 49. SEARCH TESTS
Test dashboard/search entry and preservation of context.

## 50. ACCOUNT TESTS
Test account switching, add-account entry and authentication/sync state separation.

## 51. OFFLINE/ERROR TESTS
Test no network, DB/index error, empty data and partial local data.

## 52. PERFORMANCE TESTS
Use realistic synthetic datasets to inspect startup, navigation, scrolling and dashboard recomposition.

## 53. DEVICE VALIDATION
Build/install Mail Organizer only. Launch, navigate drawer, switch account context, search, filter company, open Starred/Spam and inspect screenshots.

## 54. SCREEN RECORDING
Where useful, record drawer/account/profile interactions and dashboard transitions. Avoid retaining unnecessary private data.

## 55. LOGCAT
Inspect crashes, navigation failures, DB/index failures and ensure no tokens/private bodies leak.

## 56. ADB SAFETY
Use package-scoped commands; do not alter unrelated apps or reverse mappings.

## 57. FINAL BUILD
Run Gradle, shared/unit/database/UI/static checks.

## 58. FINAL RETEST
Fix, rebuild, reinstall and repeat full navigation/visual/accessibility/security validation.

## 59. GIT REVIEW
Confirm only Mail Organizer files changed and no secrets/generated artifacts were added.

## 60. DOCUMENTATION
Update spec/status with the final IA, dashboard behavior, navigation rules and known limitations.

## 61. EDITOR RULES
Add only permanent IA/navigation/accessibility rules genuinely discovered.

## 62. ACCEPTANCE CRITERIA
- required drawer destinations work;
- All Inbox is unified and account-aware;
- Primary/Promotional/Social/Spam/Starred are distinct;
- company filtering/pinning is category-scoped;
- email Star and company pin are separate;
- search entry/context works;
- account switcher works;
- dashboard uses real local data;
- loading/empty/error/offline states work;
- accessibility/themes/responsive behavior verified;
- tests/device QA pass;
- no later phase implemented.

## 63. FINAL REPORT
Report IA/navigation architecture, dashboard behavior, account handling, company grouping, search integration, tests, device/API, screenshots/logcat, security, workspace isolation, files, known issues, deferred work and acceptance status.

## 64. STOP
Do not execute Phase 12.
