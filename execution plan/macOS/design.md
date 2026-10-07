# macOS Visual & UX Contract

## Principle
macOS is a native desktop client, not a scaled mobile UI. Rust owns product behavior; SwiftUI presents that behavior using native macOS conventions.

## Layout and interaction
Use adaptive SwiftUI split views, resizable windows, sidebar + list + detail, optional inspector, toolbar, menu commands, keyboard shortcuts, pointer/trackpad, context menus, selection semantics, multiple windows/scenes, VoiceOver and reduced motion.

## Navigation
All Inbox, Primary, Promotional, Social, Spam, and Starred are primary destinations. Companies are filters within the selected category. Company pinning is separate from message starring.

## Visual system
Centralize typography, spacing, surfaces, semantic colors, separators, icons, motion and state tokens. Support light/dark mode, contrast and accessibility sizing. Prefer native SF Symbols/system controls for functional UI.

## Rust/Swift boundary
SwiftUI must consume Rust-owned models/use cases rather than reproduce business rules. UI state translation belongs at the boundary and must not create a second source of truth.

## Motion and QA
Sync motion must represent real sync state; never fabricate percentages. Validate multiple window sizes, light/dark mode, accessibility text, VoiceOver, keyboard-only navigation, pointer interaction, offline/error/loading states, selection restoration and large-mailbox performance.