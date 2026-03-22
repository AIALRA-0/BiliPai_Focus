# BliPai Focus v7.1.0-focus.3 Release Notes

BliPai Focus `v7.1.0-focus.3` is based on upstream `BiliPai v7.1.0` and keeps refining the home top-entry surface, follow-group management, and Dynamic follow loading for the Focus usage style.

## Highlights

- Filtered home top categories now stay strictly centered and symmetric, including the single-tab case such as keeping only `Follow`.
- Both the iOS-style and MD3-style top category rows now apply the same centered layout rule when only 1 to 3 categories remain after Focus filtering.
- The previous follow-group management refinements and Dynamic follow hydration improvements remain in place.
- README, CHANGELOG entry points, and Focus release links are now synced to the `focus.3` release line.

## Focus defaults

- Home `Recommend / Popular / Live / Game / Partition` entries hidden by default
- Search hot list disabled by default while keeping suggestions, discovery, results, and search history
- Watch history supports one-tap clear-all
- Related videos below video detail hidden by default, restorable from Focus settings
- Shared local follow-group filtering across Dynamic and Home Follow, with a master toggle

## Version info

- Focus Version: `7.1.0-focus.3`
- Upstream Base: `7.1.0`
- VersionCode: `126`
- Tag: `v7.1.0-focus.3`

## Artifact names

- Debug APK: `BliPai-Focus-debug-7.1.0-focus.3-debug.apk`
- Release APK: `BliPai-Focus-release-7.1.0-focus.3.apk`

## Verification

- `:app:testDebugUnitTest`
- `:app:lintDebug`
- `:app:assembleDebug`
- `:app:assembleRelease`
