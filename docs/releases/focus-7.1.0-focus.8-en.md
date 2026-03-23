# BliPai Focus v7.1.0-focus.8 Release Notes

BliPai Focus `v7.1.0-focus.8` stays on top of upstream `BiliPai v7.1.0`. This update improves the first visible fill behavior of Home Follow after filtering and expands the default Dynamic follow-user sync budget from `50` to up to `1000` creators.

## Highlights

- Home Follow now keeps prefetching extra Dynamic pages when the filtered first screen is still underfilled, until it reaches a steadier visible quota or truly runs out of data.
- Dynamic follow-user sync now targets up to `1000` creators by default; if you follow fewer than `1000`, it syncs the actual count instead of stopping at the first page of `50`.
- Older small follow caches are also auto-upgraded by triggering a refill instead of waiting for the cache TTL to expire.
- README, README_EN, CHANGELOG, and Focus Changelog are all updated to the `focus.8` release line and links.

## Focus defaults

- Home `Recommend / Popular / Live / Game / Partition` entries hidden by default
- Search hot list disabled by default while keeping suggestions, discovery, results, and search history
- Watch history supports one-tap clear-all
- Related videos below video detail hidden by default, restorable from Focus settings
- Shared local follow-group filtering across Dynamic and Home Follow, with a master toggle

## Version info

- Focus Version: `7.1.0-focus.8`
- Upstream Base: `7.1.0`
- VersionCode: `131`
- Tag: `v7.1.0-focus.8`

## Artifact names

- Debug APK: `BliPai-Focus-debug-7.1.0-focus.8-debug.apk`
- Release APK: `BliPai-Focus-release-7.1.0-focus.8.apk`

## Test note

- Validation currently covers only `realme Neo 7` and `Lenovo Y700 2023`
- Please open an issue if you see compatibility or behavior problems on other devices
- Focus currently tries to stay on the same upstream major version, and for now only follows upstream major releases before rolling Focus sub-versions on that baseline
