# Phase 6 — iOS Classification, Company Intelligence & Categories

## MANDATORY PLATFORM ISOLATION — READ FIRST

Target: **iOS / iPhone only**. iOS-specific UI/config/tests/docs stay in `iOS/`. No sibling platform work.

## Mission

Expose the existing deterministic intelligence systems through an iPhone-native category and company experience.

## Scope

- category presentation
- deterministic classification results
- sender/company intelligence
- company grouping within a selected category
- company filter UI
- pinned companies at top of the category company filter
- distinction between company pin and email star
- Promotional/Social organization
- Spam presentation and recovery
- classification explanations where supported
- user correction entry points needed by later phases

Do not create an iOS-only classifier.

## Validation

Use synthetic/adversarial senders and malformed metadata. Verify unknown/ambiguous classification remains safe. Verify company grouping does not leak across Gmail accounts.

**STOP AFTER PHASE 6.**
