# Skill: Integration

Master reference: `execution plan/android/editor-rules.md`.

- Calendar and Tasks are modular adapters behind a manager (Phase 17);
  each integration fails independently and never crashes the core experience
  (see `MoError.Integration`).
- Action suggestions are explainable, traceable to source mail, deduplicated,
  and confirmed by the user before any external effect.
- Gmail write operations exist ONLY in the dedicated write phase (Phase 22).
- STATUS: all integrations deferred (Phases 15/16/22). No stubs that pretend
  to integrate.
