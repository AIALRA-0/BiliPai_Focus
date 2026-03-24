# BliPai Focus v7.1.4-focus.5

Release date: 2026-03-24

## Highlights
- This release only adjusts Home FOLLOW refresh semantics, cleanly separating `Random` from `Creator cluster` and `Publish time` ordering modes.

## Main changes
- `Random` refreshes still change the seed, keep creator-interleaved reshuffling, and prioritize newly fetched items.
- `Creator cluster desc/asc` and `Publish time desc/asc` no longer add extra shuffle behavior on refresh, and no longer force newly fetched keys to the front.
- Non-random modes now simply absorb new items into the visible pool and re-sort deterministically by their own rules.

## Verification
- Passed `:app:testDebugUnitTest --tests "*HomeFollowFocusPolicyTest"`.
- Passed `:app:assembleRelease`.
