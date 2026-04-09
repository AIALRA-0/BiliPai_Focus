# BliPai Focus v7.5.1-focus.2

Release date: 2026-04-08

## Highlights
- This release stays on the upstream `7.5.1` line. The official `7.5.1` tag still points to the same source commit as `7.5.0`, so no new upstream code merge was needed here; the focus of this patch is to finish the player Home-button fix and ship it cleanly.

## Main changes
- Syncs the Focus version line to `7.5.1-focus.2 / 162`, including README, Focus changelog, release notes, and the in-app update presentation.
- Fixes the player top-left Home button so it now hard-jumps to the real Home root instead of only leaving the current page, the previous video, or appearing to do nothing because of the current back stack state.
- Portrait fullscreen playback follows the same behavior now, without first dropping back into the current detail page before attempting Home navigation.
- Keeps the back arrow unchanged; this release only corrects the navigation target of the house icon.
- Focus continues to ship the coexist package name, Focus-only update source, Home FOLLOW grouping/sorting/auto-sync, release-only asset selection, and the Focus GitHub / Telegram first-use entry points.

## Verification
- Passed `:app:testDebugUnitTest --tests "*AppTopLevelNavigationPolicyTest" --tests "*VideoDetailScreenPolicyTest" --tests "*PortraitVideoPagerPolicyTest"`.
- Passed `:app:assembleRelease`.
