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

Mail Organizer categories, priority, Action Required, company intelligence and its organization-first information architecture remain distinct.

## Android top app bar
The main mailbox screen uses a Gmail-familiar top structure:
- three-line navigation affordance on the left
- prominent search field/affordance at the top
- account/profile circle on the right
- the account affordance is also the entry point for account switching

Search must look native to the surrounding Material/Google-app visual language while retaining Mail Organizer's own tokens and branding.

## Navigation drawer
The drawer is intentionally compact and stable. Core destinations are:
1. All Inbox
2. Primary
3. Promotional
4. Social
5. Spam
6. Starred

Do not turn the drawer into a permanent list of detected companies. Company grouping belongs inside the selected category.

Folder state may show useful unread/count indicators. Spam should show a red unread/new indicator when applicable. Counts must represent real local state and must not be fabricated.

## All Inbox account identity
All Inbox is a unified cross-account view. Every message row must expose the source mailbox through a small circular account indicator or equivalent compact identity treatment.

The account indicator represents the receiving Mail Organizer/Gmail account, not the sender/company avatar.

Example:
- personal Gmail → one account circle
- college Gmail → another account circle
- work Gmail → another account circle

The full address may be secondary detail rather than permanent row text, but the user must be able to determine the source account without opening the message.

## Account switcher
The account switcher should follow familiar Google account-switching interaction patterns without copying inaccessible/proprietary artwork.

It must show:
- current account
- other connected accounts
- add another account
- account-specific sync/recovery state where relevant

Swiping the profile/account affordance should switch to the next account immediately where supported. Account switching must preserve the current mailbox context safely and never mix account-owned data.

When adding an account, reuse the Gmail → Mail Organizer recovery/sync visual language. Show the transfer animation only when real retrieval takes meaningful time.

## Gmail → Mail Organizer initial sync experience
When a newly connected account requires non-trivial recovery/synchronization time, show a dedicated progress experience rather than a blank screen.

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

Animation storyboard:
```
Gmail                         Mail Organizer

╭───────╮                     ╭───────╮
│   G   │                     │   ◉   │
╰───────╯                     ╰───────╯

             ✉  →  →  →  →

        Retrieving your mail...
        ███████░░░░░░░  54%
```

When progress cannot be measured honestly:
```
Gmail  ──────✉──────→  Mail Organizer

              • • •
        Retrieving your mail...
```

When retrieval is effectively instant, skip the blocking animation.

This animation must be lightweight and adaptive. On lower-performance profiles, simplify the moving mail element and reduce effects while preserving the meaning.

## Category company filtering
Inside a selected category, messages should be grouped or filterable by detected company/sender.

Example:
```
PROMOTIONAL

PINNED
Google          12
Facebook         7
Amazon           5

ALL COMPANIES
Canva             3
Notion            2
n8n               1
```

Selecting Google filters the current category to Google messages. Selecting Facebook filters the current category to Facebook messages. The category context remains visible.

Company filtering is not a new top-level navigation destination.

## Company pinning
Company pinning is independent from individual email starring.

A user may pin a company from the company grouping/filter UI. Pinned companies appear above unpinned companies within the relevant category's company filter.

Pinning a company:
- does not move mail to another category
- does not create a new navigation-drawer destination
- does not star individual messages
- should persist as user intent
- should work across company mail types while respecting the currently selected category

The company identity should be canonicalized so Google messages are grouped together rather than creating separate entries for each sender address where the intelligence engine can reliably determine the same company.

## Individual email starring
Every message row should have a familiar Star control:
```
╭────────────────────────────────────────╮
│  🟣  Notion Team                 ☆     │
│      Your temporary login code         │
╰────────────────────────────────────────╯
```

Tapping ☆ → ★ marks that specific email as Starred. It remains in its original category and becomes available from the global Starred destination.

Starred state is separate from company pinning and automated classification.

## Promotional and noise handling
Promotional mail should be organized rather than silently deleted. The main experience should prioritize useful mail and avoid flooding the user with promotional noise, while retaining a deliberate Promotional destination for users who need to inspect it.

Social mail receives the same organized treatment.

## Spam
Spam is a first-class destination. New/unread spam should produce a visible red indicator. Legitimate mail that lands in Spam must have a clear user-driven recovery path such as Not Spam.

Spam classification must remain distinct from ordinary promotional classification.

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