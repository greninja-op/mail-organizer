# Phase 21 — Waiting-for-Reply & Conversation Intelligence

## Mission

Implement Mail Organizer's **Waiting-for-Reply & Conversation Intelligence** system.

The goal is to understand the state of email conversations beyond individual messages, especially:

- conversations awaiting the user's reply
- conversations where the user has already replied
- conversations awaiting another person
- conversations with unresolved requests
- follow-up opportunities
- stale conversations
- conversation-level action context

This system must remain:

- local-first
- deterministic
- explainable
- account-scoped
- privacy-preserving
- conservative
- resistant to false positives
- compatible with existing classification/rules/priority/action systems

The central principle is:

> **Conversation intelligence explains what appears to be happening in a thread. It does not invent intent or automatically send messages.**

The system must never assume:

```text
No reply
=
user forgot
```

or:

```text
Question in email
=
user must reply
```

unless the available evidence supports that conclusion.

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
    - this Phase 21 prompt
    - `docs/development-status.md` if available
4. Verify Phase 20 is genuinely complete.
5. Inspect the actual implementations of:
    - Gmail sync
    - email data model
    - MIME/parser
    - thread model
    - sender/company intelligence
    - classification
    - priority
    - Action Required
    - rules/corrections
    - meeting/deadline extraction
    - Action Engine
    - Calendar
    - Tasks
    - Integration Manager
    - multi-account
    - background sync
    - offline behavior
    - cleanup/newsletter/noise system
    - search
    - dashboard
6. Determine how Gmail thread membership and message ordering are currently represented.

Do not assume a thread model exists simply because Gmail provides thread IDs.

---

# 2. Strict Sequential Execution

This session is **Phase 21 only**.

Do not implement:

- Phase 22 Gmail Modification & Optional Write Features
- Phase 23 Privacy Center & Security Hardening
- Phase 24 Performance & Battery Optimization
- Phase 25 Analytics & Insights
- Phase 26 Optional AI
- Phase 27 Advanced Automation
- Phase 28 Full Testing & QA as the dedicated final QA phase
- Phase 29 Production OAuth / Play Store Preparation
- Phase 30 Final Production Hardening

Phase 21 may prepare interfaces for future reply/follow-up actions but must not send or modify Gmail.

Execution:

```text
Read docs
↓
Verify Phase 20
↓
Inspect thread/message architecture
↓
Design conversation state model
↓
Implement deterministic conversation intelligence
↓
Build
↓
Test
↓
Install
↓
Run with realistic thread fixtures
↓
Visual QA
↓
Security/privacy review
↓
Fix
↓
Rebuild
↓
Reinstall
↓
Retest
↓
Update rules/spec/status
↓
STOP
```

---

# 3. Permanent Multi-Project Isolation

Multiple Android applications may exist in the same workspace.

Identify the Mail Organizer project root before editing.

Never modify sibling projects.

Do not touch another project's:

- source
- Gradle
- dependencies
- manifests
- resources
- assets
- tests
- SDK/JDK configuration
- signing configuration
- OAuth
- Google Cloud configuration
- Git repository
- generated files

Never build or clean another project.

All ADB operations must target only Mail Organizer.

---

# 4. Android Tooling Requirement

Use the real Android development workflow:

- project `./gradlew`
- ADB
- device/emulator
- install
- launch
- force-stop
- logcat
- screenshots
- screen recording
- dumpsys
- database inspection
- network-state inspection

Use `adb reverse` only if genuinely necessary.

---

# 5. Conversation vs Message

A core distinction must be established:

```text
Message
=
one email

Thread / Conversation
=
ordered set of related messages
```

Conversation intelligence operates primarily at the thread level while retaining message-level evidence.

Never destroy message-level traceability.

---

# 6. Gmail Thread Identity

Use the Gmail thread ID together with account identity.

Conceptually:

```text
accountId + threadId
```

is the safe conversation identity.

Do not assume:

is globally unique across accounts.

---

# 7. Conversation Model

Create or extend a structured conversation-intelligence model.

Potential fields:

```text
accountId
threadId
state
confidence
reason
lastMessageAt
lastUserMessageAt
lastOtherPartyMessageAt
pendingSince
staleSince
analysisVersion
updatedAt
```

Use only fields justified by the actual implementation.

Do not duplicate the entire thread.

---

# 8. Conversation States

Define a deterministic state model.

Possible states:

```text
NO_ACTION
AWAITING_USER_REPLY
AWAITING_OTHER_PARTY
RECENTLY_REPLIED
STALE_CONVERSATION
RESOLVED
UNKNOWN
```

The exact state names may differ if the architecture already has a better model.

Document the final state machine.

---

# 9. Do Not Overstate Certainty

Conversation state is inference.

Use language such as:

```text
Likely awaiting your reply
```

rather than:

when evidence is incomplete.

Confidence must be represented appropriately.

---

# 10. Participants

Determine message direction using available account identity.

For each message, distinguish:

- user/account sender
- other sender
- recipients
- CC
- BCC where available
- reply-to

Do not infer sender direction from display name alone.

---

# 11. Account-Aware Direction

For:

```text
Account A
```

a message sent from Account A is a user-sent message for that account.

For:

```text
Account B
```

the same email address may not have the same role.

Direction must always use the current account's identity.

---

# 12. Multi-Account Safety

The same conversation subject or sender may appear across accounts.

Do not merge them.

Example:

```text
Account A
Acme
Interview

Account B
Acme
Interview
```

must remain two independent conversations.

---

# 13. Thread Ordering

Conversation intelligence depends on correct message ordering.

Use reliable timestamps and Gmail message metadata.

Handle:

- identical timestamps
- missing timestamps
- malformed dates
- timezone differences
- imported/synthetic fixtures

Never sort solely by display-formatted date strings.

---

# 14. Latest Message

Determine the latest relevant message safely.

A thread's latest message should be based on normalized message metadata, not:

- UI insertion order
- database row order
- arbitrary API response order

---

# 15. Last User Message

Track the latest message sent by the relevant Gmail account.

Potential field:

```text
lastUserMessageAt
```

This is useful for detecting:

```text
↓
waiting for other party
```

---

# 16. Last Other-Party Message

Track the latest message received from another participant.

Potential field:

```text
lastOtherPartyMessageAt
```

Use this alongside message ordering.

---

# 17. Basic Waiting-for-Reply Logic

A conservative baseline:

```text
latest relevant message
+
latest message is from another participant
+
message appears to require/expect user response
=
LIKELY_AWAITING_USER_REPLY
```

Do not classify every incoming email as requiring a reply.

---

# 18. Reply-Expectation Signals

Use deterministic signals such as:

- direct question
- explicit request
- request for confirmation
- request for information
- scheduling question
- "please let me know"
- "can you..."
- "could you..."
- "please confirm"
- response to user's previous message
- Action Required result
- meeting/deadline context
- existing user rule

Use normalized content from Phase 5.

---

# 19. Negative Signals

Avoid false positives for:

- newsletters
- promotions
- automated notifications
- receipts
- security alerts
- no-reply messages
- informational broadcasts
- system-generated mail

A message can contain a question-like sentence without expecting a user reply.

---

# 20. No-Reply Senders

Detect likely automated/no-reply senders using:

- sender address
- known patterns such as `no-reply`
- existing notification intelligence
- automated headers where available
- existing company/sender intelligence

Do not assume every `no-reply` sender is irrelevant.

Use it as a strong negative signal for reply expectation.

---

# 21. Reply-to Handling

If:

```text
From: no-reply@example.com
Reply-To: support@example.com
```

do not simply conclude that the conversation cannot receive replies.

Use Reply-To as relevant participant metadata.

---

# 22. Thread-Level Context

A message that requests a reply may be followed by:

The conversation state must change accordingly.

Example:

```text
Other party asks question
↓
AWAITING_USER_REPLY

User replies
↓
AWAITING_OTHER_PARTY
```

Do not leave the conversation in the previous state.

---

# 23. User Reply Detection

Determine whether the user has replied using:

- sender identity
- account identity
- message timestamp
- thread membership

Do not use only subject matching.

---

# 24. Other-Party Reply Detection

After the user replies:

```text
User message
↓
waiting for other party
```

If another relevant participant replies:

```text
Other party message
↓
re-evaluate
```

Do not automatically mark the conversation resolved.

---

# 25. Resolved Conversations

A conversation may be considered resolved when evidence indicates:

- explicit completion
- final confirmation
- no outstanding request
- cancellation
- clear closure language
- user correction/rule

Do not assume silence means resolution.

---

# 26. Stale Conversations

Define a conservative stale state.

A conversation may become stale when:

```text
awaiting response
+
meaningful amount of time has passed
+
no newer relevant message
```

The stale threshold must be documented.

Do not use arbitrary extremely short thresholds.

---

# 27. Stale ≠ Forgotten

Do not phrase:

> "You forgot to reply."

Prefer:

> "No reply detected recently."

The system cannot know why a user has not replied.

---

# 28. Follow-Up Candidates

A conversation waiting on another person may eventually become a follow-up candidate.

Example:

```text
You replied 5 days ago.
No response detected.
```

This is an informational suggestion.

Do not automatically send a follow-up.

---

# 29. Follow-Up Confidence

A follow-up recommendation should depend on evidence.

Signals may include:

- elapsed time
- Action Required
- explicit expected response
- deadline
- importance
- previous conversation pattern

Do not recommend follow-up for every unanswered email.

---

# 30. Conversation Priority

Use existing Phase 9 priority.

Conversation intelligence should not create a second priority engine.

It may provide signals to the existing priority system where appropriate.

---

# 31. Action Required Integration

If:

```text
Action Required = YES
```

and the conversation is awaiting the user, this is a strong signal for attention.

If:

do not manufacture an action merely because the conversation is active.

---

# 32. Rules Integration

User rules from Phase 12 must remain authoritative.

Examples:

```text
Always treat recruiter conversations as important.
```

or:

Use the existing rule system.

Do not create a second conversation rule engine.

---

# 33. User Corrections

Allow the user to correct conversation intelligence.

Potential corrections:

```text
Not waiting for my reply
```

or:

Persistent rules must use Phase 12 architecture.

---

# 34. Explainability

Every conversation state should have an understandable explanation.

Example:

```text
Likely awaiting your reply

Why:
• Latest message is from Acme Recruiting
• It asks you to confirm an interview time
• No reply from you was detected afterward
```

For waiting on another party:

```text
Likely waiting for Acme Recruiting

Why:
• You replied 4 days ago
• No newer response was detected
```

---

# 35. Source Traceability

Conversation intelligence must retain:

- source account
- source thread
- relevant message IDs
- analysis version

The user must be able to open the underlying conversation.

Never produce an unexplained "waiting" card with no source.

---

# 36. Conversation Timeline

Where useful, display a compact timeline:

```text
Them
Monday 10:15

You
Monday 11:02

Them
Tuesday 09:40

You
Tuesday 10:10
```

This helps users understand why the system reached its conclusion.

---

# 37. Conversation Detail

Thread detail may include a compact state indicator:

```text
Awaiting your reply
```

or:

The indicator must be visually secondary to the actual email conversation.

---

# 38. Actions Screen

Add conversation-based attention items where appropriate:

```text
Awaiting your reply
3 conversations
```

or:

```text
2 conversations
```

These are organizational suggestions, not automatic actions.

---

# 39. Home Integration

Home may surface:

- conversations awaiting your reply
- conversations waiting for others
- stale conversations
- follow-up opportunities

Only show meaningful items.

Do not overwhelm the dashboard.

---

# 40. Search Integration

Extend local search where useful.

Potential filters:

```text
Awaiting my reply
Awaiting response
Stale
Resolved
```

Reuse Phase 10 search infrastructure.

Do not build another search engine.

---

# 41. Conversation Search

Search should return thread-level results where appropriate.

A query matching one message may surface its containing conversation.

Preserve account and thread identity.

---

# 42. Unified Inbox

Conversation state must work across accounts.

Unified view example:

```text
personal@gmail.com
Awaiting your reply
Acme Recruiting

college@gmail.com
Waiting for response
Professor Smith
```

Do not merge conversations between accounts.

---

# 43. Multi-Account Identity

All conversation intelligence records must be account-scoped.

Use:

```text
accountId + threadId
```

as the safe conversation identity.

---

# 44. Background Processing

Phase 19 background sync may trigger conversation analysis.

However:

```text
sync
↓
conversation analysis
```

must remain local and deterministic.

Do not send email content to external services.

---

# 45. Incremental Processing

Do not reprocess every thread after every synchronization.

Use changed messages/threads where practical.

When a message changes a thread's latest state:

```text
re-evaluate that conversation
```

---

# 46. Idempotency

Repeated processing must not create duplicate conversation states or follow-up candidates.

Use:

```text
accountId
+
threadId
+
analysisVersion
```

or an equivalent stable identity.

---

# 47. Conversation State Persistence

Persist conversation intelligence so it can be:

- displayed offline
- searched
- used by Home
- used by Actions
- recalculated after sync

Do not calculate everything synchronously whenever the user opens Home.

---

# 48. Offline Behavior

Conversation intelligence already stored locally must remain available offline.

The user should be able to:

- view waiting states
- open the source thread
- search conversation states
- review follow-up candidates

without network access.

External actions remain unavailable when connectivity is required.

---

# 49. Stale Data Communication

If offline and conversation state may be stale, communicate appropriately.

For example:

```text
Based on your last sync
```

where useful.

Do not imply the state reflects messages received after the last synchronization.

---

# 50. Gmail Writes Out of Scope

Do not implement:

- reply
- send
- forward
- archive
- mark read
- labels