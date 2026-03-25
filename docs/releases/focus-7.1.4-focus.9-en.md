# BliPai Focus v7.1.4-focus.9

Release date: 2026-03-24

## Highlights
- This release continues to fix flicker in FOLLOW random refresh and restores `Random` to true whole-pool random ordering.

## Main changes
- The pending FOLLOW refresh path now uses a no-overshoot return motion and a longer stable commit window so the new list is not applied before the bounce is actually done.
- `Random` no longer uses creator interleaving or new-item front-loading; it now randomizes the full candidate pool by random key.
- This does not change the default `Publish time desc`, the 16-card first batch, or FOLLOW load-more grouping.

## Verification
- Passed `:app:testDebugUnitTest --tests "*HomePullRefreshUiPolicyTest" --tests "*HomeFollowFocusPolicyTest"`.
- Passed `:app:assembleRelease`.
