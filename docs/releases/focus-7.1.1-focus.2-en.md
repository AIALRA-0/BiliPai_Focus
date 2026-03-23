# BliPai Focus v7.1.1-focus.2 Release Notes

BliPai Focus `v7.1.1-focus.2` is based on upstream `BiliPai v7.1.1`. This is a targeted Home Follow stability hotfix that addresses the severe empty-state and pagination-overwrite behavior seen when Focus follow-group filtering is enabled.

## ✨ Highlights

- Fixes Home Follow continuation pages overwriting earlier results when Focus follow-group filtering is enabled and incremental refresh is off; items fetched in the current round now accumulate before being projected into the final list.
- Fixes the unstable Home Follow output pattern that could look like “1 item first, then 8, then a different batch, then empty after relaunch”.
- Fixes premature “no available followed creators” empty states during cold start and settings-driven re-projection; that empty state now appears only after the follow feed has actually been resolved successfully at least once.
- Keeps the official `HOME_FOLLOW` pagination interface and overall refresh semantics intact; this release only corrects result accumulation and empty-state timing on the Focus-filtered layer.
- README / README_EN / CHANGELOG / Focus Changelog are all updated to the `7.1.1-focus.2` release line.

## 🧩 Focus Defaults

- Home `Recommend / Popular / Live / Game / Partition` entries are hidden by default
- Search hot list is disabled by default while keeping suggestions, discovery, results, and history
- Watch history supports one-tap clear-all
- Related videos below video detail are hidden by default and can be restored in Focus settings
- Dynamic and Home Follow share the same local follow-group filtering rules, with a master switch

## 📦 Version Info

- Focus Version: `7.1.1-focus.2`
- Upstream Base: `7.1.1`
- VersionCode: `136`
- Tag: `v7.1.1-focus.2`

## 📱 Artifact Names

- Debug APK: `BliPai-Focus-debug-7.1.1-focus.2-debug.apk`
- Release APK: `BliPai-Focus-release-7.1.1-focus.2.apk`

## 🧪 Testing

- Verified currently on `realme Neo 7` and `Lenovo Y700 2023`
- If you hit device-specific compatibility or behavior issues elsewhere, please open an issue
- Focus tries to stay on the same upstream major version and, for now, will continue shipping Focus sub-versions on top of upstream major releases
