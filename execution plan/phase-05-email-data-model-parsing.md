# Phase 05 — Email Data Model & Parsing

## Execution contract

Read `requirements.md`, `spec.md`, `design.md`, `editor-rules.md`, and this phase file.

Confirm Phase 05 is the first incomplete phase.

Implement **email normalization and safe parsing only**.

Do not implement classification, company intelligence, search, Calendar, Tasks, Gmail writes, AI or automation.

---

## 1. Parsing architecture

Separate three representations:

1. raw Gmail/API transport models;
2. normalized Mail Organizer domain models;
3. derived intelligence.

Raw API models must not leak through the application UI/domain unnecessarily.

Parsing must be deterministic and testable.

---

## 2. Message normalization

Normalize:

- Gmail message/thread IDs;
- sender;
- recipients;
- reply-to;
- subject;
- timestamps;
- labels;
- headers required by product behavior;
- plain-text body;
- HTML body;
- snippet/preview;
- attachment metadata.

Preserve original values where necessary for traceability, but do not duplicate unnecessary raw payloads.

Handle missing fields safely.

---

## 3. Sender and recipient normalization

Create normalized representations for:

- display name;
- email address;
- domain;
- sender/recipient role.

Correctly handle:

- quoted display names;
- Unicode names;
- malformed-but-tolerable headers;
- multiple recipients;
- missing display names;
- plus-addressing where relevant;
- case normalization where safe.

Do not infer company identity in this phase.

---

## 4. Subject normalization

Support:

- encoded subjects;
- Unicode;
- whitespace normalization;
- empty subjects;
- reply prefixes;
- forwarded prefixes.

Do not destroy the original subject needed for display/traceability.

Thread grouping should remain based on Gmail thread identity rather than a guessed subject alone.

---

## 5. MIME parsing

Handle common Gmail MIME structures, including:

- plain text;
- HTML;
- multipart/alternative;
- multipart/mixed;
- nested multipart content;
- attachments;
- inline resources.

Choose the best safe representation for application display.

Do not download attachment bodies merely to parse metadata.

Handle malformed MIME gracefully.

---

## 6. HTML safety

Email HTML is untrusted input.

Sanitize before rendering.

Rules:

- never execute JavaScript;
- never execute embedded scripts;
- remove dangerous active content;
- neutralize unsafe URLs;
- avoid automatic navigation;
- avoid automatic downloads;
- prevent unsafe resource behavior;
- preserve readable formatting where possible.

Do not allow email content to trigger application actions.

Do not automatically unsubscribe from mailing lists.

---

## 7. Plain-text fallback

When HTML is absent or unsafe:

- use plain text;
- preserve meaningful line breaks;
- normalize excessive whitespace;
- retain quoted content safely;
- preserve Unicode.

When both HTML and plain text are malformed/missing, produce a safe empty/error representation rather than crashing.

---

## 8. Preview/snippet generation

Create deterministic preview extraction.

Rules:

- never leak hidden HTML;
- strip unsafe markup;
- normalize whitespace;
- bound preview length;
- preserve useful visible text;
- handle empty content.

Do not use AI-generated summaries.

---

## 9. URLs and links

Parse links as data, not commands.

The parser may expose safe link metadata to the UI.

The viewer must later require deliberate user action before navigation.

Never:

- auto-open a URL;
- auto-download a resource;
- execute JavaScript;
- treat a URL as an application instruction.

---

## 10. Unsubscribe metadata

Detect unsubscribe-related metadata only for later user-facing organization.

Support standard metadata/header forms where available.

Do not send an unsubscribe request.

Do not auto-click unsubscribe links.

Do not let an email's instruction authorize an external action.

---

## 11. Attachment metadata

Normalize attachment metadata such as:

- filename;
- MIME type;
- size;
- attachment ID/reference;
- inline/disposition information.

Do not automatically download attachment binaries.

Avoid storing duplicate attachment data locally.

---

## 12. Signatures and quoted replies

Create deterministic handling for:

- common signature separators;
- quoted reply sections;
- forwarded-message sections.

Preserve the original body representation.

Do not over-aggressively remove content when confidence is low.

A false removal is worse than retaining a few quoted lines.

---

## 13. Date/time handling

Normalize Gmail timestamps to an unambiguous representation.

Preserve the original instant.

Do not silently convert dates based on device locale in the data layer.

UI formatting belongs to presentation.

Handle missing/invalid dates without crashing.

---

## 14. Derived parsing provenance

Where parsing transforms content, preserve enough metadata to explain:

- parser/version;
- source field;
- transformation result;
- parse failure category where relevant.

Do not store sensitive diagnostic copies unnecessarily.

---

## 15. Testing

Create synthetic fixtures for:

- plain text;
- HTML;
- multipart/alternative;
- multipart/mixed;
- nested MIME;
- malformed MIME;
- Unicode;
- encoded headers;
- missing sender;
- missing subject;
- signatures;
- quoted replies;
- forwarded messages;
- multiple recipients;
- dangerous HTML/script;
- unsafe URLs;
- attachment metadata;
- unsubscribe headers;
- empty content.

Tests must prove:

- no script execution;
- no unsafe URL activation;
- deterministic output;
- bounded previews;
- no crashes on malformed input;
- correct account/message identity preservation.

Never use real private email in fixtures.

---

## 16. Integration with local data

Persist normalized results through the repositories/database from Phase 02.

Do not create a second database.

Do not let parsing mutate unrelated intelligence state.

Parsing should be rerunnable without corrupting existing records.

---

## 17. Runtime/security validation

Build and test with the Gradle wrapper.

On Android:

- install;
- open representative synthetic messages;
- verify sanitized rendering;
- inspect logs;
- verify no script/network side effect occurs from email content;
- verify attachment metadata does not trigger downloads.

Use safe test HTML containing deliberately dangerous constructs to validate sanitization.

---

## 18. Explicit non-goals

Do not implement:

- category classification;
- priority;
- Action Required;
- company detection;
- search index;
- Calendar/Tasks;
- Gmail writes;
- AI summaries;
- automation.

---

## 19. Completion

Update permanent editor rules only when a verified parsing/security rule is genuinely new.

Update `spec.md` status only after real verification.

Document parser boundaries, sanitization behavior and known limitations.

Final sequence:

`diff review → build → tests → security/rendering QA → fix → rebuild/retest → docs/status → commit → stop`

Do not continue to Phase 06.