# Skill: Testing & QA

Master reference: `execution plan/android/editor-rules.md`.

- Layers: unit (domain, parsers, classifiers, use cases), integration
  (database via Robolectric on JVM; device/emulator where required), UI
  (Compose UI tests on device), security-sensitive (account isolation, token
  handling, permission state, sensitive logging).
- Test names describe behavior. Fakes over mocks where practical; MockK is
  cataloged and added only when a test first needs mocking.
- `Turbine` for Flow assertions; inject `AppDispatchers` + `TestDispatcher`.
- Never claim tests ran when they didn't. A phase's tests must actually
  execute in this environment or on a device/emulator — "can't run here" is
  reported, not faked.
- Full device QA lands in Phase 28; release readiness only after real
  verification (Phase 30).
