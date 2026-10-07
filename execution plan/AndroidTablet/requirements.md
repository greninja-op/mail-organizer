# Mail Organizer Android Tablet — Requirements

## Purpose
Android tablet is a first-class large-screen experience over the same Mail Organizer product core. It must not be a phone layout merely stretched to tablet dimensions.

## Platform boundary
This document applies only to Android tablets. Android-tablet-specific UI, resources, tests, configuration and release material belong under `AndroidTablet/`. The existing Android phone execution plan remains separate.

## Shared source
Reuse the common KMP/Android product core for domain/data/application behavior:
Gmail models, normalization, repositories, sync, classification, company intelligence, search, rules, priority, Action Required, temporal/conversation intelligence, Action Engine, Calendar/Tasks abstractions, automation, AI routing and persistence/state contracts.

Do not clone these engines into AndroidTablet.

## Large-screen information architecture
Use available width to expose more useful information at once:
- navigation/category context
- company/category filters
- message list
- selected message/detail
- contextual action/inspector area where justified

The layout must adapt to portrait, landscape, split/multi-window configurations, keyboard/mouse and different tablet sizes.

## Mail contract
Preserve All Inbox, Primary, Promotional, Social, Spam and Starred; unified multi-account inbox; receiving-account identity on unified rows; company filtering inside categories; company pinning independent from email star; organized Promotional/Social; first-class Spam/recovery; global Starred.

## Information density
Where space permits, a message row/workspace may show sender, subject, preview, date/time, source account, company, category, priority, Action Required, attachments and conversation state.

Useful density is preferred over decorative whitespace or clutter.

## Android large-screen behavior
Use current Android large-screen guidance and Compose adaptive/window-size APIs. Support navigation rail/drawer/sidebar patterns, two/three-pane layouts where appropriate, keyboard/mouse/pointer, context menus, drag/drop only where valuable, multi-window and configuration changes.

## Security/intelligence
Same product rules as Android phone: official OAuth, least privilege, secure token storage, account isolation, sanitized email HTML, no email-authorized external actions, deterministic-first intelligence, optional AI, explainable actions and safe automation.

## Quality
Verify multiple widths/orientations/window configurations, large fonts, TalkBack, keyboard/mouse, dark mode, offline, multi-account, large datasets, process death and background behavior. Build success alone is insufficient.