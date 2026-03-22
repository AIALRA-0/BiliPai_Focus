# BliPai Focus v7.1.0-focus.6 Release Notes

BliPai Focus `v7.1.0-focus.6` stays on top of upstream `BiliPai v7.1.0`. This release continues polishing the follow-group management input area and fixes delayed post-filter paging plus empty-state load loops in Dynamic and Home Follow.

## Highlights

- Raises the `New group` and `Search followed creators` fields a bit more, and aligns the `Add / Refresh` buttons to the same height, corner radius, and button style.
- Fixes refresh-time paging after Focus filtering on Dynamic and Home Follow, so the first visible result set stabilizes sooner instead of slowly growing from 1 item to 3 items.
- Fixes the case where Dynamic and Home Follow could still auto-load repeatedly after filtering removed every visible followed creator; the empty state now stays stable as “no available followed creators”.
- Syncs README, README_EN, CHANGELOG, and Focus Changelog to the `focus.6` release line and release-entry links.

## Focus defaults

- Home `Recommend / Popular / Live / Game / Partition` entries hidden by default
- Search hot list disabled by default while keeping suggestions, discovery, results, and search history
- Watch history supports one-tap clear-all
- Related videos below video detail hidden by default, restorable from Focus settings
- Shared local follow-group filtering across Dynamic and Home Follow, with a master toggle

## Version info

- Focus Version: `7.1.0-focus.6`
- Upstream Base: `7.1.0`
- VersionCode: `129`
- Tag: `v7.1.0-focus.6`

## Artifact names

- Debug APK: `BliPai-Focus-debug-7.1.0-focus.6-debug.apk`
- Release APK: `BliPai-Focus-release-7.1.0-focus.6.apk`

## Test note

- Validation currently covers only `realme Neo 7` and `Lenovo Y700 2023`
- Please open an issue if you see compatibility or behavior problems on other devices
- Focus currently tries to stay on the same upstream major version, and for now only follows upstream major releases before rolling Focus sub-versions on that baseline
