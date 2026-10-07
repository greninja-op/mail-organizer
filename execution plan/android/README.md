# Android UI Execution Program

This folder contains the Android-first UI/performance program. It is intentionally separate from future iOS, iPadOS, macOS and Windows UI programs.

## Goal
Perfect the Android UI and interaction foundation before platform expansion.

## Design reference
Follow execution plan/design.md and current official Android/Material documentation. Material 3 / Material 3 Expressive is the baseline. Gmail/Google-app familiarity is a target; proprietary Google artwork/source code is not copied.

## Performance reference
The UI must adapt to the device rather than blindly target the highest display refresh rate. Use Android's frame-rate scheduling/adaptive refresh mechanisms, lightweight capability profiling, thermal/power signals where supported, and measured app performance. The goal is sustained smoothness, low jank and reasonable battery use.

## Program sequence
1. Foundation and visual language — Phase 01.
2. Local data and real loading states — Phase 02 onward as the data architecture lands.
3. Real Google authentication — Phase 03.
4. Real Gmail recovery/sync and the Gmail → Mail Organizer transfer animation — Phase 04.
5. Progressive refinement and visual QA throughout all later Android phases.
6. Final performance/accessibility/device QA in Phases 24, 28 and 30.

## Non-negotiables
- No AI-generated functional icons or Google/Gmail marks.
- No fake Google credential form.
- No fake progress percentages.
- No 120 FPS target merely because the display supports 120 Hz.
- No continuous hardware benchmarking or unnecessary sensor polling.
- No animation that blocks core interaction.
- Every UI change gets a consistency review against the whole existing Android UI.
- Build success alone never proves UI completion.