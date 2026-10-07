# Phase 12 — Rules & User Corrections

## Mission

Implement the **user-controlled Rules & Corrections system** for Mail Organizer.

This phase establishes the layer that allows users to correct or override the deterministic intelligence implemented in Phases 7–9 and Phase 8 sender/company intelligence.

The user must remain the final authority over how their mail is categorized, prioritized, and interpreted.

This phase must be implemented as a **local-first, deterministic, explainable, account-safe rule system**.

Do not implement unrelated future phases.

---

# 1. Mandatory Instruction-Folder Discovery

Before making any change:

1. Identify the **Mail Organizer project root**.
2. Locate the project's instruction/documentation folder.
3. Read:
    - `requirements.md`
    - `spec.md`
    - `design.md`
    - `editor-rules.md`
    - the Phase 12 prompt
    - relevant development-status documentation if present
4. Inspect the actual repository state.
5. Confirm that Phase 11 has been completed and verified.
6. Determine the actual architecture, package structure, database implementation, navigation system, UI framework, and existing intelligence pipeline from the repository rather than assuming them.

Do not blindly recreate architecture described in this prompt if the existing implementation already provides an equivalent, well-designed solution.

Preserve working functionality from Phases 1–11.

---

# 2. Sequential Phase Rule

This project is executed strictly phase-by-phase.

For this session:

> **Implement Phase 12 only.**

Do not begin Phase 13 or any later phase.

Do not implement:

- deadline/meeting extraction
- Calendar integration
- Tasks integration
- action execution
- Gmail write operations
- cleanup automation
- newsletter unsubscribe automation
- AI classification
- analytics
- production release work

Those belong to later phases.

If you discover future requirements while implementing Phase 12, document them appropriately, but do not implement them prematurely.

At the end of Phase 12:

> Build → install → run → test → inspect → fix → rebuild → reinstall → retest → review → update documentation → stop.

---

# 3. Permanent Multi-Project Isolation

Multiple Android projects may exist in the same parent directory/workspace.

Before editing anything, explicitly identify the Mail Organizer project root.

You must never modify another project's:

- source code
- Kotlin/Java files
- Compose/XML layouts
- Gradle files
- Gradle wrapper
- settings
- dependencies
- manifests
- resources
- assets
- tests
- signing configuration
- package/application IDs
- SDK configuration
- generated files
- Git repository
- branches
- commits
- documentation

Never build, clean, install, uninstall, or test another project.

Never change global:

- Android SDK configuration
- JDK configuration
- Gradle configuration
- environment configuration

merely to make Mail Organizer work.

If a global environment issue exists, solve it in a project-local way whenever technically possible.

---

# 4. ADB and Device Isolation

Use Android tooling fully during this phase.

When relevant, use:

- project Gradle wrapper
- ADB
- device/emulator inspection
- APK installation
- application launch
- force-stop/relaunch
- logcat
- screenshots
- screen recording
- dumpsys
- package inspection
- database inspection tools if available

Every ADB operation must target **only the Mail Organizer package/application**.

Never uninstall or manipulate another application's package.

If `adb reverse` is required:

- use it only when genuinely necessary
- scope it to Mail Organizer's development requirements
- do not disturb mappings used by another project
- inspect existing mappings before changing them when necessary

---

# 5. Phase 12 Objective

Create a robust system where users can correct Mail Organizer's intelligence and create persistent rules.

The conceptual pipeline should become:

```text
Gmail data
   ↓
Synchronization
   ↓
Parsing / Normalization
   ↓
Sender / Company Intelligence
   ↓
Deterministic Classification
   ↓
Deterministic Priority
   ↓
Action Required Detection
   ↓
User Rules & Corrections
   ↓
Effective Result
   ↓
UI / Search / Dashboard / Actions Views
```

The crucial distinction is:

```text
Built-in intelligence = system recommendation
User rule/correction = user authority
Effective result = what Mail Organizer actually presents
```

A user's explicit correction must not be silently overwritten by a future sync or reclassification.

---

# 6. Rule Engine Must Be Separate From Classification

Do not turn the existing classifier into a giant rule system.

Keep these concerns independently testable:

```text
Classification Engine
        ↓
Base Classification Result

Priority Engine
        ↓
Base Priority Result

Action Required Engine
        ↓
Base Action Result

Sender / Company Intelligence
        ↓
Base Identity Result

User Rule Engine
        ↓
Effective User-Controlled Result
```

The user rule system should consume the outputs of the existing intelligence systems and apply explicit user-defined overrides.

Do not duplicate Phase 7–9 business logic inside the rule engine.

---

# 7. Precedence Model

Establish and document a deterministic precedence hierarchy.

The default hierarchy should be:

```text
Explicit user correction / override
        ↓
Enabled user-defined rule
        ↓
Built-in deterministic intelligence
        ↓
Unknown / unresolved
```

If the existing architecture requires a different hierarchy, choose the safest equivalent and document it clearly.

The system must never produce ambiguous results simply because multiple rules match.

Every conflict must have a deterministic resolution strategy.

Possible resolution mechanisms include:

- explicit priority
- rule ordering
- specificity
- newest explicit correction
- narrower scope

Do not rely on accidental database ordering.

---

# 8. User Corrections

Users must be able to correct intelligence directly from the application.

At minimum, support correction of:

### Category

Example:

```text
This email was classified as Promotions.

Correct category:
Career
```

### Priority

Example:

```text
This email is currently Medium priority.

Change to:
High
```

### Action Required

Allow the user to explicitly indicate:

```text
Action Required
No Action Required
```

Do not force a false answer when the user intentionally leaves the state unresolved.

### Sender/company intelligence

Where appropriate, allow users to correct sender/company interpretation.

Examples:

```text
This sender belongs to Company X.
```

or:

```text
Do not associate this sender with Company X.
```

Do not allow the UI to become unnecessarily complex.

Use progressive disclosure.

---

# 9. Rule Creation

Users should be able to create persistent rules from corrections.

Examples:

```text
Always classify emails from example.com as Career.
```

```text
Always classify emails from recruiter@example.com as Career.
```

```text
Always mark emails from this sender as High priority.
```

```text
Never mark this sender as Action Required.
```

```text
Emails from this company should be treated as Important.
```

Rules should operate on structured conditions rather than arbitrary executable code.

---

# 10. Supported Rule Conditions

Implement a practical initial rule vocabulary.

Where supported by the existing data model, rules may match:

### Sender

- exact sender email
- sender domain

### Company

- company identity

### Existing category

- category

### Subject

- subject keyword
- subject phrase
- structured subject condition

### Gmail metadata

- Gmail labels
- Gmail category

### Priority

- current/base priority

### Action Required

- current/base action state

### Email metadata

- attachment presence
- unsubscribe information
- recipient/account context
- sender display name where appropriate

Do not add an enormous rule language.

Prefer structured fields such as:

```text
conditionType
operator
value
```

rather than allowing arbitrary code.

---

# 11. Rule Actions

The initial rule actions should remain within the intelligence/organization layer.

Allowed examples:

```text
Set category
Set priority
Set Action Required
Set sender/company mapping
Create an explicit classification override
```

Do not use this phase to execute external actions.

Do not:

- send email
- archive Gmail messages
- delete Gmail messages
- apply Gmail labels remotely
- unsubscribe from newsletters
- create Calendar events
- create Tasks
- modify Gmail state remotely

Those belong to later phases.

---

# 12. Account Scoping

Rules must respect Mail Organizer's multi-account architecture.

Every user-specific rule must have a clearly defined scope.

Support at least:

```text
Account-specific rule
```

For example:

```text
Account A:
example.com → Career
```

must not automatically affect:

```text
Account B
```

unless the product explicitly defines a global rule.

If global rules are supported, their semantics must be explicit.

Never accidentally allow:

```text
Account A correction
        ↓
Account B classification
```

User corrections must remain account-safe.

---

# 13. Rule Data Model

Design an appropriate persistent representation.

A rule should contain enough information to determine:

- rule ID
- account scope
- enabled/disabled state
- rule name/description
- condition set
- action set
- precedence/order
- creation timestamp
- modification timestamp
- rule version
- source
- optional correction reference
- execution/explanation metadata where useful

Avoid storing arbitrary executable expressions.

Prefer serializable structured conditions.

Example conceptual model:

```text
Rule
 ├── id
 ├── accountId
 ├── name
 ├── enabled
 ├── priority/order
 ├── conditions[]
 ├── actions[]
 ├── createdAt
 ├── updatedAt
 └── version
```

Use the project's existing database conventions.

Do not introduce a second database.

---

# 14. Correction Data Model

User corrections may need to be represented separately from reusable rules.

Keep these concepts distinct:

```text
One-time correction
```

versus:

```text
Persistent rule
```

For example:

```text
This particular message is Career.
```

does not necessarily mean:

```text
All messages from this sender are Career.
```

The user must be able to make that distinction.

If the existing architecture supports explicit overrides, implement them without corrupting the original classifier result.

Preserve:

```text
base result
user override
effective result
```

rather than replacing the base result irreversibly.

---

# 15. Explainability

Every effective classification/priority/action result should remain explainable.

For example:

```text
Category: Career

Why:
• Sender domain matched "company.com"
• Built-in Career rule matched

User override:
• You previously marked this sender as Career
```

Or:

```text
Priority: High

Why:
• Base priority: Medium
• User rule "Recruiters are high priority" raised it to High
```

The system should distinguish:

```text
Built-in reason
User rule reason
Explicit user correction
```

Do not expose internal implementation details that confuse normal users.

Use human-readable explanations.

---

# 16. Correction UI

Integrate correction controls into the existing product without redesigning the entire application.

Appropriate locations include:

- email detail
- thread detail
- category information
- priority indicator
- Action Required indicator
- sender/company information

For example:

```text
Category
Career
   ↓
Why?
   ↓
Correct
```

Possible correction interaction:

```text
Correct category
 ├── Action Required
 ├── Important
 ├── Career
 ├── Education
 ├── Receipts & Orders
 ├── Security
 ├── Notifications
 ├── Newsletters
 ├── Promotions
 └── Low Value
```

After correction, clearly show that the displayed result is user-controlled.

---

# 17. "Why Is This Classified This Way?"

Where practical, expose an explanation surface.

Example:

```text
Why Career?

Built-in signals:
• Sender domain matches a known career-related company
• Subject contains recruitment-related terms

Your rules:
• You marked this sender as Career
```

This should reinforce trust.

Do not create a chatbot or AI explanation system.

Explanations should be generated from structured rule/intelligence metadata.

---

# 18. Rules Management Screen

Add a dedicated Rules area within the existing information architecture.

Follow `design.md`.

Possible structure:

```text
Rules

[ + Create Rule ]

Active
────────────────────
Recruiters → Career
Enabled

GitHub notifications → Notifications
Enabled

example.com → High Priority
Enabled

Disabled
────────────────────
...
```

Users should be able to:

- create
- view
- edit
- enable
- disable
- delete
- reorder where applicable

rules.

Use confirmation for destructive deletion where appropriate.

---

# 19. Rule Details

A rule detail/edit screen should clearly show:

```text
Rule name

When:
    Sender domain
    equals
    example.com

Then:
    Category
    = Career

Status:
    Enabled

Priority:
    ...
```

Avoid requiring users to understand programming concepts.

The UI should communicate rules in natural language where practical.

---

# 20. Rule Conflicts

The system must detect or deterministically resolve conflicts.

Example:

```text
Rule A:
example.com → Career

Rule B:
example.com → Education
```

Do not silently choose based on implementation order.

Provide an understandable mechanism such as:

```text
Rule priority
```

or:

```text
More specific rule wins
```

or:

```text
Explicit ordering
```

Document the selected mechanism.

If a conflict is dangerous or genuinely ambiguous, surface it to the user instead of guessing.

---

# 21. Rule Ordering

If ordering is supported, make it deterministic.

Example:

```text
1. Recruiter exact sender → Career
2. Company domain → Important
3. General domain → Low Value
```

A specific rule should be able to override a broad rule when the architecture supports that behavior.

Do not depend on:

- insertion order
- database row order
- object iteration order
- hash ordering

---

# 22. Rule Preview / Testing

Where practical, provide a rule preview before enabling it.

Example:

```text
This rule matches 43 local messages.

Preview:
• recruiter@example.com
• jobs@example.com
• hiring@example.com
...
```

If a full preview UI is too large for the existing architecture, implement the underlying preview/test capability and expose an appropriately scoped UI.

Preview must operate only on locally available Mail Organizer data.

Do not perform a Gmail live search merely to preview a rule.

---

# 23. Safe Rule Reprocessing

When a rule is created, edited, enabled, disabled, or removed, affected local messages may need re-evaluation.

Implement a safe reprocessing strategy.

Conceptually:

```text
Rule changed
    ↓
Determine affected local data
    ↓
Re-evaluate
    ↓
Apply effective result
    ↓
Update indexes
    ↓
Refresh UI
```

Do not blindly reprocess the entire mailbox on every small change if avoidable.

If full reprocessing is necessary, perform it in a controlled/background manner.

Do not block the main UI thread.

---

# 24. Idempotency

Rule evaluation must be idempotent.

Running:

```text
applyRules(message)
```

once or multiple times with the same:

- message
- account
- rule state
- rule version

must produce the same effective result.

Do not repeatedly mutate state in ways that compound over time.

Avoid:

```text
High → Critical → Critical → Critical
```

because a rule was repeatedly applied.

Represent the desired effective state rather than applying uncontrolled deltas.

---

# 25. Sync Safety

Gmail synchronization must never erase user intelligence.

A future sync may update:

- subject
- sender
- labels
- body
- Gmail category
- timestamps
- message metadata

but it must not silently remove:

- user corrections
- active rules
- rule priority
- explicit overrides

The processing pipeline should re-evaluate the effective result after synchronization while preserving user authority.

---

# 26. Rule Versioning

Rules and corrections should have enough version information to support future changes.

At minimum, consider:

- rule schema version
- engine/rule evaluation version
- correction version

Do not make future migrations impossible by storing opaque data.

If the database requires migrations, implement and test them properly.

Never destroy existing user rules during migration.

---

# 27. Undo / Revert

User corrections should be reversible.

Support appropriate mechanisms such as:

```text
Undo correction
```

or:

```text
Remove override
```

Removing an override should return the message to the underlying deterministic result.

Example:

```text
Before:
Base: Promotions
User override: Career
Effective: Career

Remove override

Base: Promotions
User override: none
Effective: Promotions
```

Do not permanently replace the base classification with the override.

---

# 28. Safe Regex / Pattern Handling

If regex or advanced patterns are introduced:

- do not allow arbitrary executable code
- validate patterns
- prevent malformed expressions from crashing evaluation
- protect against pathological patterns where possible
- avoid catastrophic backtracking risks
- impose reasonable limits
- treat user-provided patterns as untrusted input

However:

> Prefer structured conditions and simple operators over unrestricted regex.

Regex should not be necessary for ordinary rule creation.

---