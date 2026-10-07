# Mail Organizer — Android Visual & UX Contract

## Product design target
The Android application should feel immediately familiar to a Gmail/Google-app user while remaining a distinct Mail Organizer product. Use official Material 3 / Material 3 Expressive principles, Android system conventions, Google-app information hierarchy and familiar interaction patterns, then add Mail Organizer's own identity through restrained branding, organization-first information architecture and email intelligence.

This is inspiration and implementation guidance, not permission to copy proprietary Google artwork, trademarks, source code or inaccessible assets. Use official Material components and official Material Symbols/icons where applicable; do not generate replacement UI icons with AI.

## Design priorities
1. Performance and responsiveness.
2. Gmail-like familiarity.
3. Material/Pixel consistency.
4. Mail Organizer identity.
5. Accessibility and clarity.
6. Motion that communicates state without wasting power.

## Material and Pixel alignment
Use Jetpack Compose Material 3 as the baseline. Material 3 Expressive is the current Android design direction and adds responsive components, emphasized typography and spring-oriented motion. Dynamic color should be supported on Android 12+ with a controlled Mail Organizer fallback palette. The implementation must remain compatible with the Android system and not require a Pixel device.

Reference official Android/Material documentation during implementation and re-check current APIs before adopting experimental APIs.

## Gmail familiarity
The following should feel familiar without becoming a Gmail clone:
- account/profile entry point
- top-level mail/search affordances
- message list density and hierarchy
- sender/subject/preview/timestamp relationships
- familiar archive/star/unread concepts where those features exist
- thread-oriented reading
- swipe/gesture conventions
- clear unread/important states
- Google-style sign-in flow using official OAuth UI/components where available

Mail Organizer categories, priority, Action Required, company intelligence and its Home/Categories/Companies/Actions model remain distinct.

## No AI-generated UI artwork
Do not use generative AI to create application icons, menu icons, settings icons, navigation icons, Material symbols, Google/Gmail marks, system glyphs or other functional UI artwork. Prefer official Material Symbols, Android system icons, Google-provided assets permitted by their terms, or original vector assets created manually in code/design tools. Do not rasterize or redraw official Google marks unnecessarily.

## Adaptive performance model
The app must not equate display refresh rate with sustainable application frame rate.

At first launch, collect only the device/runtime information that is useful and permitted for performance adaptation, such as Android/API level, available memory class, CPU/GPU capability signals where reliably available, display modes/refresh rates, power/thermal state where available, reduced-motion/accessibility preferences, battery-saver state and app performance history.

Create a lightweight PerformanceProfile with capability tiers rather than hardcoding device names. The profile must select:
- animation complexity
- animation duration/spring behavior
- visual effects and blur usage
- list prefetching/window size
- image decoding/cache limits
- background processing concurrency
- sync/indexing batch sizes
- expensive transition effects
- optional haptics where supported

Never force 120 FPS merely because a display supports 120 Hz. Android's frame-rate scheduler and adaptive refresh-rate mechanisms should be respected. Prefer smoothness and sustained performance over maximum refresh rate. If CPU/GPU/thermal headroom falls, reduce expensive work and motion complexity before the UI becomes janky.

Performance adaptation must be:
- deterministic and explainable
- conservative
- reversible
- privacy-preserving
- accessible
- independent of device-brand assumptions
- tested across low/mid/high capability profiles

Do not run continuous hardware benchmarking on the user's device. Use lightweight capability detection and measured app performance signals. Re-evaluate when relevant conditions change rather than constantly polling.

## Motion system
Create a centralized motion system so every screen uses the same motion language.

Motion tiers:
- Essential: short, low-cost state changes.
- Standard: normal navigation/list interactions.
- Expressive: hero/sync/important transitions.
- Reduced motion: minimal fades/position changes when accessibility or performance requires it.

Use Material motion schemes where available. Expressive motion should be reserved for meaningful moments, not every click. No animation may block mail interaction or synchronization.

## Gmail → Mail Organizer initial sync experience
When a newly connected account requires non-trivial recovery/synchronization time, show a dedicated progress experience rather than a blank screen.

The experience should communicate:
Gmail cloud → Mail Organizer local organization layer.

Visual concept:
- Gmail mark on the left.
- Mail Organizer mark on the right.
- A small mail/message representation travels from Gmail toward Mail Organizer.
- Progress state shows that mail is being recovered/synchronized/organized.
- Progress copy communicates what is actually happening.
- Determinate progress is shown only when a trustworthy estimate/count exists.
- Otherwise use an honest indeterminate animation with meaningful stage text.
- Never fake a percentage or pretend all mail has completed when it has not.
- Allow the user to enter the app when safe while background synchronization continues, where architecture permits.
- Show per-account and aggregate progress for multi-account recovery.
- Handle pause/retry/auth/network/error states honestly.

This animation must be lightweight and adaptive. On lower-performance profiles, simplify the moving mail element and reduce effects while preserving the meaning.

## Login
The login experience should feel like a native Google/Material Android flow:
- clear Google account identity
- familiar spacing, typography and hierarchy
- official OAuth/browser flow rather than an imitation credential form
- no password collection by Mail Organizer
- explicit permissions and trust messaging
- accessible loading, cancellation, denial and retry states

Do not create a fake Google login screen that collects Google credentials.

## Core UI consistency
Centralize:
- color tokens
- typography
- shapes
- spacing
- elevation
- icons
- component states
- motion
- adaptive-performance decisions

Every screen must use the same tokens. Before completing a UI phase, perform a consistency pass across all previously implemented screens.

## Validation
Every major UI change requires:
- emulator and/or real-device validation
- light and dark mode checks
- normal and large font/accessibility checks
- portrait and supported window sizes
- screenshot review
- motion/performance inspection
- jank/frame-time inspection where practical
- regression review of existing screens

The UI is not complete merely because it compiles.

## Platform boundary
Android is the first UI to be perfected. Future iOS/iPadOS/macOS/Windows plans may share KMP business/data logic, but each platform will receive its own native UI system and platform-specific design rules later.