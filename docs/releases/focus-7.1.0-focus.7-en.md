# BliPai Focus v7.1.0-focus.7 Release Notes

BliPai Focus `v7.1.0-focus.7` stays on top of upstream `BiliPai v7.1.0`. This update is a small UI refinement that trims the `Refresh / Add` buttons in the follow-group header so they sit closer to the visual height of the search field.

## Highlights

- Reduces the visual height of the `Refresh / Add` buttons in the follow-group header so they no longer feel too thick.
- Keeps the same rounded styling and layout consistency, without changing the Focus filtering, prefetch, and empty-state fixes shipped in the previous version.
- Syncs README, README_EN, CHANGELOG, and Focus Changelog to the `focus.7` release line and release-entry links.

## Focus defaults

- Home `Recommend / Popular / Live / Game / Partition` entries hidden by default
- Search hot list disabled by default while keeping suggestions, discovery, results, and search history
- Watch history supports one-tap clear-all
- Related videos below video detail hidden by default, restorable from Focus settings
- Shared local follow-group filtering across Dynamic and Home Follow, with a master toggle

## Version info

- Focus Version: `7.1.0-focus.7`
- Upstream Base: `7.1.0`
- VersionCode: `130`
- Tag: `v7.1.0-focus.7`

## Artifact names

- Debug APK: `BliPai-Focus-debug-7.1.0-focus.7-debug.apk`
- Release APK: `BliPai-Focus-release-7.1.0-focus.7.apk`

## Test note

- Validation currently covers only `realme Neo 7` and `Lenovo Y700 2023`
- Please open an issue if you see compatibility or behavior problems on other devices
- Focus currently tries to stay on the same upstream major version, and for now only follows upstream major releases before rolling Focus sub-versions on that baseline
