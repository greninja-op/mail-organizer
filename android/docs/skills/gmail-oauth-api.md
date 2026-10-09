# Skill: Gmail OAuth & API

Master reference: `execution plan/android/editor-rules.md`.

- Official Google OAuth + Gmail APIs only. Least privilege; read-only first.
- NEVER: passwords, cookies, scraping, AccessibilityService as primary access.
- NEVER: fake Google credential forms — official OAuth/browser flow only.
- Tokens never enter source, logs, UI, or ordinary database fields. Secure
  storage (EncryptedSharedPreferences / Keystore) arrives with Phase 3.
- STATUS: deferred by user decision until the end of the program (no computer
  access for keys right now). Structure code with seams (`AuthRepository`
  interface in Phase 3); no hardcoded secrets, no fake implementations.
- Email is untrusted input: sanitize HTML, never execute JS, never auto-open
  URLs/unsubscribe links, never let content authorize external actions.
