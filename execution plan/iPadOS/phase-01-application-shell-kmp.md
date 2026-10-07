# Phase 1 — iPadOS Application Shell & Shared KMP Integration

## MANDATORY PLATFORM ISOLATION — READ FIRST

This is an iPadOS-only phase. All iPadOS-specific work belongs under iPadOS/. Never create or modify iOS/, AndroidTablet/, macOS/, Windows/ or Linux/. Shared KMP/common source remains shared; never duplicate common business logic into iPadOS.

## Mission

Create iPad app shell and clean shared KMP boundary; integrate framework; SwiftUI scene lifecycle; adaptive NavigationSplitView architecture; sidebar/content/detail containers; centralized tokens; environment/config separation; selection/state restoration foundation; pointer/keyboard focus foundation; Stage Manager/resizing baseline; loading/error/empty/offline primitives; no Gmail/OAuth/product feature implementation; clean build, Simulator launch/relaunch, multiple orientations/window sizes, keyboard/pointer, dark mode, Dynamic Type; STOP.

## Required completion protocol

Inspect root instructions and iPadOS requirements/spec/design/editor-rules. Inspect actual repository before implementation. Implement only this phase. Build and run. Exercise realistic and failure states. Fix failures. Rebuild and retest. Update status and permanent rules only when genuinely required. Never claim unrun tests or fabricate evidence.

**STOP AFTER THIS PHASE.**