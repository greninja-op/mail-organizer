# PHASE 8 — COMPANY & SENDER INTELLIGENCE

## EXECUTION CONTRACT

Confirm Phase 8 is first incomplete and Phase 7 is verified. Read all source-of-truth documents. Implement sender/company intelligence only. Do not implement search, Calendar, Tasks, Gmail writes, AI, automation or background sync.

## 1. DISCOVER ROOT
Locate instructions and Mail Organizer root before tooling.

## 2. CONFIRM PHASE 7
Stop if deterministic classification is not verified.

## 3. WORKSPACE ISOLATION
Protect sibling projects, SDKs, device apps and unrelated Google Cloud resources.

## 4. GIT BASELINE
Inspect status/diff and preserve unrelated work.

## 5. INTELLIGENCE OBJECTIVE
Resolve sender identity and company identity separately and deterministically where possible.

## 6. SENDER MODEL
Normalize display name, mailbox, domain, reply-to and relevant headers from Phase 5.

## 7. EMAIL ADDRESS NORMALIZATION
Normalize case/whitespace safely without changing meaningful mailbox identity.

## 8. DOMAIN NORMALIZATION
Normalize domains, subdomains and public suffix boundaries carefully.

## 9. CONSUMER PROVIDERS
Do not treat gmail.com, outlook.com, yahoo.com and similar consumer domains as companies merely because they are domains.

## 10. SUBDOMAIN HANDLING
Distinguish company-owned subdomains from unrelated hosted infrastructure where evidence permits.

## 11. SPOOFING RESISTANCE
Do not trust display names alone. Prefer stronger domain/address/header evidence.

## 12. DISPLAY NAME SIGNALS
Use sender names only as supporting evidence.

## 13. DOMAIN SIGNALS
Use sender domain as a strong signal when the domain clearly represents an organization.

## 14. REPLY-TO SIGNALS
Treat Reply-To as supporting evidence and document conflicts with From.

## 15. AUTHENTICITY BOUNDARY
Do not claim cryptographic sender verification unless the trusted data actually supports it.

## 16. COMPANY ENTITY
Create/maintain normalized company identity separate from sender identity.

## 17. COMPANY NAME
Produce a stable display name without destroying the underlying domain/address provenance.

## 18. COMPANY DOMAIN
Associate verified/strong domains with a company while allowing multiple domains where justified.

## 19. ALIASES
Support deterministic aliases/known domain relationships without uncontrolled web scraping.

## 20. CONFIDENCE
Represent strong/medium/weak/unknown company resolution.

## 21. EXPLANATION
Store why a company was resolved and which signals were used.

## 22. PROVENANCE
Keep resolution version and source/provenance metadata.

## 23. NO ARBITRARY LOGO FETCHING
Do not fetch random remote logos/avatars merely to decorate the UI.

## 24. NO REMOTE ENRICHMENT
Do not introduce a third-party enrichment service unless explicitly required by the product and approved in a later phase.

## 25. COMPANY PIN FOUNDATION
Persist the distinction between company identity and future user company-pinning intent. Do not turn companies into drawer destinations.

## 26. SENDER GROUPING
Allow messages to resolve to sender/company groups without losing individual sender identity.

## 27. CATEGORY CONTEXT
Company identity must remain usable as a filter inside a selected category.

## 28. ACCOUNT ISOLATION
Do not let one account's private sender corrections or local identities silently contaminate another account.

## 29. DUPLICATES
Prevent duplicate company entities caused by repeated sync/parsing.

## 30. MERGE BOUNDARY
Do not perform irreversible company merges without deterministic evidence and safe provenance.

## 31. UNKNOWN HANDLING
Allow unknown company when evidence is weak.

## 32. CONSUMER MAIL HANDLING
Personal senders from consumer providers remain people/senders, not fake companies.

## 33. MALFORMED ADDRESS HANDLING
Never crash on malformed but parseable addresses.

## 34. HEADER CONFLICTS
Test From/Reply-To/display-name/domain conflicts.

## 35. INTERNATIONALIZATION
Support Unicode names and internationalized domains where platform parsing permits.

## 36. TEST FIXTURES
Create fixtures for corporate domains, subdomains, consumer providers, aliases, malformed addresses and spoof-like names.

## 37. RESOLUTION TESTS
Test strong/medium/weak/unknown outcomes and stable explanations.

## 38. ACCOUNT TESTS
Verify account A/B sender/company state remains isolated.

## 39. IDEMPOTENCY TESTS
Repeat resolution and confirm stable entities rather than duplicates.

## 40. VERSION TESTS
Verify resolver version/provenance is persisted.

## 41. SECURITY TESTS
Prove display-name spoofing cannot override stronger address/domain evidence.

## 42. PERFORMANCE
Resolve company/sender intelligence in bounded batches, not inside Compose rendering.

## 43. DATABASE
Use Phase 2 persistence. Do not create a second database.

## 44. UI INTEGRATION
Expose only the company/sender data required by existing category/company filter boundaries. Do not build the full dashboard.

## 45. VISUAL QA
Verify company labels/grouping, account identity, empty/unknown states, light/dark and accessibility.

## 46. DEVICE VALIDATION
Build/install Mail Organizer only, inspect representative company groups, force-stop/relaunch and inspect logcat.

## 47. LOGGING
Never log complete private message bodies or sensitive account content.

## 48. FINAL BUILD
Run Gradle, shared/unit/database/Android tests and configured static checks.

## 49. FINAL RETEST
Fix, rebuild, reinstall and retest.

## 50. GIT REVIEW
Confirm only Mail Organizer changed.

## 51. DOCUMENTATION
Update spec/status with company-resolution rules, provenance and known limitations.

## 52. EDITOR RULES
Add only permanent sender/company intelligence rules genuinely discovered.

## 53. ACCEPTANCE CRITERIA
- sender/company distinction exists;
- domain normalization exists;
- consumer providers handled;
- spoof-resistant resolution exists;
- confidence/provenance exists;
- unknown state exists;
- company identity is category-filterable;
- account isolation passes;
- no remote enrichment/logo fetching;
- tests/device validation pass.

## 54. FINAL REPORT
Report normalization, resolution rules, confidence, tests, device/API, security, workspace isolation, files, issues and deferred work.

## 55. STOP
Do not execute Phase 9.
