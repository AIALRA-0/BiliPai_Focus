# BliPai Focus v7.1.1-focus.3 Release Notes

BliPai Focus `v7.1.1-focus.3` is based on upstream `BiliPai v7.1.1`. This release pulls the Home Follow Focus-filtered path back toward the official workflow and fixes stale filtered results, awkward refresh projection, and load-more stalls at the bottom.

## ✨ Highlights

- Home Follow now stays on the official `HOME_FOLLOW` single-channel pagination flow and original ordering instead of continuing with the Focus hybrid catch-up / custom sorting experiment.
- Fixes Home Follow continuing to display results projected from the previous filter state after the Focus follow filter switch or group config changes; the page now clears stale cache and performs a real reload.
- Fixes the refresh interaction feeling like results were being inserted upward; projection now follows the official pagination flow again and Focus only filters the final result layer.
- Keeps the bottom `load more` trigger fix so scrolling to the end continues loading naturally after the list size changes, without needing to switch tabs and come back.
- README / README_EN / CHANGELOG / Focus Changelog are all updated to the `7.1.1-focus.3` release line.

## 🧩 Focus Defaults

- Home `Recommend / Popular / Live / Game / Partition` entries are hidden by default
- Search hot list is disabled by default while keeping suggestions, discovery, results, and history
- Watch history supports one-tap clear-all
- Related videos below video detail are hidden by default and can be restored in Focus settings
- Dynamic and Home Follow share the same local follow-group filtering rules, with a master switch

## 📦 Version Info

- Focus Version: `7.1.1-focus.3`
- Upstream Base: `7.1.1`
- VersionCode: `137`
- Tag: `v7.1.1-focus.3`

## 📱 Artifact Names

- Debug APK: `BliPai-Focus-debug-7.1.1-focus.3-debug.apk`
- Release APK: `BliPai-Focus-release-7.1.1-focus.3.apk`

## 🧪 Testing

- Verified currently on `realme Neo 7` and `Lenovo Y700 2023`
- If you hit device-specific compatibility or behavior issues elsewhere, please open an issue
- Focus tries to stay on the same upstream major version and, for now, will continue shipping Focus sub-versions on top of upstream major releases
