# Skill: Architecture

Master reference: `execution plan/android/editor-rules.md`.

- Layers: Presentation → Application/Use Cases → Domain → Data → External APIs.
  Dependencies point inward only.
- KMP owns shared business/data logic (domain models, use cases, Gmail
  normalization, sync state, classification, company intel, rules, search,
  priority, temporal/conversation intel, actions, integration abstractions,
  persistence). Platform layers own UI, OAuth presentation, secure storage, OS
  scheduling.
- Keep Auth, Gmail Client, Sync, Parser, Classification, Company Detection,
  Rules, Actions, Calendar, Tasks, Database, Search independently testable.
- Until the second platform needs it, shared code lives in the Android module
  with KMP-compatible structure: `core/` is pure Kotlin, no Android imports.
- Never restructure working architecture for stylistic preference; audit first,
  preserve compatible work, refactor only to align with the product contract.
