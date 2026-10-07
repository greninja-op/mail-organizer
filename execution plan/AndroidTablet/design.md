# Mail Organizer Android Tablet — Visual & UX Contract

## Core principle
**THE ANDROID TABLET IS NOT A LARGE PHONE.**

Maintain Mail Organizer's product identity and semantic design language, but use the tablet viewport as a workspace.

## Layout progression
Conceptually adapt through available width:
- narrow: navigation → list → detail
- medium: navigation rail/sidebar + list, detail contextually
- wide: navigation + list + persistent detail
- very wide: navigation + list + detail + optional contextual inspector/actions

These are layout states, not fixed device categories.

## Information density
Where useful, expose sender, subject, preview, date/time, receiving account, company, category, priority, Action Required, attachment indicators and conversation state.

## Compose architecture
Use current Material 3 and Android large-screen/adaptive guidance. Prefer WindowSizeClass/current adaptive APIs and pane/scaffold patterns appropriate to the actual viewport.

Do not duplicate the phone UI and simply increase dimensions.

## Input
Support touch, stylus where genuinely useful, mouse/trackpad, keyboard focus/navigation, context menus and useful drag/drop. Touch remains first-class.

## Windowing
Support orientation changes, split-screen/multi-window, resizing/configuration changes and tablet-specific system behavior.

## Visual relationship to Android phone
Semantic colors, typography philosophy, product concepts, states and behavior remain coherent. Navigation, density, pane structure, spacing and interaction can differ significantly.

## Motion/performance
Use adaptive motion/effects and respect reduced-motion/accessibility settings, battery saver and device constraints. Do not continuously benchmark or claim 120 FPS from refresh rate alone.

## Accessibility
TalkBack, large fonts, contrast, focus order, keyboard navigation, touch targets and switch/accessibility interaction are first-class.