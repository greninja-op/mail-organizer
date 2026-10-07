# PHASE 9 — CATEGORIES, PRIORITY & ACTION REQUIRED

## EXECUTION CONTRACT

Confirm Phase 9 is first incomplete and Phase 8 is verified. Read all project rules. Implement priority and Action Required intelligence only. Do not implement the action engine, Calendar, Tasks, Gmail writes, AI or automation.

## 1. DISCOVER ROOT
Locate instruction folder and Mail Organizer root before tooling.

## 2. CONFIRM PHASE 8
Stop if sender/company intelligence is incomplete.

## 3. WORKSPACE ISOLATION
Protect sibling projects and unrelated SDK/device/cloud configuration.

## 4. GIT BASELINE
Inspect status/diff.

## 5. PRIORITY OBJECTIVE
Implement deterministic Critical/High/Medium/Low priority independently from category.

## 6. ACTION OBJECTIVE
Implement conservative Action Required = YES/NO/UNKNOWN independently from category and priority.

## 7. SEPARATION
Do not collapse category, priority and action state into one score.

## 8. INPUT SIGNALS
Use deterministic signals from category, sender/company, urgency, deadlines where already available, security, user importance, recency and trusted Gmail importance.

## 9. GMAIL IMPORTANCE
Treat Gmail importance as supporting evidence, not absolute truth.

## 10. SECURITY PRIORITY
Security/account compromise signals may raise priority but must remain explainable.

## 11. URGENCY
Recognize explicit urgency conservatively. Do not infer urgency from exclamation marks alone.

## 12. USER IMPORTANCE
Preserve explicit user importance/star/corrections separately from derived priority.

## 13. RECENCY
Use recency as a bounded supporting signal, never as the sole reason for Critical.

## 14. DEADLINE BOUNDARY
Consume structured deadline information only if it already exists. Do not implement the Phase 13 extraction engine.

## 15. ACTION REQUIRED YES
Require meaningful evidence that the recipient is expected to respond, decide, complete, approve or act.

## 16. ACTION REQUIRED NO
Use strong evidence that no user action is expected, such as ordinary informational/promotional content.

## 17. ACTION REQUIRED UNKNOWN
Use UNKNOWN when intent cannot be established reliably.

## 18. NEGATIVE SIGNALS
Allow explicit evidence to reduce false positives.

## 19. CONFLICT RESOLUTION
Define deterministic precedence for conflicting signals.

## 20. PRIORITY THRESHOLDS
Document bounded scoring/thresholds if scoring is used.

## 21. CATEGORY INDEPENDENCE
A promotional message may still be high priority if evidence supports it; a Primary message is not automatically important.

## 22. EXPLANATIONS
Persist concise reasons/signals for priority and action decisions.

## 23. VERSIONING
Persist algorithm/rule version.

## 24. PROVENANCE
Preserve signal provenance without retaining unnecessary sensitive text.

## 25. RERUN SAFETY
Reprocessing must be deterministic and idempotent.

## 26. USER CORRECTION BOUNDARY
Do not implement the full rule/correction editor; preserve hooks for Phase 12.

## 27. ACCOUNT ISOLATION
All derived states remain account-scoped.

## 28. THREAD/MESSAGE SEMANTICS
Document whether action/priority is message-level or thread-level and avoid accidental cross-thread inheritance.

## 29. TEST MATRIX — PRIORITY
Test Critical/High/Medium/Low boundaries and near-threshold cases.

## 30. TEST MATRIX — ACTION
Test YES/NO/UNKNOWN, false-positive cases and ambiguous messages.

## 31. TEST MATRIX — SECURITY
Test security alerts against promotional/notification conflicts.

## 32. TEST MATRIX — GMAIL IMPORTANCE
Test important/unimportant Gmail metadata with contradictory local signals.

## 33. TEST MATRIX — RECENCY
Test old/new messages without allowing age alone to force a high priority.

## 34. TEST MATRIX — ACCOUNT
Prove Account A cannot influence Account B.

## 35. TEST MATRIX — VERSION
Verify versioned outputs can be recomputed without duplicate records.

## 36. DATABASE
Persist derived priority/action state through Phase 2 repositories.

## 37. UI BOUNDARY
Expose real priority/action state where the existing mailbox shell needs it. Do not redesign the dashboard.

## 38. ACCESSIBILITY
Do not use color alone for priority/action state. Provide text/icon/semantic alternatives.

## 39. VISUAL QA
Validate light/dark, large text, reduced motion, empty/unknown states and Mail Organizer visual language.

## 40. PERFORMANCE
Process in bounded batches off the UI thread.

## 41. DEVICE VALIDATION
Build/install only Mail Organizer, open representative messages/categories and verify derived states.

## 42. LOGCAT
Confirm no full bodies, tokens or sensitive derived payloads leak.

## 43. FINAL BUILD
Run Gradle, shared/unit/database/Android tests and static checks.

## 44. FINAL RETEST
Fix, rebuild, reinstall and repeat runtime/security tests.

## 45. GIT REVIEW
Confirm only Mail Organizer changed.

## 46. DOCUMENTATION
Update spec/status with priority/action model, thresholds, provenance and limitations.

## 47. EDITOR RULES
Add only permanent priority/action rules genuinely discovered.

## 48. ACCEPTANCE CRITERIA
- priority independent from category;
- Action Required independent from category;
- YES/NO/UNKNOWN supported;
- deterministic conflict resolution;
- explanations/versioning;
- account isolation;
- security/ambiguity tests;
- device validation;
- no action engine or external writes.

## 49. FINAL REPORT
Report models, signals, thresholds, tests, device/API, security, workspace isolation, files, issues, deferred work and acceptance status.

## 50. STOP
Do not execute Phase 10.
