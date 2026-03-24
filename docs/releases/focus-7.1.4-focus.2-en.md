# BliPai Focus v7.1.4-focus.2

Release date: 2026-03-24

## Highlights
- This patch release stays on upstream `BiliPai v7.1.4` and focuses only on two Home FOLLOW regressions: refreshed cards appearing too early during pull-to-refresh, and load-more failing intermittently right after refresh.

## Manual refresh no longer swaps the list too early
- FOLLOW manual pull-to-refresh now stages the refreshed result until the pull indicator has fully settled and the refresh state is actually over.
- The refresh flow also blocks background preview/config-driven follow updates from rewriting the visible list while the pull animation is still active.

## Load-more resumes immediately after refresh
- FOLLOW now re-arms tail pagination as soon as refresh completion and top-reset handling finish, so reaching the bottom can trigger the next batch again without switching tabs away and back.
- This also fixes the stale state capture in the tail-trigger path, making the latest pagination gate values participate in the decision immediately after refresh.

## Verification
- Passed `:app:compileDebugKotlin`.
- Passed `HomePullRefreshUiPolicyTest`, `HomeCategoryPagePolicyTest`, `HomeFollowFocusPolicyTest`, and `HomeFollowFastFeedCoordinatorTest`.
- Passed `:app:assembleRelease`.
