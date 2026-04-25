# BliPai Focus v8.0.0-Alpha3-focus.1

## Version
- Based on upstream `BiliPai v8.0.0-Alpha3`
- Focus version: `8.0.0-Alpha3-focus.1 / 165`

## Upstream sync
- Fully syncs the official `7.9.7 / 7.9.8 / 7.9.9 / 8.0.0-Alpha1 / Alpha2 / Alpha3` line, including search discovery improvements, list watch progress, live landscape chat fallback, comment image export with QR, image-preview text toggles, real collection subscription calls, download retries, and player seek / gesture refinements.
- Keeps the Focus-specific coexist package name, Focus-only update source, FOLLOW grouping/sorting/auto-sync, and the first-run Focus channel entry points.

## Focus fixes
- Hardens the search screen against entry and typing crashes by making autofocus a one-shot guarded request and adding suggestion-mapping fallbacks.
- Fixes the occasional frozen progress bar after seek by timing out stale pending seek sessions instead of holding the UI at the dragged position forever.
- Fixes in-app update comparisons for prerelease Focus versions such as `8.0.0-Alpha3-focus.1`, so Alpha / RC / focus combinations compare correctly.
- Carries forward the Focus pagination stability patch set: separate search-discover toggle, shared pagination policy, and the `sourceHasMore / visibleHasMore / continuationAllowed` split for the default multi-follow dynamic timeline.

## Release notes
- Release APK only
- Continues to coexist with the official app
