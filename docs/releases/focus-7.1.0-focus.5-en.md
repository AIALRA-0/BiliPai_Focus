# BliPai Focus v7.1.0-focus.5 Release Notes

BliPai Focus `v7.1.0-focus.5` stays on top of upstream `BiliPai v7.1.0`. This release focuses on follow-group management stability when no creators remain visible, and on faster follow assignment workflows for larger follow lists.

## Highlights

- Fixes the empty-state flicker on the Dynamic sidebar, Dynamic horizontal follow row, and Home Follow when no followed creators remain available, replacing it with a stable “no available followed creators” state.
- Adds a search field to follow-group management, placed under the new-group input area, so creators can be filtered quickly by name or UID.
- Aligns the new-group text field and add button to the same height, corner radius, and centered layout.
- Syncs README, README_EN, CHANGELOG, and Focus Changelog to the `focus.5` release line and release-entry links.

## Focus defaults

- Home `Recommend / Popular / Live / Game / Partition` entries hidden by default
- Search hot list disabled by default while keeping suggestions, discovery, results, and search history
- Watch history supports one-tap clear-all
- Related videos below video detail hidden by default, restorable from Focus settings
- Shared local follow-group filtering across Dynamic and Home Follow, with a master toggle

## Version info

- Focus Version: `7.1.0-focus.5`
- Upstream Base: `7.1.0`
- VersionCode: `128`
- Tag: `v7.1.0-focus.5`

## Artifact names

- Debug APK: `BliPai-Focus-debug-7.1.0-focus.5-debug.apk`
- Release APK: `BliPai-Focus-release-7.1.0-focus.5.apk`

## Test note

- Validation currently covers only `realme Neo 7` and `Lenovo Y700 2023`
- Please open an issue if you see compatibility or behavior problems on other devices
- Focus currently tries to stay on the same upstream major version, and for now only follows upstream major releases before rolling Focus sub-versions on that baseline
