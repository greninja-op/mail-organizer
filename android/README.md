# Mail Organizer — Android

Privacy-first personal email organization and action layer over Gmail. Gmail remains
the authoritative cloud source of truth; this app adds local organization,
intelligence, search, prioritization, action suggestions and optional integrations.

> **Working with this repo? Start with [CONTEXT.md](CONTEXT.md)** — the living
> continuity file. Every agent must **read it before starting any work** and
> **update it after every prompt**.

> **Phase 0 — Project Audit & Development Foundation.** This repository currently
> contains the Android application foundation only: build toolchain, architecture
> skeleton, design tokens, navigation foundation, local database seed, and the
> testing setup. Product features arrive phase by phase per
> [`execution plan/`](../README.md) (see the plan repo
> `greninja-op/mail-organizer`).

## Build

Requirements: JDK 17+, Android SDK (compileSdk 35). The Gradle wrapper
downloads everything else.

```bash
./gradlew :app:assembleDebug      # build
./gradlew :app:testDebugUnitTest  # unit + Robolectric integration tests
./gradlew :app:lintDebug          # Android lint
```

Set the SDK location via `ANDROID_HOME`/`ANDROID_SDK_ROOT` or a (git-ignored)
`local.properties` with `sdk.dir=/path/to/android-sdk`.

## Project structure

```
app/                          Android application module
  src/main/java/.../core/      Result/error model, safe logging, AccountId boundary,
                              injectable dispatchers
  src/main/java/.../di/       Manual dependency container (AppContainer)
  src/main/java/.../data/     Room database seed (accounts), DataStore preferences
  src/main/java/.../ui/       Material 3 theme tokens, navigation foundation,
                              honest placeholder screen
  src/test/                   JUnit + Turbine + Robolectric tests
docs/
  architecture.md             Layer map, decisions, deferred work
  development-status.md       Phase log, baseline, risks
  skills/                     Persistent engineering skills (Phase 0 requirement)
```

## Key rules (from the execution plan)

- Phases execute **one at a time**; a phase is done only after real verification.
- **Never** fake completion, fake tests, or commit secrets.
- Gmail access is **official OAuth + Gmail API only**, least privilege, read-only
  first — and it is **deferred** (Phase 3). No credentials or API keys are in this
  repo, and none are needed until the user provides them at the end of the program.
- Account isolation is a first-class data boundary (`AccountId`).
- Local-first: Gmail → API → app → local DB → local processing → UI.

## Deferred by user decision (documented, not implemented)

Google OAuth / API keys / secrets (Phase 3), Calendar/Tasks integrations
(Phases 15/16), Gmail write features (Phase 22), production OAuth/Play Store
(Phase 29). Interfaces and seams exist for these; nothing is faked.
