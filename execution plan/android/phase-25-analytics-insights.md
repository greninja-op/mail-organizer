# Phase 25 — Analytics & Insights

## Mission

Implement Mail Organizer's **local analytics and insights layer**.

The purpose of this phase is to help the user understand their email workload, organization patterns, attention demands, and progress without turning Mail Organizer into a generic analytics dashboard.

The analytics system must answer questions such as:

- What requires my attention?
- Which categories dominate my inbox?
- Which senders or companies generate the most mail?
- How much of my inbox is actionable?
- How much promotional/newsletter/noise mail am I receiving?
- Are unresolved conversations accumulating?
- Are deadlines or meetings increasing?
- How is my inbox changing over time?
- Which organization rules or corrections are having an effect?

The core product principle remains:

> **Analytics should help the user understand and act on their email—not encourage them to chase arbitrary metrics.**

---

# 1. Mandatory Instruction-Folder Discovery

Before changing anything:

1. Identify the **Mail Organizer project root**.
2. Locate the instruction/documentation folder.
3. Read:
    - `requirements.md`
    - `spec.md`
    - `design.md`
    - `editor-rules.md`
    - this Phase 25 prompt
    - `docs/development-status.md` if available
4. Verify Phase 24 is genuinely complete.
5. Inspect the actual current implementation of:
    - local database
    - normalized email data
    - categories
    - sender/company intelligence
    - priority
    - Action Required
    - rules/corrections
    - temporal intelligence
    - action engine
    - conversation intelligence
    - cleanup/noise intelligence
    - multi-account/unified inbox
    - search/index
    - background sync
    - integrations
    - Privacy Center
    - performance architecture

Do not invent analytics based on features that do not actually exist.

---

# 2. Strict Sequential Execution

This session is **Phase 25 only**.

Do not implement:

- Phase 26 Optional AI
- Phase 27 Advanced Automation
- Phase 28 Full Testing & QA
- Phase 29 Production OAuth / Play Store Preparation
- Phase 30 Final Production Hardening

Do not introduce remote telemetry merely because this phase is called "Analytics".

The default analytics model is **local-first product insights**, not third-party user tracking.

Execution:

```text
Read docs
↓
Verify Phase 24
↓
Inspect existing data
↓
Define useful local metrics
↓
Design analytics data layer
↓
Implement aggregation
↓
Implement Insights UI
↓
Account/unified scoping
↓
Build
↓
Test
↓
Install
↓
Run on device
↓
Validate real data
↓
Stress-test analytics
↓
Check privacy/performance
↓
Fix
↓
Rebuild/reinstall/retest
↓
Update rules/spec/status
↓
STOP
```

---

# 3. Permanent Multi-Project Isolation

Multiple Android applications may exist in the same parent workspace.

Identify the Mail Organizer root before editing.

Never modify sibling projects.

Do not touch another project's:

- source
- Gradle files
- SDK configuration
- JDK configuration
- dependencies
- manifests
- resources
- tests
- assets
- signing
- Git repository
- generated files
- Google Cloud configuration
- OAuth configuration

Never build, clean, or install another project.

---

# 4. Android Tooling Requirement

Use:

- `./gradlew`
- ADB
- Android device/emulator
- install
- launch
- force-stop
- logcat
- dumpsys
- screenshots
- screen recording
- database inspection
- profiling/performance tools where useful

ADB operations must target only Mail Organizer.

---

# 5. Analytics Philosophy

Analytics must serve the user's email-management goals.

Do not create metrics merely because they are easy to calculate.

Avoid:

- vanity metrics
- gamification
- arbitrary scores
- meaningless streaks
- competitive comparisons
- addictive engagement loops
- notifications designed to make users repeatedly check statistics

The user should be able to understand:

> **What is happening in my email and what should I do about it?**

---

# 6. Local-First Analytics

By default:

```text
Gmail/local data
↓
local aggregation
↓
local insight model
↓
local UI
```

Do not send email-derived analytics to external servers.

Do not introduce third-party analytics SDKs for email-content analytics.

---

# 7. Product Analytics vs Telemetry

Clearly distinguish:

### Product Insights

Information shown to the user about their email.

Examples:

- category distribution
- action-required count
- response backlog
- newsletter volume
- deadline trends

### Developer Telemetry

Information sent externally about app usage.

Developer telemetry is **not automatically part of this phase**.

Do not implement external telemetry unless explicitly required by the actual product requirements.

---

# 8. Privacy Rule

Analytics must not weaken Phase 23 privacy guarantees.

Never send externally:

- email bodies
- subjects
- sender addresses
- recipient addresses
- message IDs
- account identifiers
- OAuth credentials
- Calendar/Tasks content

for analytics purposes.

---

# 9. Data Minimization

Use existing derived data wherever possible.

Do not duplicate entire email records into an analytics database merely to calculate counts.

Prefer aggregating from:

- category
- priority
- Action Required
- timestamps
- sender/company
- conversation state
- deadline/meeting state
- cleanup classification
- Gmail metadata already stored

---

# 10. Analytics Architecture

Use a dedicated analytics/insights layer.

Conceptually:

```text
Local Mail Data
       ↓
Analytics Repository
       ↓
Aggregation / Metric Engine
       ↓
Insight Generator
       ↓
Insight Models
       ↓
UI
```

Keep analytics separate from:

- Gmail synchronization
- classification
- rules
- action execution

---

# 11. Do Not Create a Second Classification Engine

Analytics may interpret existing results.

It must not independently decide:

- category
- priority
- Action Required
- sender/company

Reuse authoritative existing systems.

---

# 12. Do Not Create a Second Action Engine

Analytics may surface:

> "You have 12 unresolved conversations."

It must not independently create a second action-candidate system.

Reuse Phase 14/21 results.

---

# 13. Account Scoping

Every analytics query must support:

```text
accountId
```

At minimum.

Never aggregate multiple accounts accidentally.

---

# 14. Unified Analytics

A unified view may intentionally aggregate multiple accounts.

But every aggregate must retain:

- source account
- account count
- appropriate account scope

Where useful, allow:

```text
All accounts
Account A
Account B
Account C
```

---

# 15. Account Switch Behavior

When switching accounts:

- analytics must switch scope
- cached insight state must not leak
- charts/counts must refresh appropriately
- stale account data must not appear

---

# 16. Analytics Date Range

Provide useful date ranges.

At minimum consider:

- Today
- Last 7 days
- Last 30 days
- Custom range where practical

Do not make the interface unnecessarily complex.

---

# 17. Timezone

Analytics must respect the user's local timezone.

The existing temporal architecture uses Asia/Kolkata/IST testing where relevant.

Do not aggregate a message into the wrong day merely because UTC boundaries differ.

---

# 18. Historical Data

Analytics should work with available synchronized local history.

If insufficient data exists, clearly state:

> Not enough local data yet.

Do not fabricate historical values.

---

# 19. Initial Sync State

During initial synchronization:

- show analytics as unavailable/loading where appropriate
- do not show misleading zeroes
- do not claim the mailbox is empty

---

# 20. Offline Analytics

Analytics based on local data should continue working offline.

For example:

- category counts
- priority distribution
- local search
- action-required counts
- sender/company trends

must not require internet access when data is already cached.

---

# 21. Offline State

If a metric requires fresh Gmail data, make that explicit.

Example:

> Based on your last synchronized data.

Do not imply real-time accuracy when offline.

---

# 22. Core Metric Groups

Implement useful metrics from existing data.

Recommended groups:

### Inbox Health

- total synchronized messages
- unread
- important
- Action Required
- high/critical priority
- unresolved conversations

### Organization

- category distribution
- categorized vs unknown
- user-corrected items
- rule-driven items

### Sources

- top senders
- top companies
- top domains
- sender volume

### Noise

- newsletters
- promotions
- notifications
- low-value mail
- cleanup candidates

### Time

- messages received over time
- Action Required trend
- unresolved conversation trend
- deadline/meeting trend

---

# 23. Inbox Health

Create a meaningful inbox-health summary.

Possible model:

```text
Total Mail
Action Required
High Priority
Unread
Awaiting Reply
Noise
```

Do not compress these into a single arbitrary "health score" unless the score has a clear, explainable definition.

---

# 24. Avoid Arbitrary Scores

Do not create:

> "Inbox Health: 73/100"

unless there is a documented deterministic formula that genuinely helps the user.

Prefer transparent metrics.

---

# 25. Action Required Analytics

Show:

- current Action Required count
- trend over time
- priority breakdown
- category breakdown
- oldest unresolved item
- recent additions

Do not expose full sensitive content unnecessarily.

---

# 26. Priority Analytics

Show distribution:

```text
Critical
High
Medium
Low
```

Use the existing priority model.

Do not redefine priority in analytics.

---

# 27. Category Analytics

Show category distribution using the existing ten categories:

- Action Required
- Important
- Career
- Education
- Receipts & Orders
- Security
- Notifications
- Newsletters
- Promotions
- Low Value

Respect the design-system category colors.

---

# 28. Category Trends

Where sufficient history exists, show how category volume changes over time.

Avoid overly dense graphs.

---

# 29. Company Analytics

Show useful sender/company patterns.

Examples:

- highest-volume companies
- companies generating Action Required mail
- companies with unresolved conversations
- category distribution by company

Do not expose unnecessary personal profiling.

---

# 30. Sender Analytics

Possible metrics:

- most frequent senders
- senders with most Action Required mail
- senders awaiting response
- recurring senders

Keep it focused on email organization.

---

# 31. Domain Analytics

Domain-level aggregation can be useful.

Handle consumer providers carefully.

Do not imply that:

is a company.

Reuse Phase 8 company/domain semantics.

---

# 32. Newsletter Analytics

Show:

- newsletter volume
- newsletter trend
- top newsletter senders
- recurring newsletter domains
- potential cleanup candidates

Do not automatically unsubscribe.

---

# 33. Promotional Analytics

Show:

- promotion volume
- trend
- top sources

Do not imply promotional mail is automatically safe to delete.

---

# 34. Notification Analytics

Show:

- notification volume
- top notification sources
- trend

Do not confuse automated notifications with Action Required unless existing classification says so.

---

# 35. Low-Value Analytics

Show low-value mail conservatively.

Do not imply:

> Low Value = safe to delete.

Keep cleanup semantics from Phase 20.

---

# 36. Cleanup Analytics

If cleanup candidates exist, provide:

- candidate count
- category distribution
- top sources
- trend

Do not execute cleanup from the analytics screen.

---

# 37. Conversation Analytics

Reuse Phase 21.

Useful metrics:

- Awaiting User Reply
- Awaiting Other Party
- Recently Replied
- Stale Conversations
- Resolved

Avoid double-counting messages and threads.

Thread-level metrics must use thread identity.

---

# 38. Waiting-for-Reply

Show:

- number of conversations awaiting another party
- oldest waiting conversation
- trend
- priority breakdown

Do not automatically send follow-ups.

---

# 39. Deadline Analytics

Reuse Phase 13.

Show:

- upcoming deadlines
- overdue deadlines where applicable
- deadlines by category
- deadlines over time

Do not create a second deadline detector.

---

# 40. Meeting Analytics

Show:

- upcoming meetings detected from mail
- meeting volume
- interview/event patterns where meaningful

Do not turn this screen into a Calendar replacement.

---

# 41. Rule Analytics

Show useful information about user-created rules.

Examples:

- active rules
- messages affected by rules
- most-used rules
- rule corrections
- rule conflicts if available

Do not modify rules automatically.

---

# 42. User Correction Analytics

Show:

- number of user corrections
- categories most frequently corrected
- senders frequently corrected
- rules created from corrections where applicable

This can help the user improve organization.

---

# 43. Classification Confidence

Do not expose raw internal confidence scores unless useful.

If surfaced:

- explain what they mean
- avoid implying mathematical certainty

Example:

> Some messages could not be confidently categorized.

is preferable to:

> Classification confidence: 43%.

---

# 44. Unknown/Uncategorized Mail

Track messages that remain:

- unknown
- uncategorized
- low confidence

This is a useful signal for future rule improvements.

---

# 45. Data Coverage

Consider an "organization coverage" metric:

```text
categorized messages / eligible messages
```

If used, define it clearly.

Do not include unsupported messages in the denominator.

---

# 46. Explainability

Every major insight should be understandable.

For example:

> Promotions increased this week, mainly from 4 recurring senders.

The underlying source should be traceable to local data.

---

# 47. Insight Model

Create a structured model for generated insights.

Conceptually:

```text
Insight
├── id
├── accountId / scope
├── type
├── title
├── summary
├── severity/importance if justified
├── timeRange
├── metric references
├── source references
├── generatedAt
└── version
```

Avoid storing unnecessary email content.

---

# 48. Insight Types

Use a finite deterministic set.

Examples:

- ACTION_REQUIRED_SPIKE
- UNRESOLVED_CONVERSATION_GROWTH
- NEWSLETTER_INCREASE
- PROMOTION_INCREASE
- CATEGORY_SHIFT
- NEW_TOP_SENDER
- DEADLINE_CLUSTER
- MAILBOX_VOLUME_SPIKE
- ORGANIZATION_COVERAGE_CHANGE

Do not generate arbitrary text from uncontrolled email content.

---

# 49. Insight Generation

Insights should be deterministic and explainable.

Example:

```text
Metric
↓
threshold/comparison
↓
insight rule
↓
structured insight
```

Do not introduce LLM-generated analytics in Phase 25.

---

# 50. Insight Thresholds

Avoid noisy insights.

For example, do not report:

> "You received 2 more emails than yesterday."

unless that difference is genuinely meaningful.

Use conservative thresholds.

---

# 51. Trend Detection

Trend detection may use:

- absolute change
- percentage change
- moving averages
- comparison periods

Choose simple, explainable methods.

---

# 52. Small Dataset Handling

Do not generate misleading trends from tiny datasets.

Example:

```text
2 emails yesterday
4 emails today
```

should not necessarily become:

> "Your inbox volume doubled!"

---

# 53. Statistical Honesty

Avoid presenting weak evidence as a strong trend.

If insufficient data:

> Not enough history to identify a trend.

---

# 54. Comparison Periods

When comparing:

- last 7 days vs previous 7 days
- last 30 days vs previous 30 days

ensure periods have comparable coverage.

---

# 55. Sync Coverage

Do not compare two periods if one period has incomplete synchronization without indicating it.

---

# 56. Freshness

Display appropriate freshness information.

Example:

> Updated 12 minutes ago.

or:

> Based on your last sync.

Do not claim live data if not live.

---

# 57. Analytics Refresh

Analytics should update after:

- sync completion
- Gmail write affecting relevant state