# Windows Specification

## Authority
requirements > spec > design > editor-rules > existing architecture/decisions > official Windows/API documentation > engineering judgment.

## Architecture
Rust is the authoritative implementation layer for platform-neutral desktop behavior and the Windows product core. It is designed for reuse by macOS. The Windows UI layer should also be Rust-first. Native Windows SDK APIs may be reached through safe Rust bindings/adapters, but business logic must not move into another language.

Core layers: Presentation → Application/Use Cases → Domain → Data → External APIs, implemented in Rust where platform-neutral. Windows OS integration is an adapter boundary.

## Desktop contract
Adaptive sidebar + message list + detail + optional inspector; native desktop resizing; toolbar/command behavior; keyboard shortcuts; pointer/trackpad/mouse; context menus; multi-window state; accessibility; dark/light themes. Preserve Gmail-familiar information hierarchy without cloning proprietary artwork/source.

## Security
Official OAuth, least privilege, secure credential storage, no secrets in source/logs. Email is untrusted. AI is optional, schema/domain validated, isolated and cannot authorize external effects.

## Execution
One phase only. Inspect, implement, test, build, run, inspect, fix, rebuild/retest, update status/rules, stop. Never publish automatically.