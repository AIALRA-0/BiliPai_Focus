# BliPai Focus v7.1.0-focus.11 Release Notes

BliPai Focus `v7.1.0-focus.11` is based on upstream `BiliPai v7.1.0`. This release mainly rolls Home Follow refresh behavior back to the upstream official implementation.

## ✨ Highlights

- Home Follow now uses the upstream official refresh path again, instead of the extra Focus-specific foreground prefetch, background completion, and staged publish logic.
- Follow-group filtering remains available, but it now only applies to the final result set after refresh logic finishes.
- The goal of this rollback is straightforward: reduce the timing drift, delayed fill, and one-by-one reveal side effects introduced by the previous custom loading strategies.
- README / README_EN / CHANGELOG / Focus Changelog have all been updated to the `focus.11` release line.

## 🧩 Focus Defaults

- Home `Recommend / Popular / Live / Game / Partition` entries are hidden by default
- Search hot list is disabled by default while keeping suggestions, discovery, results, and history
- Watch history supports one-tap clear-all
- Related videos below video detail are hidden by default and can be restored in Focus settings
- Dynamic and Home Follow share the same local follow-group filtering rules, with a master switch

## 📦 Version Info

- Focus Version: `7.1.0-focus.11`
- Upstream Base: `7.1.0`
- VersionCode: `134`
- Tag: `v7.1.0-focus.11`

## 📱 Artifact Names

- Debug APK: `BliPai-Focus-debug-7.1.0-focus.11-debug.apk`
- Release APK: `BliPai-Focus-release-7.1.0-focus.11.apk`

## 🧪 Testing

- Verified currently on `realme Neo 7` and `Lenovo Y700 2023`
- If you hit device-specific compatibility or behavior issues elsewhere, please open an issue
- Focus tries to stay on the same upstream major version and, for now, will continue shipping Focus sub-versions on top of upstream major releases
