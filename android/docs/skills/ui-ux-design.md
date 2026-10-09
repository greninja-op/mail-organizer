# Skill: UI/UX Design

Master reference: `execution plan/android/{design.md,editor-rules.md}`.

- Gmail-familiar, never a Gmail clone. No copying Google artwork/trademarks/
  source; no AI-generated functional icons — Material Symbols, system icons,
  permitted assets, or hand-authored vectors only.
- Centralized tokens in `ui/theme/` (color, type, shapes, spacing, motion).
  Every UI change gets a consistency pass over all existing screens.
- Drawer destinations are fixed: All Inbox, Primary, Promotional, Social,
  Spam, Starred. The drawer is never a company list; company grouping lives
  inside a category. Pinning a company ≠ starring an email; starring never
  changes category.
- All Inbox rows show the *receiving account* via a compact circular
  indicator (not the sender avatar).
- Never fake progress: determinate sync UI only with trustworthy counts;
  the Gmail → Mail Organizer transfer animation runs only during real,
  meaningful sync.
- Performance: never target 120 FPS because the display supports 120 Hz.
  Respect Android's frame-rate scheduler; adapt via capability-tiered
  `PerformanceProfile` (Phase 1+); no continuous benchmarking or sensor polling.
- Build success alone never proves UI completion: every major UI change needs
  emulator/real-device validation, light+dark, font sizes, orientations,
  screenshot and jank review.
