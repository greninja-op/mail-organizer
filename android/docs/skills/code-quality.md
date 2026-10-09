# Skill: Code Quality

Master reference: `execution plan/android/editor-rules.md`.

- Optimize for: correctness → privacy → security → maintainability →
  testability → UX → performance → speed of development. In that order.
- Prefer small, pure, testable units; `MoResult`/`MoError` over thrown
  exceptions for expected failures.
- No dead code, no speculative abstractions, no commented-out blocks.
- Public APIs get KDoc explaining *why*, not just *what*.
- Android lint is part of every phase's verification; zero new warnings is the
  bar for touched code.
- Keep `docs/architecture.md` and `docs/development-status.md` truthful:
  update them when decisions change, never let them drift from the code.
