# BliPai Focus v7.1.0-focus.4 Release Notes

BliPai Focus `v7.1.0-focus.4` stays on top of upstream `BiliPai v7.1.0`. This release mainly tightens the Focus project positioning, docs presentation, and maintenance notes while keeping the current Focus feature set intact.

## Highlights

- Rewrites the Focus introduction to make the core idea explicit: remove as many recommendation-driven temptation surfaces as possible, while preserving intentional search, intentional following, and intentional revisit paths.
- Adds a Focus explanation from the angle of attention psychology and behavioral design, centering the product direction around “fewer traps, more intention”.
- README and README_EN now share the same Focus gallery structure and include a screenshot for one-tap watch-history clearing.
- “App Preview” is now explicitly labeled as “Official app preview”, with a note that most of the following section still follows the upstream official README structure.
- README now includes the main Focus diff files, tested-device scope, and the maintenance rule of keeping the same upstream major version while only following major upstream updates for now.
- Star History now points to the Focus repository itself.

## Focus defaults

- Home `Recommend / Popular / Live / Game / Partition` entries hidden by default
- Search hot list disabled by default while keeping suggestions, discovery, results, and search history
- Watch history supports one-tap clear-all
- Related videos below video detail hidden by default, restorable from Focus settings
- Shared local follow-group filtering across Dynamic and Home Follow, with a master toggle

## Version info

- Focus Version: `7.1.0-focus.4`
- Upstream Base: `7.1.0`
- VersionCode: `127`
- Tag: `v7.1.0-focus.4`

## Artifact names

- Debug APK: `BliPai-Focus-debug-7.1.0-focus.4-debug.apk`
- Release APK: `BliPai-Focus-release-7.1.0-focus.4.apk`

## Test note

- Validation currently covers only `realme Neo 7` and `Lenovo Y700 2023`
- Please open an issue if you see compatibility or behavior problems on other devices
- Focus currently tries to stay on the same upstream major version, and for now only follows upstream major releases before rolling Focus sub-versions on that baseline
