# Windows Visual & UX Contract

## Principle
Windows is a native desktop client with a Rust-first UI architecture. It is not a scaled phone/tablet UI and should feel at home with Windows desktop interaction conventions.

## Layout
Use adaptive multi-pane desktop layout: navigation/sidebar, message list, selected detail and optional inspector. Preserve selection and context during resizing and multi-window use.

## Navigation
All Inbox, Primary, Promotional, Social, Spam and Starred are primary destinations. Companies are filters within the selected category. Company pinning is separate from message starring.

## Interaction
Provide prominent search, account access, toolbar/command actions, keyboard shortcuts, pointer/mouse/trackpad behavior, context menus, selection semantics, safe drag/drop, and multi-window support.

## Visual system
Centralize typography, spacing, surfaces, borders, semantic colors, icons, motion and state tokens. Support light/dark mode, high contrast and accessibility text sizing. Prefer permitted/original assets and native Windows conventions.

## Rust UI boundary
UI state should consume Rust-owned models/use cases. Do not recreate business decisions in UI code.

## Motion and QA
Motion communicates real state. Sync progress is truthful; no fake percentages. Test multiple window sizes, themes, accessibility, keyboard/mouse navigation, empty/loading/error/offline states and large-mailbox performance.