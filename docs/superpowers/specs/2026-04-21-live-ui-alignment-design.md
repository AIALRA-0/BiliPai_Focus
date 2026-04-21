# Live UI Alignment Design

## Goal

Align `BiliPai`'s live experience to the `PiliPlus` product structure at the UI level, while preserving the existing Android Compose design language and reusing the current player, danmaku, repository, and navigation foundations.

## Scope

This design covers UI structure and routing for:

- Live plaza home
- Live search
- Live area index
- Live area detail
- Following-live list
- Live room primary layout
- Live room chat / super chat primary tabs
- Live room rank / send / moderation secondary panels

This design does not require first-pass parity for every interaction behind those surfaces. If a secondary action is not already implemented, the UI entry may exist first and be wired to the closest existing behavior.

## Constraints

- Preserve the current `BiliPai` visual language, typography, motion, surfaces, and adaptive Compose patterns.
- Do not port Flutter/GetX structure from `PiliPlus`.
- Reuse the current live playback and danmaku stack in `feature/live`, `LiveRepository`, and `LivePlayerViewModel`.
- Keep changes localized to live feature files, navigation, and minimal search/live repository glue.
- Avoid introducing new dependencies.

## Product Direction

The target is product-structure alignment, not pixel replication.

The resulting user flows should feel like:

- `LiveListScreen -> LiveSearchScreen`
- `LiveListScreen -> LiveAreaScreen -> LiveAreaDetailScreen`
- `LiveListScreen -> LiveFollowingScreen`
- `LiveListScreen -> LivePlayerScreen`
- `LivePlayerScreen -> Chat / SC tabs`
- `LivePlayerScreen -> Contribution rank / Send danmaku / Moderation panels`

## Existing Foundations To Reuse

- `LiveListScreen` already owns the live feature entry and live home data loading.
- `LivePlayerScreen` and `LivePlayerViewModel` already own playback, quality switching, audio-only mode, PiP, danmaku connection, SC seed loading, and live room state.
- `LiveChatSection` already renders live chat items, jump-to-bottom affordance, input, emoji trigger, like trigger, and user message menus.
- Search already supports live-room search in the global search stack.

## UI Architecture

### 1. Live Plaza

`LiveListScreen` becomes the live domain root page, not just a three-tab content container.

The home page should present:

- Header bar with back, search entry, and following-live shortcut
- A following-live summary strip
- A horizontal primary area selector with `推荐` as the first fixed option
- Inline content switching between recommended feed and selected first-level area feed
- A secondary tag row when a non-recommend area is active
- Grid-based live room content below
- Explicit entries to `全部标签` and area-deeper browsing

This matches `PiliPlus`'s browsing behavior more closely than the current `推荐 / 分区 / 关注` parallel-tab structure.

### 2. Live Search

`LiveSearchScreen` becomes a dedicated live-domain search page.

Structure:

- Search app bar with editable field
- Two result tabs: `正在直播` and `主播`
- Empty-state until a submission occurs
- Numeric input shortcut that may navigate directly to the room
- Live-domain result layout, not a generic search shell

Existing live search repository logic should be reused instead of inventing a new data path.

### 3. Live Area Index

`LiveAreaScreen` becomes the full-tag page.

Structure:

- App bar
- Optional "我的常用标签" section when logged in
- Top-level area tabs
- Grid of child tags within each tab
- Entry from both the home header and future moderation/search flows where useful

### 4. Live Area Detail

`LiveAreaDetailScreen` becomes the focused browsing page for an area or tag.

Structure:

- App bar with area title
- Secondary tag / sort row
- Grid or list of live cards
- Pull-to-refresh and load-more behavior

This page owns deeper exploration, keeping the home page lighter.

### 5. Following-Live Page

`LiveFollowingScreen` becomes a dedicated page for the user's followed live rooms.

Structure:

- App bar
- Count summary
- Live room grid
- Empty state for no currently-live followings

## Live Room UI

### 1. Primary Layout

`LivePlayerScreen` should be reorganized into four layers:

- Player surface
- Room information band
- Primary interaction section
- Secondary panel layer

The room page should not attempt to display every sub-surface inline.

### 2. Player Surface

Retain current foundations:

- Playback
- Quality selection
- PiP
- Audio-only mode
- Background playback
- Danmaku overlay
- Brightness/volume gestures

Reorganize controls closer to the `PiliPlus` product structure:

- Top bar: back, title/subtitle, PiP, audio-only, background playback, shutdown timer, player info
- Bottom bar: play/pause, refresh, danmaku toggle, danmaku settings, aspect ratio, quality, fullscreen

### 3. Room Information Band

A stable room information band should own:

- Live title
- Anchor name
- Watched text
- Online rank text
- Live duration
- Follow action

These fields should not remain visually fragmented.

### 4. Primary Interaction Section

The main body below the player should use first-level tabs:

- `聊天`
- `SC`

`聊天` tab:

- Message stream
- Jump-to-bottom affordance
- Input bar
- Like / emoji / send actions

`SC` tab:

- Dedicated list
- No mixing with the normal message stream in the main presentation

### 5. Secondary Panels

Secondary live interactions should be presented as sheets or dedicated transient panels:

- Contribution rank
- Send danmaku expanded panel
- Moderation / blocking / reporting entry points
- More-room actions

These should not overload the primary room body.

### 6. Adaptive Behavior

Target layout rules:

- Portrait: player on top, room info + interaction below
- Landscape non-fullscreen: player and interaction may appear in split mode
- Landscape fullscreen: player prioritized, interaction collapsible
- PiP: player only

The first pass may use simpler Compose layouts than `PiliPlus`, but the ownership of each region must stay clear.

## File-Level Design

### New screens

- `feature/live/LiveSearchScreen.kt`
- `feature/live/LiveAreaScreen.kt`
- `feature/live/LiveAreaDetailScreen.kt`
- `feature/live/LiveFollowingScreen.kt`

### New room sub-surfaces

- `feature/live/components/LiveSuperChatSection.kt`
- `feature/live/components/LiveContributionRankSheet.kt`
- `feature/live/components/LiveSendDanmakuSheet.kt`

### Likely touched existing files

- `feature/live/LiveListScreen.kt`
- `feature/live/LivePlayerScreen.kt`
- `feature/live/LivePlayerViewModel.kt`
- `feature/live/components/LiveChatSection.kt`
- `feature/live/components/LivePlayerControls.kt`
- `navigation/ScreenRoutes.kt`
- `navigation/AppNavigation.kt`

## State Ownership

- `LivePlayerViewModel` remains the single source of truth for room state.
- `LiveListScreen` should keep its own browsing-state view model for live plaza browsing.
- New plaza pages may use lightweight screen-local view models or extracted controllers, but should not move business logic into composables.
- Search should reuse the existing live search repository path and only add live-domain UI glue where needed.

## Testing Expectations

The first implementation pass should add or update targeted unit tests for:

- Live route creation
- Live plaza tab / area / page selection policy as extracted
- Live room layout mode policy if new policy logic is introduced

UI-heavy verification can be deferred to focused regression tests after the structural refactor lands.

## Out Of Scope For First Pass

- Full behavioral parity for every `PiliPlus` moderation action
- Desktop-specific live features from `PiliPlus`
- Refactoring unrelated live playback internals
- Replacing the current danmaku or player foundation

## Success Criteria

- The live domain has dedicated pages matching the `PiliPlus` product structure.
- The live home is browse-first and no longer only a flat three-tab container.
- Search, area index, area detail, and following-live pages all exist as standalone screens.
- The live room uses explicit chat / SC primary tabs.
- Secondary room actions move into dedicated panels instead of crowding the main page.
- The implementation remains consistent with `BiliPai`'s Compose architecture and current visual language.
