# PHASE 5 — EMAIL DATA MODEL & PARSING

## EXECUTION CONTRACT

Confirm Phase 5 is first incomplete and Phase 4 is verified. Read all rules. Implement normalization/parsing/security only. Do not implement classification, company intelligence, search, Calendar, Tasks, Gmail writes, AI or automation.

## 1. DISCOVER ROOT
Locate instructions and Mail Organizer root before tooling.

## 2. CONFIRM PHASE 4
Verify synchronization foundation is complete.

## 3. WORKSPACE ISOLATION
Protect sibling projects and unrelated SDK/Google resources.

## 4. GIT BASELINE
Record status/diff.

## 5. THREE-LAYER MODEL
Separate Gmail transport models, normalized domain models and derived intelligence.

## 6. MESSAGE IDENTITY
Preserve Gmail message/thread/account identity.

## 7. SENDER NORMALIZATION
Normalize display name, address and domain safely.

## 8. RECIPIENT NORMALIZATION
Support To/Cc/Bcc/Reply-To roles where available.

## 9. HEADER DECODING
Handle encoded headers, Unicode and malformed-but-tolerable values.

## 10. SUBJECT NORMALIZATION
Normalize whitespace and reply/forward prefixes without destroying original display value.

## 11. MIME FOUNDATION
Support plain, HTML and multipart MIME structures.

## 12. NESTED MIME
Handle nested multipart structures deterministically.

## 13. HTML EXTRACTION
Choose safe displayable HTML content.

## 14. HTML SANITIZATION
Treat email HTML as hostile input. Remove scripts and active/dangerous content.

## 15. URL SAFETY
Parse links as data. Block javascript/unsafe schemes and automatic navigation.

## 16. PLAIN TEXT FALLBACK
Use readable plain text when HTML is absent or unsafe.

## 17. PREVIEW
Generate deterministic bounded previews from visible safe content.

## 18. WHITESPACE
Normalize excessive whitespace without destroying meaningful formatting.

## 19. QUOTED REPLIES
Detect common quoted sections conservatively.

## 20. SIGNATURES
Detect common signatures without deleting content when confidence is low.

## 21. FORWARDED CONTENT
Handle forwarded sections without corrupting original message representation.

## 22. ATTACHMENT METADATA
Persist filename/type/size/reference/disposition only as needed.

## 23. NO AUTO-DOWNLOAD
Parsing must never download attachment binaries automatically.

## 24. UNSUBSCRIBE METADATA
Detect standard unsubscribe metadata for later UX only.

## 25. NO AUTO-UNSUBSCRIBE
Never execute an unsubscribe action based solely on email content.

## 26. DATE NORMALIZATION
Preserve the original instant in a stable data-layer representation.

## 27. LOCALE BOUNDARY
Do not format timestamps using device locale in the data layer.

## 28. MALFORMED INPUT
Return safe fallback/error representations rather than crashing.

## 29. PARSE PROVENANCE
Record parser/version/source/failure metadata only where useful and privacy-safe.

## 30. ACCOUNT PRESERVATION
Never lose account ownership during normalization.

## 31. RERUN SAFETY
Parsing the same message repeatedly must be deterministic and non-corrupting.

## 32. DATABASE INTEGRATION
Use Phase 2 persistence. Do not create a second DB.

## 33. SYNCHRONIZATION BOUNDARY
Integrate with Phase 4 output without adding a new sync engine.

## 34. SECURITY FIXTURES
Create synthetic malicious HTML, unsafe URLs, malformed MIME and suspicious headers.

## 35. PARSING FIXTURES
Create plain, HTML, multipart, nested, Unicode, missing-field and attachment fixtures.

## 36. DETERMINISM TESTS
Same input must produce the same normalized output.

## 37. SAFETY TESTS
Prove scripts do not execute and unsafe links do not become active commands.

## 38. PREVIEW TESTS
Prove previews are bounded, visible-content based and free of raw HTML.

## 39. ATTACHMENT TESTS
Prove metadata extraction does not trigger binary downloads.

## 40. RUNTIME TEST
Open representative synthetic messages on Android.

## 41. LOGCAT TEST
Confirm parsing diagnostics do not leak email bodies, tokens or secrets.

## 42. VISUAL TEST
Verify sanitized content is readable in light/dark and large-text modes.

## 43. PERFORMANCE
Avoid expensive parsing on the UI thread and avoid repeated parsing of unchanged content.

## 44. DEVICE VALIDATION
Install Mail Organizer only, render representative messages, inspect logs/screenshots/screenrecord where useful.

## 45. FINAL BUILD
Run Gradle/shared/unit/UI/static checks configured by the repository.

## 46. FINAL SECURITY REVIEW
Verify no JS, unsafe navigation, secret leakage or automatic external action.

## 47. GIT REVIEW
Confirm only Mail Organizer files changed.

## 48. DOCUMENTATION
Update spec/status with parser boundaries, sanitization policy and known limitations.

## 49. EDITOR RULES
Add permanent email-content security rules only when genuinely new.

## 50. ACCEPTANCE CRITERIA
- transport/domain separation;
- sender/recipient/subject normalization;
- MIME support;
- safe HTML;
- plain-text fallback;
- deterministic previews;
- attachment metadata only;
- unsubscribe metadata only;
- timestamps normalized;
- malformed input safe;
- deterministic/security tests pass;
- device validation completed;
- no later intelligence/functionality implemented.

## 51. FINAL REPORT
Report parser architecture, security behavior, tests, device/API, logs/screenshots, files, issues, deferred work and acceptance status.

## 52. STOP
Do not execute Phase 6.
