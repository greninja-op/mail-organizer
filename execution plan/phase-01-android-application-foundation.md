# Phase 01 — Android Application Foundation

## Execution contract
Read requirements.md, spec.md, design.md and editor-rules.md before starting. Confirm this is the first incomplete phase. Inspect the actual repository before editing. Work only inside Mail Organizer. Never modify sibling projects. Implement only this phase and do not execute later phases.

## Phase objective
Build the Android UI foundation as the first serious product-quality milestone.

### 1. Material/Pixel/Gmail visual language
- Use current Jetpack Compose Material 3 and Material 3 Expressive capabilities where stable/appropriate.
- Study and follow official Android/Material guidance for theming, typography, shapes, responsive components, motion and dynamic color.
- Make the application immediately familiar to Gmail/Google-app users without cloning proprietary source code or artwork.
- Use the Mail Organizer design identity as the controlled twist.
- Centralize all design tokens and components so future screens cannot drift visually.
- Use official Material Symbols/system icons or deliberately authored vectors; never AI-generate functional UI icons.

### 2. Authentication entry experience
- Build the sign-in entry point and surrounding loading/error/cancel/denied states.
- The app must use official Google OAuth/browser authentication when authentication is implemented.
- Never build a password field or fake Google credential collector.
- Make the visual hierarchy, spacing and interaction language feel native to Android/Google apps.

### 3. Adaptive performance foundation
Create a lightweight capability assessment and PerformanceProfile abstraction.

Assess only useful, permitted signals such as:
- Android/API level
- memory class and available memory signals
- supported display modes/refresh rates
- CPU/GPU capability signals where reliably available
- power/thermal state where supported
- battery saver/power state
- reduced-motion/accessibility preferences
- observed app frame/jank history when available

Map the result to conservative capability tiers. Do not hardcode device-brand lists.

The profile must control, through centralized policy:
- animation/motion complexity
- expensive visual effects
- list prefetch/window sizes
- image decode/cache limits
- background concurrency
- sync/index batch sizes
- optional haptics

Do not equate 120 Hz display support with a 120 FPS target. Respect Android's frame-rate scheduler and adaptive refresh behavior. Optimize for sustained smoothness and thermals.

Do not continuously benchmark the phone or poll hardware. Re-evaluate only at startup and meaningful state changes.

### 4. Motion system
Create a centralized motion policy using Material motion schemes where available.
- Essential motion: lowest cost.
- Standard motion: everyday navigation/list interactions.
- Expressive motion: hero/sync moments only.
- Reduced-motion mode: minimal movement when accessibility/performance requires it.

Every animation must have a purpose and a bounded cost. No decorative animation should interfere with scrolling, typing, navigation or synchronization.

### 5. Gmail → Mail Organizer recovery experience
Design and implement the reusable sync/recovery UI shell, but do not invent real sync data in this phase.

When real synchronization later takes measurable time:
- Gmail mark is on the left.
- Mail Organizer mark is on the right.
- A mail/message visual travels from Gmail toward Mail Organizer.
- Stage text says what is actually happening.
- Use determinate progress only when the underlying sync can provide a trustworthy estimate.
- Otherwise use an honest indeterminate state.
- Do not fabricate percentages.
- Keep the user informed if they can safely enter the app while sync continues.
- The motion must simplify automatically on lower capability profiles.

Use fixtures only as clearly labeled development data and keep the UI architecture ready for the real Phase 4 sync engine.

### 6. Core Android shell
Implement the initial application shell, navigation structure, responsive layouts, light/dark themes, loading/empty/error/offline states and accessibility foundations according to design.md.

### 7. Visual consistency gate
Before completion, review every implemented screen together:
- typography
- spacing
- icon sizing
- touch targets
- colors
- surfaces/elevation
- shapes
- navigation
- animation behavior
- light/dark mode
- large font/accessibility behavior

Fix inconsistencies before marking the phase complete.

## Mandatory verification
- Build with Gradle wrapper.
- Install and launch on an Android emulator and/or real device.
- Use ADB for install, launch, force-stop, logcat, dumpsys and screenshots; screenrecord where useful.
- Inspect frame/jank behavior where practical.
- Validate at least a low-capability and higher-capability performance profile.
- Validate reduced-motion/accessibility behavior.
- Validate light and dark themes.
- Validate supported display configurations without assuming maximum refresh rate.
- Fix, rebuild, reinstall and retest after defects.
- Never expose credentials, tokens or sensitive mail data in logs.

## Documentation and stop condition
Update editor-rules.md with any genuinely permanent rule discovered during implementation. Update spec.md only after real verification. Record blockers as [!]. Do not implement Phase 2 or later in this session. Stop when Phase 1 is verified.