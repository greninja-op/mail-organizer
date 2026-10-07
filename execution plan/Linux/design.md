# Linux Visual & UX Contract

## Product identity
Linux is a first-class Mail Organizer desktop application. It shares the same product visual language and information architecture as other desktop targets, but uses Linux-appropriate windowing, scaling, menus, notifications and input behavior.

## Layout
- Desktop multi-column workspace rather than a scaled phone screen.
- Resizable navigation, message-list and conversation panes.
- Responsive breakpoints for narrow desktop windows and large monitors.
- Keyboard-first navigation is a first-class interaction path.
- Mouse, trackpad, wheel, context menus and standard desktop focus behavior are supported.

## Visual language
Use the established Mail Organizer Material-inspired visual language, centralized tokens, consistent typography, spacing, shapes, semantic colors, iconography, dark/light themes and motion rules. Do not use proprietary Gmail artwork/source. Components should feel consistent across Android and desktop without pretending Linux is Android.

## Mailbox
Global navigation includes All Inbox, Primary, Promotional, Social, Spam and Starred. Company grouping/filtering lives inside the selected category. Company pinning is distinct from message starring. All Inbox rows identify the receiving Gmail account separately from sender/company identity. Promotional and Social are organized, not silently deleted. Spam is first-class with clear recovery/Not Spam behavior.

## States
Every important screen supports loading, empty, populated, offline, error, retry, authentication-required, sync-in-progress and partial-data states. Motion must represent real state; never fake sync percentages.

## Accessibility and performance
Support keyboard traversal, visible focus, screen readers/AT where supported, large text/scaling, reduced motion, high-DPI rendering, efficient lists, sustained responsiveness and sensible battery/CPU use. 120 Hz-capable displays do not imply a continuous 120 FPS requirement.

## Validation
Review narrow/normal/wide windows, multiple scaling factors, light/dark themes, keyboard-only operation, accessibility, real data and long-content emails. Fix visual inconsistencies before completion.