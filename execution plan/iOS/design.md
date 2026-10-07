# Mail Organizer iOS — Visual & UX Contract

## Platform

This is the iPhone/iOS visual contract only. Do not use it to implement iPadOS, Android tablet, macOS, Windows, or Linux layouts.

## Design direction

The experience should feel familiar to Gmail users without cloning Google's proprietary artwork, source code, or branding.

Use native SwiftUI and Apple's platform conventions where they improve familiarity and accessibility. Shared product concepts remain consistent with Android, but layouts must be genuinely adapted to iPhone.

## Navigation

Use an iPhone-appropriate navigation hierarchy. The user must quickly reach:
All Inbox, Primary, Promotional, Social, Spam, Starred.

Account switching must be obvious and must never imply that sender identity is the receiving account.

## Mail rows

A row should expose enough information for rapid scanning:
- sender
- subject
- preview
- time/date
- relevant category/priority/action state
- source Gmail account identity for unified inbox
- email star state where applicable

Avoid visual overload.

## Sync motion

The product may use the established Gmail → Mail Organizer retrieval concept, but iOS animation must be lightweight and truthful.

Determinate progress is shown only when a trustworthy denominator exists. Otherwise use an indeterminate state. Never fake percentages.

## States

Every important screen needs intentional:
loading, loaded, empty, offline, stale, syncing, authentication-required, permission-denied, error, and retry states as applicable.

## Accessibility

Support:
- VoiceOver
- Dynamic Type
- sufficient contrast
- Reduce Motion
- larger accessibility sizes
- logical focus/order
- labels for non-text controls
- hit targets appropriate to iPhone interaction

## Motion

Use a small, purposeful motion system:
- Essential
- Standard
- Expressive
- Reduced Motion

Respect Reduce Motion and Low Power Mode where appropriate.

## Visual system

Centralize spacing, typography, colors, shape, elevation/material treatment, iconography, and motion constants. Do not scatter magic values across views.

Do not use gradients merely because they are visually fashionable. Product identity should remain restrained and consistent.
