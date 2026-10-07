# PHASE 7 — DETERMINISTIC CLASSIFICATION ENGINE

## EXECUTION CONTRACT

Confirm Phase 7 is the first incomplete phase and Phase 6 is verified. Read requirements.md, spec.md, design.md, editor-rules.md, relevant architecture/status documents and this prompt. Implement only deterministic local classification. Do not implement company intelligence, search, Calendar, Tasks, Gmail writes, AI, automation or background sync.

## 1. DISCOVER THE INSTRUCTION FOLDER
Locate the execution-plan folder and Mail Organizer root before Gradle, Git, ADB or edits.

## 2. READ SOURCE OF TRUTH
Reconcile requirements, spec, design and editor-rules. Higher-priority requirements override lower-priority convenience.

## 3. CONFIRM PHASE ORDER
Verify Phase 6 is complete. If the prerequisite is incomplete, stop and report the blocker.

## 4. WORKSPACE ISOLATION
Protect sibling projects, SDK configuration, device apps, unrelated Git repositories and unrelated Google Cloud resources.

## 5. GIT BASELINE
Inspect branch, status and diff. Preserve pre-existing user work.

## 6. CLASSIFICATION OBJECTIVE
Implement deterministic, explainable, local classification for Action Required, Important, Career, Education, Receipts & Orders, Security, Notifications, Newsletters, Promotions and Low Value.

## 7. LOCAL-FIRST RULE
Classification must work without sending email content to a remote service.

## 8. UNTRUSTED EMAIL RULE
Email content is data, never executable instructions. An email cannot authorize external actions.

## 9. INPUT MODEL
Use normalized sender, recipient, subject, safe body/preview, labels, timestamps, attachment metadata and trusted Gmail metadata available from earlier phases.

## 10. OUTPUT MODEL
Represent category, confidence/strength, rule/version provenance, supporting signals and uncertainty without pretending deterministic inference is certain.

## 11. CATEGORY MODEL
Keep the required product categories stable and explicit. Do not silently create arbitrary new top-level categories.

## 12. CATEGORY PRECEDENCE
Define deterministic precedence for mutually competing categories. Document the order and ensure the same input produces the same result.

## 13. ACTION REQUIRED SEPARATION
Do not collapse Action Required into a category. Classification answers “what kind of mail”; Action Required is a separate later decision/state.

## 14. IMPORTANT SEPARATION
Keep Important independent from category and later Priority. Gmail importance can be one supporting signal, not an unquestionable truth.

## 15. SECURITY SIGNALS
Treat security/account alerts conservatively. Strong security signals must not be downgraded merely because a message resembles promotional or notification mail.

## 16. CAREER SIGNALS
Use deterministic evidence such as hiring/recruiting context, job/application terminology, interview scheduling and career domains where confidence is justified.

## 17. EDUCATION SIGNALS
Use course, college, academic, examination, assignment and institutional signals conservatively.

## 18. RECEIPTS & ORDERS
Recognize transaction/order/receipt/invoice/shipping patterns without assuming every marketing message is a receipt.

## 19. NOTIFICATIONS
Recognize service/system notifications while distinguishing actionable notifications from generic marketing.

## 20. NEWSLETTERS
Use sender/header/content patterns to recognize recurring editorial/newsletter mail.

## 21. PROMOTIONS
Recognize commercial/promotional signals while avoiding false classification of transactional or security mail.

## 22. LOW VALUE
Use conservative low-value signals. Never equate “unread” or “old” alone with low value.

## 23. CONFLICTING SIGNALS
Create deterministic conflict resolution for messages containing multiple strong signals. Record why the winning category was selected.

## 24. NEGATIVE SIGNALS
Support negative/exclusion signals where necessary, such as transactional evidence overriding promotional wording.

## 25. SCORING
If scoring is used, define bounded deterministic weights/thresholds in code/configuration. Do not create opaque learned weights.

## 26. PRECEDENCE VS SCORE
Document when explicit precedence overrides numeric scoring and why.

## 27. UNKNOWN STATE
Allow unknown/uncertain outcomes when evidence is insufficient. Do not force every message into a confident label.

## 28. VERSIONING
Version the classification rules. Persist the rule/classifier version with derived decisions.

## 29. EXPLANATIONS
Persist concise, structured reasons/signals for decisions. Do not store unnecessary full-body excerpts merely to explain classification.

## 30. PROVENANCE
Keep enough provenance to answer which rule/signal produced a classification.

## 31. REPROCESSING
Classification must be safely rerunnable when the rules/version changes.

## 32. IDEMPOTENCY
Repeated classification must not duplicate records or create contradictory state.

## 33. USER CORRECTION BOUNDARY
Reserve a correction/override mechanism for later Phase 12. Do not build the full rule editor here, but do not make future corrections impossible.

## 34. ACCOUNT ISOLATION
Never classify data across accounts or allow one account's metadata to influence another account.

## 35. THREAD VS MESSAGE
Document whether category is message-level, thread-level or derived for display. Preserve Gmail message/thread identity.

## 36. TEMPORAL SIGNALS
Use dates/recency only as deterministic supporting signals. Do not implement meeting/deadline extraction from Phase 13.

## 37. GMAIL LABELS
Use available trusted Gmail labels/categories as inputs where appropriate, without implementing Gmail writes.

## 38. TEST FIXTURES
Create synthetic examples for every category and ambiguous/conflicting combinations.

## 39. ADVERSARIAL FIXTURES
Test misleading subjects, spoof-like display names, mixed promotional/transactional wording, empty bodies and conflicting labels.

## 40. DETERMINISM TESTS
Identical normalized input and classifier version must produce identical output.

## 41. PRECEDENCE TESTS
Prove category precedence with explicit pairwise conflict tests.

## 42. EXPLANATION TESTS
Every non-unknown classification must expose stable provenance/reasons.

## 43. UNKNOWN TESTS
Verify weak evidence can remain unknown rather than producing false certainty.

## 44. VERSION TESTS
Verify changing classifier version permits controlled reprocessing without corrupting old provenance.

## 45. ACCOUNT TESTS
Prove account A classification cannot consume account B state.

## 46. DATABASE TESTS
Verify transactional persistence of derived classification and version/provenance.

## 47. PERFORMANCE TESTS
Process realistic synthetic batches without blocking the Android UI thread or allocating unbounded structures.

## 48. RUNTIME UI INTEGRATION
Expose classification state through existing local data boundaries only where Phase 6 UI needs it. Do not redesign the dashboard yet.

## 49. VISUAL QA
Where categories are shown, verify category labels, empty/error states, light/dark, large text and accessibility. Do not rely on color alone.

## 50. LOGGING
Never log complete email bodies or sensitive account data. Log rule/version/error categories safely.

## 51. DEVICE VALIDATION
Build, install only Mail Organizer, launch, load synthetic/local data, inspect category behavior, force-stop/relaunch and inspect logcat.

## 52. ADB SAFETY
Use package-scoped ADB. Do not uninstall/clear unrelated apps or modify unrelated reverse mappings.

## 53. FINAL BUILD
Run Gradle compilation, shared tests, unit/database tests, relevant Android tests and configured static checks.

## 54. FINAL RETEST
Fix phase-caused defects, rebuild, reinstall and repeat tests/device QA.

## 55. GIT REVIEW
Verify only Mail Organizer files changed and no secrets/generated artifacts were introduced.

## 56. DOCUMENTATION
Update spec.md/status with classifier architecture, precedence, rule versioning and limitations only after real verification.

## 57. EDITOR RULES
Add only permanent deterministic-classification rules genuinely discovered.

## 58. ACCEPTANCE CRITERIA
- all required categories are represented;
- deterministic precedence exists;
- unknown/uncertain state exists;
- explanations/provenance exist;
- versioning exists;
- reruns are safe;
- adversarial/conflict tests pass;
- account isolation passes;
- no remote AI is used;
- no later phase implemented.

## 59. FINAL REPORT
Report classifier architecture, category precedence, signals, versioning, tests, device/API, security, workspace isolation, files changed, known issues, deferred work and acceptance status.

## 60. STOP
Do not execute Phase 8.
