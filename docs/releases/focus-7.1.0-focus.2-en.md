# BliPai Focus v7.1.0-focus.2 Release Notes

BliPai Focus `v7.1.0-focus.2` is based on upstream `BiliPai v7.1.0` and continues refining quieter defaults, follow-group management, and Dynamic follow loading for the Focus usage style.

## Highlights

- Follow-group management stays in a single expandable card, so visibility, rename, delete, and member assignment now happen in one place.
- The group card header layout is tightened so status text no longer gets squeezed by edit, delete, and expand actions on narrower screens.
- Dynamic follow users now restore from local cache first and hydrate in parallel during startup, instead of waiting for the main feed to finish and then delaying hydration.
- The first-run disclaimer, README, CHANGELOG entry points, and Focus docs are now aligned with the Focus repository and release track.

## Focus defaults

- Home `Recommend / Popular / Live / Game / Partition` entries hidden by default
- Search hot list disabled by default while keeping suggestions, discovery, results, and search history
- Watch history supports one-tap clear-all
- Related videos below video detail hidden by default, restorable from Focus settings
- Shared local follow-group filtering across Dynamic and Home Follow, with a master toggle

## Version info

- Focus Version: `7.1.0-focus.2`
- Upstream Base: `7.1.0`
- VersionCode: `125`
- Tag: `v7.1.0-focus.2`

## Artifact names

- Debug APK: `BliPai-Focus-debug-7.1.0-focus.2-debug.apk`
- Release APK: `BliPai-Focus-release-7.1.0-focus.2.apk`

## Verification

- `:app:testDebugUnitTest`
- `:app:lintDebug`
- `:app:assembleDebug`
- `:app:assembleRelease`
