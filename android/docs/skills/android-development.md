# Skill: Android Development

Master reference: `execution plan/android/editor-rules.md`.

- Kotlin + Jetpack Compose, Material 3 / Material 3 Expressive baseline.
- Manual DI via `di/AppContainer`; adopt Hilt/Koin only when the graph
  justifies it (decision logged in `docs/development-status.md`).
- Every dependency must have a documented reason; keep the graph small.
- `compileSdk/targetSdk 35`, `minSdk 26`, JVM 17. Verify versions against Maven
  metadata before upgrading; never upgrade for its own sake.
- Coroutines/Flow for async; inject `AppDispatchers`, never hardcode
  `Dispatchers` in production code.
- Room (KSP) for persistence; DataStore for preferences; account-scoped
  storage keyed by `AccountId`.
- No Gmail permissions until the phase that needs them (least privilege).
