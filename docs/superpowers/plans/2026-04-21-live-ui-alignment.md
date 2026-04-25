# Live UI Alignment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restructure the live plaza and live room UI so `BiliPai` follows the `PiliPlus` page organization while preserving the existing Android Compose visual language and live playback stack.

**Architecture:** Keep the current live data and playback foundations in place, then add dedicated live-domain screens and room sub-surfaces around them. Focus the first pass on UI structure, routing, and state ownership before deeper functional parity work.

**Tech Stack:** Kotlin, Jetpack Compose, Navigation Compose, existing `feature/live` view models and repositories, existing live search/repository stack.

---

### Task 1: Add live-domain routes and navigation entries

**Files:**
- Modify: `/Users/yiyang/Desktop/BiliPai/app/src/main/java/com/android/purebilibili/navigation/ScreenRoutes.kt`
- Modify: `/Users/yiyang/Desktop/BiliPai/app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt`

- [ ] Add route objects for `LiveSearch`, `LiveArea`, `LiveAreaDetail`, and `LiveFollowing`.
- [ ] Wire new composable destinations in `AppNavigation.kt`.
- [ ] Repoint the live home search entry and following shortcut toward those routes.
- [ ] Run a targeted compile check for navigation references.

### Task 2: Restructure live plaza home into browse-first layout

**Files:**
- Modify: `/Users/yiyang/Desktop/BiliPai/app/src/main/java/com/android/purebilibili/feature/live/LiveListScreen.kt`
- Test: `/Users/yiyang/Desktop/BiliPai/app/src/test/java/com/android/purebilibili/feature/live/LiveListTabColorPolicyTest.kt`

- [ ] Replace the current three-parallel-tab mental model with a home page that foregrounds following summary, area switching, and content browsing.
- [ ] Keep existing data loading paths where possible, but introduce explicit UI state for selected primary area and selected secondary tag.
- [ ] Preserve current card visuals and adaptive grid rules.
- [ ] Run targeted tests plus a compile check.

### Task 3: Add dedicated live search screen

**Files:**
- Create: `/Users/yiyang/Desktop/BiliPai/app/src/main/java/com/android/purebilibili/feature/live/LiveSearchScreen.kt`
- Optionally modify: `/Users/yiyang/Desktop/BiliPai/app/src/main/java/com/android/purebilibili/feature/search/SearchViewModel.kt`

- [ ] Build a dedicated live search UI shell with search bar and `正在直播 / 主播` tabs.
- [ ] Reuse the existing live search logic instead of creating a new backend path.
- [ ] Allow numeric direct room navigation from the live search screen.
- [ ] Verify the screen compiles and integrates with navigation.

### Task 4: Add full-tag and area-detail browsing screens

**Files:**
- Create: `/Users/yiyang/Desktop/BiliPai/app/src/main/java/com/android/purebilibili/feature/live/LiveAreaScreen.kt`
- Create: `/Users/yiyang/Desktop/BiliPai/app/src/main/java/com/android/purebilibili/feature/live/LiveAreaDetailScreen.kt`
- Modify: `/Users/yiyang/Desktop/BiliPai/app/src/main/java/com/android/purebilibili/data/repository/LiveRepository.kt`

- [ ] Add screen-level models and UI for full-tag browsing.
- [ ] Add area-detail UI with secondary tag / sort row.
- [ ] Add any small repository helpers needed to support area-detail UI using existing live APIs.
- [ ] Verify compile and data flow.

### Task 5: Add dedicated following-live page

**Files:**
- Create: `/Users/yiyang/Desktop/BiliPai/app/src/main/java/com/android/purebilibili/feature/live/LiveFollowingScreen.kt`
- Modify: `/Users/yiyang/Desktop/BiliPai/app/src/main/java/com/android/purebilibili/data/repository/LiveRepository.kt`

- [ ] Build a standalone following-live screen that reuses existing followed-live loading.
- [ ] Keep visual style consistent with live home cards and spacing.
- [ ] Wire entry from the live home header/summary strip.
- [ ] Verify compile and empty-state behavior.

### Task 6: Restructure live room main page around chat / SC tabs

**Files:**
- Modify: `/Users/yiyang/Desktop/BiliPai/app/src/main/java/com/android/purebilibili/feature/live/LivePlayerScreen.kt`
- Modify: `/Users/yiyang/Desktop/BiliPai/app/src/main/java/com/android/purebilibili/feature/live/LiveRoomLayoutPolicy.kt`

- [ ] Move the room page toward an explicit player -> info -> interaction -> panel hierarchy.
- [ ] Introduce first-level interaction tabs for chat and SC.
- [ ] Keep existing player and danmaku behaviors intact.
- [ ] Verify layout in portrait and landscape compile paths.

### Task 7: Add SC and secondary room panels

**Files:**
- Create: `/Users/yiyang/Desktop/BiliPai/app/src/main/java/com/android/purebilibili/feature/live/components/LiveSuperChatSection.kt`
- Create: `/Users/yiyang/Desktop/BiliPai/app/src/main/java/com/android/purebilibili/feature/live/components/LiveContributionRankSheet.kt`
- Create: `/Users/yiyang/Desktop/BiliPai/app/src/main/java/com/android/purebilibili/feature/live/components/LiveSendDanmakuSheet.kt`
- Modify: `/Users/yiyang/Desktop/BiliPai/app/src/main/java/com/android/purebilibili/feature/live/LivePlayerViewModel.kt`

- [ ] Surface existing SC data in a dedicated section instead of mixing it only into chat.
- [ ] Add UI containers for rank and send panels.
- [ ] Expose minimal view-model state/callbacks needed by those panels.
- [ ] Verify compile and state ownership.

### Task 8: Tighten room controls and chat integration

**Files:**
- Modify: `/Users/yiyang/Desktop/BiliPai/app/src/main/java/com/android/purebilibili/feature/live/components/LivePlayerControls.kt`
- Modify: `/Users/yiyang/Desktop/BiliPai/app/src/main/java/com/android/purebilibili/feature/live/components/LiveChatSection.kt`

- [ ] Reorganize the control bars to match the approved top/bottom ownership.
- [ ] Ensure chat remains the main place for input, emoji, like, and jump-to-bottom.
- [ ] Keep existing message menu hooks and extend only what the new layout requires.
- [ ] Verify compile and targeted live tests.

### Task 9: Verification

**Files:**
- Test existing live tests and any new ones added during the refactor.

- [ ] Run: `./gradlew :app:testDebugUnitTest --tests 'com.android.purebilibili.feature.live.*'`
- [ ] If test filtering is too narrow for touched code, run: `./gradlew :app:testDebugUnitTest --tests '*Live*'`
- [ ] Run: `./gradlew :app:assembleDebug`
- [ ] Record any infra-only failures separately from product regressions.
