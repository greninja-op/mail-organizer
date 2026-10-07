# macOS Specification

## Authority
requirements > spec > design > editor-rules > existing architecture/decisions > official documentation > engineering judgment. Preserve newer repository decisions.

## Architecture contract
Rust is the primary implementation layer. The Rust core owns platform-neutral domain/data/business logic, application/use cases, Gmail normalization and sync, classification/company intelligence, search, priority, temporal/conversation intelligence, rules, Action Engine, integrations, persistence, automation, and AI routing. The core must be reusable by Windows.

Swift/SwiftUI owns native macOS presentation and OS integration: app/scenes, windows, menus/commands, accessibility, pointer/keyboard interaction, Keychain glue where required, notifications, and macOS-specific lifecycle APIs. Swift must not fork Rust business logic.

The Rust↔Swift boundary must be explicit, narrow, testable, cancellation-aware, and safe for ownership/threading. Prefer generated bindings or stable FFI value/handle contracts.

## Desktop contract
Adaptive sidebar + message list + detail + optional inspector; Gmail-familiar hierarchy without cloning proprietary artwork/source. Companies remain category filters. Search is primary. Native menus, split views, keyboard commands and selection state must remain coherent.

## Security
Official OAuth, least privilege, secure credential storage, no secrets in source/logs. Email HTML, links, attachments, sender text, and AI output are untrusted. AI is optional, schema/domain validated, isolated, and cannot authorize external effects.

## Execution
One phase only: inspect, implement, test, build, run, inspect, fix, rebuild/retest, update status/rules, stop. Never claim unrun evidence and never publish automatically.