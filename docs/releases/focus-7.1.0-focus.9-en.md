# BliPai Focus v7.1.0-focus.9 Release Notes

BliPai Focus `v7.1.0-focus.9` stays on top of upstream `BiliPai v7.1.0`. This update improves the refresh feel of Home Follow by switching to a “fast first paint, then background fill” strategy.

## Highlights

- Home Follow refreshes no longer stay blocked for a long time just to fill as many filtered results as possible before the first render; the first visible batch now appears sooner with a smaller foreground prefetch budget.
- Once the first batch is on screen, additional Dynamic pages can continue loading in the background to stabilize the final visible count.
- The previous `1000`-creator default follow-sync budget is preserved and does not regress to `50`.
- README, README_EN, CHANGELOG, and Focus Changelog are all updated to the `focus.9` release line and links.

## Focus defaults

- Home `Recommend / Popular / Live / Game / Partition` entries hidden by default
- Search hot list disabled by default while keeping suggestions, discovery, results, and search history
- Watch history supports one-tap clear-all
- Related videos below video detail hidden by default, restorable from Focus settings
- Shared local follow-group filtering across Dynamic and Home Follow, with a master toggle

## Version info

- Focus Version: `7.1.0-focus.9`
- Upstream Base: `7.1.0`
- VersionCode: `132`
- Tag: `v7.1.0-focus.9`

## Artifact names

- Debug APK: `BliPai-Focus-debug-7.1.0-focus.9-debug.apk`
- Release APK: `BliPai-Focus-release-7.1.0-focus.9.apk`

## Test note

- Validation currently covers only `realme Neo 7` and `Lenovo Y700 2023`
- Please open an issue if you see compatibility or behavior problems on other devices
- Focus currently tries to stay on the same upstream major version, and for now only follows upstream major releases before rolling Focus sub-versions on that baseline
