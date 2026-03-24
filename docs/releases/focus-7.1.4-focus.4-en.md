# BliPai Focus v7.1.4-focus.4

Release date: 2026-03-24

## Highlights
- This release continues tightening the Home FOLLOW refresh timing, first-batch windowing, and paging gate, and adds configurable ordering modes for the Home FOLLOW feed.

## Main changes
- Manual pull-to-refresh now stages the new FOLLOW list and commits it only after the pull indicator and content bounce have fully settled, removing the early upward flicker.
- Entering FOLLOW, recalculating it after app reopen, and re-presenting it after follow-group config changes are all hard-limited to the first `16` visible items.
- FOLLOW paging still expands in `16`-item batches and only unlocks after real downward content scroll has been observed.
- The follow-group sheet now includes a `Home FOLLOW order` selector with `Random`, `Creator cluster desc`, `Creator cluster asc`, `Publish time desc`, and `Publish time asc`; this setting affects Home FOLLOW only.

## Verification
- Passed `:app:compileDebugKotlin`.
- Passed `FocusFollowGroupStorePolicyTest`, `HomePullRefreshUiPolicyTest`, `HomeCategoryPagePolicyTest`, `HomeFollowFocusPolicyTest`, and `HomeFollowFastFeedCoordinatorTest`.
- Passed `:app:assembleRelease`.
