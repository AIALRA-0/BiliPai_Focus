# BliPai Focus v7.1.4-focus.6

Release date: 2026-03-24

## Highlights
- This release fixes the root cause behind Home FOLLOW ordering so creator/time sorting and random ordering all run on the real publish timestamp.

## Main changes
- FOLLOW dynamic items now preserve the author-module `pub_ts` when mapped into `VideoItem`.
- `Publish time desc/asc` and `Creator cluster desc/asc` now correctly split late-to-early from early-to-late.
- `Random` keeps randomness, but the overall list is now always ordered newest-to-oldest; randomness only affects items inside the same publish-time layer.
- `loadMore` now appends using the active sorted result instead of raw unsorted follow-feed order.

## Verification
- Passed `:app:testDebugUnitTest --tests "*HomeFollowFocusPolicyTest" --tests "*HomeFollowFeedMappingPolicyTest"`.
- Passed `:app:assembleRelease`.
