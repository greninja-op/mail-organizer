# Phase 15 — Categories, Company Intelligence & Company Workspace

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is iPadOS only. All iPadOS-specific artifacts belong under iPadOS/. Never touch iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Shared KMP business/data logic remains common.

## MISSION

Expose deterministic category/company intelligence in the iPad workspace. Implement All Inbox, Primary, Promotional, Social, Spam and Starred views using shared classification. Build a company context/filter area that remains within the selected category. Company pinning moves companies to the top of the company filter and is distinct from email starring. Verify account-scoped company grouping, unknown senders, ambiguous domains, classification explanations, category transitions and selection preservation across list/detail columns. Test adversarial sender metadata and cross-account leakage.

## ACCEPTANCE / VALIDATION

Inspect actual repository and prior evidence. Implement only this phase. Run targeted unit/integration tests plus iPad Simulator and real iPad validation where available. Test multiple viewport modes, accessibility and relevant failure/recovery paths. Fix, rebuild/reinstall/retest and record exact evidence. Update status/rules only when genuinely required.

**STOP AFTER THIS PHASE.**