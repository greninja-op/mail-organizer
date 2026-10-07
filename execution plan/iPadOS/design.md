# Mail Organizer iPadOS — Visual & UX Contract

## Core principle
THE IPAD IS NOT A LARGE IPHONE.

Use the viewport to expose more useful information simultaneously while preserving clarity and touchability.

## Layout
At sufficient width prefer:
Navigation/categories → message list + company/category context → selected message/conversation detail → optional contextual inspector/action region.

Columns appear/disappear according to actual available width.

## Information density
Compared with iPhone, show more rows and more metadata where useful:
sender, subject, preview, date/time, source account, category, company, priority, Action Required, attachments, conversation state and selected detail.

## Navigation
Use current native SwiftUI navigation architecture, such as NavigationSplitView where appropriate. Support sidebar, secondary content, detail, selection persistence and restoration where practical.

## Windowing
Test portrait, landscape, narrow split, medium split, full screen and Stage Manager window sizes. No layout may assume fixed width.

## Input
Use native pointer hover, keyboard shortcuts, contextual menus, focus, touch and useful drag/drop.

## Relationship to iPhone
Keep product identity, semantic colors, typography philosophy, states, icon meaning and information hierarchy recognizable. Layout, spacing, navigation, density, panel structure and interaction can differ significantly.

## Apple visual technologies
Where the supported iPadOS SDK provides public/documented Apple visual or system technologies, including supported Liquid Glass-era APIs where applicable, evaluate them for appropriate surfaces. Keep such implementation inside iPadOS/. Do not use private APIs, unofficial clones or scraped Apple assets. Do not make a visual effect mandatory if it harms compatibility, accessibility, performance or clarity.

## Accessibility
VoiceOver, Dynamic Type, Reduce Motion, contrast, hit targets, keyboard focus and accessibility sizes are first-class requirements.
