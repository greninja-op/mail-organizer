# PHASE 1 — ANDROID APPLICATION FOUNDATION

## EXECUTION CONTRACT

Confirm Phase 1 is the first incomplete phase. Read requirements.md, spec.md, design.md, editor-rules.md and relevant architecture documents. Implement Android foundation only. Do not implement Gmail OAuth, sync, classification, search, Calendar, Tasks, AI, automation or Gmail writes.

## 1. DISCOVER ROOT
Locate the Mail Organizer instruction folder and project root before any Gradle/Git/ADB command.

## 2. READ SOURCE OF TRUTH
Reconcile requirements, spec, design and editor rules.

## 3. CONFIRM PHASE 0
Verify Phase 0 is complete. Stop if it is not.

## 4. MULTI-PROJECT ISOLATION
Protect sibling Android projects and their SDK/build configuration.

## 5. GIT BASELINE
Inspect status/diff and preserve existing work.

## 6. ANDROID SHELL
Establish stable Android entry point, lifecycle and state-driven navigation.

## 7. COMPOSE/MATERIAL
Use Jetpack Compose, Material 3 and appropriate current Material 3 Expressive guidance.

## 8. MAIL ORGANIZER IDENTITY
Gmail familiarity is a hierarchy reference, not a pixel clone. Do not copy proprietary Google artwork/source.

## 9. DESIGN TOKENS
Centralize colors, typography, shapes, spacing, surfaces, icon sizes and semantic states.

## 10. TOP APP BAR
Establish three-line navigation affordance, Gmail-familiar search location and account/profile circle.

## 11. NAVIGATION DRAWER
Implement All Inbox, Primary, Promotional, Social, Spam and Starred destinations.

## 12. DRAWER RULES
Companies are not drawer destinations. Counts must be real or zero/empty, never fabricated.

## 13. ALL INBOX IDENTITY
Every All Inbox row must support a compact receiving-Gmail-account indicator distinct from sender/company identity.

## 14. STAR FOUNDATION
Establish individual Star control and Starred global destination without Gmail writes.

## 15. COMPANY FILTER BOUNDARY
Reserve category-scoped company grouping/filtering and company pinning without implementing intelligence.

## 16. ACCOUNT SWITCHER
Provide current account, connected accounts, add-account entry and status slots.

## 17. ACCOUNT SWIPE
Establish bounded profile swipe to next account where supported, without implementing sync.

## 18. AUTH BOUNDARY
Add official OAuth entry point boundary only; never collect passwords or emulate Google login.

## 19. RECOVERY/SYNC VISUAL SYSTEM
Create truthful Gmail→Mail Organizer recovery/sync visual states for later real sync.

## 20. PROGRESS HONESTY
Determinate progress only when a real trustworthy value exists; otherwise indeterminate.

## 21. MOTION TIERS
Establish Essential, Standard, Expressive and Reduced motion policies.

## 22. PERFORMANCE PROFILE
Create capability-based adaptive performance abstraction covering animation/effects/prefetch/cache/concurrency/haptics.

## 23. REFRESH RATE
Do not equate 120 Hz hardware with 120 FPS rendering. Favor sustained smoothness, thermals and battery.

## 24. ACCESSIBILITY
Semantics, touch targets, large fonts, contrast, reduced motion and non-color cues.

## 25. RESPONSIVE LAYOUT
Support portrait, landscape and supported window widths without clipping.

## 26. LIGHT/DARK
Implement light/dark themes and Android 12+ dynamic color with controlled fallback.

## 27. EMPTY/ERROR/OFFLINE
Create deliberate no-account, loading, empty, error and offline states.

## 28. COMPONENTS
Create reusable shell, drawer, account, message-row, Star, source-account, company-filter and recovery components.

## 29. STATE OWNERSHIP
Use unidirectional state. Composables must not own networking, DB transactions or tokens.

## 30. PERFORMANCE DISCIPLINE
Avoid expensive composition, unnecessary recomposition, synchronous I/O and unbounded animations.

## 31. TESTING
Create tests for navigation, state, semantics, tokens, responsive behavior and core shell components.

## 32. DEVICE BUILD
Run the project's Gradle wrapper and generate the debug APK.

## 33. INSTALLATION
Confirm package ID and install only Mail Organizer.

## 34. ADB VALIDATION
Use launch, force-stop, logcat, dumpsys, screencap and screenrecord where useful. Use adb reverse only if required.

## 35. VISUAL QA
Inspect screenshots against design.md: top bar, drawer, rows, account indicator, Star, themes and responsive behavior.

## 36. ACCESSIBILITY QA
Test TalkBack/semantics, large font, reduced motion and contrast.

## 37. PERFORMANCE QA
Inspect startup, scrolling and transitions; do not run continuous benchmarks.

## 38. SECURITY QA
Confirm no credential collection, secrets, tokens or unsafe network work.

## 39. TEST MATRIX
Test cold start, navigation, drawer, account shell, empty/error/offline, light/dark, large font, reduced motion and recreation.

## 40. GIT REVIEW
Confirm only Mail Organizer changed.

## 41. DOCUMENTATION
Update spec.md/status only after actual verification.

## 42. EDITOR RULES
Add only permanent Android UI/performance/accessibility rules discovered.

## 43. ACCEPTANCE CRITERIA
- Android shell builds;
- Material/Gmail-familiar hierarchy established;
- required drawer destinations exist;
- All Inbox account identity boundary exists;
- Star/company/account/sync UI boundaries exist;
- adaptive performance/motion/accessibility foundations exist;
- device validation completed where available;
- no later functionality implemented.

## 44. FINAL REPORT
Report build/tests/device/API, screenshots, accessibility, performance, files changed, issues and deferred work.

## 45. STOP
Do not execute Phase 2.
