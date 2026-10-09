# Skill: Privacy & Security

Master reference: `execution plan/android/editor-rules.md`.

- `AccountId` is the data boundary: every account-owned entity is keyed by it;
  preference keys go through `scopedKey()`. Never mix accounts.
- Gmail is the cloud source of truth; local state is minimized and rebuildable.
  Persist only what offline/search/intelligence needs.
- `MoLogger` is the only logging path. NEVER log: email bodies/subjects, OAuth
  tokens, refresh tokens, auth headers, passwords, full message content.
  Debug output only on debuggable builds.
- Never claim a privacy/security property (encryption, privacy) that is not
  implemented and verified.
- Never commit secrets: API keys, tokens, keystores, `google-services.json`,
  signing keys. `.gitignore` covers the known patterns; audit before every
  phase completes.
- Signing config: debug vs release separated; production secrets never in source.
