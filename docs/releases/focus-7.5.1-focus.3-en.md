# BliPai Focus v7.5.1-focus.3

Release date: 2026-04-08

## Highlights
- This build stays on the official `7.5.1` line. The official `7.5.1` tag still points to the same source commit as `7.5.0`, so no new upstream code is introduced here; this `.3` release is primarily used to verify the Focus in-app update detection and release pipeline.

## Main changes
- Syncs the Focus version line to `7.5.1-focus.3 / 163`, including README, Focus changelog, release notes, and the in-app update presentation.
- Keeps the `v7.5.1-focus.2` player Home-root fix unchanged.
- Continues to ship only the `release` APK, with no `debug` asset attached.

## Verification
- Passed `:app:assembleRelease`.
