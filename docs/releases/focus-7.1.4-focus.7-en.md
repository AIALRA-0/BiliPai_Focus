# BliPai Focus v7.1.4-focus.7

Release date: 2026-03-24

## Highlights
- This release restores fully random Home FOLLOW ordering and changes the default app-side Home FOLLOW order from `Random` to `Publish time desc`.

## Main changes
- `Random` now randomizes the whole visible pool again instead of behaving almost like publish-time ordering.
- New installs and unset configs now default Home FOLLOW to `Publish time desc`, making newest-first the default experience.
- `Publish time desc/asc` and `Creator cluster desc/asc` continue using the real publish timestamp foundation fixed in `focus.6`.

## Verification
- Passed `:app:testDebugUnitTest --tests "*HomeFollowFocusPolicyTest" --tests "*HomeFollowFeedMappingPolicyTest" --tests "*FocusFollowGroupStorePolicyTest"`.
- Passed `:app:assembleRelease`.
