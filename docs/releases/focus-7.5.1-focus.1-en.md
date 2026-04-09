# BliPai Focus v7.5.1-focus.1

Release date: 2026-04-08

## Highlights
- This release aligns the Focus line with upstream `7.5.1`. The official `7.5.1` tag points to the same source commit as `7.5.0`, so no new upstream source merge was required here; the work in this Focus release is version-line sync, release-note sync, and a local player Home-button fix.

## Main changes
- Syncs the Focus version line to `7.5.1-focus.1 / 161`, including README, Focus changelog, release notes, and the in-app update presentation.
- Fixes the player top-left Home button so standard video detail pages, portrait fullscreen, and tablet layouts now all jump straight to Home instead of falling back to the previous video/back-stack behavior.
- Keeps the back arrow unchanged; this release only corrects the navigation target of the house icon.
- Focus continues to ship the coexist package name, Focus-only update source, Home FOLLOW grouping/sorting/auto-sync, release-only asset selection, and the Focus GitHub / Telegram first-use entry points.
- Re-audits the `bli` repository and confirms there are no cross-project residues or accidentally mixed external files.

## Verification
- Passed `:app:testDebugUnitTest`.
- Passed targeted tests covering the player Home navigation chain.
- Passed `:app:assembleRelease`.
