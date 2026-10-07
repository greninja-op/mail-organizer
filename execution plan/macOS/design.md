# macOS Visual & UX Contract

## Principle
macOS should feel native to the desktop while preserving Mail Organizer's product identity and Gmail-familiar information hierarchy. It is not a scaled iPad UI.

## Layout
Use adaptive SwiftUI split views and desktop window sizing. The workspace may use sidebar + message list + detail, with an optional inspector/context pane where the width supports it. Preserve selection and navigation state when resizing.

## Navigation
The primary hierarchy includes All Inbox, Primary, Promotional, Social, Spam, and Starred. Companies appear as filters within the selected category. Company pinning moves a company to the top of the company filter and is distinct from starring individual messages.

## Toolbar and interaction
Provide prominent search, account/profile access, useful contextual actions, standard macOS toolbar/menu behavior, keyboard shortcuts, context menus, pointer states, and sensible selection behavior. Avoid fake Google login forms.

## Visual system
Use centralized design tokens for typography, spacing, surfaces, borders, semantic colors, icons, motion, and state presentation. Support light/dark mode and accessibility contrast. Prefer native SF Symbols/system controls or permitted/original vectors for functional UI.

## Motion
Motion must communicate real state. Sync animation may show Gmail-to-Mail Organizer movement only when an actual sync is occurring; determinate progress must be backed by trustworthy progress. Never fabricate percentages. Respect reduced-motion settings.

## Quality
Test multiple window sizes, light/dark mode, large accessibility text, VoiceOver, keyboard-only navigation, pointer/trackpad interaction, empty/loading/error/offline states, and selection restoration. Preserve performance during large mailbox rendering and scrolling.
