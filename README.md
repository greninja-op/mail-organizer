![Mail Stack — Organise your inbox. Simplify your day.](banner/banner.png)

# Mailstack — Execution Plan

This repository contains the execution plan for Mailstack (formerly "Mail Organizer"). Execute one phase at a time from the cloned repository.

> **Working on this repo? Start with [CONTEXT.md](CONTEXT.md)** — the canonical continuity file: current build state, standing rules, and decisions. Read it before starting any work, and update it after every prompt.

## Architecture direction
- Kotlin Multiplatform (KMP) for shared domain, data, sync, intelligence and integration logic.
- Android UI: Kotlin + Jetpack Compose.
- Future iOS/iPadOS UI: SwiftUI with shared KMP core.
- Future macOS and Windows plans will be added separately.
- Gmail remains the cloud source of truth; Mail Organizer maintains a local, rebuildable intelligence/organization layer.

## Execution rule
Read execution plan/spec.md, determine the first incomplete phase, read only that phase, inspect the actual project, implement it, verify it, update status/docs, and stop. A written roadmap is not evidence of completed implementation.