# BliPai Focus v7.3.0-focus.1

Release date: 2026-03-31

## Highlights
- This release moves Focus onto the upstream `7.3.0` baseline, bringing in the playback recovery refactor, seek-session handling, SponsorBlock stabilization, and progress markers while preserving the Focus coexist package, Focus-only update flow, and Home FOLLOW customizations.

## Main changes
- Syncs the upstream `7.2.3/7.3.0` playback lifecycle coordinator, explicit user-action tracking, recovery-compatible seek, scrubbing sessions, and expanded playback diagnostics.
- Syncs upstream SponsorBlock pre-filtering/sorting/caching, stable seek re-arming, manual skip UI, and progress-bar hint modes.
- The SponsorBlock toggle in Plugins and Playback Settings now writes back to the same settings source, removing the old double-write state split.
- Removes two unused legacy `SponsorBlockUseCase` wrapper layers so the merged codebase no longer carries redundant historical video-path abstractions.
- Switches the `baselineprofile` / Macrobenchmark target package and video-detail component over to the Focus coexist package, so the performance gate can keep validating the Focus release line directly.
- Keeps the Focus coexist package name, Focus-only update source, first-use Focus entry points, and Home FOLLOW grouping/sorting/auto-sync flow intact.

## Verification
- Passed `:app:testDebugUnitTest`.
- Passed `:app:assembleRelease`.
- Passed `:baselineprofile:pixel6Api31BenchmarkAndroidTest` (the Home Feed benchmark remains intentionally `@Ignore`; startup and video-detail benchmarks passed).
