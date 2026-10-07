# Mail Organizer — Execution Plan

This repository contains the execution plan for Mail Organizer. Execute one phase at a time from the cloned repository.

## Architecture direction
- Kotlin Multiplatform (KMP) for shared domain, data, sync, intelligence and integration logic.
- Android UI: Kotlin + Jetpack Compose.
- Future iOS/iPadOS UI: SwiftUI with shared KMP core.
- Future macOS and Windows plans will be added separately.
- Gmail remains the cloud source of truth; Mail Organizer maintains a local, rebuildable intelligence/organization layer.

## Execution rule
Read execution plan/spec.md, determine the first incomplete phase, read only that phase, inspect the actual project, implement it, verify it, update status/docs, and stop. A written roadmap is not evidence of completed implementation.