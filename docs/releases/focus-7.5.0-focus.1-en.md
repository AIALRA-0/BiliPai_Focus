# BliPai Focus v7.5.0-focus.1

Release date: 2026-04-06

## Highlights
- This release moves the Focus line onto upstream `7.5.0`, fully absorbing the published changes from `7.3.1 -> 7.5.0` across creator-space loading, playback, live recovery, danmaku, dynamic navigation, cache clearing, and build verification, while preserving the Focus coexist package, Focus-only updates, FOLLOW customizations, and first-use entry points.

## Main changes
- Syncs the upstream aggregated creator-space first-screen architecture, so creator spaces now bootstrap from the aggregate seed first and then hydrate the first contribution page and the remaining non-critical data in stages.
- Keeps the Focus creator-space compatibility layer: creator entries opened from search still accept stringified numeric fields, and the suspicious-empty first-page path still retries once after refreshing the `WBI key`.
- Syncs the upstream playback and stability work from `7.3.2 -> 7.4.3`, including quality mode, cover-to-first-frame transitions, playback diagnostics, seek recovery, selectable cache clearing, live foreground/background recovery, danmaku sync/filter/line-height tuning, and collection sorting.
- Syncs the upstream `7.5.0` additions for dynamic back-to-top, home author-row alignment, and improved portrait fullscreen transitions.
- Further stabilizes portrait aspect-ratio recovery: when leaving portrait fullscreen for in-app navigation, external navigation, or background transitions, the inline collapse offset, viewport transform, and temporary size overrides are reset before the shared player and playback sync attach again, reducing squashed frames, black bars, and wrong-aspect flashes on some systems.
- Focus continues to ship the coexist package name, Focus-only update source, Home FOLLOW grouping/sorting/auto-sync, release-only asset selection, and the Focus GitHub / Telegram first-use entry points.

## Verification
- Passed `:app:testDebugUnitTest`.
- Passed `:app:assembleRelease`.
- Passed `:baselineprofile:pixel6Api31BenchmarkAndroidTest`.
