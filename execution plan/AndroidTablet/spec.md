# Mail Organizer Android Tablet — Execution Specification

## Authority
requirements → spec → design → editor-rules → verified shared architecture/decisions → official Android/Google/Kotlin documentation → engineering judgment.

## Sequential execution
Only the first incomplete AndroidTablet phase executes. Read only that phase after inspecting the actual repository.

Every phase:
inspect → implement only this phase → build → targeted automated tests → emulator/device runtime → large-screen/accessibility/security/data/performance QA → fix → rebuild/reinstall/retest → status/docs → STOP.

Never claim unrun validation.

## Shared mobile contract
Android phone and Android tablet consume the same common KMP/Android business/data source wherever practical. Shared: models, repositories, Gmail normalization, sync, classification, company intelligence, search/index, rules, priority/Action Required, temporal/conversation intelligence, Action Engine, integration abstractions, persistence, automation and AI routing.

Tablet-specific: Compose presentation, large-screen navigation, window-size adaptation, pointer/keyboard/mouse, multi-window and Android tablet OS integration.

Do not create duplicate business engines.

## Large-screen contract
No phase is complete if it works only at one resolution. Relevant phases test portrait, landscape, narrow split, medium width, full-screen tablet and multi-window configurations.

Use content-driven/adaptive layout decisions, not device-name checks.

## Performance
Optimize sustained responsiveness, memory, battery, thermal behavior and rendering. A 120 Hz display is not a guarantee of 120 FPS.

## Release
Production signing, Play configuration, OAuth release identity, accurate data/privacy declarations and release artifact inspection are separate gates. Never publish automatically.

**STOP AFTER PHASE 30.**