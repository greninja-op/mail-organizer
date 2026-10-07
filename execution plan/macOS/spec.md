# macOS Specification

## Authority
Use this order when resolving implementation questions: requirements > spec > design > editor-rules > existing architecture/decisions > official platform/API documentation > engineering judgment. Do not silently discard newer repository decisions.

Statuses: `[ ]` not started, `[-]` in progress, `[x]` verified complete, `[!]` blocked.

## Architecture
KMP owns platform-neutral domain/data/business behavior. macOS owns SwiftUI presentation, macOS lifecycle/scenes, commands, menu bar integration where approved, Keychain, OS notifications, background mechanisms, native windowing, accessibility integration, and release/signing/notarization.

The macOS application must consume shared behavior through a deliberate adapter boundary. Do not fork business logic into Swift merely because an API is easier there.

## Desktop contract
The main workspace should provide mailbox/category context, company/category context, a rich message list, selected message/conversation detail, and optional contextual inspector. The layout must adapt to window width instead of relying on a fixed phone-style screen.

Navigation must preserve Gmail familiarity without cloning proprietary artwork or source. Search belongs in the primary desktop hierarchy. Toolbar, sidebar, split view, keyboard commands, context menus, and selection state must remain coherent.

## Security contract
Use official OAuth/browser flows and least privilege. Store credentials/tokens in Keychain or an appropriate secure mechanism; never source-control or log secrets. Treat email HTML, links, attachments, sender text, and model output as untrusted.

AI is optional and secondary. It may classify/extract/suggest only through validated schemas and domain rules. It cannot call Gmail, Calendar, Tasks, filesystem, shell, browser, or OS automation tools and cannot authorize external effects.

## Execution contract
Implement one phase only. Inspect the actual repository before edits. Build, test, run, inspect, fix, rebuild/retest, update status/rules, and stop. Never claim unrun evidence. Never publish automatically.
