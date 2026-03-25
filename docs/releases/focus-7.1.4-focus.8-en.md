# BliPai Focus v7.1.4-focus.8

Release date: 2026-03-24

## Highlights
- This release continues to harden Home FOLLOW pull-to-refresh so the new list does not swap in before the bounce animation is truly over.

## Main changes
- FOLLOW manual pull-to-refresh now waits for refresh completion, indicator reset, content-offset reset, and one extra stable settle window before applying the new list.
- The new list and the post-refresh jump-to-top now happen after that stable window, preventing cards from flashing upward above the refresh chrome.
- The extra delay only applies to FOLLOW pending refresh presentation. It does not change other tabs, load-more behavior, first-batch limits, or the current sort logic.

## Verification
- Passed `:app:testDebugUnitTest --tests "*HomePullRefreshUiPolicyTest" --tests "*HomeFollowFocusPolicyTest" --tests "*HomeFollowFeedMappingPolicyTest" --tests "*FocusFollowGroupStorePolicyTest"`.
- Passed `:app:assembleRelease`.
